package com.SIGMA.USCO.notifications.dto;

import com.SIGMA.USCO.notifications.entity.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String subject,
        String message,
        LocalDateTime createdAt,
        boolean read,
        Long studentModalityId,
        Long invitationId
) {
}