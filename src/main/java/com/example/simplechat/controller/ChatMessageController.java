package com.example.simplechat.controller;

import com.example.simplechat.dto.ChatMessageListDto;
import com.example.simplechat.dto.ChatMessageListRequestDto;
import com.example.simplechat.dto.ChatMessageRequestDto;
import com.example.simplechat.dto.NickChangeDto;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.service.ChatMessageService;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 채팅 메시지와 관련된 WebSocket 메시지와 RESTful API 요청을 모두 처리하는 컨트롤러입니다.
 */
@RequiredArgsConstructor
@RestController
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    /**
     * POST /room/{roomId}/file 특정 채팅방에 대한 파일 업로드를 처리합니다.
     *
     * @param roomId 파일을 업로드할 방의 ID입니다.
     * @param file 업로드되는 멀티파트 파일입니다.
     * @param session 사용자 인증을 위한 HTTP 세션입니다.
     * @return 성공 시 OK 상태의 {@link ResponseEntity}를 반환합니다.
     * @throws RegistrationException 사용자가 로그인하지 않은 경우 발생합니다.
     */
    @PostMapping("/room/{roomId}/file")
    public ResponseEntity<Void> uploadChatFile(
        @PathVariable("roomId") Long roomId,
        @RequestParam("file") MultipartFile file,
        HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new RegistrationException("UNAUTHORIZED", "먼저 로그인해주세요!");
        }

        chatMessageService.uploadChatFile(roomId, userId, file);

        return ResponseEntity.ok().build();
    }

    /**
     * 새 채팅 메시지를 수신하고 처리하기 위한 WebSocket 엔드포인트입니다. Destination: /chat.sendMessage
     *
     * @param msgDto 새 메시지 세부 정보가 포함된 DTO입니다.
     */
    @MessageMapping("/chat.sendMessage")
    public void recvMessage(ChatMessageRequestDto msgDto) {
        chatMessageService.addChat_publish(msgDto);
    }

    /**
     * 방에서 닉네임을 변경하려는 사용자 요청을 처리하기 위한 WebSocket 엔드포인트입니다. Destination: /chat.changeNick
     *
     * @param nickChangeDto 사용자, 방, 새 닉네임이 포함된 DTO입니다.
     */
    @MessageMapping("/chat.changeNick")
    public void changeNick(NickChangeDto nickChangeDto) {
        chatMessageService.changeNicknameInRoom(nickChangeDto);
    }

    /**
     * DELETE /api/messages/{messageId} 특정 채팅 메시지를 삭제합니다.
     *
     * @param messageId 삭제할 메시지의 ID입니다.
     * @param session 사용자 인증을 위한 HTTP 세션입니다.
     * @return 내용 없음을 나타내는 상태의 {@link ResponseEntity}를 반환합니다.
     * @throws RegistrationException 사용자가 로그인하지 않은 경우 발생합니다.
     */
    @DeleteMapping("/api/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable("messageId") Long messageId,
        HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new RegistrationException("UNAUTHORIZED", "세션 정보를 찾을 수 없습니다.");
        }
        chatMessageService.deleteMessage(messageId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/messages/{messageId} 특정 채팅 메시지의 내용을 수정합니다.
     *
     * @param messageId 수정할 메시지의 ID입니다.
     * @param payload "content" 키 아래에 새 콘텐츠를 포함하는 맵입니다.
     * @param session 사용자 인증을 위한 HTTP 세션입니다.
     * @return 내용 없음을 나타내는 상태의 {@link ResponseEntity}를 반환합니다.
     * @throws RegistrationException 사용자가 로그인하지 않은 경우 발생합니다.
     */
    @PutMapping("/api/messages/{messageId}")
    public ResponseEntity<Void> editMessage(
        @PathVariable("messageId") Long messageId,
        @RequestBody Map<String, String> payload,
        HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new RegistrationException("UNAUTHORIZED", "세션 정보를 찾을 수 없습니다.");
        }
        String newContent = payload.get("content");
        chatMessageService.editMessage(messageId, userId, newContent);
        return ResponseEntity.noContent().build();
    }

    /**
     * 이전 메시지 목록을 요청하는 WebSocket 엔드포인트입니다. 결과는 요청한 사용자의 개인 큐로 직접 전송됩니다. Destination:
     * /chat.getMessageList Reply-To: /topic/queue/reply (사용자별)
     *
     * @param msgListDto 메시지 목록에 대한 페이지네이션 매개변수가 포함된 DTO입니다.
     * @return 요청된 메시지가 포함된 {@link ChatMessageListDto}를 반환합니다.
     */
    @MessageMapping("/chat.getMessageList")
    @SendToUser("/topic/queue/reply")
    public ChatMessageListDto getMessageList(ChatMessageListRequestDto msgListDto) {
        return chatMessageService.getMessageList(msgListDto);
    }
}
