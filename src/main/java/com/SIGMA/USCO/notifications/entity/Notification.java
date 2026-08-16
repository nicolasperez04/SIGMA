package com.SIGMA.USCO.notifications.entity;

import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification", indexes = {
        @Index(name = "idx_notification_recipient_created", columnList = "recipient_user_id, created_at")
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @ToString.Include
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    @ToString.Include
    private NotificationRecipientType recipientType;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id")
    private User recipient;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by_user_id")
    private User triggeredBy;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_modality_id")
    private StudentModality studentModality;

    @Column(name = "invitation_id")
    @ToString.Include
    private Long invitationId;

    @ToString.Include
    private String subject;


    @Column(columnDefinition = "TEXT")
    private String message;

    @ToString.Include
    private LocalDateTime createdAt;
    @ToString.Include
    private LocalDateTime sentAt;

    @ToString.Include
    private boolean emailSent = false;
    @ToString.Include
    private boolean inAppDelivered = false;

    @Column(name = "delivery_attempts", nullable = false)
    @ToString.Include
    private int deliveryAttempts = 0;

    @Column(name = "last_attempt_at")
    @ToString.Include
    private LocalDateTime lastAttemptAt;

    @Column(name = "attachment_path", length = 1000)
    @ToString.Include
    private String attachmentPath;

    @Column(name = "attachment_name", length = 1000)
    @ToString.Include
    private String attachmentName;

    @Column(name = "is_read", nullable = false)
    @ToString.Include
    private boolean read = false;
    @ToString.Include
    private LocalDateTime readAt;

}
