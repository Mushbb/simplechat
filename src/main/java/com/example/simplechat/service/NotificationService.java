package com.example.simplechat.service;

import com.example.simplechat.model.Notification;
import com.example.simplechat.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Notification save(Notification notification) {
        return notificationRepository.save(notification);
    }

    public Optional<Notification> findById(long notificationId) {
        return notificationRepository.findById(notificationId);
    }

    public List<Notification> getNotificationsForUser(long receiverId, Boolean isRead) {
        return notificationRepository.findByReceiverId(receiverId, isRead);
    }

    public void markNotificationsAsRead(List<Long> notificationIds, Long receiverId) {
        notificationRepository.updateIsReadStatus(notificationIds, receiverId, true);
    }

    public void deleteNotification(long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
}
