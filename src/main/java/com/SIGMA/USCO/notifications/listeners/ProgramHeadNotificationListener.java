package com.SIGMA.USCO.notifications.listeners;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.notifications.entity.Notification;
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.event.*;
import com.SIGMA.USCO.notifications.repository.NotificationRepository;
import com.SIGMA.USCO.notifications.service.NotificationDispatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Component
public class ProgramHeadNotificationListener {

    private final StudentModalityRepository studentModalityRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDispatcherService dispatcher;
    private final UserRepository userRepository;
    private final StudentDocumentRepository studentDocumentRepository;

    @EventListener
    @Transactional
    public void handleModalityStartedEvent(StudentModalityStarted event){
        StudentModality studentModality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        List<User> programHeads = userRepository.findAllByRoles_Name("PROGRAM_HEAD");
        String degreeModalityName = studentModality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = studentModality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        String subject = "Nueva modalidad iniciada - Estudiantes asociados";
        String message = """
         Estimado/a Jefatura de Programa,
        
        Reciba un cordial saludo.
        
        Le informamos que ha sido registrada oficialmente en el sistema una nueva modalidad de grado con el siguiente detalle:
        
        Modalidad de grado:
        "%s"
        
        Estudiantes asociados:
        %s
        
        A partir de este registro, el proceso académico correspondiente queda activo y disponible para su revisión.
        
        Se solicita amablemente verificar la información ingresada y proceder con la validación institucional conforme a la normativa vigente.
        
        Puede consultar los detalles completos en la plataforma.
        
        Cordialmente,
        Sistema de Gestión Académica
    """.formatted(
        modalidadInfo,
        getStudentList(studentModality)
    );
    for (User programHead : programHeads) {
        Notification notification = Notification.builder()
                .type(NotificationType.MODALITY_STARTED)
                .recipientType(NotificationRecipientType.PROGRAM_HEAD)
                .recipient(programHead)
                .triggeredBy(null)
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
    public void onStudentDocumentUpdated(StudentDocumentUpdatedEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                        .orElseThrow();
        StudentDocument document = studentDocumentRepository.findById(event.getStudentDocumentId())
                        .orElseThrow();
        User student = modality.getLeader();
        List<User> programHeads = userRepository.findAllByRoles_Name("PROGRAM_HEAD");
        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        String subject = "Documento actualizado por estudiante";
        String message = """
        Estimado(a) Jefatura de Programa:

        Reciba un cordial saludo.

        Nos permitimos informar que un estudiante ha realizado la actualización de un documento previamente solicitado, en el marco del proceso académico correspondiente.

        A continuación, se relaciona la información pertinente:

        Nombre del estudiante: %s.
        Correo institucional: %s.
        Modalidad de grado: "%s".
        Documento actualizado: "%s".
        Estado actual del documento: %s.

        En este sentido, se solicita ingresar a la plataforma institucional, con el fin de revisar el documento actualizado y continuar con el trámite correspondiente, conforme a la normativa académica vigente.

        Este mensaje constituye una notificación automática generada como constancia de la actualización registrada y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
    """.formatted(
                student.getName() + " " + student.getLastName(),
                student.getEmail(),
                modalidadInfo,
                document.getDocumentConfig().getDocumentName(),
                translateDocumentStatus(document.getStatus())
        );
    for (User programHead : programHeads) {
        Notification notification = Notification.builder()
                .type(NotificationType.DOCUMENT_UPLOADED)
                .recipientType(NotificationRecipientType.PROGRAM_HEAD)
                .recipient(programHead)
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

    @EventListener
    public void handleDefenseScheduledEvent(DefenseScheduledEvent event){
        StudentModality studentModality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        List<User> programHeads = userRepository.findAllByRoles_Name("PROGRAM_HEAD");
        String degreeModalityName = studentModality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = studentModality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        String subject = "Sustentación programada - Estudiantes asociados";
        String message = """
                Estimada Jefatura de Programa:

                Reciba un cordial saludo.

                Nos permitimos informar que ha sido programada oficialmente la sustentación correspondiente a la modalidad de grado, conforme al proceso académico establecido.

                A continuación, se relaciona la información pertinente:

                Modalidad de grado: "%s".
                Estudiantes asociados: %s.
                Fecha y hora de la sustentación: %s.
                Lugar: %s.

                En este sentido, se solicita adoptar las medidas académicas y logísticas necesarias, con el fin de garantizar el adecuado desarrollo de la sustentación conforme a la normativa institucional vigente.

                Podrá consultar información adicional a través de la plataforma institucional.

                Este mensaje constituye una notificación automática generada como constancia de la programación realizada y para efectos de control y trazabilidad institucional.

                Atentamente,

                Sistema de Gestión Académica
                Universidad Surcolombiana
                """.formatted(
                modalidadInfo,
                getStudentList(studentModality),
                event.getDefenseDate().toString(),
                event.getDefenseLocation()
        );
        for (User programHead : programHeads) {

            Notification notification = Notification.builder()
                    .type(NotificationType.DEFENSE_SCHEDULED)
                    .recipientType(NotificationRecipientType.PROGRAM_HEAD)
                    .recipient(programHead)
                    .triggeredBy(null)
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
    public void onDirectorAssigned(DirectorAssignedEvent event){

        StudentModality studentModality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();

        List<User> programHeads =
                userRepository.findAllByRoles_Name("PROGRAM_HEAD");

        String degreeModalityName = studentModality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = studentModality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }

        String subject = "Nuevo director asignado - Estudiantes asociados";

        String message = """
        Estimada Jefatura de Programa:

        Reciba un cordial saludo.

        Nos permitimos informar que ha sido registrada la asignación de un director para la modalidad de grado, conforme al proceso académico establecido.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".
        Estudiantes asociados: %s.
        Director asignado: %s.

        En virtud de esta asignación, el director designado podrá iniciar el acompañamiento académico correspondiente, conforme a los lineamientos institucionales vigentes.

        Podrá consultar el detalle completo a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada como constancia de la asignación realizada y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
    """.formatted(
                modalidadInfo,
                getStudentList(studentModality),
                studentModality.getProjectDirector()
        );
        for (User programHead : programHeads) {

            Notification notification = Notification.builder()
                    .type(NotificationType.DIRECTOR_ASSIGNED)
                    .recipientType(NotificationRecipientType.PROGRAM_HEAD)
                    .recipient(programHead)
                    .triggeredBy(null)
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
    @Transactional
    public void FinalDefenseResult(FinalDefenseResultEvent event){
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        User director = modality.getProjectDirector();
        if (director == null) {
            return;
        }
        List<User> programHeads = userRepository.findAllByRoles_Name("PROGRAM_HEAD");
        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        String subject = "Resultado de la defensa final - Estudiantes asociados";
        String message = """
        Estimado(a) %s:

        Reciba un cordial saludo.

        Nos permitimos informar que ha concluido la sustentación final correspondiente a la modalidad de grado bajo su dirección, conforme al proceso académico establecido.

        A continuación, se relaciona la información pertinente:

        Estudiantes asociados: %s.
        Modalidad de grado: "%s".
        Resultado de la sustentación: %s.
        Distinción académica: %s.
        Observaciones del jurado: %s.

        El resultado ha sido registrado oficialmente en el sistema. Podrá consultar el detalle completo y la documentación asociada a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada como constancia del resultado registrado y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                director.getName(),
                getStudentList(modality),
                modalidadInfo,
                translateModalityProcessStatus(event.getFinalStatus()),
                translateAcademicDistinction(event.getAcademicDistinction()),
                event.getObservations() != null && !event.getObservations().isBlank()
                        ? event.getObservations()
                        : "No se registran observaciones."
        );
        for (User programHead : programHeads) {

            Notification notification = Notification.builder()
                    .type(NotificationType.DEFENSE_COMPLETED)
                    .recipientType(NotificationRecipientType.PROGRAM_HEAD)
                    .recipient(programHead)
                    .triggeredBy(null)
                    .studentModality(modality)
                    .subject(subject)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
        }

    }

    @EventListener
    public void ModalityApproved(ModalityApprovedByCommitteeEvent event){
        StudentModality modality =
                studentModalityRepository.findById(event.getStudentModalityId())
                        .orElseThrow();

        List<User> programHeads = userRepository.findAllByRoles_Name("PROGRAM_HEAD");

        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }

        String subject = "Modalidad aprobada por el comité de currículo de programa - Estudiante: " + modality.getLeader().getName() + " " + modality.getLeader().getLastName();

        String message = """
        Estimada Jefatura de Programa:

        Reciba un cordial saludo.

        Nos permitimos informar que la modalidad de grado ha sido aprobada oficialmente por el Comité de Currículo del programa académico, conforme a la normativa institucional vigente.

        A continuación, se relaciona la información pertinente:

        Nombre del estudiante: %s.
        Correo institucional: %s.
        Modalidad de grado: "%s".
        Fecha de aprobación: %s.

        La decisión ha sido registrada en el sistema y el proceso académico continúa conforme a los lineamientos establecidos.

        Podrá consultar el detalle completo a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada como constancia de la decisión registrada y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica – SIGMA
        Universidad Surcolombiana
        """.formatted(
                modality.getLeader().getName(),
                modality.getLeader().getEmail(),
                modalidadInfo,
                modality.getSelectionDate()
        );
        for (User programHead : programHeads) {

            Notification notification = Notification.builder()
                    .type(NotificationType.MODALITY_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE)
                    .recipientType(NotificationRecipientType.PROGRAM_HEAD)
                    .recipient(programHead)
                    .triggeredBy(null)
                    .studentModality(modality)
                    .subject(subject)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
        }
    }

    @EventListener
    public void handleDirectorNotifiesProgramHeadForFinalReview(DirectorNotifiesProgramHeadForFinalReviewEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User director = modality.getProjectDirector();
        String directorNombre = director != null
                ? director.getName() + " " + director.getLastName()
                : "El director de proyecto";

        List<User> programHeads = userRepository.findAllByRoles_Name("PROGRAM_HEAD");

        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }

        String subject = "Documentos finales listos para revisión - " + modalidadInfo;

        String message = """
        Estimada Jefatura de Programa:

        Reciba un cordial saludo.

        Nos permitimos informar que el Director de Proyecto %s ha registrado que los documentos finales de la modalidad de grado se encuentran disponibles para su revisión institucional previa a la sustentación, conforme al proceso académico establecido.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".
        Estudiantes asociados: %s.

        En este sentido, se solicita ingresar a la plataforma institucional con el fin de verificar la documentación final y, una vez validada, proceder con la notificación a los jurados evaluadores para dar continuidad al proceso de sustentación.

        Este mensaje constituye una notificación automática generada como constancia de la actuación registrada y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                directorNombre,
                modalidadInfo,
                getStudentList(modality)
        );

        for (User programHead : programHeads) {
            Notification notification = Notification.builder()
                    .type(NotificationType.DIRECTOR_NOTIFIES_PROGRAM_HEAD_FINAL_REVIEW)
                    .recipientType(NotificationRecipientType.PROGRAM_HEAD)
                    .recipient(programHead)
                    .triggeredBy(director)
                    .studentModality(modality)
                    .subject(subject)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
        }
    }

    private String getStudentList(StudentModality modality) {
        // Obtiene la lista de estudiantes asociados a la modalidad
        if (modality.getMembers() == null || modality.getMembers().isEmpty()) {
            return "Sin estudiantes asociados";
        }
        return modality.getMembers().stream()
            .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName() + " (" + m.getStudent().getEmail() + ")")
            .collect(Collectors.joining(", "));
    }

    private String translateDocumentStatus(DocumentStatus status) {
        if (status == null) return "N/A";
        return switch (status) {
            case PENDING -> "Pendiente";
            case ACCEPTED_FOR_PROGRAM_HEAD_REVIEW -> "Aceptado por Jefatura de Programa";
            case REJECTED_FOR_PROGRAM_HEAD_REVIEW -> "Rechazado por Jefatura de Programa";
            case CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD -> "Correcciones solicitadas por Jefatura de Programa";
            case CORRECTION_RESUBMITTED -> "Corrección reenviada";
            case ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW -> "Aceptado por Comité de Currículo";
            case REJECTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW -> "Rechazado por Comité de Currículo";
            case CORRECTIONS_REQUESTED_BY_PROGRAM_CURRICULUM_COMMITTEE -> "Correcciones solicitadas por Comité de Currículo";
            case ACCEPTED_FOR_EXAMINER_REVIEW -> "Aceptado por revisión de Jurado";
            case REJECTED_FOR_EXAMINER_REVIEW -> "Rechazado por Jurado";
            case CORRECTIONS_REQUESTED_BY_EXAMINER -> "Correcciones solicitadas por Jurado";
            case EDIT_REQUESTED -> "Solicitud de edición pendiente";
            case EDIT_REQUEST_APPROVED -> "Aprobado para edición";
            case EDIT_REQUEST_REJECTED -> "Rechazado para edición";
        };
    }
    private String translateModalityProcessStatus(ModalityProcessStatus status) {
        if (status == null) return "N/A";
        return switch (status) {
            case MODALITY_SELECTED -> "Modalidad seleccionada";
            case UNDER_REVIEW_PROGRAM_HEAD -> "En revisión por Jefatura de programa y/o coordinación de modalidades";
            case CORRECTIONS_REQUESTED_PROGRAM_HEAD -> "Correcciones solicitadas por Jefatura";
            case CORRECTIONS_SUBMITTED -> "Correcciones enviadas";
            case CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD -> "Correcciones enviadas a Jefatura de Programa y/o coordinador de modalidades";
            case CORRECTIONS_SUBMITTED_TO_COMMITTEE -> "Correcciones enviadas al Comité de Currículo";
            case CORRECTIONS_SUBMITTED_TO_EXAMINERS -> "Correcciones enviadas a los Jurados";
            case CORRECTIONS_APPROVED -> "Correcciones aprobadas";
            case CORRECTIONS_REJECTED_FINAL -> "Correcciones rechazadas (final)";
            case READY_FOR_PROGRAM_CURRICULUM_COMMITTEE -> "Lista para Comité de Currículo";
            case UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE -> "En revisión por Comité de Currículo";
            case CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE -> "Correcciones solicitadas por Comité de Currículo";
            case READY_FOR_DIRECTOR_ASSIGNMENT -> "Lista para asignación de Director de Proyecto";
            case READY_FOR_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE -> "Lista para aprobación por Comité de Currículo";
            case APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE -> "Aprobado por Comité de Currículo";
            case PROPOSAL_APPROVED -> "Propuesta aprobada";
            case PENDING_PROGRAM_HEAD_FINAL_REVIEW -> "Pendiente de revisión final por Jefatura de Programa";
            case APPROVED_BY_PROGRAM_HEAD_FINAL_REVIEW -> "Documentos finales aprobados por Jefatura de Programa";
            case DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR -> "Sustentación solicitada por Director";
            case DEFENSE_SCHEDULED -> "Sustentación programada";
            case EXAMINERS_ASSIGNED -> "Jurados asignados";
            case READY_FOR_EXAMINERS -> "Lista para jurados";
            case DOCUMENTS_APPROVED_BY_EXAMINERS -> "Documentos de propuesta aprobados por los jurados";
            case SECONDARY_DOCUMENTS_APPROVED_BY_EXAMINERS -> "Documentos finales aprobados por los jurados";
            case DOCUMENT_REVIEW_TIEBREAKER_REQUIRED -> "Revisión de documentos con desempate requerida";
            case EDIT_REQUESTED_BY_STUDENT -> "Edición de documento solicitado por estudiante";
            case CORRECTIONS_REQUESTED_EXAMINERS -> "Correcciones solicitadas por jurados";
            case READY_FOR_DEFENSE -> "Lista para sustentación";
            case FINAL_REVIEW_COMPLETED -> "Revisión final completada";
            case DEFENSE_COMPLETED -> "Sustentación realizada";
            case UNDER_EVALUATION_PRIMARY_EXAMINERS -> "En evaluación por jurados principales";
            case DISAGREEMENT_REQUIRES_TIEBREAKER -> "Desacuerdo, requiere desempate";
            case UNDER_EVALUATION_TIEBREAKER -> "En evaluación por jurado de desempate";
            case EVALUATION_COMPLETED -> "Evaluación completada";
            case PENDING_DISTINCTION_COMMITTEE_REVIEW -> "Aprobado - Distinción honorífica pendiente de revisión por el Comité";
            case GRADED_APPROVED -> "Aprobado";
            case GRADED_FAILED -> "Reprobado";
            case MODALITY_CLOSED -> "Modalidad cerrada";
            case SEMINAR_CANCELED -> "Seminario cancelado";
            case MODALITY_CANCELLED -> "Modalidad cancelada";
            case CANCELLATION_REQUESTED -> "Cancelación solicitada";
            case CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR -> "Cancelación aprobada por Director";
            case CANCELLATION_REJECTED_BY_PROJECT_DIRECTOR -> "Cancelación rechazada por Director";
            case CANCELLED_WITHOUT_REPROVAL -> "Cancelada sin reprobación";
            case CANCELLATION_REJECTED -> "Cancelación rechazada";
            case CANCELLED_BY_CORRECTION_TIMEOUT -> "Cancelada por tiempo de corrección";
        };
    }
    private String translateAcademicDistinction(AcademicDistinction distinction) {
        if (distinction == null) return "N/A";
        return switch (distinction) {
            case NO_DISTINCTION -> "Sin distinción";
            case AGREED_APPROVED -> "Aprobado por acuerdo";
            case AGREED_MERITORIOUS -> "Meritorio por acuerdo";
            case AGREED_LAUREATE -> "Laureado por acuerdo";
            case AGREED_REJECTED -> "Rechazado por acuerdo";
            case DISAGREEMENT_PENDING_TIEBREAKER -> "Desacuerdo, pendiente desempate";
            case TIEBREAKER_APPROVED -> "Aprobado por desempate";
            case TIEBREAKER_MERITORIOUS -> "Meritorio por desempate";
            case TIEBREAKER_LAUREATE -> "Laureado por desempate";
            case TIEBREAKER_REJECTED -> "Rechazado por desempate";
            case REJECTED_BY_COMMITTEE -> "Rechazado por comité";
            case PENDING_COMMITTEE_MERITORIOUS -> "Mención Meritoria propuesta (pendiente del comité)";
            case PENDING_COMMITTEE_LAUREATE -> "Mención Laureada propuesta (pendiente del comité)";
            case TIEBREAKER_PENDING_COMMITTEE_MERITORIOUS -> "Mención Meritoria por desempate (pendiente del comité)";
            case TIEBREAKER_PENDING_COMMITTEE_LAUREATE -> "Mención Laureada por desempate (pendiente del comité)";
        };
    }

}
