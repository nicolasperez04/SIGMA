package com.SIGMA.USCO.notifications.service;

import com.SIGMA.USCO.Modalities.entity.AcademicCertificate;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.enums.CertificateStatus;
import com.SIGMA.USCO.Modalities.repository.AcademicCertificateRepository;
import com.SIGMA.USCO.Modalities.repository.ExaminerCertificateRepository;
import com.SIGMA.USCO.notifications.entity.Notification;
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("F7 - Outbox: NotificationRetryScheduler reintenta entregas fallidas y marca certificados SENT")
class NotificationRetrySchedulerTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationDispatcherService dispatcher;
    @Mock
    private AcademicCertificateRepository academicCertificateRepository;
    @Mock
    private ExaminerCertificateRepository examinerCertificateRepository;

    @InjectMocks
    private NotificationRetryScheduler scheduler;

    @Test
    @DisplayName("F7: notificación fallida con attempts<3 se reintenta con onSuccess no nulo")
    void retriesFailedNotificationBelowMaxAttempts() {
        Notification failed = Notification.builder()
                .id(1L)
                .recipientType(NotificationRecipientType.STUDENT)
                .emailSent(false)
                .deliveryAttempts(2)
                .build();
        when(notificationRepository.findByEmailSentFalseAndDeliveryAttemptsLessThan(3)).thenReturn(List.of(failed));

        scheduler.retryFailedDeliveries();

        ArgumentCaptor<Runnable> onSuccessCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(dispatcher, times(1)).retryDispatch(eq(failed), onSuccessCaptor.capture());
        assertThat(onSuccessCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("F7: la notificación con attempts>=3 no entra en la consulta del scheduler")
    void doesNotRetryNotificationAtMaxAttempts() {
        Notification exhausted = Notification.builder()
                .id(2L)
                .recipientType(NotificationRecipientType.STUDENT)
                .emailSent(false)
                .deliveryAttempts(3)
                .build();
        when(notificationRepository.findByEmailSentFalseAndDeliveryAttemptsLessThan(3)).thenReturn(List.of());

        scheduler.retryFailedDeliveries();

        verify(dispatcher, never()).retryDispatch(any(), any());
        assertThat(exhausted.getDeliveryAttempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("F7: markCertificatesSent marca SENT los certificados GENERATED de la modalidad")
    void marksGeneratedCertificatesAsSentOnSuccess() {
        StudentModality modality = StudentModality.builder().id(10L).build();
        Notification delivered = Notification.builder()
                .id(3L)
                .recipientType(NotificationRecipientType.STUDENT)
                .emailSent(false)
                .deliveryAttempts(1)
                .studentModality(modality)
                .build();
        AcademicCertificate cert = AcademicCertificate.builder()
                .id(1L)
                .status(CertificateStatus.GENERATED)
                .build();
        when(notificationRepository.findByEmailSentFalseAndDeliveryAttemptsLessThan(3)).thenReturn(List.of(delivered));
        when(academicCertificateRepository.findByStudentModalityIdAndStatus(10L, CertificateStatus.GENERATED))
                .thenReturn(List.of(cert));

        scheduler.retryFailedDeliveries();

        ArgumentCaptor<Runnable> onSuccessCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(dispatcher).retryDispatch(eq(delivered), onSuccessCaptor.capture());
        onSuccessCaptor.getValue().run();

        ArgumentCaptor<AcademicCertificate> certCaptor = ArgumentCaptor.forClass(AcademicCertificate.class);
        verify(academicCertificateRepository).save(certCaptor.capture());
        assertThat(certCaptor.getValue().getStatus()).isEqualTo(CertificateStatus.SENT);
    }
}