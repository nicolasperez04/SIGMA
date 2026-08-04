package com.SIGMA.USCO.notifications.service;

import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.Users.service.AuthService;
import com.SIGMA.USCO.notifications.entity.Notification;
import com.SIGMA.USCO.notifications.repository.NotificationRepository;
import com.SIGMA.USCO.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        return SecurityUtils.getCurrentUser();
    }

    public ResponseEntity<?> getMyNotifications() {

        User user = getCurrentUser();

        List<Notification> notifications =
                notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(user.getId());

        List<Map<String, Object>> response = notifications.stream()
                .map(n -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", n.getId());
                    map.put("type", n.getType());
                    map.put("subject", n.getSubject());
                    map.put("message", n.getMessage());
                    map.put("createdAt", n.getCreatedAt());
                    map.put("read", n.isRead());
                    map.put(
                            "studentModalityId",
                            n.getStudentModality() != null
                                    ? n.getStudentModality().getId()
                                    : null
                    );
                    map.put("invitationId", n.getInvitationId());
                    return map;
                })
                .toList();

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> getUnreadCount() {

        User user = getCurrentUser();

        long count = notificationRepository.countByRecipient_IdAndReadFalse(user.getId());

        return ResponseEntity.ok(
                Map.of("unreadCount", count)
        );
    }

    public ResponseEntity<?> getNotificationDetail(Long notificationId) {

        User user = getCurrentUser();
        Notification notification = notificationRepository.findByIdAndRecipient_Id(notificationId, user.getId())
                        .orElseThrow(() ->
                                new RuntimeException("Notificación no encontrada")
                        );

        Map<String, Object> response = new HashMap<>();
        response.put("id", notification.getId());
        response.put("type", notification.getType());
        response.put("subject", notification.getSubject());
        response.put("message", notification.getMessage());
        response.put("createdAt", notification.getCreatedAt());
        response.put("read", notification.isRead());
        response.put("studentModalityId",
                notification.getStudentModality() != null
                        ? notification.getStudentModality().getId()
                        : null
        );
        response.put("invitationId",
                notification.getInvitationId() != null
                        ? notification.getInvitationId()
                        : null
        );

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> markAsRead(Long notificationId) {

        User user = getCurrentUser();

        Notification notification = notificationRepository.findByIdAndRecipient_Id(notificationId, user.getId())
                        .orElseThrow(() ->
                                new RuntimeException("Notificación no encontrada")
                        );

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Notificación marcada como leída"
                )
        );
    }
}
