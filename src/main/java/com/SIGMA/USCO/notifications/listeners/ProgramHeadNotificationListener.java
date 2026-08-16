package com.SIGMA.USCO.notifications.listeners;

import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.shared.util.TranslationUtils;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.Modalities.event.ModalityEvent;
import com.SIGMA.USCO.notifications.service.NotificationFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.SIGMA.USCO.notifications.service.NotificationBuilderHelper;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@RequiredArgsConstructor
@Component
public class ProgramHeadNotificationListener {

    private final StudentModalityRepository studentModalityRepository;
    private final NotificationFactory notificationFactory;
    private final UserRepository userRepository;
    private final StudentDocumentRepository studentDocumentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleEvent(ModalityEvent event) {
        try {
            switch (event.getType()) {
                case MODALITY_STARTED -> handleModalityStartedEvent(event);
                case DOCUMENT_UPLOADED -> onStudentDocumentUpdated(event);
                case DEFENSE_SCHEDULED -> handleDefenseScheduledEvent(event);
                case DIRECTOR_ASSIGNED -> onDirectorAssigned(event);
                case DEFENSE_COMPLETED -> FinalDefenseResult(event);
                case MODALITY_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE -> ModalityApproved(event);
                case DIRECTOR_NOTIFIES_PROGRAM_HEAD_FINAL_REVIEW -> handleDirectorNotifiesProgramHeadForFinalReview(event);
                default -> log.warn("Unhandled ModalityEvent type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error en ProgramHeadNotificationListener procesando evento {} (studentModalityId={})",
                    event.getType(), event.getStudentModalityId(), e);
            throw e;
        }
    }

    private void handleModalityStartedEvent(ModalityEvent event){
        StudentModality studentModality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        List<User> programHeads = userRepository.findAllProgramHeads();
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(studentModality);
        String subject = NotificationMessageTemplates.PROGRAM_HEAD_MODALITY_STARTED_SUBJECT;
        String message = NotificationMessageTemplates.programHeadModalityStarted(
                modalidadInfo,
                TranslationUtils.getStudentList(studentModality)
        );
        for (User programHead : programHeads) {
            notificationFactory.buildAndDispatch(
                    NotificationType.MODALITY_STARTED,
                    NotificationRecipientType.PROGRAM_HEAD,
                    programHead, null, studentModality,
                    subject, message
            );

        }
    }

    private void onStudentDocumentUpdated(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        StudentDocument document = studentDocumentRepository.findById(
                event.get(ModalityEvent.KEY_STUDENT_DOCUMENT_ID, Long.class))
                .orElseThrow(() -> new NotFoundException("Documento no encontrado"));
        User student = modality.getLeader();
        List<User> programHeads = userRepository.findAllProgramHeads();
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = NotificationMessageTemplates.PROGRAM_HEAD_DOCUMENT_UPDATED_SUBJECT;
        String message = NotificationMessageTemplates.programHeadDocumentUpdated(
                student.getName() + " " + student.getLastName(),
                student.getEmail(),
                modalidadInfo,
                document.getDocumentConfig().getDocumentName(),
                TranslationUtils.translateDocumentStatus(document.getStatus())
        );
        for (User programHead : programHeads) {
            notificationFactory.buildAndDispatch(
                    NotificationType.DOCUMENT_UPLOADED,
                    NotificationRecipientType.PROGRAM_HEAD,
                    programHead, student, modality,
                    subject, message
            );

        }
    }

    private void handleDefenseScheduledEvent(ModalityEvent event){
        StudentModality studentModality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        List<User> programHeads = userRepository.findAllProgramHeads();
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(studentModality);
        LocalDateTime defenseDate = event.get(ModalityEvent.KEY_DEFENSE_DATE, LocalDateTime.class);
        String defenseLocation = event.get(ModalityEvent.KEY_DEFENSE_LOCATION, String.class);
        String subject = NotificationMessageTemplates.PROGRAM_HEAD_DEFENSE_SCHEDULED_SUBJECT;
        String message = NotificationMessageTemplates.programHeadDefenseScheduled(
                modalidadInfo,
                TranslationUtils.getStudentList(studentModality),
                TranslationUtils.formatDateTime(defenseDate),
                defenseLocation
        );
        for (User programHead : programHeads) {
            notificationFactory.buildAndDispatch(
                    NotificationType.DEFENSE_SCHEDULED,
                    NotificationRecipientType.PROGRAM_HEAD,
                    programHead, null, studentModality,
                    subject, message
            );

        }
    }

    private void onDirectorAssigned(ModalityEvent event){
        StudentModality studentModality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        List<User> programHeads = userRepository.findAllProgramHeads();
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(studentModality);
        String subject = NotificationMessageTemplates.PROGRAM_HEAD_DIRECTOR_ASSIGNED_SUBJECT;
        User director = studentModality.getProjectDirector();
        String directorName = director != null
                ? director.getName() + " " + director.getLastName()
                : "Sin director asignado";
        String message = NotificationMessageTemplates.programHeadDirectorAssigned(
                modalidadInfo,
                TranslationUtils.getStudentList(studentModality),
                directorName
        );
        for (User programHead : programHeads) {
            notificationFactory.buildAndDispatch(
                    NotificationType.DIRECTOR_ASSIGNED,
                    NotificationRecipientType.PROGRAM_HEAD,
                    programHead, null, studentModality,
                    subject, message
            );

        }
    }

    private void FinalDefenseResult(ModalityEvent event){
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        User director = modality.getProjectDirector();
        if (director == null) {
            return;
        }
        List<User> programHeads = userRepository.findAllProgramHeads();
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        ModalityProcessStatus finalStatus = event.get(ModalityEvent.KEY_FINAL_STATUS, ModalityProcessStatus.class);
        AcademicDistinction academicDistinction = event.get(ModalityEvent.KEY_ACADEMIC_DISTINCTION, AcademicDistinction.class);
        String observations = event.get(ModalityEvent.KEY_OBSERVATIONS, String.class);
        String subject = NotificationMessageTemplates.PROGRAM_HEAD_FINAL_DEFENSE_RESULT_SUBJECT;
        String message = NotificationMessageTemplates.programHeadFinalDefenseResult(
                director.getName(),
                TranslationUtils.getStudentList(modality),
                modalidadInfo,
                TranslationUtils.translateModalityProcessStatus(finalStatus),
                TranslationUtils.translateAcademicDistinction(academicDistinction),
                observations != null && !observations.isBlank()
                        ? observations
                        : "No se registran observaciones."
        );
        for (User programHead : programHeads) {
            notificationFactory.buildAndDispatch(
                    NotificationType.DEFENSE_COMPLETED,
                    NotificationRecipientType.PROGRAM_HEAD,
                    programHead, null, modality,
                    subject, message
            );

        }
    }

    private void ModalityApproved(ModalityEvent event){
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        List<User> programHeads = userRepository.findAllProgramHeads();
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = NotificationMessageTemplates.programHeadModalityApprovedSubject(
                modality.getLeader().getName() + " " + modality.getLeader().getLastName());
        String message = NotificationMessageTemplates.programHeadModalityApproved(
                modality.getLeader().getName(),
                modality.getLeader().getEmail(),
                modalidadInfo,
                TranslationUtils.formatDateTime(modality.getSelectionDate())
        );
        for (User programHead : programHeads) {
            notificationFactory.buildAndDispatch(
                    NotificationType.MODALITY_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE,
                    NotificationRecipientType.PROGRAM_HEAD,
                    programHead, null, modality,
                    subject, message
            );

        }
    }

    private void handleDirectorNotifiesProgramHeadForFinalReview(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        User director = modality.getProjectDirector();
        String directorNombre = director != null
                ? director.getName() + " " + director.getLastName()
                : "El director de proyecto";

        List<User> programHeads = userRepository.findAllProgramHeads();

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String subject = NotificationMessageTemplates.programHeadFinalReviewReadySubject(modalidadInfo);

        String message = NotificationMessageTemplates.programHeadFinalReviewReady(
                directorNombre,
                modalidadInfo,
                TranslationUtils.getStudentList(modality)
        );

        for (User programHead : programHeads) {
            notificationFactory.buildAndDispatch(
                    NotificationType.DIRECTOR_NOTIFIES_PROGRAM_HEAD_FINAL_REVIEW,
                    NotificationRecipientType.PROGRAM_HEAD,
                    programHead, director, modality,
                    subject, message
            );

        }
    }

}
