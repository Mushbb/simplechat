package com.example.simplechat.controller;

import com.example.simplechat.dto.ProfileUpdateRequestDto;
import com.example.simplechat.dto.UserProfileDto;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}/profile")
    public UserProfileDto getUserProfile(@PathVariable("userId") Long userId) {
        return userService.getUserProfile(userId);
    }

    @PutMapping("/profile")
    public UserProfileDto changeUserProfile(@RequestBody ProfileUpdateRequestDto profileDto, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        return userService.changeUserProfile(profileDto, userId);
    }

    @PostMapping("/profile/image")
    public ResponseEntity<Map<String, String>> uploadProfileImage(
            @RequestParam("profileImage") MultipartFile file,
            HttpSession session
    ) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new RegistrationException("UNAUTHORIZED", "Please login first!");
        }
        String newImageUrl = userService.updateProfileImage(userId, file);

        // 클라이언트가 즉시 이미지를 업데이트할 수 있도록 새 이미지 URL을 반환
        return ResponseEntity.ok(Map.of("profileImageUrl", newImageUrl));
    }
}
