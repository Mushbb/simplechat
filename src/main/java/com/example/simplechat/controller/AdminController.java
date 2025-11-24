package com.example.simplechat.controller;

import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.service.AdminService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/command")
    public ResponseEntity<Map<String, String>> executeAdminCommand(@RequestBody Map<String, String> payload, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        // 관리자(userId=0)가 아니면 접근 거부
        if (userId == null || userId != 0) { // Assume userId 0 is admin for now
            throw new RegistrationException("FORBIDDEN", "관리자만 사용할 수 있는 기능입니다.");
        }

        String command = payload.get("command");
        if (command == null || command.isBlank()) {
            throw new RegistrationException("BAD_REQUEST", "실행할 명령어를 입력해주세요.");
        }

        String result = adminService.executeAdminCommand(command);

        return ResponseEntity.ok(Map.of("message", result));
    }
}
