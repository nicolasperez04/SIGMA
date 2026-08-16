package com.SIGMA.USCO.notifications.service;

import com.SIGMA.USCO.config.EmailService;
import com.SIGMA.USCO.notifications.entity.Notification;
import com.SIGMA.USCO.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcherService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final PlatformTransactionManager transactionManager;

    /**
     * Paquete generado para envío con adjunto: ruta absoluta del archivo, nombre de archivo
     * y el id del certificado asociado (para marcar SENT al confirmar el envío).
     */
    public record GeneratedAttachment(Path path, String name, Long certificateId) {}

    // Set para deduplicación: evita que el mismo adjunto se envíe múltiples veces
    // para la misma notificación en procesamiento asincrónico
    private final Set<Long> processingNotificationsWithAttachment = ConcurrentHashMap.newKeySet();

    // ponytail: regla estable (T7.13a): jefes/comités no reciben correo; emailSent=true los excluye del retry
    private boolean shouldSendEmail(Notification notification) {
        return switch (notification.getRecipientType()) {
            case STUDENT, PROJECT_DIRECTOR, EXAMINER -> true;
            case PROGRAM_HEAD -> false; // ESTO SIRVE PARA QUE NO SE ENVÍEN CORREOS A LOS JEFES DE PROGRAMA
            case PROGRAM_CURRICULUM_COMMITTEE -> false; //  ESTO SIRVE PARA QUE NO SE ENVÍEN CORREOS A LOS COMITÉS DE CURRÍCULO DE PROGRAMA
            default -> false;
        };
    }

    private boolean sendEmailIfApplicable(Notification n) {
        if (!shouldSendEmail(n)) {
            // ponytail: regla estable: jefes/comités no reciben correo; emailSent=true los excluye del retry
            n.setEmailSent(true);
            return true;
        }
        try {
            emailService.sendEmail(
                    n.getRecipient().getEmail(),
                    n.getSubject(),
                    n.getMessage()
            );
            n.setEmailSent(true);
            n.setSentAt(LocalDateTime.now());
            return true;
        } catch (Exception ex) {
            log.error("Error enviando correo para notificación id={}", n.getId(), ex);
            n.setEmailSent(false);
            return false;
        }
    }

    private void markAttempt(Notification n) {
        n.setDeliveryAttempts(n.getDeliveryAttempts() + 1);
        n.setLastAttemptAt(LocalDateTime.now());
    }

    @Transactional
    @Async("notificationTaskExecutor")
    public void dispatch(Notification notification) {
        markAttempt(notification);
        sendEmailIfApplicable(notification);
        notification.setInAppDelivered(true);
        notificationRepository.save(notification);
    }

    /**
     * Envía el correo con adjunto; el adjunto se genera (una sola vez) dentro del executor
     * vía {@code generator}. El callback {@code onSuccess} corre tras un envío exitoso.
     * Fallos de correo/generación quedan registrados con emailSent=false (outbox de Fase 7).
     * ponytail: @Transactional en el método NO se propaga al hilo async (se abre/cierra en el
     * caller); el generator navega lazy (OSIV off) -> se envuelve en TransactionTemplate para
     * que corra con sesión activa en el executor.
     */
    @Async("notificationTaskExecutor")
    public void dispatchWithAttachment(Notification notification, Supplier<GeneratedAttachment> generator, Consumer<Long> onSuccess) {
        // ponytail: guard anti doble envío concurrente; la BD (emailSent+attempts) es la fuente de verdad con el outbox de Fase 7
        if (!processingNotificationsWithAttachment.add(notification.getId())) {
            log.warn("Notificación ID {} ya está siendo procesada con adjunto, ignorando duplicado", notification.getId());
            return;
        }

        try {
            if (shouldSendEmail(notification)) {
                markAttempt(notification);
                GeneratedAttachment bundle = new TransactionTemplate(transactionManager)
                        .execute(status -> generator.get());
                notification.setAttachmentPath(bundle.path().toString());
                notification.setAttachmentName(bundle.name());
                emailService.sendEmailWithAttachment(
                        notification.getRecipient().getEmail(),
                        notification.getSubject(),
                        notification.getMessage(),
                        bundle.path().toFile(),
                        bundle.name()
                );

                notification.setEmailSent(true);
                notification.setSentAt(LocalDateTime.now());
                if (onSuccess != null) {
                    onSuccess.accept(bundle.certificateId());
                }
            } else {
                // ponytail: no aplica correo (jefe/comité) -> nunca cae en retry
                notification.setEmailSent(true);
            }
        } catch (Exception ex) {
            log.error("Error enviando correo con adjunto para notificación id={}", notification.getId(), ex);
            notification.setEmailSent(false);
        } finally {
            // Remover de la cola de procesamiento cuando termina
            processingNotificationsWithAttachment.remove(notification.getId());
        }

        notification.setInAppDelivered(true);

        notificationRepository.save(notification);
    }

    /**
     * Reintento síncrono del outbox (llamado por NotificationRetryScheduler).
     */
    @Transactional
    public void retryDispatch(Notification notification, Runnable onSuccess) {
        markAttempt(notification);
        if (!shouldSendEmail(notification)) {
            notification.setEmailSent(true);
        } else if (notification.getAttachmentPath() != null) {
            try {
                emailService.sendEmailWithAttachment(
                        notification.getRecipient().getEmail(),
                        notification.getSubject(),
                        notification.getMessage(),
                        Path.of(notification.getAttachmentPath()).toFile(),
                        notification.getAttachmentName()
                );
                notification.setEmailSent(true);
                notification.setSentAt(LocalDateTime.now());
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } catch (Exception ex) {
                log.error("Error reenviando correo con adjunto para notificación id={}", notification.getId(), ex);
                notification.setEmailSent(false);
            }
        } else {
            sendEmailIfApplicable(notification);
        }

        notification.setInAppDelivered(true);
        notificationRepository.save(notification);
    }
}
