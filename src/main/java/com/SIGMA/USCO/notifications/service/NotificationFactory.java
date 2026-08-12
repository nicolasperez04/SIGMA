package com.SIGMA.USCO.notifications.service;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.notifications.entity.Notification;
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationFactory {

    private final NotificationRepository notificationRepository;
    private final NotificationDispatcherService dispatcher;

    public Notification buildAndDispatch(NotificationType type,
                                          NotificationRecipientType recipientType,
                                          User recipient,
                                          StudentModality modality,
                                          String subject,
                                          String message) {
        return buildAndDispatch(type, recipientType, recipient, null, modality, subject, message);
    }

    public Notification buildAndDispatch(NotificationType type,
                                          NotificationRecipientType recipientType,
                                          User recipient,
                                          User triggeredBy,
                                          StudentModality modality,
                                          String subject,
                                          String message) {
        Notification notification = NotificationBuilderHelper.buildNotification(
                type, recipientType, recipient, triggeredBy, modality, subject, message);
        notificationRepository.save(notification);
        dispatcher.dispatch(notification);
        return notification;
    }

    public Notification buildAndSave(NotificationType type,
                                      NotificationRecipientType recipientType,
                                      User recipient,
                                      User triggeredBy,
                                      StudentModality modality,
                                      String subject,
                                      String message) {
        Notification notification = NotificationBuilderHelper.buildNotification(
                type, recipientType, recipient, triggeredBy, modality, subject, message);
        return notificationRepository.save(notification);
    }

    public void saveAndDispatch(Notification notification) {
        notificationRepository.save(notification);
        dispatcher.dispatch(notification);
    }
}
