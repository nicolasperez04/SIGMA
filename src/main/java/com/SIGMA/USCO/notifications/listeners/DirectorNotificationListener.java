package com.SIGMA.USCO.notifications.listeners;

import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.shared.util.TranslationUtils;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.Modalities.event.ModalityEvent;
import com.SIGMA.USCO.notifications.service.NotificationBuilderHelper;
import com.SIGMA.USCO.notifications.service.NotificationFactory;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.entity.enums.AcademicDistinction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class DirectorNotificationListener {

    private final StudentModalityRepository studentModalityRepository;
    private final NotificationFactory notificationFactory;
    private final UserRepository userRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final com.SIGMA.USCO.academic.repository.StudentProfileRepository studentProfileRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleEvent(ModalityEvent event) {
        try {
            switch (event.getType()) {
                case MODALITY_CANCELLATION_APPROVED -> handleCancellationApproved(event);
                case MODALITY_CANCELLATION_REJECTED -> handleCancellationRejected(event);
                case MODALITY_CANCELLATION_REQUESTED -> handleCancellationRequested(event);
                case MODALITY_APPROVED_BY_EXAMINERS -> handleModalityApprovedByExaminers(event);
                case DOCUMENT_CORRECTIONS_REQUESTED -> handleCorrectionsRequested(event);
                case CORRECTION_REJECTED_FINAL -> handleCorrectionRejectedFinal(event);
                case DOCUMENT_REVIEW_TIEBREAKER_REQUIRED -> handleTiebreakerRequired(event);
                case MODALITY_READY_FOR_DEFENSE -> handleModalityReadyForDefense(event);
                case DIRECTOR_ASSIGNED -> handleDirectorAssigned(event);
                case DEFENSE_COMPLETED -> handleFinalDefenseResult(event);
                case DOCUMENT_UPLOADED -> handleStudentDocumentUpdated(event);
                default -> log.warn("Unhandled ModalityEvent type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error en DirectorNotificationListener procesando evento {} (studentModalityId={})",
                    event.getType(), event.getStudentModalityId(), e);
            throw e;
        }
    }

    private void handleCancellationApproved(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        if (sm.getProjectDirector() == null) return;

        String miembros = TranslationUtils.getStudentList(sm);

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(sm);
        String subject = NotificationMessageTemplates.DIRECTOR_CANCELLATION_SUBJECT;
        String message = NotificationMessageTemplates.directorCancellationApproved(
                sm.getProjectDirector().getName(),
                modalidadInfo,
                miembros
        );

        notificationFactory.buildAndDispatch(
                NotificationType.MODALITY_CANCELLATION_APPROVED,
                NotificationRecipientType.PROJECT_DIRECTOR,
                sm.getProjectDirector(),
                sm, subject, message
        );
    }

    private void handleCancellationRejected(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        if (sm.getProjectDirector() == null) return;

        String miembros = TranslationUtils.getStudentList(sm);

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(sm);
        String subject = NotificationMessageTemplates.DIRECTOR_CANCELLATION_SUBJECT;
        String message = NotificationMessageTemplates.directorCancellationRejected(
                sm.getProjectDirector().getName(),
                modalidadInfo,
                miembros
        );

        notificationFactory.buildAndDispatch(
                NotificationType.MODALITY_CANCELLATION_REJECTED,
                NotificationRecipientType.PROJECT_DIRECTOR,
                sm.getProjectDirector(),
                sm, subject, message
        );
    }

    private void handleCancellationRequested(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        if (sm.getProjectDirector() == null) return;

        String miembros = TranslationUtils.getStudentList(sm);
        User leader = sm.getLeader();
        var leaderProfile = studentProfileRepository.findByUserId(leader.getId()).orElse(null);

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(sm);
        String subject = NotificationMessageTemplates.DIRECTOR_CANCELLATION_REQUESTED_SUBJECT;
        String message = NotificationMessageTemplates.directorCancellationRequested(
                sm.getProjectDirector().getName(),
                modalidadInfo,
                sm.getAcademicProgram().getName(),
                leader.getName() + " " + leader.getLastName(),
                leaderProfile != null && leaderProfile.getSemester() != null ? String.valueOf(leaderProfile.getSemester()) : "No registrado",
                leaderProfile != null && leaderProfile.getStudentCode() != null ? leaderProfile.getStudentCode() : "No registrado",
                leader.getEmail(),
                miembros
        );

        notificationFactory.buildAndDispatch(
                NotificationType.MODALITY_CANCELLATION_REQUESTED,
                NotificationRecipientType.PROJECT_DIRECTOR,
                sm.getProjectDirector(),
                sm, subject, message
        );
    }

    private void handleModalityApprovedByExaminers(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        if (sm.getProjectDirector() == null) return;

        String modalityName = sm.getProgramDegreeModality().getDegreeModality().getName();
        String programName = sm.getAcademicProgram().getName();
        String approvalDate = TranslationUtils.formatDateTime(LocalDateTime.now());
        String subject = "Notificación oficial – Modalidad aprobada por jurado evaluador";

        notificationFactory.buildAndDispatch(
                NotificationType.MODALITY_APPROVED_BY_EXAMINERS,
                NotificationRecipientType.PROJECT_DIRECTOR,
                sm.getProjectDirector(),
                sm, subject,
                NotificationMessageTemplates.modalityApprovedByExaminers(
                        sm.getProjectDirector().getName(),
                        modalityName,
                        programName,
                        approvalDate,
                        TranslationUtils.getStudentList(sm)
                )
        );
    }

    private void handleCorrectionsRequested(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        if (sm.getProjectDirector() == null) return;

        String requestedByName = event.get(ModalityEvent.KEY_REQUESTED_BY, String.class);
        NotificationRecipientType requestedBy = requestedByName != null && !requestedByName.isBlank()
                ? NotificationRecipientType.valueOf(requestedByName)
                : null;
        if (requestedBy != NotificationRecipientType.EXAMINER) return;

        String documentName = studentDocumentRepository.findById(event.get(ModalityEvent.KEY_STUDENT_DOCUMENT_ID, Long.class))
                .map(doc -> doc.getDocumentConfig().getDocumentName())
                .orElse("Documento académico");
        String observations = event.get(ModalityEvent.KEY_OBSERVATIONS, String.class);
        String resolvedObservations = observations != null && !observations.isBlank()
                ? observations
                : "No se registraron observaciones adicionales.";
        String studentName = sm.getLeader().getName() + " " + sm.getLeader().getLastName();
        String subject = "Notificación oficial – Correcciones solicitadas por jurado evaluador";

        notificationFactory.buildAndDispatch(
                NotificationType.DOCUMENT_CORRECTIONS_REQUESTED,
                NotificationRecipientType.PROJECT_DIRECTOR,
                sm.getProjectDirector(),
                sm, subject,
                NotificationMessageTemplates.directorCorrectionsRequested(
                        sm.getProjectDirector().getName(),
                        studentName,
                        documentName,
                        resolvedObservations
                )
        );
    }

    private void handleCorrectionRejectedFinal(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        if (sm.getProjectDirector() == null) return;

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(sm);
        String documentName = event.get(ModalityEvent.KEY_DOCUMENT_NAME, String.class);
        String reason = event.get(ModalityEvent.KEY_REASON, String.class);
        String resolvedReason = reason != null && !reason.isBlank()
                ? reason
                : "No se registran motivos adicionales.";
        String studentName = sm.getLeader().getName() + " " + sm.getLeader().getLastName();
        String subject = "Notificación oficial – Cancelación de modalidad por rechazo definitivo de correcciones";

        notificationFactory.buildAndDispatch(
                NotificationType.CORRECTION_REJECTED_FINAL,
                NotificationRecipientType.PROJECT_DIRECTOR,
                sm.getProjectDirector(),
                sm, subject,
                NotificationMessageTemplates.directorCorrectionRejectedFinal(
                        sm.getProjectDirector().getName(),
                        studentName,
                        modalidadInfo,
                        documentName,
                        resolvedReason
                )
        );
    }

    private void handleTiebreakerRequired(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        if (sm.getProjectDirector() == null) return;

        String documentName = event.get(ModalityEvent.KEY_DOCUMENT_NAME, String.class);
        String studentName = sm.getLeader().getName() + " " + sm.getLeader().getLastName();
        String subject = "Notificación oficial – Decisión dividida de jurados: se requiere jurado de desempate";

        notificationFactory.buildAndDispatch(
                NotificationType.DOCUMENT_REVIEW_TIEBREAKER_REQUIRED,
                NotificationRecipientType.PROJECT_DIRECTOR,
                sm.getProjectDirector(),
                sm, subject,
                NotificationMessageTemplates.directorTiebreakerRequired(
                        sm.getProjectDirector().getName(),
                        studentName,
                        documentName
                )
        );
    }

    private void handleModalityReadyForDefense(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        if (sm.getProjectDirector() == null) return;

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(sm);
        String subject = "Modalidad lista para revisión final por parte de los jurados – Documentos finales aprobados";
        String message = NotificationMessageTemplates.directorModalityReadyForDefense(
                sm.getProjectDirector().getName(),
                TranslationUtils.getStudentList(sm),
                modalidadInfo
        );
        notificationFactory.buildAndDispatch(
                NotificationType.MODALITY_READY_FOR_DEFENSE,
                NotificationRecipientType.PROJECT_DIRECTOR,
                sm.getProjectDirector(),
                sm, subject, message
        );
    }

    private void handleDirectorAssigned(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        User director = userRepository.findById(event.get(ModalityEvent.KEY_DIRECTOR_ID, Long.class))
                .orElseThrow(() -> new NotFoundException("Director de proyecto no encontrado"));

        String miembros = TranslationUtils.getStudentList(modality);

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String directorSubject = NotificationMessageTemplates.DIRECTOR_ASSIGNED_SUBJECT;
        String directorMessage = NotificationMessageTemplates.directorAssigned(
                director.getName(),
                modalidadInfo,
                modality.getProgramDegreeModality().getAcademicProgram().getName(),
                miembros,
                TranslationUtils.formatDateTime(modality.getUpdatedAt())
        );

        notificationFactory.buildAndDispatch(
                NotificationType.DIRECTOR_ASSIGNED,
                NotificationRecipientType.PROJECT_DIRECTOR,
                director,
                modality, directorSubject, directorMessage
        );
    }

    private void handleFinalDefenseResult(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        User director = modality.getProjectDirector();
        if (director == null) return;

        String miembros = TranslationUtils.getStudentList(modality);

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = NotificationMessageTemplates.DIRECTOR_FINAL_DEFENSE_RESULT_SUBJECT;
        String message = NotificationMessageTemplates.directorFinalDefenseResult(
                director.getName(),
                miembros,
                modalidadInfo,
                TranslationUtils.translateModalityProcessStatus(event.get(ModalityEvent.KEY_FINAL_STATUS, ModalityProcessStatus.class)),
                TranslationUtils.translateAcademicDistinction(event.get(ModalityEvent.KEY_ACADEMIC_DISTINCTION, AcademicDistinction.class)),
                event.get(ModalityEvent.KEY_OBSERVATIONS, String.class) != null ? event.get(ModalityEvent.KEY_OBSERVATIONS, String.class) : "N/A"
        );

        notificationFactory.buildAndDispatch(
                NotificationType.DEFENSE_COMPLETED,
                NotificationRecipientType.PROJECT_DIRECTOR,
                director,
                modality, subject, message
        );
    }

    private void handleStudentDocumentUpdated(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        if (modality.getProjectDirector() == null) return;

        StudentDocument document = studentDocumentRepository.findById(event.get(ModalityEvent.KEY_STUDENT_DOCUMENT_ID, Long.class))
                .orElseThrow(() -> new NotFoundException("Documento no encontrado"));

        User student = modality.getLeader();
        User director = modality.getProjectDirector();

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = NotificationMessageTemplates.DIRECTOR_STUDENT_DOCUMENT_UPDATED_SUBJECT;
        String message = NotificationMessageTemplates.directorStudentDocumentUpdated(
                director.getName(),
                student.getName() + " " + student.getLastName(),
                modalidadInfo,
                document.getDocumentConfig().getDocumentName(),
                TranslationUtils.translateDocumentStatus(document.getStatus())
        );

        notificationFactory.buildAndDispatch(
                NotificationType.DOCUMENT_UPLOADED,
                NotificationRecipientType.PROJECT_DIRECTOR,
                director, student,
                modality, subject, message
        );
    }
}
