package com.SIGMA.USCO.notifications.service;

import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.notifications.entity.Notification;
import com.SIGMA.USCO.notifications.repository.NotificationRepository;
import com.SIGMA.USCO.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyNotifications() {

        User user = getCurrentUser();

        List<Notification> notifications =
                notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(user.getId());

        return notifications.stream()
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
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUnreadCount() {

        User user = getCurrentUser();

        long count = notificationRepository.countByRecipient_IdAndReadFalse(user.getId());

        return Map.of("unreadCount", count);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationDetail(Long notificationId) {

        User user = getCurrentUser();
        Notification notification = notificationRepository.findByIdAndRecipient_Id(notificationId, user.getId())
                        .orElseThrow(() ->
                                new NotFoundException("Notificación no encontrada")
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

        return response;
    }

    public Map<String, Object> markAsRead(Long notificationId) {

        User user = getCurrentUser();

        Notification notification = notificationRepository.findByIdAndRecipient_Id(notificationId, user.getId())
                        .orElseThrow(() ->
                                new NotFoundException("Notificación no encontrada")
                        );

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }

        return Map.of(
                "success", true,
                "message", "Notificación marcada como leída"
        );
    }
}
