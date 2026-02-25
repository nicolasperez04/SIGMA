package com.SIGMA.USCO.notifications.listeners;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.documents.entity.DocumentStatus;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.notifications.entity.Notification;
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.event.CancellationRequestedEvent;
import com.SIGMA.USCO.notifications.event.ModalityApprovedByProgramHead;
import com.SIGMA.USCO.notifications.event.StudentDocumentUpdatedEvent;
import com.SIGMA.USCO.notifications.repository.NotificationRepository;
import com.SIGMA.USCO.notifications.service.NotificationDispatcherService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@RequiredArgsConstructor
@Component
public class CommitteeNotificationListener {

    private final StudentModalityRepository studentModalityRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDispatcherService dispatcher;
    private final UserRepository userRepository;
    private final StudentDocumentRepository studentDocumentRepository;

    @EventListener
    public void onCancellationRequested(CancellationRequestedEvent event){

        StudentModality studentModality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();

        List<User> committeeMembers = userRepository.findAllByRoles_Name("PROGRAM_CURRICULUM_COMMITTEE");

        String subject = "Solicitud de cancelación de modalidad";

        String message = """
                Se ha recibido formalmente una *solicitud de cancelación de modalidad de grado* presentada por el siguiente estudiante:
                
                **"%s"**
                
                La solicitud ha sido registrada en el sistema y se encuentra pendiente de revisión por parte del Comité de Currículo del Programa.
                
                Se solicita amablemente evaluar la petición conforme a la normativa institucional vigente y proceder con el análisis, validación y decisión correspondiente dentro de los plazos establecidos.
                
                Este mensaje constituye una notificación oficial generada automáticamente para garantizar la trazabilidad y gestión oportuna del proceso.
                
                """.formatted(
                studentModality.getLeader().getName() + " " + studentModality.getLeader().getLastName()
        );

        for (User committeeMember : committeeMembers) {

            Notification notification = Notification.builder()
                    .type(NotificationType.MODALITY_CANCELLATION_REQUESTED)
                    .recipientType(NotificationRecipientType.PROGRAM_CURRICULUM_COMMITTEE)
                    .recipient(committeeMember)
                    .triggeredBy(studentModality.getLeader())
                    .studentModality(studentModality)
                    .subject(subject)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
        }

    }

    @EventListener
    public void onModalityApprovedByProgramHead(ModalityApprovedByProgramHead event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();

        List<User> committeeMembers =
                userRepository.findAllByRoles_Name("PROGRAM_CURRICULUM_COMMITTEE");

        String subject = "Modalidad de grado aprobada por Jefatura de Programa";

        String message = """
                La modalidad de grado del estudiante:

                "%s"

                ha sido aprobada por jefatura del programa. Por favor,
                proceda con las siguientes etapas del proceso.

                Sistema SIGMA
                """.formatted(
                modality.getLeader().getName() + " " + modality.getLeader().getLastName()
        );

        for (User committeMember : committeeMembers) {

            Notification notification = Notification.builder()
                    .type(NotificationType.MODALITY_APPROVED_BY_PROGRAM_HEAD)
                    .recipientType(NotificationRecipientType.PROGRAM_CURRICULUM_COMMITTEE)
                    .recipient(committeMember)
                    .triggeredBy(modality.getLeader())
                    .studentModality(modality)
                    .subject(subject)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
        }




    }

    private static final EnumSet<ModalityProcessStatus> VALID_STATES =
            EnumSet.of(
                    ModalityProcessStatus.READY_FOR_PROGRAM_CURRICULUM_COMMITTEE,
                    ModalityProcessStatus.UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE,
                    ModalityProcessStatus.PROPOSAL_APPROVED,
                    ModalityProcessStatus.DEFENSE_SCHEDULED
            );

    @EventListener
    public void onStudentDocumentUpdated(StudentDocumentUpdatedEvent event) {

        StudentModality modality =
                studentModalityRepository.findById(event.getStudentModalityId())
                        .orElseThrow();

        if (!VALID_STATES.contains(modality.getStatus())) {
            return;
        }

        StudentDocument document = studentDocumentRepository.findById(event.getStudentDocumentId())
                        .orElseThrow();

        User student = modality.getLeader();

        String subject = "Documento actualizado – Modalidad en revisión";

        String message = """
                Se informa que el estudiante %s ha realizado la actualización de un documento asociado a una modalidad de grado que actualmente se encuentra en estado de revisión.
                
                A continuación, se detallan los datos relevantes del proceso:

                Modalidad:
                "%s"

                Documento actualizado:
                "%s"

                Estado del documento:
                %s

                Dado que la modalidad se encuentra en una fase activa de evaluación, se solicita al Comité de Currículo del Programa verificar la nueva versión del documento, validar su contenido conforme a los lineamientos académicos establecidos y continuar con el trámite correspondiente dentro del flujo definido.
                
                Esta notificación es generada automáticamente como parte del mecanismo de control y trazabilidad de las actualizaciones realizadas durante el proceso de evaluación.
                """.formatted(
                student.getName() + " " + student.getLastName(),
                modality.getProgramDegreeModality().getAcademicProgram().getName(),
                document.getDocumentConfig().getDocumentName(),
                translateDocumentStatus(document.getStatus())
        );

        List<User> committeeMembers =
                userRepository.findAllByRoles_Name("PROGRAM_CURRICULUM_COMMITTEE");

        for (User committee : committeeMembers) {

            Notification notification = Notification.builder()
                    .type(NotificationType.DOCUMENT_UPLOADED)
                    .recipientType(NotificationRecipientType.PROGRAM_CURRICULUM_COMMITTEE)
                    .recipient(committee)
                    .triggeredBy(student)
                    .studentModality(modality)
                    .subject(subject)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
        }
    }

    private String translateDocumentStatus(DocumentStatus status) {
        return switch (status) {
            case PENDING -> "Pendiente";
            case ACCEPTED_FOR_PROGRAM_HEAD_REVIEW -> "Aceptado para revisión de Jefatura";
            case REJECTED_FOR_PROGRAM_HEAD_REVIEW -> "Rechazado por Jefatura";
            case CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD -> "Correcciones solicitadas por Jefatura";
            case CORRECTION_RESUBMITTED -> "Corrección reenviada";
            case ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW -> "Aceptado para revisión de Comité de Currículo";
            case REJECTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW -> "Rechazado por Comité de Currículo";
            case CORRECTIONS_REQUESTED_BY_PROGRAM_CURRICULUM_COMMITTEE -> "Correcciones solicitadas por Comité de Currículo";
            case ACCEPTED_FOR_EXAMINER_REVIEW -> "Aceptado para revisión de Jurado";
            case REJECTED_FOR_EXAMINER_REVIEW -> "Rechazado por Jurado";
            case CORRECTIONS_REQUESTED_BY_EXAMINER -> "Correcciones solicitadas por Jurado";
        };
    }


}
