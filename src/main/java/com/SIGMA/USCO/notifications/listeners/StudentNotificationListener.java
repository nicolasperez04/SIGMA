package com.SIGMA.USCO.notifications.listeners;

import com.SIGMA.USCO.Modalities.Entity.AcademicCertificate;
import com.SIGMA.USCO.Modalities.Entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.Entity.enums.CertificateStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.notifications.entity.Notification;
import com.SIGMA.USCO.common.util.TranslationUtils;
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.event.ModalityEvent;
import com.SIGMA.USCO.notifications.service.AcademicCertificatePdfService;
import com.SIGMA.USCO.notifications.service.NotificationBuilderHelper;
import com.SIGMA.USCO.notifications.service.NotificationDispatcherService;
import com.SIGMA.USCO.notifications.service.NotificationFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

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
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
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
        ).orElseThrow();
        StudentModality modality = document.getStudentModality();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String subject = "Correcciones solicitadas en documento académico – Acción requerida";
        NotificationRecipientType requestedBy = event.get(ModalityEvent.KEY_REQUESTED_BY, NotificationRecipientType.class);
        String observations = event.get(ModalityEvent.KEY_OBSERVATIONS, String.class);
        for (var member : members) {
            User student = member.getStudent();

            String requestedByText;
            if (requestedBy == NotificationRecipientType.PROGRAM_HEAD) {
                requestedByText = "la Jefatura de Programa y/o Coordinación de Modalidades";
            } else if (requestedBy == NotificationRecipientType.EXAMINER) {
                requestedByText = "un jurado evaluador";
            } else {
                requestedByText = "el Comité de Currículo del Programa";
            }

            String message = NotificationMessageTemplates.correctionsRequested(
                    student.getName(),
                    requestedByText,
                    document.getDocumentConfig().getDocumentName(),
                    observations != null && !observations.isBlank()
                            ? observations
                            : "No se registraron observaciones adicionales."
            );
            notificationFactory.buildAndDispatch(NotificationType.DOCUMENT_CORRECTIONS_REQUESTED, NotificationRecipientType.STUDENT, student, modality, subject, message);

        }
    }


    private void handleCancellationRequested(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                sm.getId(),
                MemberStatus.ACTIVE
        );
        String subject = "Solicitud de cancelación registrada – Modalidad de grado";
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(sm);
        for (var member : members) {
            User student = member.getStudent();
            String message = NotificationMessageTemplates.cancellationRequested(student.getName(), modalidadInfo);
            notificationFactory.buildAndDispatch(NotificationType.MODALITY_CANCELLATION_REQUESTED, NotificationRecipientType.STUDENT, student, sm, subject, message);

        }
    }


    private void handleCancellationApproved(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                sm.getId(),
                MemberStatus.ACTIVE
        );
        String subject = "Cancelación aprobada – Modalidad de grado";
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(sm);
        for (var member : members) {
            User student = member.getStudent();
            String message = NotificationMessageTemplates.cancellationApproved(student.getName(), modalidadInfo);
            notificationFactory.buildAndDispatch(NotificationType.MODALITY_CANCELLATION_APPROVED, NotificationRecipientType.STUDENT, student, sm, subject, message);

        }
    }


    private void handleCancellationRejected(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                sm.getId(),
                MemberStatus.ACTIVE
        );
        String subject = "Cancelación no aprobada – Modalidad de grado";
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(sm);
        String reason = event.get(ModalityEvent.KEY_REASON, String.class);
        for (var member : members) {
            User student = member.getStudent();
            String message = NotificationMessageTemplates.cancellationRejected(
                    student.getName(),
                    modalidadInfo,
                    reason != null && !reason.isBlank()
                            ? reason
                            : "No se especifican motivos adicionales."
            );
            notificationFactory.buildAndDispatch(NotificationType.MODALITY_CANCELLATION_REJECTED, NotificationRecipientType.STUDENT, student, sm, subject, message);

        }
    }


    private void handleDefenseScheduled(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        User director = modality.getProjectDirector();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String studentSubject = "Sustentación programada – Modalidad de Grado";
        Object defenseDate = event.get(ModalityEvent.KEY_DEFENSE_DATE, Object.class);
        String defenseLocation = event.get(ModalityEvent.KEY_DEFENSE_LOCATION, String.class);
        for (var member : members) {
            User student = member.getStudent();
            String studentMessage = NotificationMessageTemplates.defenseScheduled(
                    student.getName(),
                    modalidadInfo,
                    String.valueOf(defenseDate),
                    defenseLocation,
                    director != null
                            ? director.getName() + " " + director.getLastName()
                            : "No asignado"
            );
            notificationFactory.buildAndDispatch(NotificationType.DEFENSE_SCHEDULED, NotificationRecipientType.STUDENT, student, modality, studentSubject, studentMessage);

        }
    }


    private void handleDirectorAssigned(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        User director = userRepository.findById(event.get(ModalityEvent.KEY_DIRECTOR_ID, Long.class))
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String studentSubject = "Director de proyecto asignado – Modalidad de grado";
        for (var member : members) {
            User student = member.getStudent();
            String studentMessage = NotificationMessageTemplates.directorAssigned(
                    student.getName(),
                    modalidadInfo,
                    director.getName() + " " + director.getLastName(),
                    director.getEmail()
            );
            notificationFactory.buildAndDispatch(NotificationType.DIRECTOR_ASSIGNED, NotificationRecipientType.STUDENT, student, modality, studentSubject, studentMessage);

        }
    }


    private void handleDefenseResult(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();

        ModalityProcessStatus finalStatus = event.get(ModalityEvent.KEY_FINAL_STATUS, ModalityProcessStatus.class);
        boolean approved = finalStatus == ModalityProcessStatus.GRADED_APPROVED;
        boolean approvedPendingCommitteeReview = finalStatus == ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW;
        boolean shouldSendCertificate = approved || approvedPendingCommitteeReview;

        String studentSubject = shouldSendCertificate
                ? (approvedPendingCommitteeReview
                ? "Resultado de sustentación – Modalidad aprobada (distinción en revisión del comité)"
                : "Resultado final de sustentación – Modalidad aprobada")
                : "Resultado final de sustentación – Modalidad no aprobada";

        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(), MemberStatus.ACTIVE
        );

        if (members == null || members.isEmpty()) {
            StudentModalityMember syntheticLeaderMember = StudentModalityMember.builder()
                    .studentModality(modality)
                    .student(modality.getLeader())
                    .status(MemberStatus.ACTIVE)
                    .isLeader(true)
                    .build();
            members = List.of(syntheticLeaderMember);
        }

        AcademicCertificate certificate = null;
        Path pdfPath = null;
        if (shouldSendCertificate) {
            try {
                log.info("Generando acta de aprobación para la modalidad ID: {}", modality.getId());

                boolean isComplete = isCompleteModality(modality);
                if (isComplete) {
                    certificate = certificatePdfService.generateCertificate(modality);
                } else {
                    certificate = certificatePdfService.generateCertificateForCommitteeApproval(modality);
                }
                pdfPath = certificatePdfService.getCertificatePath(modality.getId());
                log.info("Acta PDF generada exitosamente para la modalidad ID: {}", modality.getId());
            } catch (Exception e) {
                log.error("Error generando acta PDF para modalidad ID {}: {}", modality.getId(), e.getMessage(), e);
            }
        }

        User leader = modality.getLeader();
        if (leader != null) {
            String leaderMessage = shouldSendCertificate
                    ? buildApprovedStudentMessage(leader, modality, event)
                    : buildRejectedStudentMessage(leader, modality, event);

            Notification leaderNotification = notificationFactory.buildAndSave(
                    NotificationType.DEFENSE_COMPLETED, NotificationRecipientType.STUDENT,
                    leader, null, modality, studentSubject, leaderMessage);

if (shouldSendCertificate && pdfPath != null) {
                // dispatchWithAttachment es @Async; fallos de email se manejan dentro (emailSent=false + log)
                dispatcher.dispatchWithAttachment(
                        leaderNotification,
                        pdfPath,
                        "ACTA_DE_APROBACION.pdf"
                );
                log.info("Acta enviada al líder {} (modalidad ID {})", leader.getId(), modality.getId());
            } else {
                dispatcher.dispatch(leaderNotification);
            }
        }

        for (StudentModalityMember member : members) {
            if (leader != null && member.getStudent().getId().equals(leader.getId())) {
                continue;
            }

            User student = member.getStudent();
            String studentMessage = shouldSendCertificate
                    ? buildApprovedStudentMessage(student, modality, event)
                    : buildRejectedStudentMessage(student, modality, event);

            notificationFactory.buildAndDispatch(
                    NotificationType.DEFENSE_COMPLETED, NotificationRecipientType.STUDENT,
                    student, null, modality, studentSubject, studentMessage);
            log.info("Notificación enviada al miembro {} (modalidad ID {}, sin acta adjunta)", student.getId(), modality.getId());
        }

        if (certificate != null && shouldSendCertificate) {
            try {
                certificatePdfService.updateCertificateStatus(certificate.getId(), CertificateStatus.SENT);
                log.info("Estado del certificado actualizado a SENT para modalidad ID: {}", modality.getId());
            } catch (Exception e) {
                log.error("Error actualizando estado del certificado: {}", e.getMessage());
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
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Modalidad de grado aprobada – Comité de Currículo";
        for (var member : members) {
            User student = member.getStudent();
            String message = NotificationMessageTemplates.modalityApprovedByCommittee(
                    student.getName(),
                    modalidadInfo,
                    modality.getProjectDirector() != null
                            ? modality.getProjectDirector().getName() + " " +
                            modality.getProjectDirector().getLastName()
                            : "No se registra director asignado.",
                    String.valueOf(modality.getUpdatedAt())
            );
            notificationFactory.buildAndDispatch(NotificationType.MODALITY_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE, NotificationRecipientType.STUDENT, student, modality, subject, message);

        }
    }


    private void handleModalityApprovedByProgramHead(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Modalidad de grado aprobada – Jefatura de Programa y/o Coordinación de Modalidades";
        for (var member : members) {
            User student = member.getStudent();
            String message = NotificationMessageTemplates.modalityApprovedByProgramHead(student.getName(), modalidadInfo);
            notificationFactory.buildAndDispatch(NotificationType.MODALITY_APPROVED_BY_PROGRAM_HEAD, NotificationRecipientType.STUDENT, student, modality, subject, message);

        }
    }


    private void handleCorrectionDeadlineReminder(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        Integer daysRemaining = event.get(ModalityEvent.KEY_DAYS_REMAINING, Integer.class, 0);
        Object deadline = event.get(ModalityEvent.KEY_DEADLINE, Object.class);
        String subject = "Recordatorio oficial – Plazo de correcciones (%d días restantes)"
                .formatted(daysRemaining);
        for (var member : members) {
            User student = member.getStudent();
            String message = NotificationMessageTemplates.correctionDeadlineReminder(
                    student.getName(),
                    modalidadInfo,
                    daysRemaining,
                    String.valueOf(deadline)
            );
            notificationFactory.buildAndDispatch(NotificationType.CORRECTION_DEADLINE_REMINDER, NotificationRecipientType.STUDENT, student, modality, subject, message);

            log.info("Recordatorio de plazo de corrección enviado al estudiante {}", student.getId());
        }
    }


    private void handleCorrectionDeadlineExpired(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Notificación oficial – Cancelación automática de modalidad por vencimiento de plazo";
        Object requestDate = event.get(ModalityEvent.KEY_REQUEST_DATE, Object.class);
        for (var member : members) {
            User student = member.getStudent();
            String message = NotificationMessageTemplates.correctionDeadlineExpired(
                    student.getName(),
                    modalidadInfo,
                    String.valueOf(requestDate)
            );
            notificationFactory.buildAndDispatch(NotificationType.CORRECTION_DEADLINE_EXPIRED, NotificationRecipientType.STUDENT, student, modality, subject, message);

            log.info("Notificación de cancelación por vencimiento enviada al estudiante {}", student.getId());
        }
    }


    private void handleCorrectionResubmitted(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Notificación oficial – Documento corregido recibido";
        String documentName = event.get(ModalityEvent.KEY_DOCUMENT_NAME, String.class);
        for (var member : members) {
            User student = member.getStudent();
            String message = NotificationMessageTemplates.correctionResubmitted(
                    student.getName(),
                    modalidadInfo,
                    documentName,
                    LocalDateTime.now().toLocalDate().toString()
            );
            notificationFactory.buildAndDispatch(NotificationType.CORRECTION_RESUBMITTED, NotificationRecipientType.STUDENT, student, modality, subject, message);

            log.info("Notificación de resubmisión de corrección enviada al estudiante {}", student.getId());
        }
    }


    private void handleCorrectionApproved(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Notificación oficial – Correcciones aprobadas";
        String documentName = event.get(ModalityEvent.KEY_DOCUMENT_NAME, String.class);
        for (var member : members) {
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
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Notificación oficial – Cancelación de modalidad por rechazo definitivo de correcciones";
        String documentName = event.get(ModalityEvent.KEY_DOCUMENT_NAME, String.class);
        String reason = event.get(ModalityEvent.KEY_REASON, String.class);
        for (var member : members) {
            User student = member.getStudent();
            String message = NotificationMessageTemplates.correctionRejectedFinal(
                    student.getName(),
                    modalidadInfo,
                    documentName,
                    reason != null && !reason.isBlank()
                            ? reason
                            : "No se registran motivos adicionales."
            );
            notificationFactory.buildAndDispatch(NotificationType.CORRECTION_REJECTED_FINAL, NotificationRecipientType.STUDENT,
                    student, modality, subject, message
            );

            log.info("Notificación de rechazo final de corrección enviada al estudiante {}", student.getId());
        }
    }


    private void handleModalityClosedByCommittee(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        User committeeMember = userRepository.findById(event.get(ModalityEvent.KEY_COMMITTEE_MEMBER_ID, Long.class))
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Notificación oficial – Cierre de modalidad por decisión del Comité de Currículo";
        String reason = event.get(ModalityEvent.KEY_REASON, String.class);
        for (var member : members) {
            User student = member.getStudent();
            String message = NotificationMessageTemplates.modalityClosedByCommittee(
                    student.getName(),
                    modalidadInfo,
                    modality.getAcademicProgram().getName(),
                    committeeMember.getName(),
                    committeeMember.getLastName(),
                    LocalDateTime.now().toString(),
                    reason != null && !reason.isBlank()
                            ? reason
                            : "No se registran motivos adicionales."
            );
            notificationFactory.buildAndDispatch(NotificationType.MODALITY_CLOSED_BY_COMMITTEE, NotificationRecipientType.STUDENT,
                    student, committeeMember, modality, subject, message
            );

            log.info("Notificación de cierre de modalidad por comité enviada al estudiante {}", student.getId());
        }
    }


    private void handleModalityInvitationSent(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User invitee = userRepository.findById(event.get(ModalityEvent.KEY_INVITEE_ID, Long.class))
                .orElseThrow(() -> new RuntimeException("Estudiante invitado no encontrado"));

        User inviter = userRepository.findById(event.get(ModalityEvent.KEY_INVITER_ID, Long.class))
                .orElseThrow(() -> new RuntimeException("Estudiante que invita no encontrado"));

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        Long invitationId = event.get(ModalityEvent.KEY_INVITATION_ID, Long.class);

        String subject = "Invitación para unirte a una modalidad de grado grupal – SIGMA";

        String message = NotificationMessageTemplates.modalityInvitationSent(
                invitee.getName(),
                modalidadInfo,
                modality.getAcademicProgram().getName(),
                inviter.getName() + " " + inviter.getLastName(),
                LocalDateTime.now().toString(),
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
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User acceptedBy = userRepository.findById(event.get(ModalityEvent.KEY_ACCEPTED_BY_ID, Long.class))
                .orElseThrow(() -> new RuntimeException("Estudiante que aceptó no encontrado"));

        User leader = userRepository.findById(event.get(ModalityEvent.KEY_LEADER_ID, Long.class))
                .orElseThrow(() -> new RuntimeException("Líder del grupo no encontrado"));

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String subject = "Un estudiante aceptó tu invitación a la modalidad grupal – SIGMA";

        String message = NotificationMessageTemplates.modalityInvitationAccepted(
                leader.getName(),
                acceptedBy.getName() + " " + acceptedBy.getLastName(),
                modalidadInfo,
                modality.getAcademicProgram().getName(),
                LocalDateTime.now().toString()
        );

        notificationFactory.buildAndDispatch(NotificationType.MODALITY_INVITATION_ACCEPTED, NotificationRecipientType.STUDENT,
                leader, acceptedBy, modality, subject, message
        );


        log.info("Notificación de aceptación de invitación enviada al líder {} por el estudiante {}",
                leader.getId(), acceptedBy.getId());
    }


    private void handleModalityInvitationRejected(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User rejectedBy = userRepository.findById(event.get(ModalityEvent.KEY_REJECTED_BY_ID, Long.class))
                .orElseThrow(() -> new RuntimeException("Estudiante que rechazó no encontrado"));

        User leader = userRepository.findById(event.get(ModalityEvent.KEY_LEADER_ID, Long.class))
                .orElseThrow(() -> new RuntimeException("Líder del grupo no encontrado"));

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String subject = "Un estudiante rechazó tu invitación a la modalidad grupal – SIGMA";

        String message = NotificationMessageTemplates.modalityInvitationRejected(
                leader.getName(),
                rejectedBy.getName() + " " + rejectedBy.getLastName(),
                modalidadInfo,
                modality.getAcademicProgram().getName(),
                LocalDateTime.now().toString(),
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
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User committeeMember = userRepository.findById(event.get(ModalityEvent.KEY_COMMITTEE_MEMBER_ID, Long.class))
                .orElseThrow(() -> new RuntimeException("Miembro del comité no encontrado"));

        List<StudentModalityMember> activeMembers = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String subject = "¡Felicitaciones! — Modalidad de Grado Aprobada por el Comité de Currículo";

        AcademicCertificate certificate = null;
        Path pdfPath = null;
        try {
            log.info("Generando acta simplificada (comité) para la modalidad ID: {}", modality.getId());
            certificate = certificatePdfService.generateCertificateForCommitteeApproval(modality);
            pdfPath = certificatePdfService.getCertificatePath(modality.getId());
            log.info("Acta simplificada generada exitosamente: {}", pdfPath);
        } catch (IOException e) {
            log.error("Error generando acta simplificada para modalidad ID {}: {}",
                    modality.getId(), e.getMessage(), e);
        }

        String observations = event.get(ModalityEvent.KEY_OBSERVATIONS, String.class);

        for (StudentModalityMember memberEntry : activeMembers) {
            User student = memberEntry.getStudent();

            String message = NotificationMessageTemplates.modalityFinalApprovedByCommittee(
                    student.getName(),
                    student.getLastName(),
                    modalidadInfo,
                    modality.getProgramDegreeModality().getAcademicProgram().getName(),
                    modality.getProgramDegreeModality().getAcademicProgram().getFaculty().getName(),
                    committeeMember.getName(),
                    committeeMember.getLastName(),
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern(
                            "d 'de' MMMM 'de' yyyy", java.util.Locale.forLanguageTag("es-CO"))),
                    observations != null && !observations.isBlank()
                            ? "Observaciones del Comité: " + observations + ".\n\n"
                            : ""
            );

            Notification notification = notificationFactory.buildAndSave(
                    NotificationType.MODALITY_FINAL_APPROVED_BY_COMMITTEE, NotificationRecipientType.STUDENT,
                    student, committeeMember, modality, subject, message);

if (pdfPath != null) {
                // dispatchWithAttachment es @Async; fallos de email se manejan dentro (emailSent=false + log)
                dispatcher.dispatchWithAttachment(notification, pdfPath, "ACTA_DE_APROBACION.pdf");
                log.info("Acta simplificada enviada al estudiante {} (modalidad ID {})",
                        student.getId(), modality.getId());
            } else {
                dispatcher.dispatch(notification);
            }
        }

        if (certificate != null) {
            try {
                certificatePdfService.updateCertificateStatus(certificate.getId(), CertificateStatus.SENT);
            } catch (Exception e) {
                log.warn("No se pudo actualizar el estado del certificado: {}", e.getMessage());
            }
        }

        log.info("Notificaciones de aprobación final (comité) enviadas para modalidad ID {}",
                modality.getId());
    }


    private void handleModalityRejectedByCommittee(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User student = userRepository.findById(event.get(ModalityEvent.KEY_STUDENT_ID, Long.class))
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        User committeeMember = userRepository.findById(event.get(ModalityEvent.KEY_COMMITTEE_MEMBER_ID, Long.class))
                .orElseThrow(() -> new RuntimeException("Miembro del comité no encontrado"));

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String subject = "IMPORTANTE: Modalidad de Grado NO APROBADA - Decisión del Comité";

        String reason = event.get(ModalityEvent.KEY_REASON, String.class);

        String message = NotificationMessageTemplates.modalityRejectedByCommittee(
                student.getName(),
                modalidadInfo,
                modality.getAcademicProgram().getName(),
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
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
        Object startDate = event.get(ModalityEvent.KEY_START_DATE, Object.class);
        Integer totalHours = event.get(ModalityEvent.KEY_TOTAL_HOURS, Integer.class, 0);

        String subject = "Inicio de Seminario: " + seminarName;

        String message = NotificationMessageTemplates.seminarStarted(
                recipientName,
                seminarName,
                programName,
                String.valueOf(startDate),
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
        Object cancelledDate = event.get(ModalityEvent.KEY_CANCELLED_DATE, Object.class);
        String reason = event.get(ModalityEvent.KEY_REASON, String.class);

        String subject = "Cancelación de Seminario: " + seminarName;

        String message = NotificationMessageTemplates.seminarCancelled(
                recipientName,
                seminarName,
                programName,
                String.valueOf(cancelledDate),
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
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User examiner = userRepository
                .findById(event.get(ModalityEvent.KEY_EXAMINER_ID, Long.class))
                .orElseThrow(() -> new RuntimeException("Jurado no encontrado"));

        var members = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(
                        modality.getId(),
                        MemberStatus.ACTIVE
                );

        String subject = "Notificación oficial – Modalidad aprobada por jurado evaluador";

        String modalityName = modality.getProgramDegreeModality().getDegreeModality().getName();

        for (var member : members) {
            User student = member.getStudent();

            String personalizedMessage = NotificationMessageTemplates.modalityApprovedByExaminers(
                    student.getName(),
                    modalityName,
                    modality.getAcademicProgram().getName(),
                    LocalDateTime.now().toString()
            );

            notificationFactory.buildAndDispatch(NotificationType.MODALITY_APPROVED_BY_EXAMINERS, NotificationRecipientType.STUDENT,
                    student, examiner, modality, subject, personalizedMessage
            );

        }
    }


    private void handleExaminersAssigned(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));
        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
        List<DefenseExaminer> examiners = modality.getDefenseExaminers();
        String jurados = examiners.stream()
                .map(e -> e.getExaminer().getName() + " " + e.getExaminer().getLastName() + " (" + TranslationUtils.translateExaminerType(e.getExaminerType()) + ")")
                .toList()
                .isEmpty() ? "-" : String.join(", ", examiners.stream()
                .map(e -> e.getExaminer().getName() + " " + e.getExaminer().getLastName() + " (" + TranslationUtils.translateExaminerType(e.getExaminerType()) + ")")
                .toList());
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Asignación de jurados evaluadores a tu modalidad de grado";

        for (StudentModalityMember member : members) {
            User student = member.getStudent();
            String message = NotificationMessageTemplates.examinersAssigned(
                    student.getName() + " " + student.getLastName(),
                    modalidadInfo,
                    modality.getProgramDegreeModality().getAcademicProgram().getName(),
                    jurados,
                    LocalDateTime.now().toString()
            );

            notificationFactory.buildAndDispatch(NotificationType.EXAMINER_ASSIGNED, NotificationRecipientType.STUDENT,
                    student, modality, subject, message
            );

        }
    }


    private void handleDocumentEditResolved(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        List<StudentModalityMember> members = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);

        boolean approved = Boolean.TRUE.equals(event.get(ModalityEvent.KEY_APPROVED, Boolean.class));
        NotificationType type = approved ? NotificationType.DOCUMENT_EDIT_APPROVED : NotificationType.DOCUMENT_EDIT_REJECTED;
        String subject = approved
                ? "Solicitud de edición de documento aprobada"
                : "Solicitud de edición de documento rechazada";

        String documentName = event.get(ModalityEvent.KEY_DOCUMENT_NAME, String.class);
        String resolutionNotes = event.get(ModalityEvent.KEY_RESOLUTION_NOTES, String.class);

        for (StudentModalityMember member : members) {
            User student = member.getStudent();
            String resolutionText = resolutionNotes != null && !resolutionNotes.isBlank()
                    ? resolutionNotes
                    : "No se registran observaciones adicionales.";
            String message = approved
                    ? NotificationMessageTemplates.documentEditApproved(student.getName(), documentName, resolutionText)
                    : NotificationMessageTemplates.documentEditRejected(student.getName(), documentName, resolutionText);

            notificationFactory.buildAndDispatch(type, NotificationRecipientType.STUDENT,
                    student, modality, subject, message
            );

        }
    }


    private boolean isCompleteModality(StudentModality modality) {
        boolean hasDefenseDate = modality.getDefenseDate() != null;
        boolean hasExaminers = modality.getDefenseExaminers() != null && !modality.getDefenseExaminers().isEmpty();
        boolean hasDirector = modality.getProjectDirector() != null;
        return hasDefenseDate || hasExaminers || hasDirector;
    }

}

