package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.Entity.ModalityProcessStatusHistory;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.ModalityProcessStatusHistoryRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.notifications.event.ModalityEvent;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import org.springframework.context.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorrectionDeadlineSchedulerService {

    private final StudentModalityRepository studentModalityRepository;
    private final ModalityProcessStatusHistoryRepository historyRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * ========================================
     * SCHEDULER - GESTIÓN DE PLAZOS DE CORRECCIÓN
     * ========================================
     *
     * Este scheduler implementa el reglamento de modalidades de grado:
     *
     * 📋 REGLAS:
     * 1. El estudiante tiene 30 días calendario para presentar correcciones
     * 2. Se envía recordatorio automático a los 20 días
     * 3. El estudiante tiene MÁXIMO 3 OPORTUNIDADES para corregir
     * 4. Si pasa el plazo de 30 días → Propuesta RECHAZADA automáticamente
     * 5. Si se agotan los 3 intentos → Propuesta RECHAZADA automáticamente
     *
     * 🔄 FRECUENCIA DE EJECUCIÓN:
     * - PRODUCCIÓN: Diariamente a las 8:00 AM
     * - TESTING: Cada minuto (cambiar cron para testing)
     *
     * ⚠️ IMPORTANTE: Para testing, cambiar días por minutos en la lógica
     */
    @Scheduled(cron = "0 0 8 * * ?") // PRODUCCIÓN: Ejecutar todos los días a las 8:00 AM
    // @Scheduled(cron = "0 * * * * ?") // TESTING: Ejecutar cada minuto
    @Transactional
    public void checkCorrectionDeadlines() {
        log.info("🔍 ========== INICIANDO VERIFICACIÓN DE PLAZOS DE CORRECCIÓN ==========");
        log.info("⏰ Fecha y hora de ejecución: {}", LocalDateTime.now());

        LocalDateTime now = LocalDateTime.now();

        List<StudentModality> modalitiesWithCorrections = studentModalityRepository.findByStatusIn(
                List.of(
                        ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD,
                        ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE
                )
        );

        log.info("📋 Encontradas {} modalidades con correcciones solicitadas", modalitiesWithCorrections.size());

        if (modalitiesWithCorrections.isEmpty()) {
            log.info("✅ No hay modalidades pendientes de corrección. Finalizando verificación.");
            log.info("========== VERIFICACIÓN COMPLETADA ==========\n");
            return;
        }

        int recordatoriosEnviados = 0;
        int modalidadesRechazadas = 0;

        for (StudentModality modality : modalitiesWithCorrections) {

            if (modality.getCorrectionRequestDate() == null || modality.getCorrectionDeadline() == null) {
                log.warn("⚠️ Modalidad {} no tiene fechas de corrección configuradas. Saltando...", modality.getId());
                continue;
            }

            long daysSinceRequest = ChronoUnit.DAYS.between(modality.getCorrectionRequestDate(), now);
            long daysUntilDeadline = ChronoUnit.DAYS.between(now, modality.getCorrectionDeadline());

            log.info("📊 Modalidad {}: {} días desde solicitud, {} días hasta plazo límite, {} intentos usados",
                     modality.getId(), daysSinceRequest, daysUntilDeadline, modality.getCorrectionAttempts());

            // CASO 1: Han pasado 30 días o más → RECHAZAR automáticamente
            if (daysUntilDeadline <= 0) {
                log.warn("❌ Modalidad {} superó el plazo de 30 días. Rechazando propuesta...", modality.getId());
                rejectModalityByTimeout(modality, "Plazo de 30 días vencido");
                modalidadesRechazadas++;
            }
            // CASO 2: Han pasado 20 días → Enviar recordatorio (solo una vez)
            else if (daysSinceRequest >= 20 && (modality.getCorrectionReminderSent() == null || !modality.getCorrectionReminderSent())) {
                log.info("📧 Modalidad {} ha alcanzado 20 días. Enviando recordatorio...", modality.getId());
                sendDeadlineReminder(modality, (int) daysUntilDeadline);
                recordatoriosEnviados++;
            } else {
                log.debug("✓ Modalidad {} aún dentro del plazo normal", modality.getId());
            }
        }

        log.info("📊 RESUMEN DE EJECUCIÓN:");
        log.info("   - Recordatorios enviados: {}", recordatoriosEnviados);
        log.info("   - Propuestas rechazadas por timeout: {}", modalidadesRechazadas);
        log.info("========== VERIFICACIÓN COMPLETADA ==========\n");
    }

    /**
     * Envía un recordatorio al estudiante sobre el plazo de corrección
     */
    private void sendDeadlineReminder(StudentModality modality, int daysRemaining) {
        log.info("📧 Enviando recordatorio para modalidad {}", modality.getId());
        log.info("   Días restantes: {}", daysRemaining);
        log.info("   Intentos usados: {} de 3", modality.getCorrectionAttempts());

        // Publicar evento de recordatorio
        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.CORRECTION_DEADLINE_REMINDER, modality.getId(), null, Map.of(
                        ModalityEvent.KEY_STUDENT_ID, modality.getLeader().getId(),
                        ModalityEvent.KEY_DEADLINE, modality.getCorrectionDeadline(),
                        ModalityEvent.KEY_DAYS_REMAINING, daysRemaining
                ))
        );

        // Marcar que el recordatorio fue enviado
        modality.setCorrectionReminderSent(true);
        studentModalityRepository.save(modality);

        log.info("✅ Recordatorio enviado exitosamente para modalidad {}", modality.getId());
    }

    /**
     * Rechaza automáticamente la propuesta por vencimiento del plazo de 30 días
     * o por agotar los 3 intentos de corrección
     *
     * @param modality Modalidad a rechazar
     * @param reason Razón del rechazo
     */
    private void rejectModalityByTimeout(StudentModality modality, String reason) {
        log.warn("🔴 Rechazando propuesta de modalidad {} por: {}", modality.getId(), reason);
        log.warn("   Intentos usados: {} de 3", modality.getCorrectionAttempts());
        log.warn("   Fecha de solicitud: {}", modality.getCorrectionRequestDate());
        log.warn("   Plazo límite: {}", modality.getCorrectionDeadline());

        // Cambiar estado a CORRECTIONS_REJECTED_FINAL
        modality.setStatus(ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL);
        modality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(modality);

        // Registrar en historial
        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(modality)
                        .status(ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL)
                        .changeDate(LocalDateTime.now())
                        .responsible(null) // Rechazo automático del sistema
                        .observations(
                                "Propuesta rechazada automáticamente. " +
                                "Razón: " + reason + ". " +
                                "Fecha de solicitud de correcciones: " + modality.getCorrectionRequestDate() + ". " +
                                "Plazo límite: " + modality.getCorrectionDeadline() + ". " +
                                "Intentos de corrección usados: " + modality.getCorrectionAttempts() + " de 3."
                        )
                        .build()
        );

        // Publicar evento de rechazo
        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.CORRECTION_DEADLINE_EXPIRED, modality.getId(), null, Map.of(
                        ModalityEvent.KEY_STUDENT_ID, modality.getLeader().getId(),
                        ModalityEvent.KEY_REQUEST_DATE, modality.getCorrectionRequestDate()
                ))
        );

        log.info("✅ Propuesta de modalidad {} rechazada exitosamente", modality.getId());
    }
}

