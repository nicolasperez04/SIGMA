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
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.common.util.TranslationUtils;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.event.ModalityEvent;
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
        StudentModality studentModality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        List<User> programHeads = userRepository.findAllByRoles_Name("PROGRAM_HEAD");
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(studentModality);
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
                .orElseThrow();
        StudentDocument document = studentDocumentRepository.findById(
                event.get(ModalityEvent.KEY_STUDENT_DOCUMENT_ID, Long.class))
                .orElseThrow();
        User student = modality.getLeader();
        List<User> programHeads = userRepository.findAllByRoles_Name("PROGRAM_HEAD");
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
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
        StudentModality studentModality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        List<User> programHeads = userRepository.findAllByRoles_Name("PROGRAM_HEAD");
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(studentModality);
        LocalDateTime defenseDate = event.get(ModalityEvent.KEY_DEFENSE_DATE, LocalDateTime.class);
        String defenseLocation = event.get(ModalityEvent.KEY_DEFENSE_LOCATION, String.class);
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
                TranslationUtils.getStudentList(studentModality),
                defenseDate.toString(),
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
        StudentModality studentModality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        List<User> programHeads = userRepository.findAllByRoles_Name("PROGRAM_HEAD");
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(studentModality);
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
                TranslationUtils.getStudentList(studentModality),
                studentModality.getProjectDirector()
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
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        User director = modality.getProjectDirector();
        if (director == null) {
            return;
        }
        List<User> programHeads = userRepository.findAllByRoles_Name("PROGRAM_HEAD");
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        ModalityProcessStatus finalStatus = event.get(ModalityEvent.KEY_FINAL_STATUS, ModalityProcessStatus.class);
        AcademicDistinction academicDistinction = event.get(ModalityEvent.KEY_ACADEMIC_DISTINCTION, AcademicDistinction.class);
        String observations = event.get(ModalityEvent.KEY_OBSERVATIONS, String.class);
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
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        List<User> programHeads = userRepository.findAllByRoles_Name("PROGRAM_HEAD");
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
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
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User director = modality.getProjectDirector();
        String directorNombre = director != null
                ? director.getName() + " " + director.getLastName()
                : "El director de proyecto";

        List<User> programHeads = userRepository.findAllByRoles_Name("PROGRAM_HEAD");

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

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
