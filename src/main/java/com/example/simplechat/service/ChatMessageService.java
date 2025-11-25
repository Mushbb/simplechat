package com.example.simplechat.service;

import com.example.simplechat.dto.ChatMessageDto;
import com.example.simplechat.dto.ChatMessageListDto;
import com.example.simplechat.dto.ChatMessageListRequestDto;
import com.example.simplechat.dto.ChatMessageRequestDto;
import com.example.simplechat.dto.NickChangeDto;
import com.example.simplechat.dto.NotificationDto;
import com.example.simplechat.event.ChangeNicknameEvent;
import com.example.simplechat.event.ChatMessageAddedToRoomEvent;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.model.ChatMessage;
import com.example.simplechat.model.ChatRoom;
import com.example.simplechat.model.Notification;
import com.example.simplechat.model.User;
import com.example.simplechat.repository.FileRepository;
import com.example.simplechat.repository.MessageRepository;
import com.example.simplechat.repository.RoomUserRepository;
import com.example.simplechat.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 채팅 메시지와 관련된 비즈니스 로직을 처리하는 서비스 클래스입니다. 메시지 생성, 수정, 삭제, 멘션 처리 및 파일 업로드를 포함합니다.
 */
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageService.class);
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

    /**
     * 새 채팅 메시지를 저장하고, 실시간 배포를 위한 이벤트를 발행하며, 멘션된 모든 사용자에게 알림을 보냅니다.
     *
     * @param msgDto 보낼 메시지의 세부 정보가 포함된 DTO
     */
    public void addChat_publish(ChatMessageRequestDto msgDto) {
        String authorName = roomUserRepository.getNickname(msgDto.authorId(), msgDto.roomId());
        ChatMessage savedMessage = msgRepository.save(new ChatMessage(msgDto, authorName));
        eventPublisher.publishEvent(
            new ChatMessageAddedToRoomEvent(this, savedMessage, msgDto.roomId()));

        // 멘션된 사용자에게 알림 보내기
        if (msgDto.mentionedUserIds() != null && !msgDto.mentionedUserIds().isEmpty()) {
            sendMentionNotifications(msgDto, savedMessage);
        }
    }

    private void sendMentionNotifications(ChatMessageRequestDto msgDto, ChatMessage savedMessage) {
        User author = userRepository.findById(msgDto.authorId())
            .orElseThrow(() -> new RegistrationException("NOT_FOUND", "작성자를 찾을 수 없습니다."));
        ChatRoom room = chatRoomService.getRoomById(msgDto.roomId());

        for (Long mentionedUserId : msgDto.mentionedUserIds()) {
            userRepository.findById(mentionedUserId).ifPresent(mentionedUser -> {
                if (!mentionedUser.getId().equals(author.getId())) { // 자신을 멘션한 경우 제외
                    String content =
                        author.getNickname() + "님이 '" + room.getName() + "' 방에서 당신을 멘션했습니다.";
                    Notification notification = new Notification(
                        mentionedUserId,
                        Notification.NotificationType.MENTION,
                        content,
                        room.getId(),
                        null // 현재는 메타데이터 필요 없음
                    );
                    notificationService.save(notification);

                    // 실시간 알림 전송
                    messagingTemplate.convertAndSendToUser(
                        mentionedUser.getUsername(),
                        "/queue/notifications",
                        NotificationDto.from(notification)
                    );
                }
            });
        }
    }

    /**
     * 메시지를 삭제합니다. 메시지 작성자 또는 방 관리자만 이 작업을 수행할 수 있습니다. 클라이언트를 업데이트하기 위해 삭제 이벤트를
     * 발행합니다.
     *
     * @param messageId 삭제할 메시지의 ID
     * @param userId 삭제를 요청하는 사용자의 ID
     * @throws RegistrationException 메시지를 찾을 수 없거나 사용자에게 권한이 없는 경우
     */
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        ChatMessage message = msgRepository.findById(messageId)
            .orElseThrow(() -> new RegistrationException("NOT_FOUND", "메시지를 찾을 수 없습니다."));

        Long roomId = message.getRoom_id();
        String userRole = roomUserRepository.getRole(userId, roomId);

        boolean canDelete = "ADMIN".equals(userRole) || message.getAuthor_id().equals(userId);

        if (!canDelete) {
            throw new RegistrationException("FORBIDDEN", "이 메시지를 삭제할 권한이 없습니다.");
        }

        msgRepository.deleteById(messageId);

        // 삭제 이벤트 발행
        ChatMessage deleteEventMessage = new ChatMessage(messageId, roomId,
            ChatMessage.MsgType.DELETE);
        eventPublisher.publishEvent(
            new ChatMessageAddedToRoomEvent(this, deleteEventMessage, roomId));

        logger.info("메시지 {}가 사용자 {}에 의해 삭제되었습니다.", messageId, userId);
    }

    /**
     * 기존 메시지의 내용을 수정합니다. 메시지 작성자 또는 방 관리자만 이 작업을 수행할 수 있습니다. 클라이언트에 업데이트 이벤트를
     * 발행합니다.
     *
     * @param messageId 수정할 메시지의 ID
     * @param userId 수정을 요청하는 사용자의 ID
     * @param newContent 메시지의 새 내용
     * @throws RegistrationException 메시지를 찾을 수 없거나 사용자에게 권한이 없는 경우
     */
    @Transactional
    public void editMessage(Long messageId, Long userId, String newContent) {
        ChatMessage message = msgRepository.findById(messageId)
            .orElseThrow(() -> new RegistrationException("NOT_FOUND", "메시지를 찾을 수 없습니다."));

        String userRole = roomUserRepository.getRole(userId, message.getRoom_id());
        boolean canEdit = "ADMIN".equals(userRole) || message.getAuthor_id().equals(userId);

        if (!canEdit) {
            throw new RegistrationException("FORBIDDEN", "이 메시지를 수정할 권한이 없습니다.");
        }

        // 내용이 null이거나 비어 있거나 변경되지 않은 경우 업데이트하지 않음
        if (newContent == null || newContent.isBlank() || newContent.equals(message.getContent())) {
            return;
        }

        message.setContent(newContent);
        message.setMsg_type(ChatMessage.MsgType.UPDATE);
        msgRepository.save(message);

        // 업데이트 이벤트 발행
        eventPublisher.publishEvent(
            new ChatMessageAddedToRoomEvent(this, message, message.getRoom_id()));

        logger.info("메시지 {}가 사용자 {}에 의해 수정되었습니다.", messageId, userId);
    }

    /**
     * 페이지네이션을 사용하여 지정된 방의 채팅 메시지 목록을 검색합니다.
     *
     * @param msgListDto 페이지네이션 매개변수(roomId, beginId, rowCount)를 포함하는 DTO
     * @return 메시지 목록을 포함하는 {@link ChatMessageListDto}
     */
    public ChatMessageListDto getMessageList(ChatMessageListRequestDto msgListDto) {
        List<ChatMessage> messages = msgRepository.findTopNByRoomIdOrderById(
            msgListDto.roomId(),
            msgListDto.beginId(),
            msgListDto.rowCount(),
            "DESC");

        List<ChatMessageDto> messageDtos = mapMessagesToDto(messages);

        // 발견된 URL에 대한 링크 미리보기를 비동기적으로 요청
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
                    .map(url -> url != null && !url.isBlank() ? profileStaticUrlPrefix + "/" + url
                        : profileStaticUrlPrefix + "/default.png")
                    .orElse(profileStaticUrlPrefix + "/default.png");

                return new ChatMessageDto(msg, profileImageUrl);
            })
            .collect(Collectors.toList());
    }

    /**
     * 채팅방에 파일 업로드를 처리합니다. 파일을 저장하고, 해당하는 채팅 메시지를 생성하며, 이벤트를 발행합니다.
     *
     * @param roomId 파일이 업로드된 방의 ID
     * @param userId 파일을 업로드하는 사용자의 ID
     * @param file 업로드된 파일
     */
    public void uploadChatFile(Long roomId, Long userId, MultipartFile file) {
        String storedFilename = chatFileRepository.save(file);
        String originalFilename = file.getOriginalFilename();
        String fileInfoContent = originalFilename + ":" + storedFilename;

        String authorName = roomUserRepository.getNickname(userId, roomId);
        ChatMessage fileMessage = new ChatMessage(roomId, userId, authorName, fileInfoContent,
            ChatMessage.MsgType.FILE);

        ChatMessage savedMessage = msgRepository.save(fileMessage);
        eventPublisher.publishEvent(
            new ChatMessageAddedToRoomEvent(this, savedMessage, roomId));
    }

    /**
     * 특정 채팅방 내에서 사용자의 닉네임을 변경하고, 방의 다른 사용자에게 알리기 위해 이벤트를 발행합니다.
     *
     * @param nickChangeDto userId, roomId 및 새 닉네임을 포함하는 DTO
     */
    @Transactional
    public void changeNicknameInRoom(NickChangeDto nickChangeDto) {
        Long userId = nickChangeDto.userId();
        Long roomId = nickChangeDto.roomId();
        String newNickname = nickChangeDto.newNickname();

        roomUserRepository.updateNickname(userId, roomId, newNickname);

        eventPublisher.publishEvent(new ChangeNicknameEvent(this, userId, roomId, newNickname));
        logger.info("사용자 {}의 방 {} 내 닉네임이 {} (으)로 변경되었습니다.", userId, roomId, newNickname);
    }
}

