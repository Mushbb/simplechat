package com.example.simplechat.controller;

import com.example.simplechat.dto.ChatMessageListDto;
import com.example.simplechat.dto.ChatMessageListRequestDto;
import com.example.simplechat.dto.ChatMessageRequestDto;
import com.example.simplechat.dto.NickChangeDto;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.service.ChatMessageService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RequiredArgsConstructor
@RestController
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @PostMapping("/room/{roomId}/file")
    public ResponseEntity<Void> uploadChatFile(
            @PathVariable("roomId") Long roomId,
            @RequestParam("file") MultipartFile file,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new RegistrationException("UNAUTHORIZED", "Please login first!");
        }

        chatMessageService.uploadChatFile(roomId, userId, file);

        return ResponseEntity.ok().build();
    }

    @MessageMapping("/chat.sendMessage")
    public void recvMessage(ChatMessageRequestDto msgDto) {
        chatMessageService.addChat_publish(msgDto);
    }

    @MessageMapping("/chat.changeNick")
    public void changeNick(NickChangeDto nickChangeDto) {
        chatMessageService.changeNicknameInRoom(nickChangeDto);
    }

    @DeleteMapping("/api/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable("messageId") Long messageId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new RegistrationException("UNAUTHORIZED", "세션 정보를 찾을 수 없습니다.");
        }
        chatMessageService.deleteMessage(messageId, userId);
        return ResponseEntity.noContent().build();
    }

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

    @MessageMapping("/chat.getMessageList")
    @SendToUser("/topic/queue/reply")
    public ChatMessageListDto getMessageList(ChatMessageListRequestDto msgListDto) {
        return chatMessageService.getMessageList(msgListDto);
    }
}
