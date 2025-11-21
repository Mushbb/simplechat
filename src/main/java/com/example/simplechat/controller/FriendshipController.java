package com.example.simplechat.controller;

import com.example.simplechat.dto.FriendRequestDto;
import com.example.simplechat.dto.FriendResponseDto;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.service.FriendshipService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/friends")
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/requests")
    public ResponseEntity<Void> sendFriendRequest(@RequestBody FriendRequestDto requestDto, HttpSession session) {
        Long senderId = (Long) session.getAttribute("userId");
        if (senderId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        friendshipService.sendFriendRequest(senderId, requestDto.receiverId());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<FriendResponseDto>> getFriends(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(friendshipService.getFriends(userId));
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> removeFriend(@PathVariable("friendId") Long friendId, HttpSession session) {
        Long removerId = (Long) session.getAttribute("userId");
        if (removerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        friendshipService.removeFriend(removerId, friendId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status/{otherUserId}")
    public ResponseEntity<Map<String, String>> getFriendshipStatus(@PathVariable("otherUserId") Long otherUserId, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("userId");
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(friendshipService.getFriendshipStatus(currentUserId, otherUserId));
    }
}
