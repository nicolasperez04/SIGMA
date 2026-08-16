package com.SIGMA.USCO.notifications.listeners;

import com.SIGMA.USCO.Modalities.entity.AcademicCertificate;
import com.SIGMA.USCO.Modalities.entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.entity.enums.CertificateStatus;
import com.SIGMA.USCO.Modalities.entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.notifications.entity.Notification;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.shared.util.TranslationUtils;
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.Modalities.event.ModalityEvent;
import com.SIGMA.USCO.notifications.service.AcademicCertificatePdfService;
import com.SIGMA.USCO.notifications.service.CertificatePdfSupport;
import com.SIGMA.USCO.notifications.service.NotificationBuilderHelper;
import com.SIGMA.USCO.notifications.service.NotificationDispatcherService;
import com.SIGMA.USCO.notifications.service.NotificationDispatcherService.GeneratedAttachment;
import com.SIGMA.USCO.notifications.service.NotificationFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class StudentNotificationListener {

    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final NotificationFactory notificationFactory;
    private final NotificationDispatcherService dispatcher;
    private final UserRepository userRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final AcademicCertificatePdfService certificatePdfService;

@Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleEvent(ModalityEvent event) {
        try {
            switch (event.getType()) {
            case MODALITY_STARTED -> handleModalityStarted(event);
            case DOCUMENT_CORRECTIONS_REQUESTED -> handleDocumentCorrectionsRequested(event);
            case MODALITY_CANCELLATION_REQUESTED -> handleCancellationRequested(event);
            case MODALITY_CANCELLATION_APPROVED -> handleCancellationApproved(event);
            case MODALITY_CANCELLATION_REJECTED -> handleCancellationRejected(event);
            case DEFENSE_SCHEDULED -> handleDefenseScheduled(event);
            case DIRECTOR_ASSIGNED -> handleDirectorAssigned(event);
            case DEFENSE_COMPLETED -> handleDefenseResult(event);
            case MODALITY_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE -> handleModalityApprovedByCommittee(event);
            case MODALITY_APPROVED_BY_PROGRAM_HEAD -> handleModalityApprovedByProgramHead(event);
            case CORRECTION_DEADLINE_REMINDER -> handleCorrectionDeadlineReminder(event);
            case CORRECTION_DEADLINE_EXPIRED -> handleCorrectionDeadlineExpired(event);
            case CORRECTION_RESUBMITTED -> handleCorrectionResubmitted(event);
            case CORRECTION_APPROVED -> handleCorrectionApproved(event);
            case CORRECTION_REJECTED_FINAL -> handleCorrectionRejectedFinal(event);
            case MODALITY_CLOSED_BY_COMMITTEE -> handleModalityClosedByCommittee(event);
            case MODALITY_INVITATION_RECEIVED -> handleModalityInvitationSent(event);
            case MODALITY_INVITATION_ACCEPTED -> handleModalityInvitationAccepted(event);
            case MODALITY_INVITATION_REJECTED -> handleModalityInvitationRejected(event);
            case MODALITY_FINAL_APPROVED_BY_COMMITTEE -> handleModalityFinalApprovedByCommittee(event);
            case MODALITY_REJECTED_BY_COMMITTEE -> handleModalityRejectedByCommittee(event);
            case SEMINAR_STARTED -> handleSeminarStarted(event);
            case SEMINAR_CANCELLED -> handleSeminarCancelled(event);
            case MODALITY_APPROVED_BY_EXAMINERS -> handleModalityApprovedByExaminers(event);
            case DOCUMENT_REVIEW_TIEBREAKER_REQUIRED -> handleDocumentReviewTiebreakerRequired(event);
            case DIRECTOR_NOTIFIES_PROGRAM_HEAD_FINAL_REVIEW -> handleFinalDocumentsSentToProgramHead(event);
            case MODALITY_READY_FOR_DEFENSE -> handleModalityReadyForDefense(event);
            case EXAMINER_FINAL_REVIEW_COMPLETED -> handleExaminerFinalReviewCompleted(event);
            case EXAMINER_ASSIGNED -> handleExaminersAssigned(event);
            case DOCUMENT_EDIT_APPROVED, DOCUMENT_EDIT_REJECTED -> handleDocumentEditResolved(event);
            default -> log.warn("Unhandled notification type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error en StudentNotificationListener procesando evento {} (studentModalityId={})",
                    event.getType(), event.getStudentModalityId(), e);
            throw e;
        }
    }

    private void handleModalityStarted(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        User student = modality.getLeader();
        String subject = "Modalidad iniciada – SIGMA";
        String message = NotificationMessageTemplates.modalityStarted(
                student.getName(),
                NotificationBuilderHelper.buildModalityInfo(modality),
                TranslationUtils.translateModalityProcessStatus(modality.getStatus())
        );
        notificationFactory.buildAndDispatch(NotificationType.MODALITY_STARTED, NotificationRecipientType.STUDENT, student, modality, subject, message);

    }


    private void handleDocumentCorrectionsRequested(ModalityEvent event) {
        StudentDocument document = studentDocumentRepository.findById(
                event.get(ModalityEvent.KEY_STUDENT_DOCUMENT_ID, Long.class)
        ).orElseThrow(() -> new NotFoundException("Documento no encontrado"));
        StudentModality modality = document.getStudentModality();
        String subject = "Correcciones solicitadas en documento académico – Acción requerida";
        String requestedByName = event.get(ModalityEvent.KEY_REQUESTED_BY, String.class);
        NotificationRecipientType requestedBy = requestedByName != null && !requestedByName.isBlank()
                ? NotificationRecipientType.valueOf(requestedByName)
                : null;
        String observations = event.get(ModalityEvent.KEY_OBSERVATIONS, String.class);
        String requestedByText;
        if (requestedBy == NotificationRecipientType.PROGRAM_HEAD) {
            requestedByText = "la Jefatura de Programa y/o Coordinación de Modalidades";
        } else if (requestedBy == NotificationRecipientType.EXAMINER) {
            requestedByText = "un jurado evaluador";
        } else {
            requestedByText = "el Comité de Currículo del Programa";
        }
        String resolvedObservations = observations != null && !observations.isBlank()
                ? observations
                : "No se registraron observaciones adicionales.";
        dispatchToActiveMembers(modality, NotificationType.DOCUMENT_CORRECTIONS_REQUESTED, null, subject,
                student -> NotificationMessageTemplates.correctionsRequested(
                        student.getName(),
                        requestedByText,
                        document.getDocumentConfig().getDocumentName(),
                        resolvedObservations
                ));
    }


    private void handleDocumentReviewTiebreakerRequired(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        String documentName = event.get(ModalityEvent.KEY_DOCUMENT_NAME, String.class);
        String subject = "Decisión dividida de jurados – Se requiere jurado de desempate";
        dispatchToActiveMembers(modality, NotificationType.DOCUMENT_REVIEW_TIEBREAKER_REQUIRED, null, subject,
                student -> NotificationMessageTemplates.tiebreakerRequired(
                        student.getName(),
                        documentName
                ));
    }


    private void handleFinalDocumentsSentToProgramHead(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        User director = modality.getProjectDirector();
        String directorNombre = director != null
                ? director.getName() + " " + director.getLastName()
                : "El director de proyecto";
        String subject = "Documentos finales enviados a revisión de Jefatura de Programa";
        dispatchToActiveMembers(modality, NotificationType.DIRECTOR_NOTIFIES_PROGRAM_HEAD_FINAL_REVIEW, null, subject,
                student -> NotificationMessageTemplates.finalDocumentsSentToProgramHead(
                        student.getName(),
                        directorNombre,
                        modalidadInfo
                ));
    }


    private void handleModalityReadyForDefense(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Modalidad lista para revisión final por parte de los jurados – Documentos finales aprobados";
        dispatchToActiveMembers(modality, NotificationType.MODALITY_READY_FOR_DEFENSE, null, subject,
                student -> NotificationMessageTemplates.modalityReadyForDefense(
                        student.getName(),
                        modalidadInfo
                ));
    }


    private void handleExaminerFinalReviewCompleted(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Aprobación final de documentos – Puede programar la sustentación";
        dispatchToActiveMembers(modality, NotificationType.EXAMINER_FINAL_REVIEW_COMPLETED, null, subject,
                student -> NotificationMessageTemplates.studentFinalReviewCompleted(
                        student.getName(),
                        modalidadInfo
                ));
    }


    private void handleCancellationRequested(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        String subject = "Solicitud de cancelación registrada – Modalidad de grado";
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(sm);
        dispatchToActiveMembers(sm, NotificationType.MODALITY_CANCELLATION_REQUESTED, null, subject,
                student -> NotificationMessageTemplates.cancellationRequested(student.getName(), modalidadInfo));
    }


    private void handleCancellationApproved(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        String subject = "Cancelación aprobada – Modalidad de grado";
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(sm);
        dispatchToActiveMembers(sm, NotificationType.MODALITY_CANCELLATION_APPROVED, null, subject,
                student -> NotificationMessageTemplates.cancellationApproved(student.getName(), modalidadInfo));
    }


    private void handleCancellationRejected(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        String subject = "Cancelación no aprobada – Modalidad de grado";
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(sm);
        String reason = event.get(ModalityEvent.KEY_REASON, String.class);
        String resolvedReason = reason != null && !reason.isBlank()
                ? reason
                : "No se especifican motivos adicionales.";
        dispatchToActiveMembers(sm, NotificationType.MODALITY_CANCELLATION_REJECTED, null, subject,
                student -> NotificationMessageTemplates.cancellationRejected(student.getName(), modalidadInfo, resolvedReason));
    }


    private void handleDefenseScheduled(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        User director = modality.getProjectDirector();
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String studentSubject = "Sustentación programada – Modalidad de Grado";
        LocalDateTime defenseDate = event.get(ModalityEvent.KEY_DEFENSE_DATE, LocalDateTime.class);
        String defenseLocation = event.get(ModalityEvent.KEY_DEFENSE_LOCATION, String.class);
        String directorName = director != null
                ? director.getName() + " " + director.getLastName()
                : "No asignado";
        String defenseDateText = TranslationUtils.formatDateTime(defenseDate);
        dispatchToActiveMembers(modality, NotificationType.DEFENSE_SCHEDULED, null, studentSubject,
                student -> NotificationMessageTemplates.defenseScheduled(
                        student.getName(),
                        modalidadInfo,
                        defenseDateText,
                        defenseLocation,
                        directorName
                ));
    }


    private void handleDirectorAssigned(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        User director = userRepository.findById(event.get(ModalityEvent.KEY_DIRECTOR_ID, Long.class))
                .orElseThrow(() -> new NotFoundException("Director de proyecto no encontrado"));
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String studentSubject = "Director de proyecto asignado – Modalidad de grado";
        String directorFullName = director.getName() + " " + director.getLastName();
        dispatchToActiveMembers(modality, NotificationType.DIRECTOR_ASSIGNED, null, studentSubject,
                student -> NotificationMessageTemplates.directorAssigned(
                        student.getName(),
                        modalidadInfo,
                        directorFullName,
                        director.getEmail()
                ));
    }


    private void handleDefenseResult(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        ModalityProcessStatus finalStatus = event.get(ModalityEvent.KEY_FINAL_STATUS, ModalityProcessStatus.class);
        boolean approved = finalStatus == ModalityProcessStatus.GRADED_APPROVED;
        boolean approvedPendingCommitteeReview = finalStatus == ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW;
        boolean shouldSendCertificate = approved || approvedPendingCommitteeReview;

        String studentSubject = shouldSendCertificate
                ? (approvedPendingCommitteeReview
                ? "Resultado de sustentación – Modalidad aprobada (distinción en revisión del comité)"
                : "Resultado final de sustentación – Modalidad aprobada")
                : "Resultado final de sustentación – Modalidad no aprobada";

        List<StudentModalityMember> members = activeMembers(modality);

        if (members == null || members.isEmpty()) {
            StudentModalityMember syntheticLeaderMember = StudentModalityMember.builder()
                    .studentModality(modality)
                    .student(modality.getLeader())
                    .status(MemberStatus.ACTIVE)
                    .isLeader(true)
                    .build();
            members = List.of(syntheticLeaderMember);
        }

        // El acta se genera UNA vez (supplier memoizado) aunque haya N destinatarios;
        // los dispatchWithAttachment corren en el executor en paralelo y comparten el cache sincronizado
        Supplier<GeneratedAttachment> attachmentSupplier = new Supplier<>() {
            private GeneratedAttachment cached;

            @Override
            public synchronized GeneratedAttachment get() {
                if (cached == null) {
                    try {
                        StudentModality m = studentModalityRepository.findById(modality.getId())
                                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
                        AcademicCertificate c = CertificatePdfSupport.isCompleteModality(m)
                                ? certificatePdfService.generateCertificate(m)
                                : certificatePdfService.generateCertificateForCommitteeApproval(m);
                        cached = new GeneratedAttachment(
                                certificatePdfService.getCertificatePath(m.getId()),
                                "ACTA_DE_APROBACION.pdf",
                                c.getId());
                    } catch (IOException e) {
                        throw new RuntimeException("No se pudo generar el acta de aprobación", e);
                    }
                }
                return cached;
            }
        };

        for (StudentModalityMember member : members) {
            User student = member.getStudent();
            String studentMessage = shouldSendCertificate
                    ? buildApprovedStudentMessage(student, modality, event)
                    : buildRejectedStudentMessage(student, modality, event);

            Notification notification = notificationFactory.buildAndSave(
                    NotificationType.DEFENSE_COMPLETED, NotificationRecipientType.STUDENT,
                    student, null, modality, studentSubject, studentMessage);

            if (shouldSendCertificate) {
                dispatcher.dispatchWithAttachment(notification, attachmentSupplier, certId -> {
                    try {
                        certificatePdfService.updateCertificateStatus(certId, CertificateStatus.SENT);
                    } catch (Exception e) {
                        log.error("Error actualizando estado del certificado: {}", e.getMessage(), e);
                    }
                });
                log.info("Acta enviada al estudiante {} (modalidad ID {})", student.getId(), modality.getId());
            } else {
                dispatcher.dispatch(notification);
            }
        }
    }


    private String buildApprovedStudentMessage(User student, StudentModality modality, ModalityEvent event) {
        String observaciones = TranslationUtils.localizeObservations(event.get(ModalityEvent.KEY_OBSERVATIONS, String.class));
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        return NotificationMessageTemplates.defenseResultApproved(
                student.getName(),
                modalidadInfo,
                TranslationUtils.translateAcademicDistinction(event.get(ModalityEvent.KEY_ACADEMIC_DISTINCTION, AcademicDistinction.class)),
                observaciones != null && !observaciones.isBlank() ? observaciones : "No se registran observaciones."
        );
    }

    private String buildRejectedStudentMessage(User student, StudentModality modality, ModalityEvent event) {
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String observations = event.get(ModalityEvent.KEY_OBSERVATIONS, String.class);
        return NotificationMessageTemplates.defenseResultRejected(
                student.getName(),
                modalidadInfo,
                observations != null && !observations.isBlank()
                        ? observations
                        : "No se registran observaciones adicionales."
        );
    }


    private void handleModalityApprovedByCommittee(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Modalidad de grado aprobada – Comité de Currículo";
        String directorName = modality.getProjectDirector() != null
                ? modality.getProjectDirector().getName() + " " +
                modality.getProjectDirector().getLastName()
                : "No se registra director asignado.";
        String approvalDate = TranslationUtils.formatDateTime(modality.getUpdatedAt());
        dispatchToActiveMembers(modality, NotificationType.MODALITY_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE, null, subject,
                student -> NotificationMessageTemplates.modalityApprovedByCommittee(
                        student.getName(),
                        modalidadInfo,
                        directorName,
                        approvalDate
                ));
    }


    private void handleModalityApprovedByProgramHead(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Modalidad de grado aprobada – Jefatura de Programa y/o Coordinación de Modalidades";
        dispatchToActiveMembers(modality, NotificationType.MODALITY_APPROVED_BY_PROGRAM_HEAD, null, subject,
                student -> NotificationMessageTemplates.modalityApprovedByProgramHead(student.getName(), modalidadInfo));
    }


    private void handleCorrectionDeadlineReminder(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        Integer daysRemaining = event.get(ModalityEvent.KEY_DAYS_REMAINING, Integer.class, 0);
        LocalDateTime deadline = event.get(ModalityEvent.KEY_DEADLINE, LocalDateTime.class);
        String subject = "Recordatorio oficial – Plazo de correcciones (%d días restantes)"
                .formatted(daysRemaining);
        for (var member : activeMembers(modality)) {
            User student = member.getStudent();
            String message = NotificationMessageTemplates.correctionDeadlineReminder(
                    student.getName(),
                    modalidadInfo,
                    daysRemaining,
                    TranslationUtils.formatDateTime(deadline)
            );
            notificationFactory.buildAndDispatch(NotificationType.CORRECTION_DEADLINE_REMINDER, NotificationRecipientType.STUDENT, student, modality, subject, message);

            log.info("Recordatorio de plazo de corrección enviado al estudiante {}", student.getId());
        }
    }


    private void handleCorrectionDeadlineExpired(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Notificación oficial – Cancelación automática de modalidad por vencimiento de plazo";
        LocalDateTime requestDate = event.get(ModalityEvent.KEY_REQUEST_DATE, LocalDateTime.class);
        for (var member : activeMembers(modality)) {
            User student = member.getStudent();
            String message = NotificationMessageTemplates.correctionDeadlineExpired(
                    student.getName(),
                    modalidadInfo,
                    TranslationUtils.formatDateTime(requestDate)
            );
            notificationFactory.buildAndDispatch(NotificationType.CORRECTION_DEADLINE_EXPIRED, NotificationRecipientType.STUDENT, student, modality, subject, message);

            log.info("Notificación de cancelación por vencimiento enviada al estudiante {}", student.getId());
        }
    }


    private void handleCorrectionResubmitted(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Notificación oficial – Documento corregido recibido";
        String documentName = event.get(ModalityEvent.KEY_DOCUMENT_NAME, String.class);
        for (var member : activeMembers(modality)) {
            User student = member.getStudent();
            String message = NotificationMessageTemplates.correctionResubmitted(
                    student.getName(),
                    modalidadInfo,
                    documentName,
                    TranslationUtils.formatDateTime(LocalDateTime.now())
            );
            notificationFactory.buildAndDispatch(NotificationType.CORRECTION_RESUBMITTED, NotificationRecipientType.STUDENT, student, modality, subject, message);

            log.info("Notificación de resubmisión de corrección enviada al estudiante {}", student.getId());
        }
    }


    private void handleCorrectionApproved(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Notificación oficial – Correcciones aprobadas";
        String documentName = event.get(ModalityEvent.KEY_DOCUMENT_NAME, String.class);
        for (var member : activeMembers(modality)) {
            User student = member.getStudent();
            String message = NotificationMessageTemplates.correctionApproved(
                    student.getName(),
                    modalidadInfo,
                    documentName
            );
            notificationFactory.buildAndDispatch(NotificationType.CORRECTION_APPROVED, NotificationRecipientType.STUDENT, student, modality, subject, message);

            log.info("Notificación de aprobación de corrección enviada al estudiante {}", student.getId());
        }
    }


    private void handleCorrectionRejectedFinal(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Notificación oficial – Cancelación de modalidad por rechazo definitivo de correcciones";
        String documentName = event.get(ModalityEvent.KEY_DOCUMENT_NAME, String.class);
        String reason = event.get(ModalityEvent.KEY_REASON, String.class);
        String resolvedReason = reason != null && !reason.isBlank()
                ? reason
                : "No se registran motivos adicionales.";

        // Los miembros pueden haberse eliminado (rechazo de documento final cancela la modalidad y borra la relación).
        // En ese caso el evento trae los ids capturados antes del borrado.
        List<Long> notifiedStudentIds = event.get(ModalityEvent.KEY_STUDENT_IDS, List.class);

        if (notifiedStudentIds != null && !notifiedStudentIds.isEmpty()) {
            for (Object rawId : notifiedStudentIds) {
                userRepository.findById(((Number) rawId).longValue())
                        .ifPresent(student -> dispatchCorrectionRejectedFinal(student, modality, modalidadInfo, subject, documentName, resolvedReason));
            }
            return;
        }

        for (var member : activeMembers(modality)) {
            User student = member.getStudent();
            dispatchCorrectionRejectedFinal(student, modality, modalidadInfo, subject, documentName, resolvedReason);
        }
    }

    private void dispatchCorrectionRejectedFinal(User student, StudentModality modality, String modalidadInfo, String subject,
                                                 String documentName, String resolvedReason) {
        String message = NotificationMessageTemplates.correctionRejectedFinal(
                student.getName(),
                modalidadInfo,
                documentName,
                resolvedReason
        );
        notificationFactory.buildAndDispatch(NotificationType.CORRECTION_REJECTED_FINAL, NotificationRecipientType.STUDENT,
                student, modality, subject, message
        );
        log.info("Notificación de rechazo final de corrección enviada al estudiante {}", student.getId());
    }


    private void handleModalityClosedByCommittee(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        User committeeMember = userRepository.findById(event.get(ModalityEvent.KEY_COMMITTEE_MEMBER_ID, Long.class))
                .orElseThrow(() -> new NotFoundException("Miembro del comité no encontrado"));
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Notificación oficial – Cierre de modalidad por decisión del Comité de Currículo";
        String reason = event.get(ModalityEvent.KEY_REASON, String.class);
        String resolvedReason = reason != null && !reason.isBlank()
                ? reason
                : "No se registran motivos adicionales.";
        String decisionDate = TranslationUtils.formatDateTime(LocalDateTime.now());
        String programName = modality.getAcademicProgram().getName();
        for (var member : activeMembers(modality)) {
            User student = member.getStudent();
            String message = NotificationMessageTemplates.modalityClosedByCommittee(
                    student.getName(),
                    modalidadInfo,
                    programName,
                    committeeMember.getName(),
                    committeeMember.getLastName(),
                    decisionDate,
                    resolvedReason
            );
            notificationFactory.buildAndDispatch(NotificationType.MODALITY_CLOSED_BY_COMMITTEE, NotificationRecipientType.STUDENT,
                    student, committeeMember, modality, subject, message
            );

            log.info("Notificación de cierre de modalidad por comité enviada al estudiante {}", student.getId());
        }
    }


    private void handleModalityInvitationSent(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        User invitee = userRepository.findById(event.get(ModalityEvent.KEY_INVITEE_ID, Long.class))
                .orElseThrow(() -> new NotFoundException("Estudiante invitado no encontrado"));

        User inviter = userRepository.findById(event.get(ModalityEvent.KEY_INVITER_ID, Long.class))
                .orElseThrow(() -> new NotFoundException("Estudiante que invita no encontrado"));

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        Long invitationId = event.get(ModalityEvent.KEY_INVITATION_ID, Long.class);

        String subject = "Invitación para unirte a una modalidad de grado grupal – SIGMA";

        String message = NotificationMessageTemplates.modalityInvitationSent(
                invitee.getName(),
                modalidadInfo,
                modality.getAcademicProgram().getName(),
                inviter.getName() + " " + inviter.getLastName(),
                TranslationUtils.formatDateTime(LocalDateTime.now()),
                inviter.getName()
        );

        Notification notification = Notification.builder()
                .type(NotificationType.MODALITY_INVITATION_RECEIVED)
                .recipientType(NotificationRecipientType.STUDENT)
                .recipient(invitee)
                .triggeredBy(inviter)
                .studentModality(modality)
                .invitationId(invitationId)
                .subject(subject)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        notificationFactory.saveAndDispatch(notification);

        log.info("Notificación de invitación a modalidad grupal enviada al estudiante {} por el estudiante {}",
                invitee.getId(), inviter.getId());
    }


    private void handleModalityInvitationAccepted(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        User acceptedBy = userRepository.findById(event.get(ModalityEvent.KEY_ACCEPTED_BY_ID, Long.class))
                .orElseThrow(() -> new NotFoundException("Estudiante que aceptó no encontrado"));

        User leader = userRepository.findById(event.get(ModalityEvent.KEY_LEADER_ID, Long.class))
                .orElseThrow(() -> new NotFoundException("Líder del grupo no encontrado"));

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String subject = "Un estudiante aceptó tu invitación a la modalidad grupal – SIGMA";

        String message = NotificationMessageTemplates.modalityInvitationAccepted(
                leader.getName(),
                acceptedBy.getName() + " " + acceptedBy.getLastName(),
                modalidadInfo,
                modality.getAcademicProgram().getName(),
                TranslationUtils.formatDateTime(LocalDateTime.now())
        );

        notificationFactory.buildAndDispatch(NotificationType.MODALITY_INVITATION_ACCEPTED, NotificationRecipientType.STUDENT,
                leader, acceptedBy, modality, subject, message
        );


        log.info("Notificación de aceptación de invitación enviada al líder {} por el estudiante {}",
                leader.getId(), acceptedBy.getId());
    }


    private void handleModalityInvitationRejected(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        User rejectedBy = userRepository.findById(event.get(ModalityEvent.KEY_REJECTED_BY_ID, Long.class))
                .orElseThrow(() -> new NotFoundException("Estudiante que rechazó no encontrado"));

        User leader = userRepository.findById(event.get(ModalityEvent.KEY_LEADER_ID, Long.class))
                .orElseThrow(() -> new NotFoundException("Líder del grupo no encontrado"));

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String subject = "Un estudiante rechazó tu invitación a la modalidad grupal – SIGMA";

        String message = NotificationMessageTemplates.modalityInvitationRejected(
                leader.getName(),
                rejectedBy.getName() + " " + rejectedBy.getLastName(),
                modalidadInfo,
                modality.getAcademicProgram().getName(),
                TranslationUtils.formatDateTime(LocalDateTime.now()),
                3,
                studentModalityMemberRepository.countByStudentModalityIdAndStatus(
                        modality.getId(),
                        MemberStatus.ACTIVE
                )
        );

        notificationFactory.buildAndDispatch(NotificationType.MODALITY_INVITATION_REJECTED, NotificationRecipientType.STUDENT,
                leader, rejectedBy, modality, subject, message
        );


        log.info("Notificación de rechazo de invitación enviada al líder {} por el estudiante {}",
                leader.getId(), rejectedBy.getId());
    }


    private void handleModalityFinalApprovedByCommittee(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        User committeeMember = userRepository.findById(event.get(ModalityEvent.KEY_COMMITTEE_MEMBER_ID, Long.class))
                .orElseThrow(() -> new NotFoundException("Miembro del comité no encontrado"));

        List<StudentModalityMember> members = activeMembers(modality);

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String subject = "¡Felicitaciones! — Modalidad de Grado Aprobada por el Comité de Currículo";

        // El acta se genera UNA vez (supplier memoizado) aunque haya N destinatarios;
        // los dispatchWithAttachment corren en el executor en paralelo y comparten el cache sincronizado
        Supplier<GeneratedAttachment> attachmentSupplier = new Supplier<>() {
            private GeneratedAttachment cached;

            @Override
            public synchronized GeneratedAttachment get() {
                if (cached == null) {
                    try {
                        StudentModality m = studentModalityRepository.findById(modality.getId())
                                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
                        AcademicCertificate c = certificatePdfService.generateCertificateForCommitteeApproval(m);
                        cached = new GeneratedAttachment(
                                certificatePdfService.getCertificatePath(m.getId()),
                                "ACTA_DE_APROBACION.pdf",
                                c.getId());
                    } catch (IOException e) {
                        throw new RuntimeException("No se pudo generar el acta de aprobación", e);
                    }
                }
                return cached;
            }
        };

        String observations = event.get(ModalityEvent.KEY_OBSERVATIONS, String.class);

        for (StudentModalityMember memberEntry : members) {
            User student = memberEntry.getStudent();

            String message = NotificationMessageTemplates.modalityFinalApprovedByCommittee(
                    student.getName(),
                    student.getLastName(),
                    modalidadInfo,
                    modality.getProgramDegreeModality().getAcademicProgram().getName(),
                    modality.getProgramDegreeModality().getAcademicProgram().getFaculty().getName(),
                    committeeMember.getName(),
                    committeeMember.getLastName(),
                    TranslationUtils.formatDateTime(LocalDateTime.now()),
                    observations != null && !observations.isBlank()
                            ? "Observaciones del Comité: " + observations + ".\n\n"
                            : ""
            );

            Notification notification = notificationFactory.buildAndSave(
                    NotificationType.MODALITY_FINAL_APPROVED_BY_COMMITTEE, NotificationRecipientType.STUDENT,
                    student, committeeMember, modality, subject, message);

            // ponytail: si la generación del acta falla, el dispatcher lo captura (emailSent=false -> outbox);
            // el retry no puede regenerarla (generator no se guarda) y reenviaría el correo simple sin adjunto
            dispatcher.dispatchWithAttachment(notification, attachmentSupplier, certId -> {
                try {
                    certificatePdfService.updateCertificateStatus(certId, CertificateStatus.SENT);
                } catch (Exception e) {
                    log.warn("No se pudo actualizar el estado del certificado: {}", e.getMessage());
                }
            });
            log.info("Acta simplificada enviada al estudiante {} (modalidad ID {})",
                    student.getId(), modality.getId());
        }

        log.info("Notificaciones de aprobación final (comité) enviadas para modalidad ID {}",
                modality.getId());
    }


    private void handleModalityRejectedByCommittee(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        User student = userRepository.findById(event.get(ModalityEvent.KEY_STUDENT_ID, Long.class))
                .orElseThrow(() -> new NotFoundException("Estudiante no encontrado"));

        User committeeMember = userRepository.findById(event.get(ModalityEvent.KEY_COMMITTEE_MEMBER_ID, Long.class))
                .orElseThrow(() -> new NotFoundException("Miembro del comité no encontrado"));

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String subject = "IMPORTANTE: Modalidad de Grado NO APROBADA - Decisión del Comité";

        String reason = event.get(ModalityEvent.KEY_REASON, String.class);

        String message = NotificationMessageTemplates.modalityRejectedByCommittee(
                student.getName(),
                modalidadInfo,
                modality.getAcademicProgram().getName(),
                TranslationUtils.formatDateTime(LocalDateTime.now()),
                reason != null && !reason.isBlank()
                        ? reason
                        : "No se registran motivos adicionales."
        );

        notificationFactory.buildAndDispatch(NotificationType.MODALITY_REJECTED_BY_COMMITTEE, NotificationRecipientType.STUDENT,
                student, committeeMember, modality, subject, message
        );

    }


    private void handleSeminarStarted(ModalityEvent event) {
        String seminarName = event.get(ModalityEvent.KEY_SEMINAR_NAME, String.class);
        String recipientName = event.get(ModalityEvent.KEY_RECIPIENT_NAME, String.class);
        String recipientEmail = event.get(ModalityEvent.KEY_RECIPIENT_EMAIL, String.class);
        String programName = event.get(ModalityEvent.KEY_PROGRAM_NAME, String.class);
        LocalDateTime startDate = event.get(ModalityEvent.KEY_START_DATE, LocalDateTime.class);
        Integer totalHours = event.get(ModalityEvent.KEY_TOTAL_HOURS, Integer.class, 0);

        String subject = "Inicio de Seminario: " + seminarName;

        String message = NotificationMessageTemplates.seminarStarted(
                recipientName,
                seminarName,
                programName,
                TranslationUtils.formatDateTime(startDate),
                totalHours
        );

        User recipient = userRepository.findByEmail(recipientEmail).orElse(null);

        if (recipient != null) {
            notificationFactory.buildAndDispatch(NotificationType.SEMINAR_STARTED, NotificationRecipientType.STUDENT,
                    recipient, null, null, subject, message
            );

        } else {
            log.warn("No se encontró usuario con email {} para notificación de inicio de seminario", recipientEmail);
        }
    }


    private void handleSeminarCancelled(ModalityEvent event) {
        String seminarName = event.get(ModalityEvent.KEY_SEMINAR_NAME, String.class);
        String recipientName = event.get(ModalityEvent.KEY_RECIPIENT_NAME, String.class);
        String recipientEmail = event.get(ModalityEvent.KEY_RECIPIENT_EMAIL, String.class);
        String programName = event.get(ModalityEvent.KEY_PROGRAM_NAME, String.class);
        LocalDateTime cancelledDate = event.get(ModalityEvent.KEY_CANCELLED_DATE, LocalDateTime.class);
        String reason = event.get(ModalityEvent.KEY_REASON, String.class);

        String subject = "Cancelación de Seminario: " + seminarName;

        String message = NotificationMessageTemplates.seminarCancelled(
                recipientName,
                seminarName,
                programName,
                TranslationUtils.formatDateTime(cancelledDate),
                reason != null ? "\nMotivo: " + reason : ""
        );

        User recipient = userRepository.findByEmail(recipientEmail).orElse(null);

        if (recipient != null) {
            notificationFactory.buildAndDispatch(NotificationType.SEMINAR_CANCELLED, NotificationRecipientType.STUDENT,
                    recipient, null, null, subject, message
            );

        } else {
            log.warn("No se encontró usuario con email {} para notificación de cancelación de seminario", recipientEmail);
        }
    }


    private void handleModalityApprovedByExaminers(ModalityEvent event) {
        StudentModality modality = studentModalityRepository
                .findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        User examiner = userRepository
                .findById(event.get(ModalityEvent.KEY_EXAMINER_ID, Long.class))
                .orElseThrow(() -> new NotFoundException("Jurado no encontrado"));

        String subject = "Notificación oficial – Modalidad aprobada por jurado evaluador";

        String modalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String programName = modality.getAcademicProgram().getName();
        String approvalDate = TranslationUtils.formatDateTime(LocalDateTime.now());

        dispatchToActiveMembers(modality, NotificationType.MODALITY_APPROVED_BY_EXAMINERS, examiner, subject,
                student -> NotificationMessageTemplates.modalityApprovedByExaminers(
                        student.getName(),
                        modalityName,
                        programName,
                        approvalDate,
                        null
                ));
    }


    private void handleExaminersAssigned(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        List<DefenseExaminer> examiners = modality.getDefenseExaminers();
        String jurados = examiners.stream()
                .map(e -> e.getExaminer().getName() + " " + e.getExaminer().getLastName() + " (" + TranslationUtils.translateExaminerType(e.getExaminerType()) + ")")
                .toList()
                .isEmpty() ? "-" : String.join(", ", examiners.stream()
                .map(e -> e.getExaminer().getName() + " " + e.getExaminer().getLastName() + " (" + TranslationUtils.translateExaminerType(e.getExaminerType()) + ")")
                .toList());
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Asignación de jurados evaluadores a tu modalidad de grado";
        String programName = modality.getProgramDegreeModality().getAcademicProgram().getName();
        String assignmentDate = TranslationUtils.formatDateTime(LocalDateTime.now());

        dispatchToActiveMembers(modality, NotificationType.EXAMINER_ASSIGNED, null, subject,
                student -> NotificationMessageTemplates.examinersAssigned(
                        student.getName() + " " + student.getLastName(),
                        modalidadInfo,
                        programName,
                        jurados,
                        assignmentDate
                ));
    }


    private void handleDocumentEditResolved(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        boolean approved = Boolean.TRUE.equals(event.get(ModalityEvent.KEY_APPROVED, Boolean.class));
        NotificationType type = approved ? NotificationType.DOCUMENT_EDIT_APPROVED : NotificationType.DOCUMENT_EDIT_REJECTED;
        String subject = approved
                ? "Solicitud de edición de documento aprobada"
                : "Solicitud de edición de documento rechazada";

        String documentName = event.get(ModalityEvent.KEY_DOCUMENT_NAME, String.class);
        String resolutionNotes = event.get(ModalityEvent.KEY_RESOLUTION_NOTES, String.class);
        String resolutionText = resolutionNotes != null && !resolutionNotes.isBlank()
                ? resolutionNotes
                : "No se registran observaciones adicionales.";

        dispatchToActiveMembers(modality, type, null, subject,
                student -> approved
                        ? NotificationMessageTemplates.documentEditApproved(student.getName(), documentName, resolutionText)
                        : NotificationMessageTemplates.documentEditRejected(student.getName(), documentName, resolutionText));
    }


    private List<StudentModalityMember> activeMembers(StudentModality modality) {
        return studentModalityMemberRepository.findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
    }

    private void dispatchToActiveMembers(StudentModality modality, NotificationType type, User triggeredBy,
                                         String subject, Function<User, String> message) {
        for (StudentModalityMember member : activeMembers(modality)) {
            User student = member.getStudent();
            notificationFactory.buildAndDispatch(type, NotificationRecipientType.STUDENT, student, triggeredBy,
                    modality, subject, message.apply(student));
        }
    }

}

