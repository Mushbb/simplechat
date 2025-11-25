package com.example.simplechat.listener;

import com.example.simplechat.dto.ChatMessageDto;
import com.example.simplechat.dto.UserEventDto;
import com.example.simplechat.dto.UserEventDto.EventType;
import com.example.simplechat.event.ChangeNicknameEvent;
import com.example.simplechat.event.ChatMessageAddedToRoomEvent;
import com.example.simplechat.event.UserEnteredRoomEvent;
import com.example.simplechat.event.UserExitedRoomEvent;
import com.example.simplechat.model.User;
import com.example.simplechat.repository.UserRepository;
import com.example.simplechat.service.LinkPreviewService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 채팅 활동(예: 새 메시지, 사용자 입장/퇴장)과 관련된 애플리케이션 이벤트를 수신하고 해당하는 메시지를 WebSocket 클라이언트에
 * 브로드캐스트합니다. 모든 리스너는 비동기적으로 작동합니다.
 */
@RequiredArgsConstructor
@Component
public class ChatMessageActivityListener {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageActivityListener.class);

    @Value("${file.profile-static-url-prefix}")
    private String profileStaticUrlPrefix;

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final LinkPreviewService linkPreviewService;

    /**
     * 방에 새 메시지가 추가될 때의 이벤트를 처리합니다. 메시지를 방의 공용 토픽에 브로드캐스트하고, URL이 있는 경우 링크 미리보기 생성을
     * 트리거합니다.
     *
     * @param event 메시지와 방 세부 정보가 포함된 {@link ChatMessageAddedToRoomEvent}
     */
    @Async
    @EventListener
    public void handleChatMessageAddedToRoom(ChatMessageAddedToRoomEvent event) {
        Long roomId = event.getroomId();
        Long authorId = event.getChatMessage().getAuthor_id();

        String profileImageUrl = userRepository.findProfileById(authorId)
            .map(profileData -> (String) profileData.get("profile_image_url"))
            .map(url -> url != null && !url.isBlank() ? profileStaticUrlPrefix + "/" + url
                : profileStaticUrlPrefix + "/default.png")
            .orElse(profileStaticUrlPrefix + "/default.png");

        ChatMessageDto msgDto = new ChatMessageDto(event.getChatMessage(), profileImageUrl);

        try {
            messagingTemplate.convertAndSend("/topic/" + roomId + "/public", msgDto);
            logger.info("WebSocket 메시지가 /topic/{}/public (messageId: {})으로 전송되었습니다.", roomId,
                msgDto.messageId());

            // URL이 발견되면 비동기적으로 링크 미리보기를 생성하고 전송합니다.
            String url = linkPreviewService.findFirstUrl(msgDto.content());
            if (url != null) {
                linkPreviewService.generateAndSendPreview(msgDto.messageId(), roomId, url);
            }
        } catch (MessagingException e) {
            logger.error("새 채팅 메시지에 대한 WebSocket 메시지 전송 중 오류 발생.", e);
        }
    }

    /**
     * 사용자가 방에 입장할 때의 이벤트를 처리합니다. 사용자 이벤트 DTO를 방의 사용자 토픽에 브로드캐스트하여 클라이언트에게 새 사용자를
     * 알립니다.
     *
     * @param event 사용자와 방 세부 정보가 포함된 {@link UserEnteredRoomEvent}
     */
    @Async
    @EventListener
    public void handleUserEnteredRoom(UserEnteredRoomEvent event) {
        User user = event.getUser();
        Long roomId = event.getRoomId();

        String imageUrl = user.getProfile_image_url();
        String fullProfileImageUrl = (imageUrl == null || imageUrl.isBlank())
            ? profileStaticUrlPrefix + "/default.png"
            : profileStaticUrlPrefix + "/" + imageUrl;

        UserEventDto userDto = new UserEventDto(
            EventType.ENTER,
            user.getId(),
            user.getNickname(),
            event.getUserType(),
            fullProfileImageUrl
        );

        try {
            messagingTemplate.convertAndSend("/topic/" + roomId + "/users", userDto);
            logger.info("WebSocket 사용자 ENTER 이벤트가 /topic/{}/users (사용자: {})로 전송되었습니다.", roomId,
                user.getUsername());
        } catch (MessagingException e) {
            logger.error("사용자 입장에 대한 WebSocket 메시지 전송 중 오류 발생.", e);
        }
    }

    /**
     * 사용자가 방에서 나가거나 제거될 때의 이벤트를 처리합니다. 사용자 이벤트 DTO를 방의 사용자 토픽에 브로드캐스트하여 클라이언트에게
     * 알립니다.
     *
     * @param event 사용자와 방 세부 정보가 포함된 {@link UserExitedRoomEvent}
     */
    @Async
    @EventListener
    public void handleUserExitedRoom(UserExitedRoomEvent event) {
        Long userId = event.getUserId();
        Long roomId = event.getRoomId();
        EventType eventType = event.getEventType();
        UserEventDto userDto = new UserEventDto(eventType, userId, null, null, null);

        try {
            messagingTemplate.convertAndSend("/topic/" + roomId + "/users", userDto);
            logger.info("WebSocket 사용자 이벤트 {}가 /topic/{}/users (사용자: {})로 전송되었습니다.",
                eventType, roomId, userId);
        } catch (MessagingException e) {
            logger.error("사용자 퇴장/제거에 대한 WebSocket 메시지 전송 중 오류 발생.", e);
        }
    }

    /**
     * 사용자가 방에서 닉네임을 변경할 때의 이벤트를 처리합니다. 사용자 이벤트 DTO를 방의 사용자 토픽에 브로드캐스트하여 클라이언트에게 변경
     * 사항을 알립니다.
     *
     * @param event 닉네임 변경 세부 정보가 포함된 {@link ChangeNicknameEvent}
     */
    @Async
    @EventListener
    public void handleChangeNicknameEvent(ChangeNicknameEvent event) {
        Long userId = event.getUserId();
        Long roomId = event.getRoomId();
        String newNickname = event.getNewNickname();

        UserEventDto userDto = new UserEventDto(EventType.NICK_CHANGE, userId, newNickname, null,
            null);

        try {
            messagingTemplate.convertAndSend("/topic/" + roomId + "/users", userDto);
            logger.info(
                "WebSocket 사용자 NICK_CHANGE 이벤트가 /topic/{}/users (사용자: {}, 새 닉네임: {})로 전송되었습니다.",
                roomId, userId, newNickname);
        } catch (MessagingException e) {
            logger.error("닉네임 변경에 대한 WebSocket 메시지 전송 중 오류 발생.", e);
        }
    }
}