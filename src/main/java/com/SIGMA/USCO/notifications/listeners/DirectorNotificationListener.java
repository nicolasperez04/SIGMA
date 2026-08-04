package com.SIGMA.USCO.notifications.listeners;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
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
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class DirectorNotificationListener {

    private final StudentModalityRepository studentModalityRepository;
    private final NotificationFactory notificationFactory;
    private final UserRepository userRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;

    @EventListener
    public void handleEvent(ModalityEvent event) {
        switch (event.getType()) {
            case MODALITY_CANCELLATION_APPROVED -> handleCancellationApproved(event);
            case MODALITY_CANCELLATION_REJECTED -> handleCancellationRejected(event);
            case MODALITY_CANCELLATION_REQUESTED -> handleCancellationRequested(event);
            case DIRECTOR_ASSIGNED -> handleDirectorAssigned(event);
            case DEFENSE_COMPLETED -> handleFinalDefenseResult(event);
            case DOCUMENT_UPLOADED -> handleStudentDocumentUpdated(event);
            default -> log.warn("Unhandled ModalityEvent type: {}", event.getType());
        }
    }

    private void handleCancellationApproved(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        if (sm.getProjectDirector() == null) return;

        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(sm.getId(), MemberStatus.ACTIVE);
        String miembros = members.stream()
            .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName() + " (" + m.getStudent().getEmail() + ")")
            .collect(Collectors.joining(", "));

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(sm);
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
                modalidadInfo,
                miembros
        );

        notificationFactory.buildAndDispatch(
                NotificationType.MODALITY_CANCELLATION_APPROVED,
                NotificationRecipientType.PROJECT_DIRECTOR,
                sm.getProjectDirector(),
                sm, subject, message
        );
    }

    private void handleCancellationRejected(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        if (sm.getProjectDirector() == null) return;

        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(sm.getId(), MemberStatus.ACTIVE);
        String miembros = members.stream()
            .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName() + " (" + m.getStudent().getEmail() + ")")
            .collect(Collectors.joining(", "));

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(sm);
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
                
                        Sistema de Gestión de Modalidades de Grado 
                        Universidad Surcolombiana
                
                
                """.formatted(
                sm.getProjectDirector().getName(),
                modalidadInfo,
                miembros
        );

        notificationFactory.buildAndDispatch(
                NotificationType.MODALITY_CANCELLATION_REJECTED,
                NotificationRecipientType.PROJECT_DIRECTOR,
                sm.getProjectDirector(),
                sm, subject, message
        );
    }

    private void handleCancellationRequested(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        if (sm.getProjectDirector() == null) return;

        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(sm.getId(), MemberStatus.ACTIVE);
        String miembros = members.stream()
            .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName() + " (" + m.getStudent().getEmail() + ")")
            .collect(Collectors.joining(", "));

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(sm);
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
                modalidadInfo,
                miembros
        );

        notificationFactory.buildAndDispatch(
                NotificationType.MODALITY_CANCELLATION_REQUESTED,
                NotificationRecipientType.PROJECT_DIRECTOR,
                sm.getProjectDirector(),
                sm, subject, message
        );
    }

    private void handleDirectorAssigned(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        User director = userRepository.findById(event.get(ModalityEvent.KEY_DIRECTOR_ID, Long.class)).orElseThrow();

        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
        String miembros = members.stream()
            .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName() + " (" + m.getStudent().getEmail() + ")")
            .collect(Collectors.joining(", "));

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String directorSubject = "Asignación como Director de Proyecto a modalidad de grado";
        String directorMessage = """
                Estimado/a %s,
                
                Reciba un cordial saludo.
                
                Le informamos que ha sido designado/a oficialmente como Director/a de Proyecto
                para la siguiente modalidad de grado, conforme al registro realizado en el
                sistema institucional.
                
                A continuación, se presentan los datos asociados al proceso:
                
                Modalidad de grado:
                "%s"
                
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
                modalidadInfo,
                modality.getProgramDegreeModality().getAcademicProgram().getName(),
                miembros,
                modality.getUpdatedAt()
        );

        notificationFactory.buildAndDispatch(
                NotificationType.DIRECTOR_ASSIGNED,
                NotificationRecipientType.PROJECT_DIRECTOR,
                director,
                modality, directorSubject, directorMessage
        );
    }

    private void handleFinalDefenseResult(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        User director = modality.getProjectDirector();
        if (director == null) return;

        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
        String miembros = members.stream()
            .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName() + " (" + m.getStudent().getEmail() + ")")
            .collect(Collectors.joining(", "));

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
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
                modalidadInfo,
                miembros,
                TranslationUtils.translateModalityProcessStatus(event.get(ModalityEvent.KEY_FINAL_STATUS, ModalityProcessStatus.class)),
                TranslationUtils.translateAcademicDistinction(event.get(ModalityEvent.KEY_ACADEMIC_DISTINCTION, AcademicDistinction.class)),
                event.get(ModalityEvent.KEY_OBSERVATIONS, String.class) != null ? event.get(ModalityEvent.KEY_OBSERVATIONS, String.class) : "N/A"
        );

        notificationFactory.buildAndDispatch(
                NotificationType.DEFENSE_COMPLETED,
                NotificationRecipientType.PROJECT_DIRECTOR,
                director,
                modality, subject, message
        );
    }

    private void handleStudentDocumentUpdated(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        if (modality.getProjectDirector() == null) return;

        StudentDocument document = studentDocumentRepository.findById(event.get(ModalityEvent.KEY_STUDENT_DOCUMENT_ID, Long.class)).orElseThrow();

        User student = modality.getLeader();
        User director = modality.getProjectDirector();

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Documento actualizado – Estudiante asignado";
        String message = """
                Estimado/a %s,
                
                Reciba un cordial saludo.
                
                Le informamos que el estudiante %s ha realizado una actualización en uno de los documentos asociados a la modalidad de grado que actualmente se encuentra bajo su dirección académica.
                
                A continuación, se detallan los datos correspondientes:
                
                Modalidad de grado:
                "%s"
                
                Documento actualizado:
                "%s"
                
                Estado actual del documento:
                %s
                
                Esta actualización puede requerir su revisión, validación o retroalimentación según el estado reportado y la fase del proceso académico.
                
                Le invitamos a ingresar a la plataforma institucional para consultar la versión más reciente del documento y continuar con el seguimiento académico correspondiente.
                
                Cordialmente,
                Sistema de Gestión Académica
            """.formatted(
                director.getName(),
                student.getName() + " " + student.getLastName(),
                modalidadInfo,
                document.getDocumentConfig().getDocumentName(),
                TranslationUtils.translateDocumentStatus(document.getStatus())
        );

        notificationFactory.buildAndDispatch(
                NotificationType.DOCUMENT_UPLOADED,
                NotificationRecipientType.PROJECT_DIRECTOR,
                director, student,
                modality, subject, message
        );
    }
}
