package com.SIGMA.USCO.notifications.listeners;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.event.ModalityEvent;
import com.SIGMA.USCO.notifications.service.NotificationBuilderHelper;
import com.SIGMA.USCO.notifications.service.NotificationFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class CommitteeNotificationListener {

    private final StudentModalityRepository studentModalityRepository;
    private final NotificationFactory notificationFactory;
    private final UserRepository userRepository;
    private final StudentDocumentRepository studentDocumentRepository;

    private static final EnumSet<ModalityProcessStatus> VALID_STATES =
            EnumSet.of(
                    ModalityProcessStatus.READY_FOR_PROGRAM_CURRICULUM_COMMITTEE,
                    ModalityProcessStatus.UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE,
                    ModalityProcessStatus.PROPOSAL_APPROVED,
                    ModalityProcessStatus.DEFENSE_SCHEDULED
            );

    @EventListener
    public void handleEvent(ModalityEvent event) {
        switch (event.getType()) {
            case MODALITY_CANCELLATION_REQUESTED -> handleCancellationRequested(event);
            case MODALITY_APPROVED_BY_PROGRAM_HEAD -> handleModalityApprovedByProgramHead(event);
            case DOCUMENT_UPLOADED -> handleStudentDocumentUpdated(event);
            default -> log.warn("Unhandled ModalityEvent type: {}", event.getType());
        }
    }

    private void handleCancellationRequested(ModalityEvent event) {
        StudentModality studentModality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();

        List<User> committeeMembers = userRepository.findAllByRoles_Name("PROGRAM_CURRICULUM_COMMITTEE");

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(studentModality);

        String subject = "Solicitud de cancelación de modalidad";

        String message = """
                La modalidad de grado del estudiante:

                "%s"

                ha sido revisada y aprobada por la Jefatura del Programa.

                En consecuencia, el proceso ha sido habilitado para la revisión y gestión
                por parte del Comité de Currículo del Programa. Se solicita a los miembros
                del comité proceder con las etapas correspondientes del proceso académico,
                de acuerdo con las funciones y responsabilidades establecidas.

                Por favor, ingrese al sistema  para consultar los detalles de la
                modalidad registrada y continuar con el flujo de evaluación y seguimiento.

                Sistema SIGMA
                Plataforma de Gestión de Modalidades de Grado

                """.formatted(
                modalidadInfo
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
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();

        List<User> committeeMembers =
                userRepository.findAllByRoles_Name("PROGRAM_CURRICULUM_COMMITTEE");

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String subject = "Modalidad de grado aprobada por Jefatura de Programa";

        String message = """
                La modalidad de grado del estudiante:

                "%s"

                ha sido aprobada por jefatura del programa. Por favor,
                proceda con las siguientes etapas del proceso.

                Sistema SIGMA
                """.formatted(
                modalidadInfo
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
                        .orElseThrow();

        if (!VALID_STATES.contains(modality.getStatus())) {
            return;
        }

        StudentDocument document = studentDocumentRepository.findById(
                event.get(ModalityEvent.KEY_STUDENT_DOCUMENT_ID, Long.class))
                        .orElseThrow();

        User student = modality.getLeader();

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String subject = "Documento actualizado – Modalidad en revisión";

        String message = """
                Se informa que el estudiante:

                "%s"

                ha realizado la actualización de un documento asociado a su modalidad de grado,
                la cual actualmente se encuentra en una etapa activa de revisión por parte del
                Comité de Currículo del Programa.

                Información del proceso:

                Programa académico:
                "%s"

                Documento actualizado:
                "%s"

                Estado actual del documento:
                %s

                Debido a que la modalidad se encuentra en fase de evaluación, se solicita a los
                miembros del Comité de Currículo revisar la nueva versión del documento,
                verificar que su contenido cumpla con los lineamientos académicos establecidos
                y continuar con el procedimiento correspondiente dentro del flujo de evaluación
                definido para las modalidades de grado.

                Para consultar el documento actualizado y realizar el seguimiento respectivo,
                por favor ingrese al sistema.

                Plataforma de Gestión de Modalidades de Grado
                """.formatted(
                student.getName() + " " + student.getLastName(),
                modalidadInfo,
                document.getDocumentConfig().getDocumentName(),
                TranslationUtils.translateDocumentStatus(document.getStatus())
        );

        List<User> committeeMembers =
                userRepository.findAllByRoles_Name("PROGRAM_CURRICULUM_COMMITTEE");

        for (User committee : committeeMembers) {
            notificationFactory.buildAndDispatch(
                    NotificationType.DOCUMENT_UPLOADED,
                    NotificationRecipientType.PROGRAM_CURRICULUM_COMMITTEE,
                    committee, student,
                    modality, subject, message);
        }
    }

}
