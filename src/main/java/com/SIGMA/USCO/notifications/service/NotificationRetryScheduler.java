package com.SIGMA.USCO.notifications.service;

import com.SIGMA.USCO.Modalities.entity.enums.CertificateStatus;
import com.SIGMA.USCO.Modalities.repository.AcademicCertificateRepository;
import com.SIGMA.USCO.Modalities.repository.ExaminerCertificateRepository;
import com.SIGMA.USCO.notifications.entity.Notification;
import com.SIGMA.USCO.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryScheduler {

    private final NotificationRepository notificationRepository;
    private final NotificationDispatcherService dispatcher;
    private final AcademicCertificateRepository academicCertificateRepository;
    private final ExaminerCertificateRepository examinerCertificateRepository;

    // ponytail: 5 min es un compromiso razonable para actas oficiales (actas oficiales no requieren latencia menor)
    @Scheduled(fixedDelay = 300000)
    public void retryFailedDeliveries() {
        List<Notification> failed = notificationRepository.findByEmailSentFalseAndDeliveryAttemptsLessThan(3);
        for (Notification n : failed) {
            try {
                dispatcher.retryDispatch(n, () -> markCertificatesSent(n));
                if (!n.isEmailSent()) {
                    log.error("Entrega fallida definitiva para notificación id={} (type={}, attempts={}) — ALERTA: revisar SMTP/certificado", n.getId(), n.getType(), n.getDeliveryAttempts());
                }
            } catch (Exception e) {
                log.error("Error en reintento de notificación id={}", n.getId(), e);
            }
        }
    }

    private void markCertificatesSent(Notification n) {
        if (n.getStudentModality() == null) return;
        Long mId = n.getStudentModality().getId();
        academicCertificateRepository.findByStudentModalityIdAndStatus(mId, CertificateStatus.GENERATED).forEach(c -> {
            c.setStatus(CertificateStatus.SENT);
            academicCertificateRepository.save(c);
        });
        examinerCertificateRepository.findByStudentModalityIdAndStatus(mId, CertificateStatus.GENERATED).forEach(c -> {
            c.setStatus(CertificateStatus.SENT);
            examinerCertificateRepository.save(c);
        });
    }
}
