package com.SIGMA.USCO.notifications.service;

import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.notifications.entity.Notification;
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;

import java.time.LocalDateTime;

public final class NotificationBuilderHelper {

    private NotificationBuilderHelper() {}

    public static Notification buildNotification(
            NotificationType type,
            NotificationRecipientType recipientType,
            User recipient,
            StudentModality modality,
            String subject,
            String message) {
        return buildNotification(type, recipientType, recipient, null, modality, subject, message);
    }

    public static Notification buildNotification(
            NotificationType type,
            NotificationRecipientType recipientType,
            User recipient,
            User triggeredBy,
            StudentModality modality,
            String subject,
            String message) {
        return Notification.builder()
                .type(type)
                .recipientType(recipientType)
                .recipient(recipient)
                .triggeredBy(triggeredBy)
                .studentModality(modality)
                .subject(subject)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static String buildModalityInfo(StudentModality modality) {
        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        if (projectTitle != null && !projectTitle.isBlank()) {
            return degreeModalityName + " \u2013 " + projectTitle;
        }
        return degreeModalityName;
    }
}
