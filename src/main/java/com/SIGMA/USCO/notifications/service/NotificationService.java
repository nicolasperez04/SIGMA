package com.SIGMA.USCO.notifications.service;

import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.common.web.OperationResultResponse;
import com.SIGMA.USCO.notifications.dto.NotificationResponse;
import com.SIGMA.USCO.notifications.dto.UnreadCountResponse;
import com.SIGMA.USCO.notifications.entity.Notification;
import com.SIGMA.USCO.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(User user, Integer page, Integer size) {

        // ponytail: paginación opcional y contract-safe — sin page/size devuelve la lista completa (comportamiento previo)
        Pageable pageable = (page != null && size != null)
                ? PageRequest.of(page, size)
                : Pageable.unpaged();

        List<Notification> notifications =
                notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(user.getId(), pageable);

        return notifications.stream()
                .map(n -> new NotificationResponse(
                        n.getId(),
                        n.getType(),
                        n.getSubject(),
                        n.getMessage(),
                        n.getCreatedAt(),
                        n.isRead(),
                        n.getStudentModality() != null ? n.getStudentModality().getId() : null,
                        n.getInvitationId()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(User user) {

        long count = notificationRepository.countByRecipient_IdAndReadFalse(user.getId());

        return new UnreadCountResponse(count);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotificationDetail(User user, Long notificationId) {

        Notification notification = notificationRepository.findByIdAndRecipient_Id(notificationId, user.getId())
                        .orElseThrow(() ->
                                new NotFoundException("Notificación no encontrada")
                        );

        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getSubject(),
                notification.getMessage(),
                notification.getCreatedAt(),
                notification.isRead(),
                notification.getStudentModality() != null
                        ? notification.getStudentModality().getId()
                        : null,
                notification.getInvitationId() != null
                        ? notification.getInvitationId()
                        : null
        );
    }

    @Transactional
    public OperationResultResponse markAsRead(User user, Long notificationId) {

        Notification notification = notificationRepository.findByIdAndRecipient_Id(notificationId, user.getId())
                        .orElseThrow(() ->
                                new NotFoundException("Notificación no encontrada")
                        );

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }

        return new OperationResultResponse(true, "Notificación marcada como leída");
    }
}
