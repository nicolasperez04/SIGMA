package com.SIGMA.USCO.notifications.listeners;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.documents.entity.DocumentStatus;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.notifications.entity.Notification;
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.event.*;
import com.SIGMA.USCO.notifications.repository.NotificationRepository;
import com.SIGMA.USCO.notifications.service.NotificationDispatcherService;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class DirectorNotificationListener {

    private final StudentModalityRepository studentModalityRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDispatcherService dispatcher;
    private final UserRepository userRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;

    @EventListener
    public void onCancellationApproved(CancellationApprovedEvent event) {

        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();

        if (sm.getProjectDirector() == null) {
            return;
        }

        // Obtener todos los miembros ACTIVOS de la modalidad
        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(sm.getId(), MemberStatus.ACTIVE);
        String miembros = members.stream()
            .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName() + " (" + m.getStudent().getEmail() + ")")
            .collect(Collectors.joining(", "));

        String subject = "Cancelación de modalidad APROBADA";
        String message = """
                Estimado/a %s,
                
                Reciba un cordial saludo.
                
                Le informamos que el Comité de Currículo del programa académico ha aprobado formalmente la solicitud de cancelación de la modalidad de grado:
                
                **“%s”**
                
                correspondiente a los siguientes estudiantes:
                %s
                
                En consecuencia, la modalidad queda cancelada conforme a los procedimientos institucionales vigentes. Si requiere información adicional o seguimiento sobre el caso, le recomendamos comunicarse con la jefatura del programa académico.
                
                Esta notificación se genera como parte del proceso oficial de gestión académica y registro institucional.
                
                Cordialmente,
                Sistema de Gestión Académica de la universidad Surcolombiana.
                
                """.formatted(
                sm.getProjectDirector().getName(),
                sm.getProgramDegreeModality().getDegreeModality().getName(),
                miembros
        );

        Notification notification = Notification.builder()
                .type(NotificationType.MODALITY_CANCELLATION_APPROVED)
                .recipientType(NotificationRecipientType.PROJECT_DIRECTOR)
                .recipient(sm.getProjectDirector())
                .triggeredBy(null)
                .studentModality(sm)
                .subject(subject)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        dispatcher.dispatch(notification);

    }

    @EventListener
    public void onCancellationRejected(CancellationApprovedEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        if (sm.getProjectDirector() == null) {
            return;
        }
        // Obtener todos los miembros ACTIVOS de la modalidad
        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(sm.getId(), MemberStatus.ACTIVE);
        String miembros = members.stream()
            .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName() + " (" + m.getStudent().getEmail() + ")")
            .collect(Collectors.joining(", "));
        String subject = "Cancelación de modalidad rechazada";
        String message = """
                Estimado/a %s,
                
                Reciba un cordial saludo.
                
                Le informamos que el Comité de Currículo del programa académico ha emitido decisión respecto a la solicitud de cancelación de la modalidad de grado:
                
                **“%s”**
                
                correspondiente a los siguientes estudiantes:
                %s, la cual ha sido **rechazada**.
                
                En consecuencia, la modalidad continuará su curso conforme al estado académico vigente y a los lineamientos institucionales establecidos.
                
                Para conocer los fundamentos de la decisión o realizar el seguimiento correspondiente al proceso, le recomendamos comunicarse con la jefatura del programa académico.
                
                Esta notificación se genera como parte del procedimiento oficial de registro y control de decisiones académicas dentro del sistema.
                
                Cordialmente,
                Sistema de Gestión Académica de la universidad Surcolombiana.
                
                """.formatted(
                sm.getProjectDirector().getName(),
                sm.getProgramDegreeModality().getDegreeModality().getName(),
                miembros
        );
        Notification notification = Notification.builder()
                .type(NotificationType.MODALITY_CANCELLATION_REJECTED)
                .recipientType(NotificationRecipientType.PROJECT_DIRECTOR)
                .recipient(sm.getProjectDirector())
                .triggeredBy(null)
                .studentModality(sm)
                .subject(subject)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
        dispatcher.dispatch(notification);
    }

    @EventListener
    public void onCancellationRequested(CancellationRequestedEvent event){
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        if (sm.getProjectDirector() == null) {
            return;
        }
        // Obtener todos los miembros ACTIVOS de la modalidad
        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(sm.getId(), MemberStatus.ACTIVE);
        String miembros = members.stream()
            .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName() + " (" + m.getStudent().getEmail() + ")")
            .collect(Collectors.joining(", "));
        String subject = "Solicitud de cancelación de modalidad recibida";
        String message = """
                Estimado/a %s,
                
                Reciba un cordial saludo.
                
                Le informamos que los siguientes estudiantes han presentado formalmente una solicitud de cancelación de la modalidad de grado:
                
                %s
                
                **“%s”**
                
                De conformidad con el procedimiento académico establecido, esta solicitud requiere su análisis y concepto como Director/a de Proyecto. Una vez emitida su valoración, el caso será remitido al Comité de Currículo del programa académico para la evaluación y decisión definitiva.
                
                Agradecemos gestionar la revisión correspondiente dentro de los plazos institucionales y realizar el seguimiento a través de la plataforma académica.
                
                Cordialmente,
                Sistema de Gestión Académica de la universidad Surcolombiana.
                
                """.formatted(
                sm.getProjectDirector().getName(),
                miembros,
                sm.getProgramDegreeModality().getDegreeModality().getName()
        );
        Notification notification = Notification.builder()
                .type(NotificationType.MODALITY_CANCELLATION_REQUESTED)
                .recipientType(NotificationRecipientType.PROJECT_DIRECTOR)
                .recipient(sm.getProjectDirector())
                .triggeredBy(null)
                .studentModality(sm)
                .subject(subject)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
        dispatcher.dispatch(notification);
    }

    @EventListener
    public void handleDefenseScheduled(DefenseScheduledEvent event) {

        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();

        User director = modality.getProjectDirector();

        if (director == null) {
            return;
        }

        // Obtener todos los miembros ACTIVOS de la modalidad
        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
        String miembros = members.stream()
            .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName() + " (" + m.getStudent().getEmail() + ")")
            .collect(Collectors.joining(", "));

        String directorSubject = "Notificación de sustentación programada para estudiantes asignados";

        String directorMessage = """
                Estimado/a %s,
                
                Reciba un cordial saludo.
                
                Le informamos que ha sido programada la sustentación de la modalidad de grado correspondiente a los siguientes estudiantes bajo su dirección académica:
                
                %s
                
                **Modalidad de grado:**
                “%s”
                
                **Fecha y hora de sustentación:**
                %s
                
                **Lugar:**
                %s
                
                En su calidad de Director/a de Proyecto, se solicita su participación y acompañamiento durante la sustentación, garantizando el cumplimiento de los lineamientos académicos y evaluativos establecidos por el programa.
                
                Le recomendamos verificar la información en la plataforma institucional y preparar el proceso conforme a la normativa vigente.
                
                Cordialmente,
                Sistema de Gestión Académica
                
                """.formatted(
                director.getName(),
                miembros,
                modality.getProgramDegreeModality().getDegreeModality().getName(),
                event.getDefenseDate(),
                event.getDefenseLocation()
        );



        Notification notification = Notification.builder()
                .type(NotificationType.DEFENSE_SCHEDULED)
                .recipientType(NotificationRecipientType.PROJECT_DIRECTOR)
                .recipient(director)
                .triggeredBy(null)
                .studentModality(modality)
                .subject(directorSubject)
                .message(directorMessage)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
        dispatcher.dispatch(notification);
    }

    @EventListener
    public void DirectorAssigned(DirectorAssignedEvent event){

        StudentModality modality =
                studentModalityRepository.findById(event.getStudentModalityId())
                        .orElseThrow();


        User director = userRepository.findById(event.getDirectorId())
                .orElseThrow();

        // Obtener todos los miembros ACTIVOS de la modalidad
        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
        String miembros = members.stream()
            .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName() + " (" + m.getStudent().getEmail() + ")")
            .collect(Collectors.joining(", "));

        String directorSubject = "Asignación como Director de Proyecto a modalidad de grado";

        String directorMessage = """
                Estimado/a %s,
                
                Reciba un cordial saludo.
                
                Le informamos que ha sido designado/a oficialmente como **Director/a de Proyecto** para la siguiente modalidad de grado, conforme a la asignación registrada en el sistema institucional.
                
                A continuación, se detallan los datos correspondientes:
                
                **Estudiantes:**
                %s
                
                **Programa académico:**
                “%s”
                
                **Fecha de asignación:**
                %s
                
                A partir de esta designación, usted asume la responsabilidad de orientar, supervisar y acompañar el desarrollo académico del proyecto de grado, garantizando el cumplimiento de los lineamientos, cronogramas y criterios de evaluación establecidos por el programa.
                
                Le recomendamos ingresar a la plataforma institucional para revisar la información completa del caso y dar inicio formal al proceso de seguimiento.
                
                Cordialmente,
                Sistema de Gestión Académica
                
                """.formatted(
                director.getName(),
                miembros,
                modality.getProgramDegreeModality().getAcademicProgram().getName(),
                modality.getUpdatedAt()
        );



        Notification notification = Notification.builder()
                .type(NotificationType.DIRECTOR_ASSIGNED)
                .recipientType(NotificationRecipientType.PROJECT_DIRECTOR)
                .recipient(director)
                .triggeredBy(null)
                .studentModality(modality)
                .subject(directorSubject)
                .message(directorMessage)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
        dispatcher.dispatch(notification);
    }

    @EventListener
    public void FinalDefenseResultEvent(FinalDefenseResultEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        User director = modality.getProjectDirector();
        if (director == null) {
            return;
        }
        // Obtener todos los miembros ACTIVOS de la modalidad
        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
        String miembros = members.stream()
            .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName() + " (" + m.getStudent().getEmail() + ")")
            .collect(Collectors.joining(", "));
        String subject = "Resultado de la sustentación final – Estudiantes asignados";
        String message = """
                Estimado/a %s,
                
                Reciba un cordial saludo.
                
                Le informamos que la sustentación final de la modalidad de grado bajo su dirección académica ha sido realizada y registrada oficialmente en el sistema, con el siguiente resultado:
                
                Estudiantes:
                %s
                
                Modalidad de grado:
                “%s”
                
                Resultado final:
                %s
                
                Distinción académica:
                %s
                
                Observaciones del jurado/comité evaluador:
                %s
                
                Este resultado marca el cierre académico del proceso de sustentación. En su calidad de Director/a, le sugerimos verificar el estado actualizado en la plataforma institucional y coordinar, si corresponde, los trámites administrativos y académicos posteriores con la jefatura del programa.
                
                Agradecemos el acompañamiento y orientación brindados durante el desarrollo del proyecto.
                
                Cordialmente,
                Sistema de Gestión Académica
                """.formatted(
                director.getName(),
                miembros,
                modality.getProgramDegreeModality().getDegreeModality().getName(),
                translateModalityProcessStatus(event.getFinalStatus()),
                translateAcademicDistinction(event.getAcademicDistinction()),
                event.getObservations() != null ? event.getObservations() : "N/A"
        );
        Notification notification = Notification.builder()
                .type(NotificationType.DEFENSE_COMPLETED)
                .recipientType(NotificationRecipientType.PROJECT_DIRECTOR)
                .recipient(director)
                .triggeredBy(null)
                .studentModality(modality)
                .subject(subject)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
        dispatcher.dispatch(notification);
    }

    @EventListener
    public void onStudentDocumentUpdated(StudentDocumentUpdatedEvent event) {

        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();

        if (modality.getProjectDirector() == null) {
            return;
        }

        StudentDocument document = studentDocumentRepository.findById(event.getStudentDocumentId())
                .orElseThrow();

        User student = modality.getLeader();
        User director = modality.getProjectDirector();

        String subject = "Documento actualizado – Estudiante asignado";

        String message = """
                Estimado/a %s,
                
                Reciba un cordial saludo.
                
                Le informamos que el estudiante %s ha realizado una actualización en uno de los documentos asociados a la modalidad de grado que actualmente se encuentra bajo su dirección académica.
                
                A continuación, se detallan los datos correspondientes:
                
                Modalidad de grado:
                “%s”
                
                Documento actualizado:
                “%s”
                
                Estado actual del documento:
                %s
                
                Esta actualización puede requerir su revisión, validación o retroalimentación según el estado reportado y la fase del proceso académico.
                
                Le invitamos a ingresar a la plataforma institucional para consultar la versión más reciente del documento y continuar con el seguimiento académico correspondiente.
                
                Cordialmente,
                Sistema de Gestión Académica
            """.formatted(
                director.getName(),
                student.getName() + " " + student.getLastName(),
                modality.getProgramDegreeModality().getDegreeModality().getName(),
                document.getDocumentConfig().getDocumentName(),
                translateDocumentStatus(document.getStatus())
        );

        Notification notification = Notification.builder()
                .type(NotificationType.DOCUMENT_UPLOADED)
                .recipientType(NotificationRecipientType.PROJECT_DIRECTOR)
                .recipient(director)
                .triggeredBy(student)
                .studentModality(modality)
                .subject(subject)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        dispatcher.dispatch(notification);
    }


    private String translateDocumentStatus(DocumentStatus status) {
        if (status == null) return "N/A";
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

    private String translateModalityProcessStatus(ModalityProcessStatus status) {
        if (status == null) return "N/A";
        return switch (status) {
            case MODALITY_SELECTED -> "Modalidad seleccionada";
            case UNDER_REVIEW_PROGRAM_HEAD -> "En revisión por Jefatura";
            case CORRECTIONS_REQUESTED_PROGRAM_HEAD -> "Correcciones solicitadas por Jefatura";
            case CORRECTIONS_SUBMITTED -> "Correcciones enviadas";
            case CORRECTIONS_APPROVED -> "Correcciones aprobadas";
            case CORRECTIONS_REJECTED_FINAL -> "Correcciones rechazadas (final)";
            case READY_FOR_PROGRAM_CURRICULUM_COMMITTEE -> "Lista para Comité de Currículo";
            case UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE -> "En revisión por Comité de Currículo";
            case CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE -> "Correcciones solicitadas por Comité de Currículo";
            case PROPOSAL_APPROVED -> "Propuesta aprobada";
            case DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR -> "Sustentación solicitada por Director";
            case DEFENSE_SCHEDULED -> "Sustentación programada";
            case EXAMINERS_ASSIGNED -> "Jueces asignados";
            case READY_FOR_EXAMINERS -> "Lista para jueces";
            case CORRECTIONS_REQUESTED_EXAMINERS -> "Correcciones solicitadas por jueces";
            case READY_FOR_DEFENSE -> "Lista para sustentación";
            case FINAL_REVIEW_COMPLETED -> "Revisión final completada";
            case DEFENSE_COMPLETED -> "Sustentación realizada";
            case UNDER_EVALUATION_PRIMARY_EXAMINERS -> "En evaluación por jueces principales";
            case DISAGREEMENT_REQUIRES_TIEBREAKER -> "Desacuerdo, requiere desempate";
            case UNDER_EVALUATION_TIEBREAKER -> "En evaluación por juez de desempate";
            case EVALUATION_COMPLETED -> "Evaluación completada";
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
        };
    }

    // Usar estos métodos en todos los emails
    // Ejemplo: translateDocumentStatus(document.getStatus())
    // Ejemplo: translateModalityProcessStatus(modality.getStatus())
    // Ejemplo: translateAcademicDistinction(event.getAcademicDistinction())
}
