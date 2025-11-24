package com.example.simplechat.controller;

import com.example.simplechat.dto.ChatRoomListDto;
import com.example.simplechat.dto.InviteRequestDto;
import com.example.simplechat.dto.RoomCreateDto;
import com.example.simplechat.dto.RoomEnterDto;
import com.example.simplechat.dto.RoomInitDataDto;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.service.ChatRoomService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/room")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @GetMapping("/list")
    public List<ChatRoomListDto> getRoomList(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        return chatRoomService.getRoomList(userId);
    }

    @PostMapping("/create")
    public Long createRoom(@RequestBody RoomCreateDto roomcreateDto, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new RegistrationException("UNAUTHORIZED", "Please login first!");
        }
        return chatRoomService.createRoom(roomcreateDto, userId);
    }

    @PostMapping("/{roomId}/users")
    public Long enterRoom(@PathVariable("roomId") Long roomId, @RequestBody RoomEnterDto enterDto, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new RegistrationException("UNAUTHORIZED", "Please login first!");
        }

        return chatRoomService.enterRoom(roomId, userId, enterDto.password());
    }

    @DeleteMapping("/{roomId}/users")
    public void exitRoom(@PathVariable("roomId") Long roomId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new RegistrationException("UNAUTHORIZED", "세션 정보를 찾을 수 없습니다.");
        }
        chatRoomService.exitRoom(roomId, userId);
    }

    @DeleteMapping("/{roomId}")
    public void deleteRoom(@PathVariable("roomId") Long roomId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new RegistrationException("UNAUTHORIZED", "세션 정보를 찾을 수 없습니다.");
        }
        chatRoomService.deleteRoom(roomId, userId);
    }

    @GetMapping("/my-rooms")
    public ResponseEntity<List<ChatRoomListDto>> getMyRooms(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<ChatRoomListDto> myRooms = chatRoomService.findRoomsByUserId(userId);
        return ResponseEntity.ok(myRooms);
    }

    @GetMapping("/{roomId}/init")
    public RoomInitDataDto initRoom(@PathVariable("roomId") Long roomId, @RequestParam(name="lines", defaultValue="20") int lines,HttpSession session) {
        Long userId = (Long)session.getAttribute("userId");

        return chatRoomService.initRoom(roomId, userId, lines);
    }

    @DeleteMapping("/{roomId}/users/{userId}")
    public void kickUserFromRoom(@PathVariable("roomId") Long roomId, @PathVariable("userId") Long userIdToKick, HttpSession session) {
        Long kickerId = (Long) session.getAttribute("userId");
        if (kickerId == null) {
            throw new RegistrationException("UNAUTHORIZED", "세션 정보를 찾을 수 없습니다.");
        }
        chatRoomService.kickUser(roomId, kickerId, userIdToKick);
    }

    @PostMapping("/{roomId}/invite")
    public ResponseEntity<Void> inviteUserToRoom(
            @PathVariable("roomId") Long roomId,
            @RequestBody InviteRequestDto inviteDto,
            HttpSession session) {

        Long inviterId = (Long) session.getAttribute("userId");
        if (inviterId == null) {
            throw new RegistrationException("UNAUTHORIZED", "로그인이 필요합니다.");
        }

        chatRoomService.inviteUserToRoom(roomId, inviterId, inviteDto.userId());

        return ResponseEntity.ok().build();
    }
}
