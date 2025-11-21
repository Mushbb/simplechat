package com.example.simplechat.service;

import com.example.simplechat.dto.*;
import com.example.simplechat.event.ChangeNicknameEvent;
import com.example.simplechat.event.ChatMessageAddedToRoomEvent;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.model.ChatMessage;
import com.example.simplechat.model.ChatRoom;
import com.example.simplechat.model.Notification;
import com.example.simplechat.model.User;
import com.example.simplechat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final MessageRepository msgRepository;
    private final RoomUserRepository roomUserRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final LinkPreviewService linkPreviewService;
    private final NotificationService notificationService;
    private final ChatRoomService chatRoomService;

    @Qualifier("chatFileRepository")
    private final FileRepository chatFileRepository;

    @Value("${file.profile-static-url-prefix}")
    private String profileStaticUrlPrefix;

    public void addChat_publish(ChatMessageRequestDto msgDto) {
        String AuthorName = roomUserRepository.getNickname(msgDto.authorId(), msgDto.roomId());
        ChatMessage savedMessage = msgRepository.save(new ChatMessage(msgDto, AuthorName));
        eventPublisher.publishEvent(new ChatMessageAddedToRoomEvent(this, savedMessage, msgDto.roomId()));

        // 멘션된 사용자들에게 알림 전송
        if (msgDto.mentionedUserIds() != null && !msgDto.mentionedUserIds().isEmpty()) {
            User author = userRepository.findById(msgDto.authorId())
                    .orElseThrow(() -> new RegistrationException("NOT_FOUND", "메시지 작성자를 찾을 수 없습니다."));
            ChatRoom room = chatRoomService.getRoomById(msgDto.roomId());

            for (Long mentionedUserId : msgDto.mentionedUserIds()) {
                User mentionedUser = userRepository.findById(mentionedUserId)
                        .orElse(null); // 멘션된 사용자가 없으면 스킵

                if (mentionedUser != null && !mentionedUser.getId().equals(author.getId())) { // 자기 자신 멘션 제외
                    String content = author.getNickname() + "님이 '" + room.getName() + "' 방에서 회원님을 멘션했습니다.";
                    Notification notification = new Notification(
                            mentionedUserId,
                            Notification.NotificationType.MENTION,
                            content,
                            room.getId(), // relatedEntityId를 roomId로 사용
                            null // metadata는 필요시 추가
                    );
                    notificationService.save(notification);

                    // 실시간 알림 전송
                    messagingTemplate.convertAndSendToUser(
                            mentionedUser.getUsername(),
                            "/queue/notifications",
                            NotificationDto.from(notification)
                    );
                }
            }
        }
    }

    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        // 1. 메시지 정보 조회
        ChatMessage message = msgRepository.findById(messageId)
                .orElseThrow(() -> new RegistrationException("NOT_FOUND", "메시지를 찾을 수 없습니다."));

        Long roomId = message.getRoom_id();

        // 2. 권한 검증
        String userRole = roomUserRepository.getRole(userId, roomId);
        boolean isAdmin = "ADMIN".equals(userRole);
        boolean isAuthor = message.getAuthor_id().equals(userId);

        if (!isAdmin && !isAuthor) {
            throw new RegistrationException("FORBIDDEN", "메시지를 삭제할 권한이 없습니다.");
        }

        // 3. DB에서 메시지 삭제
        msgRepository.deleteById(messageId);

        // 4. 삭제 이벤트 발행
        ChatMessage deleteEventMessage = new ChatMessage(messageId, roomId, ChatMessage.MsgType.DELETE);
        eventPublisher.publishEvent(new ChatMessageAddedToRoomEvent(this, deleteEventMessage, roomId));

        System.out.println("Message " + messageId + " has been deleted by user " + userId);
    }

    @Transactional
    public void editMessage(Long messageId, Long userId, String newContent) {
        // 1. 메시지 정보 조회
        ChatMessage message = msgRepository.findById(messageId)
                .orElseThrow(() -> new RegistrationException("NOT_FOUND", "메시지를 찾을 수 없습니다."));

        Long roomId = message.getRoom_id();

        // 2. 권한 검증
        String userRole = roomUserRepository.getRole(userId, roomId);
        boolean isAdmin = "ADMIN".equals(userRole);
        boolean isAuthor = message.getAuthor_id().equals(userId);

        if (!isAdmin && !isAuthor) {
            throw new RegistrationException("FORBIDDEN", "메시지를 수정할 권한이 없습니다.");
        }

        // 3. 내용이 비어있거나, 기존 내용과 같으면 수정하지 않음
        if (newContent == null || newContent.isBlank() || newContent.equals(message.getContent())) {
            return;
        }

        // 4. DB 업데이트
        message.setContent(newContent);
        message.setMsg_type(ChatMessage.MsgType.UPDATE); // 이벤트 발행을 위해 타입 변경
        msgRepository.save(message);

        // 5. 수정 이벤트 발행
        eventPublisher.publishEvent(new ChatMessageAddedToRoomEvent(this, message, roomId));

        System.out.println("Message " + messageId + " has been edited by user " + userId);
    }

    public ChatMessageListDto getMessageList(ChatMessageListRequestDto msgListDto) {
        List<ChatMessage> messages = msgRepository.findTopNByRoomIdOrderById(
                msgListDto.roomId(),
                msgListDto.beginId(),
                msgListDto.rowCount(),
                "DESC");

        List<ChatMessageDto> messageDtos = mapMessagesToDto(messages);

        // 각 메시지에 대해 비동기적으로 미리보기 생성 요청
        messageDtos.forEach(dto -> {
            String url = linkPreviewService.findFirstUrl(dto.content());
            if (url != null) {
                linkPreviewService.generateAndSendPreview(dto.messageId(), msgListDto.roomId(), url);
            }
        });

        return new ChatMessageListDto(msgListDto.roomId(), messageDtos.reversed());
    }

    private List<ChatMessageDto> mapMessagesToDto(List<ChatMessage> messages) {
        return messages.stream()
                .map(msg -> {
                    String profileImageUrl = userRepository.findProfileById(msg.getAuthor_id())
                            .map(profileData -> (String) profileData.get("profile_image_url"))
                            .map(url -> url != null && !url.isBlank() ? profileStaticUrlPrefix + "/" + url : profileStaticUrlPrefix + "/default.png")
                            .orElse(profileStaticUrlPrefix + "/default.png");

                    return new ChatMessageDto(msg, profileImageUrl);
                })
                .collect(Collectors.toList());
    }
	
    public void uploadChatFile(Long roomId, Long userId, MultipartFile file) {
        // 1. 파일 저장
        String storedFilename = chatFileRepository.save(file);
        String originalFilename = file.getOriginalFilename();

        // 2. 파일 정보를 담은 메시지 생성 (원본명:저장명)
        //    나중에 클라이언트에서 a 태그로 만들 때 파싱해서 사용
        String fileInfoContent = originalFilename + ":" + storedFilename;

        // 3. 채팅 메시지 객체 생성
        String authorName = roomUserRepository.getNickname(userId, roomId);
        ChatMessage fileMessage = new ChatMessage(roomId, userId, authorName, fileInfoContent, ChatMessage.MsgType.FILE);

        // 4. 메시지 DB 저장 및 이벤트 발행
        ChatMessage savedMessage = msgRepository.save(fileMessage);
        eventPublisher.publishEvent(new ChatMessageAddedToRoomEvent(this, savedMessage, roomId));
    }

    @Transactional
    public void changeNicknameInRoom(NickChangeDto nickChangeDto) {
        // (필요하다면) 닉네임 유효성 검사 (길이, 중복 등) 로직 추가

        Long userId = nickChangeDto.userId();
        Long roomId = nickChangeDto.roomId();
        String newNickname = nickChangeDto.newNickname();

        // 1. DB의 chat_room_users 테이블에 있는 닉네임을 업데이트
        roomUserRepository.updateNickname(userId, roomId, newNickname);

        // 2. 닉네임 변경 이벤트를 발행하여 다른 사용자들에게 알림
        eventPublisher.publishEvent(new ChangeNicknameEvent(this, userId, roomId, newNickname));
        System.out.println("User " + userId + "'s nickname in room " + roomId + " changed to " + newNickname);
    }
}
