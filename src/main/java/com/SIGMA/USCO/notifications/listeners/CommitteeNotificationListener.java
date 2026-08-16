package com.SIGMA.USCO.notifications.listeners;

import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.EnumSet;
import java.util.List;
import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Component
public class CommitteeNotificationListener {

    private final StudentModalityRepository studentModalityRepository;
    private final NotificationFactory notificationFactory;
    private final UserRepository userRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final com.SIGMA.USCO.academic.repository.StudentProfileRepository studentProfileRepository;

    private static final EnumSet<ModalityProcessStatus> VALID_STATES =
            EnumSet.of(
                    ModalityProcessStatus.READY_FOR_PROGRAM_CURRICULUM_COMMITTEE,
                    ModalityProcessStatus.UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE,
                    ModalityProcessStatus.PROPOSAL_APPROVED,
                    ModalityProcessStatus.DEFENSE_SCHEDULED
            );

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleEvent(ModalityEvent event) {
        try {
            switch (event.getType()) {
                case MODALITY_CANCELLATION_REQUESTED -> handleCancellationRequested(event);
                case MODALITY_APPROVED_BY_PROGRAM_HEAD -> handleModalityApprovedByProgramHead(event);
                case DOCUMENT_UPLOADED -> handleStudentDocumentUpdated(event);
                case CORRECTION_RESUBMITTED -> handleCorrectionResubmitted(event);
                default -> log.warn("Unhandled ModalityEvent type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error en CommitteeNotificationListener procesando evento {} (studentModalityId={})",
                    event.getType(), event.getStudentModalityId(), e);
            throw e;
        }
    }

    private void handleCancellationRequested(ModalityEvent event) {
        StudentModality studentModality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        List<User> committeeMembers = userRepository.findAllProgramCurriculumCommittee();

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(studentModality);
        User leader = studentModality.getLeader();
        var leaderProfile = studentProfileRepository.findByUserId(leader.getId()).orElse(null);

        String subject = NotificationMessageTemplates.COMMITTEE_CANCELLATION_REQUESTED_SUBJECT;

        String message = NotificationMessageTemplates.committeeCancellationRequested(
                modalidadInfo,
                leader.getName() + " " + leader.getLastName(),
                studentModality.getAcademicProgram().getName(),
                leaderProfile != null && leaderProfile.getSemester() != null ? String.valueOf(leaderProfile.getSemester()) : "No registrado",
                leaderProfile != null && leaderProfile.getStudentCode() != null ? leaderProfile.getStudentCode() : "No registrado",
                leader.getEmail(),
                TranslationUtils.getStudentList(studentModality)
        );

        for (User committeeMember : committeeMembers) {
            notificationFactory.buildAndDispatch(
                    NotificationType.MODALITY_CANCELLATION_REQUESTED,
                    NotificationRecipientType.PROGRAM_CURRICULUM_COMMITTEE,
                    committeeMember, studentModality.getLeader(),
                    studentModality, subject, message);
        }
    }

    private void handleModalityApprovedByProgramHead(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        List<User> committeeMembers =
                userRepository.findAllProgramCurriculumCommittee();

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        User leader = modality.getLeader();
        var leaderProfile = studentProfileRepository.findByUserId(leader.getId()).orElse(null);

        String subject = NotificationMessageTemplates.COMMITTEE_MODALITY_APPROVED_SUBJECT;

        String message = NotificationMessageTemplates.committeeModalityApproved(
                modalidadInfo,
                leader.getName() + " " + leader.getLastName(),
                modality.getAcademicProgram().getName(),
                leaderProfile != null && leaderProfile.getSemester() != null ? String.valueOf(leaderProfile.getSemester()) : "No registrado",
                leaderProfile != null && leaderProfile.getStudentCode() != null ? leaderProfile.getStudentCode() : "No registrado",
                leader.getEmail(),
                TranslationUtils.getStudentList(modality)
        );

        for (User committeeMember : committeeMembers) {
            notificationFactory.buildAndDispatch(
                    NotificationType.MODALITY_APPROVED_BY_PROGRAM_HEAD,
                    NotificationRecipientType.PROGRAM_CURRICULUM_COMMITTEE,
                    committeeMember, modality.getLeader(),
                    modality, subject, message);
        }
    }

    private void handleStudentDocumentUpdated(ModalityEvent event) {
        StudentModality modality =
                studentModalityRepository.findById(event.getStudentModalityId())
                        .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        if (!VALID_STATES.contains(modality.getStatus())) {
            return;
        }

        StudentDocument document = studentDocumentRepository.findById(
                event.get(ModalityEvent.KEY_STUDENT_DOCUMENT_ID, Long.class))
                        .orElseThrow(() -> new NotFoundException("Documento no encontrado"));

        User student = modality.getLeader();

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String subject = NotificationMessageTemplates.COMMITTEE_DOCUMENT_UPDATED_SUBJECT;

        String message = NotificationMessageTemplates.committeeDocumentUpdated(
                student.getName() + " " + student.getLastName(),
                modalidadInfo,
                document.getDocumentConfig().getDocumentName(),
                TranslationUtils.translateDocumentStatus(document.getStatus())
        );

        List<User> committeeMembers =
                userRepository.findAllProgramCurriculumCommittee();

        for (User committee : committeeMembers) {
            notificationFactory.buildAndDispatch(
                    NotificationType.DOCUMENT_UPLOADED,
                    NotificationRecipientType.PROGRAM_CURRICULUM_COMMITTEE,
                    committee, student,
                    modality, subject, message);
        }
    }

    private void handleCorrectionResubmitted(ModalityEvent event) {
        StudentModality modality =
                studentModalityRepository.findById(event.getStudentModalityId())
                        .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        // ponytail: solo se notifica al comité cuando las correcciones las solicitó el propio comité;
        // las de jefatura (CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD) y jurados (_TO_EXAMINERS) se filtran aquí.
        // El estado genérico CORRECTIONS_SUBMITTED (endpoint /resubmit-correction, no usado por el frontend) no aplica.
        if (modality.getStatus() != ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_COMMITTEE) {
            return;
        }

        StudentDocument document = studentDocumentRepository.findById(
                event.get(ModalityEvent.KEY_DOCUMENT_ID, Long.class))
                        .orElseThrow(() -> new NotFoundException("Documento no encontrado"));

        User student = modality.getLeader();

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String subject = NotificationMessageTemplates.COMMITTEE_CORRECTION_RESUBMITTED_SUBJECT;

        String message = NotificationMessageTemplates.committeeCorrectionResubmitted(
                student.getName() + " " + student.getLastName(),
                modalidadInfo,
                document.getDocumentConfig().getDocumentName(),
                TranslationUtils.formatDateTime(LocalDateTime.now())
        );

        List<User> committeeMembers =
                userRepository.findAllProgramCurriculumCommittee();

        for (User committee : committeeMembers) {
            notificationFactory.buildAndDispatch(
                    NotificationType.CORRECTION_RESUBMITTED,
                    NotificationRecipientType.PROGRAM_CURRICULUM_COMMITTEE,
                    committee, student,
                    modality, subject, message);
        }
    }

}
