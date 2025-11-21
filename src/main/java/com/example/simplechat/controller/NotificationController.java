package com.example.simplechat.controller;

import com.example.simplechat.dto.NotificationDto;
import com.example.simplechat.model.Notification;
import com.example.simplechat.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> getUnreadNotifications(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(notificationService.getNotificationsForUser(userId, false));
    }

    @PutMapping("/mark-as-read")
    public ResponseEntity<Void> markNotificationsAsRead(@RequestBody List<Long> notificationIds, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        notificationService.markNotificationsAsRead(notificationIds, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{notificationId}/accept")
    public ResponseEntity<Void> acceptNotification(@PathVariable("notificationId") Long notificationId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        notificationService.acceptNotification(notificationId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{notificationId}/reject")
    public ResponseEntity<Void> rejectNotification(@PathVariable("notificationId") Long notificationId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        notificationService.rejectNotification(notificationId, userId);
        return ResponseEntity.noContent().build();
    }
}
