package com.SIGMA.USCO.notifications.listeners;

import com.SIGMA.USCO.Modalities.Entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
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

        String subject = "Concepto del Director de Proyecto sobre solicitud de cancelación de modalidad";

        String message = """
                 Estimado/a %s,
                
                        Reciba un cordial saludo.
                
                        Le informamos que el/la Director/a del proyecto ha emitido un concepto favorable
                        respecto a la solicitud de cancelación de la siguiente modalidad de grado:
                
                        Modalidad:
                        "%s"
                
                        Estudiantes vinculados al proceso:
                        %s
                
                        De acuerdo con el procedimiento académico institucional, la solicitud será ahora
                        remitida al Comité de Currículo del programa académico, instancia que realizará
                        la evaluación correspondiente y emitirá la decisión definitiva sobre la cancelación
                        de la modalidad de grado.
                
                        El comité podrá determinar la aprobación o el rechazo de la solicitud, decisión
                        que será notificada oportunamente a través del sistema institucional.
                
                        Esta comunicación se emite con el fin de mantener la trazabilidad y el registro
                        formal del proceso académico asociado a las modalidades de grado.
                
                        Atentamente,
                
                        Sistema de Gestión de Modalidades de Grado
                        Universidad Surcolombiana
                
                
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
        String subject = "Concepto del Director de Proyecto sobre solicitud de cancelación de modalidad";
        String message = """
                Estimado/a %s,
                
                        Reciba un cordial saludo.
                
                        Le informamos que el/la Director/a del proyecto ha evaluado la solicitud de
                        cancelación correspondiente a la siguiente modalidad de grado:
                
                        Modalidad:
                        "%s"
                
                        Estudiantes vinculados al proceso:
                        %s
                
                        Después de realizar la revisión correspondiente, el/la Director/a del proyecto
                        ha emitido un concepto no favorable respecto a la solicitud de cancelación.
                
                        En consecuencia, la solicitud no será remitida al Comité de Currículo y
                        la modalidad de grado continuará su desarrollo conforme al estado académico
                        vigente y a los lineamientos institucionales establecidos para el proceso
                        de modalidades de grado.
                
                        Esta notificación se emite con el fin de mantener la trazabilidad y el
                        registro formal del proceso académico dentro del sistema institucional.
                
                        Si requiere información adicional o desea realizar seguimiento al caso,
                        puede comunicarse con la Jefatura del Programa Académico correspondiente.
                
                        Atentamente,
                
                        Sistema de Gestión de Modalidades de Grado \s
                        Universidad Surcolombiana
                
                
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
                
                Le informamos que se ha registrado una solicitud formal de cancelación
                correspondiente a la siguiente modalidad de grado:
                
                Modalidad:
                "%s"
                
                Estudiantes vinculados al proceso:
                %s
                
                De acuerdo con el procedimiento académico establecido, esta solicitud
                requiere su revisión y concepto en calidad de Director/a de Proyecto.
                Una vez emitida su valoración, el caso será remitido al Comité de
                Currículo del Programa para su análisis y decisión final.
                
                Le agradecemos realizar la revisión correspondiente dentro de los
                plazos institucionales establecidos y efectuar el seguimiento del
                proceso a través del sistema.
                
                Atentamente,
                
                Sistema SIGMA
                Sistema de Gestión de Modalidades de Grado
                Universidad Surcolombiana
                
                """.formatted(
                sm.getProjectDirector().getName(),
                sm.getProgramDegreeModality().getDegreeModality().getName(),
                miembros

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
                
                Le informamos que ha sido designado/a oficialmente como Director/a de Proyecto
                para la siguiente modalidad de grado, conforme al registro realizado en el
                sistema institucional.
                
                A continuación, se presentan los datos asociados al proceso:
                
                Programa académico:
                "%s"
                
                Estudiantes vinculados al proyecto:
                %s
                
                Fecha de asignación:
                %s
                
                A partir de esta designación, usted asume la responsabilidad de orientar,
                supervisar y acompañar el desarrollo académico del proyecto de grado,
                garantizando el cumplimiento de los lineamientos, cronogramas y criterios
                de evaluación establecidos por el programa académico.
                
                Le recomendamos ingresar al sistema para consultar la información completa
                de la modalidad y realizar el seguimiento correspondiente al proceso.
                
                Atentamente,
                
                
                Sistema de Gestión de Modalidades de Grado
                Universidad Surcolombiana
                
                """.formatted(
                director.getName(),
                modality.getProgramDegreeModality().getAcademicProgram().getName(),
                miembros,
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
                
                Le informamos que la sustentación final correspondiente a la modalidad
                de grado bajo su dirección académica ha sido realizada y registrada
                oficialmente en el sistema.
                
                A continuación, se presentan los detalles del proceso:
                
                Modalidad de grado:
                "%s"
                
                Estudiantes:
                %s
                
                Resultado final:
                %s
                
                Distinción académica:
                %s
                
                Observaciones del jurado o comité evaluador:
                %s
                
                Este resultado marca la finalización del proceso académico de
                sustentación. En su calidad de Director/a de Proyecto, le recomendamos
                verificar el estado actualizado de la modalidad en el sistema y, si
                corresponde, coordinar los trámites académicos y administrativos
                posteriores con la jefatura del programa.
                
                Agradecemos el acompañamiento y la orientación brindados durante el
                desarrollo del proyecto de grado.
                
                Atentamente,
                
                Sistema SIGMA
                Sistema de Gestión de Modalidades de Grado
                Universidad Surcolombiana
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
            case EDIT_REQUESTED -> "Solicitud de edición pendiente de revisión por Jurado";
            case EDIT_REQUEST_APPROVED -> "Solicitud de edición aprobada por Jurado (el estudiante puede resubir el documento)";
            case EDIT_REQUEST_REJECTED -> "Solicitud de edición rechazada por Jurado";
        };
    }

    private String translateModalityProcessStatus(ModalityProcessStatus status) {
        if (status == null) return "N/A";
        return switch (status) {
            case MODALITY_SELECTED -> "Modalidad seleccionada";
            case UNDER_REVIEW_PROGRAM_HEAD -> "En revisión por Jefatura de programa y/o coordinación de modalidades";
            case CORRECTIONS_REQUESTED_PROGRAM_HEAD -> "Correcciones solicitadas por Jefatura";
            case CORRECTIONS_SUBMITTED -> "Correcciones enviadas";
            case CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD -> "Correcciones enviadas a Jefatura de Programa y/o coordinación de modalidades";
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
            case PENDING_PROGRAM_HEAD_FINAL_REVIEW -> "Pendiente revisión final por Jefatura de Programa";
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
            case PENDING_COMMITTEE_MERITORIOUS -> "Mención Meritoria propuesta (pendiente de aprobación del comité)";
            case PENDING_COMMITTEE_LAUREATE -> "Mención Laureada propuesta (pendiente de aprobación del comité)";
            case TIEBREAKER_PENDING_COMMITTEE_MERITORIOUS -> "Mención Meritoria propuesta por desempate (pendiente del comité)";
            case TIEBREAKER_PENDING_COMMITTEE_LAUREATE -> "Mención Laureada propuesta por desempate (pendiente del comité)";
        };
    }


}
