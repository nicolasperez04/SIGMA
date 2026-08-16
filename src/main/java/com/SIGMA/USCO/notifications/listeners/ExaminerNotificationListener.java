package com.SIGMA.USCO.notifications.listeners;

import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.notifications.entity.Notification;
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.Modalities.event.ModalityEvent;
import com.SIGMA.USCO.notifications.service.NotificationBuilderHelper;
import com.SIGMA.USCO.notifications.service.NotificationDispatcherService;
import com.SIGMA.USCO.notifications.service.NotificationFactory;
import com.SIGMA.USCO.Modalities.entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.repository.DefenseExaminerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.SIGMA.USCO.Modalities.entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.notifications.service.ExaminerCertificatePdfService;
import com.SIGMA.USCO.Modalities.entity.ExaminerCertificate;
import com.SIGMA.USCO.Modalities.entity.enums.CertificateStatus;
import com.SIGMA.USCO.Modalities.repository.ExaminerCertificateRepository;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.shared.util.TranslationUtils;
import java.nio.file.Path;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExaminerNotificationListener {

    private final DefenseExaminerRepository defenseExaminerRepository;
    private final NotificationFactory notificationFactory;
    private final NotificationDispatcherService dispatcher;
    private final UserRepository userRepository;
    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final ExaminerCertificatePdfService examinerCertificatePdfService;
    private final ExaminerCertificateRepository examinerCertificateRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleEvent(ModalityEvent event) {
        try {
            switch (event.getType()) {
                case READY_FOR_DEFENSE_REQUESTED -> handleDefenseReadyByDirectorEvent(event);
                case EXAMINER_FINAL_REVIEW_COMPLETED -> handleExaminerFinalReviewCompletedEvent(event);
                case DEFENSE_SCHEDULED -> handleDefenseScheduled(event);
                case DOCUMENT_EDIT_REQUESTED -> onDocumentEditRequested(event);
                case CORRECTION_RESUBMITTED -> onCorrectionResubmitted(event);
                case DEFENSE_COMPLETED -> onFinalDefenseApproved(event);
                case EXAMINER_ASSIGNED -> notifyExaminersAssignment(event.getStudentModalityId());
                default -> log.warn("Unhandled ModalityEvent type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error en ExaminerNotificationListener procesando evento {} (studentModalityId={})",
                    event.getType(), event.getStudentModalityId(), e);
            throw e;
        }
    }

    public void notifyExaminersAssignment(Long studentModalityId) {
        StudentModality modality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        List<DefenseExaminer> examiners = defenseExaminerRepository.findByStudentModalityId(studentModalityId);

        // Obtener todos los miembros ACTIVOS de la modalidad
        List<StudentModalityMember> members = activeMembers(modality);

        String studentsString = members.isEmpty()
                ? (modality.getLeader() != null
                        ? modality.getLeader().getName() + " " + modality.getLeader().getLastName()
                                + " (" + modality.getLeader().getEmail() + ")"
                        : "-")
                : members.stream()
                        .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName()
                                + " (" + m.getStudent().getEmail() + ")")
                        .collect(Collectors.joining("\n                        "));

        String directorName = modality.getProjectDirector() != null
                ? modality.getProjectDirector().getName() + " " + modality.getProjectDirector().getLastName()
                : "No asignado";

        String programName = modality.getProgramDegreeModality().getAcademicProgram().getName();
        String facultyName = modality.getProgramDegreeModality().getAcademicProgram().getFaculty() != null
                ? modality.getProgramDegreeModality().getAcademicProgram().getFaculty().getName()
                : "";

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        for (DefenseExaminer examinerAssignment : examiners) {
            User examiner = examinerAssignment.getExaminer();

            String examinerRoleLabel = TranslationUtils.translateExaminerType(examinerAssignment.getExaminerType());

            String subject = NotificationMessageTemplates.EXAMINER_DESIGNATION_SUBJECT;

            String message = NotificationMessageTemplates.examinerDesignation(
                    examiner.getName(),
                    examiner.getLastName(),
                    examinerRoleLabel,
                    modalidadInfo,
                    programName,
                    facultyName,
                    studentsString,
                    directorName,
                    TranslationUtils.formatDateTime(LocalDateTime.now())
            );

            notificationFactory.buildAndDispatch(NotificationType.EXAMINER_ASSIGNED,
                    NotificationRecipientType.EXAMINER,
                    examiner, modality, subject, message);
        }

        // ── Construir resumen de jurados asignados para el mensaje de estudiantes y director ──
        String examinersListForOthers = examiners.stream()
                .map(e -> {
                    String roleLabel = TranslationUtils.translateExaminerType(e.getExaminerType());
                    return "- " + e.getExaminer().getName() + " " + e.getExaminer().getLastName()
                            + " (" + roleLabel + ")";
                })
                .collect(Collectors.joining("\n"));

        // ── Notificar al director de proyecto si está asignado ──
        User director = modality.getProjectDirector();
        if (director != null) {
            String directorSubject = NotificationMessageTemplates.DIRECTOR_EXAMINERS_ASSIGNED_SUBJECT;

            String directorMessage = NotificationMessageTemplates.directorExaminersAssigned(
                    director.getName() + " " + director.getLastName(),
                    modalidadInfo,
                    programName,
                    facultyName,
                    studentsString,
                    (examinersListForOthers != null && !examinersListForOthers.isBlank())
                            ? examinersListForOthers
                            : "Pendiente de asignación",
                    TranslationUtils.formatDateTime(LocalDateTime.now())
            );

            notificationFactory.buildAndDispatch(NotificationType.EXAMINER_ASSIGNED,
                    NotificationRecipientType.PROJECT_DIRECTOR,
                    director, modality, directorSubject, directorMessage);
        }
    }

    private void handleDefenseReadyByDirectorEvent(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        User examiner = userRepository.findById(event.get(ModalityEvent.KEY_EXAMINER_ID, Long.class))
                .orElseThrow(() -> new NotFoundException("Jurado no encontrado"));

        String miembros = TranslationUtils.getStudentList(modality);

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String subject = NotificationMessageTemplates.EXAMINER_DEFENSE_READY_SUBJECT;

        String message = NotificationMessageTemplates.examinerDefenseReady(
                examiner.getName(),
                examiner.getLastName(),
                miembros,
                modalidadInfo
        );

        notificationFactory.buildAndDispatch(
                NotificationType.READY_FOR_DEFENSE_REQUESTED,
                NotificationRecipientType.EXAMINER,
                examiner, modality, subject, message);
    }

    private void handleExaminerFinalReviewCompletedEvent(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        User director = userRepository.findById(event.get(ModalityEvent.KEY_PROJECT_DIRECTOR_ID, Long.class))
                .orElseThrow(() -> new NotFoundException("Director de proyecto no encontrado"));

        String miembros = TranslationUtils.getStudentList(modality);

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String subject = NotificationMessageTemplates.EXAMINER_FINAL_REVIEW_COMPLETED_SUBJECT;

        String message = NotificationMessageTemplates.examinerFinalReviewCompleted(
                director.getName(),
                director.getLastName(),
                miembros,
                modalidadInfo
        );

        notificationFactory.buildAndDispatch(
                NotificationType.FINAL_APPROVED,
                NotificationRecipientType.PROJECT_DIRECTOR,
                director, modality, subject, message);
    }

    private void handleDefenseScheduled(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        LocalDateTime defenseDate = event.get(ModalityEvent.KEY_DEFENSE_DATE, LocalDateTime.class);
        String defenseLocation = event.get(ModalityEvent.KEY_DEFENSE_LOCATION, String.class);

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        List<DefenseExaminer> examiners = defenseExaminerRepository.findByStudentModalityId(event.getStudentModalityId());
        for (DefenseExaminer examinerAssignment : examiners) {
            User examiner = examinerAssignment.getExaminer();
            String subject = NotificationMessageTemplates.EXAMINER_DEFENSE_SCHEDULED_SUBJECT;
            String message = NotificationMessageTemplates.examinerDefenseScheduled(
                    examiner.getName(),
                    examiner.getLastName(),
                    modalidadInfo,
                    TranslationUtils.formatDateTime(defenseDate),
                    defenseLocation,
                    modality.getProjectDirector() != null
                            ? modality.getProjectDirector().getName() + " " + modality.getProjectDirector().getLastName()
                            : "Pendiente de asignación",
                    modality.getMembers() != null && !modality.getMembers().isEmpty()
                            ? modality.getMembers().stream()
                            .map(member -> member.getStudent().getName() + " " + member.getStudent().getLastName())
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("")
                            : modality.getLeader().getName() + " " + modality.getLeader().getLastName()
            );

            notificationFactory.buildAndDispatch(
                    NotificationType.DEFENSE_SCHEDULED,
                    NotificationRecipientType.EXAMINER,
                    examiner, modality, subject, message);
        }

    }

    /**
     * Notifica a los jurados asignados a la modalidad cuando un estudiante solicita
     * editar un documento previamente aprobado.
     */
    private void onDocumentEditRequested(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        List<DefenseExaminer> examiners = defenseExaminerRepository.findByStudentModalityId(event.getStudentModalityId());

        String studentNames = TranslationUtils.getStudentList(modality, false);

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String documentName = event.get(ModalityEvent.KEY_DOCUMENT_NAME, String.class);
        Long editRequestId = event.get(ModalityEvent.KEY_EDIT_REQUEST_ID, Long.class);
        String reason = event.get(ModalityEvent.KEY_REASON, String.class);

        String subject = NotificationMessageTemplates.EXAMINER_DOCUMENT_EDIT_REQUESTED_SUBJECT;

        for (DefenseExaminer examinerAssignment : examiners) {
            User examiner = examinerAssignment.getExaminer();
            String message = NotificationMessageTemplates.examinerDocumentEditRequested(
                    examiner.getName(),
                    examiner.getLastName(),
                    modalidadInfo,
                    modality.getProgramDegreeModality().getAcademicProgram().getName(),
                    (studentNames != null && !studentNames.isBlank()) ? studentNames : "No registrado",
                    documentName,
                    editRequestId,
                    reason != null && !reason.isBlank()
                            ? reason
                            : "No se registra motivo"
            );

            notificationFactory.buildAndDispatch(
                    NotificationType.DOCUMENT_EDIT_REQUESTED,
                    NotificationRecipientType.EXAMINER,
                    examiner, modality, subject, message);
        }
    }

    /**
     * Notifica al/los jurado(s) que solicitaron correcciones cuando el estudiante
     * re-sube el documento corregido, para que sepan que ya está disponible.
     */
    @SuppressWarnings("unchecked")
    private void onCorrectionResubmitted(ModalityEvent event) {
        List<Long> examinerIds = event.get(ModalityEvent.KEY_EXAMINER_IDS, List.class);
        if (examinerIds == null || examinerIds.isEmpty()) {
            // Correcciones solicitadas por jefatura/comité (sin jurados) -> nada que notificar
            return;
        }

        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        String studentNames = TranslationUtils.getStudentList(modality, false);
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String programName = modality.getProgramDegreeModality().getAcademicProgram().getName();
        String documentName = event.get(ModalityEvent.KEY_DOCUMENT_NAME, String.class);

        String subject = NotificationMessageTemplates.EXAMINER_CORRECTION_RESUBMITTED_SUBJECT;

        for (Long examinerId : examinerIds) {
            User examiner = userRepository.findById(examinerId)
                    .orElseThrow(() -> new NotFoundException("Jurado no encontrado"));

            String message = NotificationMessageTemplates.examinerCorrectionResubmitted(
                    examiner.getName(),
                    examiner.getLastName(),
                    modalidadInfo,
                    programName,
                    (studentNames != null && !studentNames.isBlank()) ? studentNames : "No registrado",
                    documentName != null ? documentName : "Documento académico"
            );

            notificationFactory.buildAndDispatch(
                    NotificationType.CORRECTION_RESUBMITTED,
                    NotificationRecipientType.EXAMINER,
                    examiner, modality, subject, message);
        }
    }

    /**
     * Genera y envía actas de participación a todos los jurados
     * cuando la sustentación es aprobada y completada.
     */
    private void onFinalDefenseApproved(ModalityEvent event) {
        Long modalityId = event.getStudentModalityId();
        StudentModality modality = studentModalityRepository.findById(modalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        ModalityProcessStatus finalStatus = event.get(ModalityEvent.KEY_FINAL_STATUS, ModalityProcessStatus.class);

        // Solo procesar si fue aprobada
        if (finalStatus == null ||
            !finalStatus.name().contains("APPROVED")) {
            log.debug("Evento de defensa no es aprobatorio, se omite generación de actas para jurados");
            return;
        }

        log.info("Generando actas de participación para jurados de modalidad ID: {}", modalityId);

        // Obtener todos los jurados asignados
        List<DefenseExaminer> examiners = modality.getDefenseExaminers();
        if (examiners == null || examiners.isEmpty()) {
            log.warn("No hay jurados asignados para la modalidad ID: {}", modalityId);
            return;
        }

        // Generar y enviar acta a cada jurado
        for (DefenseExaminer examiner : examiners) {
            try {
                log.info("Generando acta para jurado {} en modalidad ID: {}",
                    examiner.getExaminer().getId(), modalityId);

                // Crear notificación para el jurado
                User examinerUser = examiner.getExaminer();
                String subject = NotificationMessageTemplates.EXAMINER_PARTICIPATION_ACT_SUBJECT;

                String message = buildExaminerParticipationMessage(examinerUser, modality, examiner);

                Notification notification = notificationFactory.buildAndSave(
                        NotificationType.DEFENSE_COMPLETED,
                        NotificationRecipientType.EXAMINER,
                        examinerUser, null, modality, subject, message);

                // El acta se genera LAZY dentro del dispatch async (OSIV OFF), sin bloquear el request
                dispatcher.dispatchWithAttachment(notification,
                        () -> {
                            try {
                                // ponytail: re-cargar modality por id dentro del async (el modality capturado
                                // del listener queda detached tras REQUIRES_NEW; la tx async lo hace managed)
                                StudentModality m = studentModalityRepository.findById(modality.getId())
                                        .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
                                DefenseExaminer de = defenseExaminerRepository.findById(examiner.getId())
                                        .orElseThrow(() -> new NotFoundException("Jurado no encontrado"));
                                ExaminerCertificate c = examinerCertificatePdfService.generateExaminerCertificate(m, de);
                                Path pdf = examinerCertificatePdfService.getCertificatePath(modalityId, de.getExaminer().getId());
                                return new NotificationDispatcherService.GeneratedAttachment(pdf,
                                        "ACTA_JURADO_" + c.getCertificateNumber() + ".pdf",
                                        c.getId());
                            } catch (IOException e) {
                                throw new RuntimeException("No se pudo generar el acta del jurado", e);
                            }
                        },
                        certId -> examinerCertificatePdfService.updateCertificateStatus(certId, CertificateStatus.SENT));

                log.info("Acta enviada al jurado {} (modalidad ID {})",
                    examinerUser.getId(), modalityId);

            } catch (Exception e) {
                log.error("Error generando acta para jurado {} en modalidad ID {}: {}",
                    examiner.getExaminer().getId(), modalityId, e.getMessage(), e);
            }
        }

        log.info("Proceso de generación de actas para jurados completado para modalidad ID: {}",
            modalityId);
    }

    /**
     * Construye el mensaje para el jurado sobre su participación
     */
    private String buildExaminerParticipationMessage(User examiner, StudentModality modality, DefenseExaminer defenseExaminer) {
        String examinerRole = TranslationUtils.translateExaminerType(defenseExaminer.getExaminerType());

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        List<StudentModalityMember> members = activeMembers(modality);
        String studentNames;
        if (!members.isEmpty()) {
            studentNames = members.stream()
                    .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName())
                    .collect(Collectors.joining(", "));
        } else {
            studentNames = modality.getLeader() != null
                    ? modality.getLeader().getName() + " " + modality.getLeader().getLastName()
                    : "No registrado";
        }

        return NotificationMessageTemplates.examinerParticipationAct(
                examiner.getName(),
                examinerRole,
                modalidadInfo,
                modality.getProgramDegreeModality().getAcademicProgram().getName(),
                modality.getProgramDegreeModality().getAcademicProgram().getFaculty().getName(),
                studentNames
        );
    }

    private List<StudentModalityMember> activeMembers(StudentModality modality) {
        return studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
    }

}
