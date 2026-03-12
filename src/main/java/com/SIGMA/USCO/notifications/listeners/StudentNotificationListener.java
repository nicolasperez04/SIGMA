package com.SIGMA.USCO.notifications.listeners;

import com.SIGMA.USCO.Modalities.Entity.AcademicCertificate;
import com.SIGMA.USCO.Modalities.Entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.Entity.enums.CertificateStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.notifications.entity.Notification;
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.event.*;
import com.SIGMA.USCO.notifications.repository.NotificationRepository;
import com.SIGMA.USCO.notifications.service.AcademicCertificatePdfService;
import com.SIGMA.USCO.notifications.service.NotificationDispatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StudentNotificationListener {

    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDispatcherService dispatcher;
    private final UserRepository userRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final AcademicCertificatePdfService certificatePdfService;


    @EventListener
    public void ModalityStarted(StudentModalityStarted event){
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        User student = modality.getLeader();
        String subject = "Modalidad iniciada – SIGMA";
        String message = """
                Estimado/a %s,
                
                Reciba un cordial saludo.
                
                Le informamos que su modalidad de grado ha sido
                registrada e iniciada oficialmente en el sistema
                con el siguiente detalle:
                
                ───────────────────────────────
                MODALIDAD DE GRADO
                ───────────────────────────────
                "%s"
                
                ───────────────────────────────
                ESTADO ACTUAL DEL PROCESO
                ───────────────────────────────
                %s
                
                La modalidad se encuentra actualmente en etapa de
                revisión y evaluación por parte de la Jefatura de
                Programa y del Comité de Currículo correspondiente.
                
                ───────────────────────────────
                RECOMENDACIONES
                ───────────────────────────────
                Le recomendamos consultar periódicamente el sistema
                 y mantenerse atento/a a las notificaciones,
                ya que por este medio se comunicarán solicitudes,
                observaciones o decisiones relacionadas con su proceso.
        
                Sistema de Gestión Académica
        """.formatted(
            student.getName(),
            modality.getProgramDegreeModality().getDegreeModality().getName(),
            translateModalityProcessStatus(modality.getStatus())
        );
        Notification notification = Notification.builder()
                .type(NotificationType.MODALITY_STARTED)
                .recipientType(NotificationRecipientType.STUDENT)
                .recipient(student)
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
    public void onDocumentCorrectionsRequested(DocumentCorrectionsRequestedEvent event) {
        StudentDocument document = studentDocumentRepository.findById(event.getStudentDocumentId())
                .orElseThrow();
        StudentModality modality = document.getStudentModality();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
            modality.getId(),
            MemberStatus.ACTIVE
        );
        String subject = "Correcciones solicitadas en documento académico – Acción requerida";
        for (var member : members) {
            User student = member.getStudent();
            String message = """
        Estimado/a %s,

        Reciba un cordial saludo.

        Se informa que %s ha solicitado la realización de
        correcciones en uno de los documentos asociados
        a su modalidad de grado, conforme al proceso de revisión académica.

        ───────────────────────────────
        DOCUMENTO
        ───────────────────────────────
        "%s"

        ───────────────────────────────
        OBSERVACIONES REGISTRADAS
        ───────────────────────────────
        %s

        ───────────────────────────────
        ACCIÓN REQUERIDA
        ───────────────────────────────
        Se solicita ingresar a la plataforma, revisar
        detalladamente las observaciones indicadas y realizar
        los ajustes correspondientes para continuar con el
        proceso académico dentro de los plazos establecidos.

        Este mensaje constituye una notificación automática
        generada para efectos de control y trazabilidad
        del proceso.

        Sistema de Gestión Académica
        """.formatted(
            student.getName(),
            event.getRequestedBy() == NotificationRecipientType.PROGRAM_HEAD
                ? "la Jefatura de Programa y/o Coordinación de Modalidades"
                : "el Comité de Currículo del Programa",
            document.getDocumentConfig().getDocumentName(),
            event.getObservations() != null && !event.getObservations().isBlank()
                ? event.getObservations()
                : "No se registraron observaciones adicionales"
        );
            Notification notification = Notification.builder()
                    .type(NotificationType.DOCUMENT_CORRECTIONS_REQUESTED)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
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
    public void onCancellationRequested(CancellationRequestedEvent event){
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
            sm.getId(),
            MemberStatus.ACTIVE
        );
        String subject = "Solicitud de cancelación registrada – Modalidad de grado";
        for (var member : members) {
            User student = member.getStudent();
            String message = """
        Estimado/a %s,

        Reciba un cordial saludo.

        Le informamos que su solicitud de cancelación de la siguiente
        modalidad de grado ha sido registrada correctamente en el sistema:

        ───────────────────────────────
        MODALIDAD DE GRADO
        ───────────────────────────────
        "%s"

        ───────────────────────────────
        ESTADO DEL PROCESO
        ───────────────────────────────
        La solicitud será evaluada inicialmente por el director
        del proyecto y posteriormente por el Comité de Currículo
        del programa académico correspondiente.

        Una vez se emita una decisión oficial, usted será
        notificado/a oportunamente a través de la plataforma.

        Este mensaje constituye una notificación automática
        generada para efectos de control y trazabilidad
        del proceso académico.

        Sistema de Gestión Académica
        """.formatted(
            student.getName(),
            sm.getProgramDegreeModality().getDegreeModality().getName()
        );
            Notification notification = Notification.builder()
                    .type(NotificationType.MODALITY_CANCELLATION_REQUESTED)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
                    .triggeredBy(null)
                    .studentModality(sm)
                    .subject(subject)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
        }
    }

    @EventListener
    public void onCancellationApproved(CancellationApprovedEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
            sm.getId(),
            MemberStatus.ACTIVE
        );
        String subject = "Cancelación aprobada – Modalidad de grado";
        for (var member : members) {
            User student = member.getStudent();
            String message = """
        Estimado/a %s,

        Reciba un cordial saludo.

        Le informamos que el Comité de Currículo del programa académico
        ha aprobado oficialmente su solicitud de cancelación de la
        siguiente modalidad de grado:

        ───────────────────────────────
        MODALIDAD DE GRADO
        ───────────────────────────────
        "%s"

        ───────────────────────────────
        DECISIÓN
        ───────────────────────────────
        La modalidad queda cerrada de manera oficial y el
        proceso académico asociado finaliza a partir de la
        fecha de esta decisión.

        Si requiere orientación adicional o desea recibir
        información complementaria sobre su situación académica,
        puede comunicarse con la Jefatura de Programa.

        Esta notificación se genera automáticamente para
        efectos de control y trazabilidad institucional.

        Sistema de Gestión Académica – SIGMA
        """.formatted(
            student.getName(),
            sm.getProgramDegreeModality().getDegreeModality().getName()
        );
            Notification notification = Notification.builder()
                    .type(NotificationType.MODALITY_CANCELLATION_APPROVED)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
                    .triggeredBy(null)
                    .studentModality(sm)
                    .subject(subject)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
        }
    }

    @EventListener
    public void onCancellationRejected(CancellationRejectedEvent event){
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
            sm.getId(),
            MemberStatus.ACTIVE
        );
        String subject = "Cancelación no aprobada – Modalidad de grado";
        for (var member : members) {
            User student = member.getStudent();
            String message = """
        Estimado/a %s,

        Reciba un cordial saludo.

        Le informamos que el Comité de Currículo del programa académico
        ha decidido no aprobar su solicitud de cancelación de la
        siguiente modalidad de grado:

        ───────────────────────────────
        MODALIDAD DE GRADO
        ───────────────────────────────
        "%s"

        ───────────────────────────────
        MOTIVO DE LA DECISIÓN
        ───────────────────────────────
        %s

        En consecuencia, la modalidad de grado continúa activa
        bajo las condiciones previamente establecidas dentro
        del proceso académico.

        Si requiere mayor claridad sobre esta decisión o desea
        recibir orientación adicional, puede comunicarse con
        la Jefatura de Programa.

        Esta notificación se genera automáticamente para
        efectos de control y trazabilidad institucional.

        Sistema de Gestión Académica.
        """.formatted(
            student.getName(),
            sm.getProgramDegreeModality().getDegreeModality().getName(),
            event.getReason() != null ? event.getReason() : "No especificado"
        );
            Notification notification = Notification.builder()
                    .type(NotificationType.MODALITY_CANCELLATION_REJECTED)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
                    .triggeredBy(null)
                    .studentModality(sm)
                    .subject(subject)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
        }
    }

    @EventListener
    public void handleDefenseScheduled(DefenseScheduledEvent event) {

        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();

        User director = modality.getProjectDirector();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
            modality.getId(),
            MemberStatus.ACTIVE
        );
        String studentSubject =
                "Sustentación programada – Modalidad de Grado";

        for (var member : members) {
            User student = member.getStudent();
            String studentMessage = """
        Estimado/a %s,

        Reciba un cordial saludo.

        Le informamos que la sustentación correspondiente a su
        modalidad de grado ha sido programada con la siguiente información:

        ───────────────────────────────
        MODALIDAD DE GRADO
        ───────────────────────────────
        "%s"

        ───────────────────────────────
        DETALLES DE LA SUSTENTACIÓN
        ───────────────────────────────
        Fecha y hora: %s
        Lugar: %s
        Director asignado: %s

        De acuerdo con la normativa institucional, usted deberá realizar
        la divulgación pública de su proyecto con al menos tres (3) días
        hábiles de anticipación a la fecha de la sustentación, en lugares
        visibles y de acceso público definidos por el programa académico.

        Se recomienda presentarse con la debida antelación y cumplir
        estrictamente con los lineamientos académicos establecidos
        para el desarrollo de la sesión de defensa.

        

        Sistema de Gestión Académica
        """.formatted(
                student.getName(),
                modality.getProgramDegreeModality().getDegreeModality().getName(),
                event.getDefenseDate(),
                event.getDefenseLocation(),
                director != null
                        ? director.getName() + " " + director.getLastName()
                        : "No asignado"
        );
            Notification notification = Notification.builder()
                    .type(NotificationType.DEFENSE_SCHEDULED)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
                    .triggeredBy(null)
                    .studentModality(modality)
                    .subject(studentSubject)
                    .message(studentMessage)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
        }
    }

    @EventListener
    public void DirectorAssigned(DirectorAssignedEvent event){

        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                        .orElseThrow();

        User director = userRepository.findById(event.getDirectorId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
            modality.getId(),
            MemberStatus.ACTIVE
        );
        String studentSubject =
                "Director de proyecto asignado – Modalidad de grado";
        for (var member : members) {
            User student = member.getStudent();
            String studentMessage = """
        Estimado/a %s,

        Reciba un cordial saludo.

        Le informamos que ha sido designado oficialmente un
        Director de Proyecto para su modalidad de grado,
        conforme a los lineamientos académicos vigentes.

        ───────────────────────────────
        MODALIDAD DE GRADO
        ───────────────────────────────
        "%s"

        ───────────────────────────────
        DIRECTOR ASIGNADO
        ───────────────────────────────
        Nombre: %s
        Correo electrónico: %s

        A partir de este momento, el director asignado será
        su orientador académico principal durante el desarrollo
        de la modalidad de grado y el responsable del seguimiento
        del proceso.

        Se recomienda establecer contacto oportunamente con el
        director para coordinar las actividades iniciales y
        definir el plan de trabajo correspondiente.

        

        Sistema de Gestión Académica
        """.formatted(
                student.getName(),
                modality.getProgramDegreeModality().getDegreeModality().getName(),
                director.getName() + " " + director.getLastName(),
                director.getEmail()
        );
            Notification notification = Notification.builder()
                    .type(NotificationType.DIRECTOR_ASSIGNED)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
                    .triggeredBy(null)
                    .studentModality(modality)
                    .subject(studentSubject)
                    .message(studentMessage)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
        }
    }

    @EventListener
    public void handleDefenseResult(FinalDefenseResultEvent event){

        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                        .orElseThrow();

        boolean approved = event.getFinalStatus() == ModalityProcessStatus.GRADED_APPROVED;

        String studentSubject = approved
                ? "Resultado final de sustentación – Modalidad aprobada"
                : "Resultado final de sustentación – Modalidad no aprobada";

        // Obtener todos los miembros activos
        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
            modality.getId(), MemberStatus.ACTIVE
        );

        for (StudentModalityMember member : members) {
            User student = member.getStudent();
            String studentMessage = approved
                    ? buildApprovedStudentMessage(student, modality, event)
                    : buildRejectedStudentMessage(student, modality, event);

            Notification notification = Notification.builder()
                    .type(NotificationType.DEFENSE_COMPLETED)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
                    .triggeredBy(null)
                    .studentModality(modality)
                    .subject(studentSubject)
                    .message(studentMessage)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);

            if (approved) {
                try {
                    log.info("Generando acta de aprobación para la modalidad ID: {}", modality.getId());


                    AcademicCertificate certificate = certificatePdfService.generateCertificate(modality);


                    Path pdfPath = certificatePdfService.getCertificatePath(modality.getId());


                    dispatcher.dispatchWithAttachment(
                            notification,
                            pdfPath,
                            "ACTA_DE_APROBACION.pdf"
                    );


                    certificatePdfService.updateCertificateStatus(certificate.getId(), CertificateStatus.SENT);

                    log.info("Acta PDF generada y enviada exitosamente para la modalidad ID: {}", modality.getId());

                } catch (IOException e) {
                    log.error("Error generando o enviando acta PDF para modalidad ID {}: {}",
                            modality.getId(), e.getMessage(), e);


                    dispatcher.dispatch(notification);
                }
            } else {

                dispatcher.dispatch(notification);
            }
        }
    }

    private String buildApprovedStudentMessage(User student, StudentModality modality, FinalDefenseResultEvent event) {
        String observaciones = event.getObservations();
        // Detecta si la observación contiene la distinción propuesta y confirmada
        if (observaciones != null && observaciones.contains("Distinción propuesta:") && observaciones.contains("Distinción confirmada:")) {
            // Extrae los enums de la observación
            String regex = "Distinción propuesta: ([A-Z_]+) → Distinción confirmada: ([A-Z_]+)";
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(observaciones);
            if (matcher.find()) {
                String propuesta = matcher.group(1);
                String confirmada = matcher.group(2);
                AcademicDistinction propuestaEnum = null;
                AcademicDistinction confirmadaEnum = null;
                try {
                    propuestaEnum = AcademicDistinction.valueOf(propuesta);
                    confirmadaEnum = AcademicDistinction.valueOf(confirmada);
                } catch (Exception ignored) {}
                String propuestaLabel = propuestaEnum != null ? translateAcademicDistinction(propuestaEnum) : propuesta;
                String confirmadaLabel = confirmadaEnum != null ? translateAcademicDistinction(confirmadaEnum) : confirmada;
                // Reemplaza en la observación
                observaciones = observaciones.replace(
                    "Distinción propuesta: " + propuesta + " → Distinción confirmada: " + confirmada,
                    "Distinción propuesta: " + propuestaLabel + " → Distinción confirmada: " + confirmadaLabel
                );
            }
        }
        return """
            Estimado/a %s,

            Reciba un cordial saludo.

            Nos permitimos informarle que, una vez realizada la sustentación
            y evaluado el resultado por los jurados designados, ha aprobado
            oficialmente la modalidad de grado:

            ───────────────────────────────
            MODALIDAD DE GRADO
            ───────────────────────────────
            "%s"

            ───────────────────────────────
            RESULTADO ACADÉMICO
            ───────────────────────────────
            Mención académica: %s
            Observaciones registradas: %s

            Se adjunta a este correo el ACTA DE APROBACIÓN en formato PDF,
            documento oficial que certifica la culminación satisfactoria
            de su modalidad de grado conforme a la normatividad académica vigente.

            ───────────────────────────────
            PRÓXIMOS PASOS
            ───────────────────────────────
            Para finalizar su proceso académico, deberá comunicarse con la
            Jefatura de Programa con el fin de adelantar los trámites
            administrativos correspondientes.

            Reciba nuestras felicitaciones por este importante logro académico.



            Sistema de Gestión Académica
            Universidad Surcolombiana
            """.formatted(
                student.getName(),
                modality.getProgramDegreeModality().getDegreeModality().getName(),
                translateAcademicDistinction(event.getAcademicDistinction()),
                observaciones != null && !observaciones.isBlank() ? observaciones : "Ninguna"
        );
    }

    private String buildRejectedStudentMessage(User student, StudentModality modality, FinalDefenseResultEvent event) {
        return """
            Estimado/a %s,

            Reciba un cordial saludo.

            Nos permitimos informarle que, una vez realizada la sustentación
            y evaluado el resultado por los jurados designados, la modalidad
            de grado relacionada a continuación no ha sido aprobada en esta oportunidad:

            ───────────────────────────────
            MODALIDAD DE GRADO
            ───────────────────────────────
            "%s"

            ───────────────────────────────
            OBSERVACIONES DE LOS JURADOS
            ───────────────────────────────
            %s

            De acuerdo con la normativa académica vigente, le recomendamos
            revisar detenidamente las observaciones consignadas y establecer
            comunicación con su Director de Proyecto, así como con la
            Jefatura de Programa, con el fin de definir los pasos a seguir
            dentro del proceso académico correspondiente.



            Sistema de Gestión Académica – SIGMA
            Universidad Surcolombiana
            """.formatted(
                student.getName(),
                modality.getProgramDegreeModality().getDegreeModality().getName(),
                event.getObservations() != null && !event.getObservations().isBlank()
                        ? event.getObservations()
                        : "No se registraron observaciones adicionales"
        );
    }

    @EventListener
    public void ModalityApprovedByCommittee(ModalityApprovedByCommitteeEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
            modality.getId(),
            MemberStatus.ACTIVE
        );
        String subject = "Modalidad de grado aprobada – Comité de Currículo";
        for (var member : members) {
            User student = member.getStudent();
            String message = """
        Estimado/a %s,

        Reciba un cordial saludo.

        Nos permitimos informarle que su modalidad de grado
        relacionada a continuación ha sido aprobada oficialmente
        por el Comité de Currículo del programa académico:

        ───────────────────────────────
        MODALIDAD DE GRADO
        ───────────────────────────────
        "%s"

        ───────────────────────────────
        ESTADO ACTUAL DEL PROCESO
        ───────────────────────────────
        Propuesta aprobada por el Comité de Currículo.

        Director de Proyecto: %s
        Fecha de aprobación: %s

        A partir de esta decisión, la modalidad continúa con la
        siguiente etapa del proceso académico, correspondiente
        a la evaluación y aprobación por parte del jurado designado.

        Se recomienda mantenerse atento/a a las notificaciones
        del sistema y conservar comunicación permanente con su
        Director de Proyecto y con la Jefatura de Programa para
        el adecuado seguimiento del proceso.

        

        Sistema de Gestión Académica 
        Universidad Surcolombiana
        """.formatted(
            student.getName(),
            modality.getProgramDegreeModality().getDegreeModality().getName(),
            modality.getProjectDirector() != null
                ? modality.getProjectDirector().getName() + " " +
                  modality.getProjectDirector().getLastName()
                : "Aún no asignado",
            modality.getUpdatedAt()
        );
            Notification notification = Notification.builder()
                    .type(NotificationType.MODALITY_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
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
    public void ModalityApprovedByProgramHead(ModalityApprovedByProgramHead event) {

        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
            modality.getId(),
            MemberStatus.ACTIVE
        );
        String subject = "Modalidad de grado aprobada – Jefatura de Programa y/o Coordinación de Modalidades";
        for (var member : members) {
            User student = member.getStudent();
            String message = """
        Estimado/a %s,

        Reciba un cordial saludo.

        Nos permitimos informarle que la siguiente modalidad de grado
        ha sido aprobada oficialmente por la Jefatura del programa académico y/o coordinador de modalidades:

        ───────────────────────────────
        MODALIDAD DE GRADO
        ───────────────────────────────
        "%s"

        ───────────────────────────────
        ESTADO ACTUAL DEL PROCESO
        ───────────────────────────────
        Aprobada por Jefatura de Programa.

        La modalidad continuará con la etapa de evaluación por parte
        del Comité de Currículo del programa académico, instancia que
        deberá emitir la decisión correspondiente para dar continuidad
        al proceso.

        Se recomienda mantenerse atento/a a las notificaciones del sistema
        y conservar comunicación con la Jefatura de Programa ante cualquier
        inquietud relacionada con el trámite.

     
        Sistema de Gestión Académica 
        Universidad Surcolombiana
        """.formatted(
            student.getName(),
            modality.getProgramDegreeModality().getDegreeModality().getName()
        );
            Notification notification = Notification.builder()
                    .type(NotificationType.MODALITY_APPROVED_BY_PROGRAM_HEAD)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
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
    public void handleCorrectionDeadlineReminder(CorrectionDeadlineReminderEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
            modality.getId(),
            MemberStatus.ACTIVE
        );
        String subject = "Recordatorio oficial – Plazo de correcciones (%d días restantes)"
                .formatted(event.getDaysRemaining());
        for (var member : members) {
            User student = member.getStudent();
            String message = """
        Estimado/a %s,

        Reciba un cordial saludo.

        Le recordamos que actualmente tiene correcciones pendientes
        asociadas a la siguiente modalidad de grado:

        ───────────────────────────────
        MODALIDAD DE GRADO
        ───────────────────────────────
        "%s"

        ───────────────────────────────
        PLAZO LÍMITE DE ENTREGA
        ───────────────────────────────
        Días restantes: %d
        Fecha límite: %s

        Es indispensable que realice las correcciones solicitadas y
        cargue nuevamente el documento antes de la fecha indicada.
        En caso de no cumplir con el plazo establecido, el sistema
        procederá con la cancelación automática de la modalidad,
        conforme a la normativa académica vigente.

        ───────────────────────────────
        PROCEDIMIENTO PARA CARGAR EL DOCUMENTO
        ───────────────────────────────
        1. Realice las correcciones indicadas en su documento.
        2. Ingrese a la plataforma.
        3. Acceda al módulo "Mis Documentos".
        4. Seleccione el documento que desea actualizar y cargue la versión corregida.

        Si presenta alguna dificultad o requiere orientación adicional,
        deberá comunicarse a la mayor brevedad con la Jefatura de Programa.

        Esta notificación se genera automáticamente como recordatorio
        preventivo dentro del proceso académico.

        Sistema de Gestión Académica – SIGMA
        Universidad Surcolombiana
        """.formatted(
            student.getName(),
            modality.getProgramDegreeModality().getDegreeModality().getName(),
            event.getDaysRemaining(),
            event.getDeadline().toLocalDate()
        );
            Notification notification = Notification.builder()
                    .type(NotificationType.CORRECTION_DEADLINE_REMINDER)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
                    .triggeredBy(null)
                    .studentModality(modality)
                    .subject(subject)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
            log.info("Recordatorio de plazo de corrección enviado al estudiante {}", student.getId());
        }
    }

    @EventListener
    public void handleCorrectionDeadlineExpired(CorrectionDeadlineExpiredEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
            modality.getId(),
            MemberStatus.ACTIVE
        );
        String subject = "Notificación oficial – Cancelación automática de modalidad por vencimiento de plazo";
        for (var member : members) {
            User student = member.getStudent();
            String message = """
            Estimado/a %s,

            Reciba un cordial saludo.

            Por medio de la presente, se le informa que la siguiente modalidad de grado:

            ───────────────────────────────
            "%s"
            ───────────────────────────────

            ha sido CANCELADA AUTOMÁTICAMENTE debido al vencimiento
            del plazo establecido para la entrega de las correcciones solicitadas,
            sin que se haya efectuado la carga del documento corregido dentro del
            término reglamentario.

            ───────────────────────────────
            DETALLES DEL PROCESO
            ───────────────────────────────
            Fecha de solicitud de correcciones: %s
            Plazo máximo otorgado: 30 días calendario
            Estado final del proceso: CANCELADA

            La cancelación se realiza conforme a la normativa académica vigente
            y al reglamento institucional aplicable a las modalidades de grado.

            ───────────────────────────────
            PROCEDIMIENTO PARA CONTINUAR SU PROCESO DE GRADO
            ───────────────────────────────
            Para retomar su proceso académico deberá:
            1. Postular una nueva modalidad de grado.
            2. Iniciar nuevamente el procedimiento desde la etapa inicial.
            3. Cumplir con los requisitos y tiempos establecidos por el programa académico.

            Se recomienda comunicarse con la Jefatura del Programa para recibir
            orientación formal sobre los pasos a seguir.

            Esta notificación es generada automáticamente por el Sistema
            de Gestión Académica como constancia del cierre del proceso.

            Sistema de Gestión Académica
            Universidad Surcolombiana
            """.formatted(
                student.getName(),
                modality.getProgramDegreeModality().getDegreeModality().getName(),
                event.getRequestDate().toLocalDate()
        );
            Notification notification = Notification.builder()
                    .type(NotificationType.CORRECTION_DEADLINE_EXPIRED)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
                    .triggeredBy(null)
                    .studentModality(modality)
                    .subject(subject)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
            log.info("Notificación de cancelación por vencimiento enviada al estudiante {}", student.getId());
        }
    }

    @EventListener
    public void handleCorrectionResubmitted(CorrectionResubmittedEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
            modality.getId(),
            MemberStatus.ACTIVE
        );
        String subject = "Notificación oficial – Documento corregido recibido";
        for (var member : members) {
            User student = member.getStudent();
            String message = """
            Estimado/a %s,

            Reciba un cordial saludo.

            El Sistema de Gestión Académica ha registrado correctamente
            la carga del documento corregido correspondiente a la siguiente modalidad de grado:

            ───────────────────────────────
            MODALIDAD DE GRADO
            ───────────────────────────────
            "%s"

            ───────────────────────────────
            DETALLE DEL DOCUMENTO
            ───────────────────────────────
            Nombre del archivo: %s
            Fecha de envío: %s

            Estado actual del proceso:
            CORRECCIONES ENVIADAS – PENDIENTE DE REVISIÓN

            El documento será evaluado por las autoridades competentes.
            Una vez finalice la revisión, recibirá la notificación oficial
            con el resultado correspondiente.

            Le recomendamos permanecer atento/a a futuras comunicaciones
            emitidas por el sistema.

            Esta notificación es generada automáticamente como constancia
            del registro de la nueva versión del documento.

            Sistema de Gestión Académica
            Universidad Surcolombiana
            """.formatted(
                student.getName(),
                modality.getProgramDegreeModality().getDegreeModality().getName(),
                event.getDocumentName(),
                LocalDateTime.now().toLocalDate()
        );
            Notification notification = Notification.builder()
                    .type(NotificationType.CORRECTION_RESUBMITTED)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
                    .triggeredBy(null)
                    .studentModality(modality)
                    .subject(subject)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
            log.info("Notificación de resubmisión de corrección enviada al estudiante {}", student.getId());
        }
    }

    @EventListener
    public void handleCorrectionApproved(CorrectionApprovedEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
            modality.getId(),
            MemberStatus.ACTIVE
        );
        String subject = "Notificación oficial – Correcciones aprobadas";
        for (var member : members) {
            User student = member.getStudent();
            String message = """
            Estimado/a %s,

            Reciba un cordial saludo.

            Nos permitimos informarle que las correcciones remitidas
            han sido APROBADAS por el jurado evaluador.

            ───────────────────────────────
            MODALIDAD DE GRADO
            ───────────────────────────────
            "%s"

            ───────────────────────────────
            DOCUMENTO EVALUADO
            ───────────────────────────────
            Nombre del archivo: %s

            Estado actual del proceso:
            CORRECCIONES APROBADAS

            En consecuencia, su modalidad de grado continúa
            con el desarrollo normal del procedimiento académico,
            conforme a las disposiciones institucionales vigentes.

            Recibirá notificación oficial cuando se genere
            la siguiente actuación dentro del proceso.

            Esta comunicación es generada automáticamente
            por el Sistema de Gestión Académica como constancia
            de la decisión registrada.

            Sistema de Gestión Académica 
            Universidad Surcolombiana
            """.formatted(
                student.getName(),
                modality.getProgramDegreeModality().getDegreeModality().getName(),
                event.getDocumentName()
        );
            Notification notification = Notification.builder()
                    .type(NotificationType.CORRECTION_APPROVED)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
                    .triggeredBy(null)
                    .studentModality(modality)
                    .subject(subject)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
            log.info("Notificación de aprobación de corrección enviada al estudiante {}", student.getId());
        }
    }

    @EventListener
    public void handleCorrectionRejectedFinal(CorrectionRejectedFinalEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
            modality.getId(),
            MemberStatus.ACTIVE
        );
        String subject = "Notificación oficial – Cancelación de modalidad por rechazo definitivo de correcciones";
        for (var member : members) {
            User student = member.getStudent();
            String message = """
            Estimado/a %s,

            Reciba un cordial saludo.

            Por medio de la presente se le informa que, tras la evaluación
            realizada por el jurado designado, las correcciones remitidas
            no fueron aprobadas, razón por la cual se procede a la
            CANCELACIÓN DEFINITIVA de la siguiente modalidad de grado:

            ───────────────────────────────
            "%s"
            ───────────────────────────────

            Documento evaluado:
            %s

            Estado final del proceso:
            RECHAZADO – MODALIDAD CANCELADA

            ───────────────────────────────
            MOTIVO REGISTRADO
            ───────────────────────────────
            %s

            La presente decisión se adopta conforme a la normativa
            académica vigente aplicable a las modalidades de grado.

            ───────────────────────────────
            PROCEDIMIENTO PARA CONTINUAR SU PROCESO DE GRADO
            ───────────────────────────────
            Para continuar con su proceso académico deberá:
            1. Postular una nueva modalidad de grado.
            2. Iniciar nuevamente el procedimiento desde la etapa inicial.
            3. Cumplir con los requisitos y términos establecidos por el programa.

            Se recomienda comunicarse con la Jefatura del Programa
            para recibir orientación formal sobre las alternativas disponibles.

            Esta notificación es generada automáticamente por el
            Sistema de Gestión Académica como constancia del cierre definitivo del proceso.

            Sistema de Gestión Académica – SIGMA
            Universidad Surcolombiana
            """.formatted(
                student.getName(),
                modality.getProgramDegreeModality().getDegreeModality().getName(),
                event.getDocumentName(),
                event.getReason()
        );
            Notification notification = Notification.builder()
                    .type(NotificationType.CORRECTION_REJECTED_FINAL)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
                    .triggeredBy(null)
                    .studentModality(modality)
                    .subject(subject)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
            log.info("Notificación de rechazo final de corrección enviada al estudiante {}", student.getId());
        }
    }

    @EventListener
    public void handleModalityClosedByCommittee(ModalityClosedByCommitteeEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();

        User committeeMember = userRepository.findById(event.getCommitteeMemberId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
            modality.getId(),
            MemberStatus.ACTIVE
        );
        String subject = "Notificación oficial – Cierre de modalidad por decisión del Comité de Currículo";
        for (var member : members) {
            User student = member.getStudent();
            String message = """
            Estimado/a %s,

            Reciba un cordial saludo.

            Por medio de la presente se le informa que el Comité de Currículo
            del Programa ha decidido el CIERRE de la siguiente modalidad de grado:

            ───────────────────────────────
            "%s"
            ───────────────────────────────

            Programa académico:
            %s

            Estado del proceso:
            MODALIDAD CERRADA

            Decisión adoptada por:
            %s %s

            Fecha de registro de la decisión:
            %s

            ───────────────────────────────
            MOTIVO DEL CIERRE
            ───────────────────────────────
            %s

            La decisión se adopta conforme a la normativa académica
            vigente y a las competencias del Comité de Currículo.

            ───────────────────────────────
            ORIENTACIÓN PARA CONTINUAR EL PROCESO
            ───────────────────────────────
            Para continuar con su proceso de grado se recomienda:

            1. Solicitar orientación formal ante la Jefatura del Programa.
            2. Recibir asesoría académica sobre las alternativas disponibles.
            3. En caso de ser procedente, iniciar una nueva modalidad de grado
               conforme al reglamento institucional.

            Esta comunicación es generada automáticamente por el
            Sistema de Gestión Académica como constancia de la decisión registrada.

            Sistema de Gestión Académica
            Universidad Surcolombiana
                """.formatted(
                student.getName(),
                modality.getProgramDegreeModality().getDegreeModality().getName(),
                modality.getAcademicProgram().getName(),
                committeeMember.getName(),
                committeeMember.getLastName(),
                LocalDateTime.now().toString(),
                event.getReason()
        );
            Notification notification = Notification.builder()
                    .type(NotificationType.MODALITY_CLOSED_BY_COMMITTEE)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
                    .triggeredBy(committeeMember)
                    .studentModality(modality)
                    .subject(subject)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
            log.info("Notificación de cierre de modalidad por comité enviada al estudiante {}", student.getId());
        }
    }

    @EventListener
    public void onModalityInvitationSent(ModalityInvitationSentEvent event) {

        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User invitee = userRepository.findById(event.getInviteeId())
                .orElseThrow(() -> new RuntimeException("Estudiante invitado no encontrado"));

        User inviter = userRepository.findById(event.getInviterId())
                .orElseThrow(() -> new RuntimeException("Estudiante que invita no encontrado"));

        String subject = "Invitación para unirte a una modalidad de grado grupal – SIGMA";

        String message = """
                Estimado/a %s,
                
                Has recibido una invitación para unirte a una modalidad de grado grupal.
                
                **Detalles de la invitación:**
                
                - **Modalidad:** %s
                - **Programa académico:** %s
                - **Invitado por:** %s
                - **Fecha de invitación:** %s
                
                **¿Qué significa esto?**
                
                %s te ha invitado a formar parte de su grupo para desarrollar la modalidad de grado de manera colaborativa. 
                Si aceptas esta invitación, formarás parte del equipo y podrás trabajar en conjunto en todos los documentos 
                y actividades requeridas para completar la modalidad.
                
                **Consideraciones importantes:**
                
                - Solo puedes pertenecer a una modalidad de grado a la vez.
                - Al aceptar la invitación, te comprometes a trabajar de forma colaborativa con el grupo.
                - Puedes aceptar o rechazar la invitación desde la plataforma.
                
                
                **¿Cómo responder?**
                
                1. Ingresa a la plataforma SIGMA
                2. Dirígete a la sección de "Mis Invitaciones" o "Notificaciones"
                3. Revisa los detalles de la invitación
                4. Acepta o rechaza según tu decisión
                
                Te recomendamos coordinar con %s antes de tomar una decisión, para asegurar 
                que todos los miembros del grupo estén alineados con los objetivos y compromisos del proyecto.
                
                Cordialmente,
                
                Sistema Interno de Gestión Académica
                Universidad Surcolombiana - SIGMA
                """.formatted(
                invitee.getName(),
                modality.getProgramDegreeModality().getDegreeModality().getName(),
                modality.getAcademicProgram().getName(),
                inviter.getName() + " " + inviter.getLastName(),
                LocalDateTime.now().toString(),
                inviter.getName() + " " + inviter.getLastName(),
                inviter.getName()
        );

        Notification notification = Notification.builder()
                .type(NotificationType.MODALITY_INVITATION_RECEIVED)
                .recipientType(NotificationRecipientType.STUDENT)
                .recipient(invitee)
                .triggeredBy(inviter)
                .studentModality(modality)
                .invitationId(event.getInvitationId())
                .subject(subject)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        dispatcher.dispatch(notification);

        log.info("Notificación de invitación a modalidad grupal enviada al estudiante {} por el estudiante {}",
                invitee.getId(), inviter.getId());
    }


    @EventListener
    public void onModalityInvitationAccepted(ModalityInvitationAcceptedEvent event) {

        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User acceptedBy = userRepository.findById(event.getAcceptedById())
                .orElseThrow(() -> new RuntimeException("Estudiante que aceptó no encontrado"));

        User leader = userRepository.findById(event.getLeaderId())
                .orElseThrow(() -> new RuntimeException("Líder del grupo no encontrado"));

        String subject = "Un estudiante aceptó tu invitación a la modalidad grupal – SIGMA";

        String message = """
                Estimado/a %s,
                
                ¡Buenas noticias! Un estudiante ha aceptado tu invitación para unirse a tu modalidad de grado grupal.
                
                **Detalles de la aceptación:**
                
                - **Estudiante:** %s
                - **Modalidad:** %s
                - **Programa académico:** %s
                - **Fecha de aceptación:** %s
                
                **¿Qué sigue ahora?**
                
                %s ahora es parte oficial de tu grupo de modalidad. Pueden trabajar juntos en:
                
                - Subir y actualizar documentos compartidos.
                - Coordinar actividades y entregas.
                - Preparar presentaciones y sustentaciones en equipo.
                
                **Próximos pasos:**
                
                1. Coordina con tu equipo los roles y responsabilidades
                2. Establece canales de comunicación efectivos
                3. Planifica el desarrollo del proyecto o actividad
                4. Comienza a trabajar en los documentos requeridos
                
                Recuerda que todos los miembros del grupo tienen los mismos derechos y responsabilidades 
                dentro de la modalidad, y todos pueden subir o actualizar los documentos necesarios.
                
                ¡Mucho éxito en su trabajo colaborativo!
                
                Cordialmente,
                
                Sistema Interno de Gestión Académica
                Universidad Surcolombiana - SIGMA
                """.formatted(
                leader.getName(),
                acceptedBy.getName() + " " + acceptedBy.getLastName(),
                modality.getProgramDegreeModality().getDegreeModality().getName(),
                modality.getAcademicProgram().getName(),
                LocalDateTime.now().toString(),
                acceptedBy.getName()
        );

        Notification notification = Notification.builder()
                .type(NotificationType.MODALITY_INVITATION_ACCEPTED)
                .recipientType(NotificationRecipientType.STUDENT)
                .recipient(leader)
                .triggeredBy(acceptedBy)
                .studentModality(modality)
                .subject(subject)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        dispatcher.dispatch(notification);

        log.info("Notificación de aceptación de invitación enviada al líder {} por el estudiante {}",
                leader.getId(), acceptedBy.getId());
    }


    @EventListener
    public void onModalityInvitationRejected(ModalityInvitationRejectedEvent event) {

        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User rejectedBy = userRepository.findById(event.getRejectedById())
                .orElseThrow(() -> new RuntimeException("Estudiante que rechazó no encontrado"));

        User leader = userRepository.findById(event.getLeaderId())
                .orElseThrow(() -> new RuntimeException("Líder del grupo no encontrado"));

        String subject = "Un estudiante rechazó tu invitación a la modalidad grupal – SIGMA";

        String message = """
                Estimado/a %s,
                
                Te informamos que un estudiante ha rechazado tu invitación para unirse a tu modalidad de grado grupal.
                
                **Detalles del rechazo:**
                
                - **Estudiante:** %s
                - **Modalidad:** %s
                - **Programa académico:** %s
                - **Fecha de rechazo:** %s
                
                **¿Qué significa esto?**
                
                %s decidió no formar parte de tu grupo para esta modalidad. Esto puede deberse a diversas razones:
                
                - Ya tiene compromisos con otros grupos o proyectos
                - Prefiere realizar la modalidad de forma individual
                - No puede cumplir con los requisitos o tiempos del proyecto
                - Tiene otros planes académicos
                
                **¿Qué puedes hacer ahora?**
                
                1. **Invitar a otro estudiante:** Puedes enviar una nueva invitación a otro compañero que esté disponible
                2. **Continuar con el grupo actual:** Si ya tienes otros miembros, pueden continuar con la modalidad
                3. **Realizar la modalidad de forma individual:** Si prefieres, puedes continuar solo
                
                Recuerda que tienes hasta **%d** miembros máximo (incluyéndote) para formar el grupo. 
                Actualmente tienes %d miembro(s) activo(s) en tu modalidad.
                
                **Próximos pasos:**
                
                - Revisa la lista de estudiantes elegibles para invitar
                - Coordina con los miembros actuales del grupo (si los hay)
                - Asegúrate de que todos estén alineados con los objetivos del proyecto
                
                No te desanimes, puedes invitar a otros compañeros que estén interesados en trabajar contigo.
                
                Cordialmente,
                
                Sistema Interno de Gestión Académica
                Universidad Surcolombiana - SIGMA
                """.formatted(
                leader.getName(),
                rejectedBy.getName() + " " + rejectedBy.getLastName(),
                modality.getProgramDegreeModality().getDegreeModality().getName(),
                modality.getAcademicProgram().getName(),
                LocalDateTime.now().toString(),
                rejectedBy.getName(),
                3, // MAX_GROUP_SIZE
                studentModalityMemberRepository.countByStudentModalityIdAndStatus(
                        modality.getId(),
                        MemberStatus.ACTIVE
                )
        );

        Notification notification = Notification.builder()
                .type(NotificationType.MODALITY_INVITATION_REJECTED)
                .recipientType(NotificationRecipientType.STUDENT)
                .recipient(leader)
                .triggeredBy(rejectedBy)
                .studentModality(modality)
                .subject(subject)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        dispatcher.dispatch(notification);

        log.info("Notificación de rechazo de invitación enviada al líder {} por el estudiante {}",
                leader.getId(), rejectedBy.getId());
    }


    @EventListener
    public void handleModalityFinalApprovedByCommittee(ModalityFinalApprovedByCommitteeEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User committeeMember = userRepository.findById(event.getCommitteeMemberId())
                .orElseThrow(() -> new RuntimeException("Miembro del comité no encontrado"));

        // Obtener todos los miembros activos de la modalidad
        List<StudentModalityMember> activeMembers = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);

        String subject = "¡Felicitaciones! — Modalidad de Grado Aprobada por el Comité de Currículo";

        // Generar el acta simplificada UNA SOLA VEZ (la misma para todos los miembros)
        AcademicCertificate certificate = null;
        Path pdfPath = null;
        try {
            log.info("Generando acta simplificada (comité) para la modalidad ID: {}", modality.getId());
            certificate = certificatePdfService.generateCertificateForCommitteeApproval(modality);
            pdfPath = certificatePdfService.getCertificatePath(modality.getId());
            log.info("Acta simplificada generada exitosamente: {}", pdfPath);
        } catch (IOException e) {
            log.error("Error generando acta simplificada para modalidad ID {}: {}",
                    modality.getId(), e.getMessage(), e);
        }

        for (StudentModalityMember memberEntry : activeMembers) {
            User student = memberEntry.getStudent();

            String message = """
                    Estimado/a %s %s,

                    Reciba un cordial y afectuoso saludo de la Universidad Surcolombiana.

                    Nos complace informarle que su modalidad de grado:

                        "%s"

                    ha sido APROBADA DEFINITIVAMENTE por el Comité de Currículo del Programa \
                    Académico de %s, de %s.

                    ─────────────────────────────────────────
                    INFORMACIÓN DEL PROCESO
                    ─────────────────────────────────────────
                    • Programa académico : %s
                    • Facultad           : %s
                    • Aprobado por       : %s %s (Comité de Currículo)
                    • Fecha de aprobación: %s
                    %s
                    ─────────────────────────────────────────
                    PRÓXIMOS PASOS
                    ─────────────────────────────────────────
                    Se adjunta a este correo el ACTA DE APROBACIÓN oficial en formato PDF,
                    documento que certifica la culminación satisfactoria de su proceso académico.

                    Le recomendamos comunicarse con la Jefatura de Programa para adelantar
                    los trámites administrativos finales necesarios para la culminación de su
                    proceso de grado.

                    Una vez más, ¡muchas felicitaciones por este importante logro académico!

                    Cordialmente,

                    Comité de Currículo del Programa Académico
                    Sistema de Gestión Académica
                    Universidad Surcolombiana
                    """.formatted(
                    student.getName(),
                    student.getLastName(),
                    modality.getProgramDegreeModality().getDegreeModality().getName(),
                    modality.getProgramDegreeModality().getAcademicProgram().getName(),
                    modality.getProgramDegreeModality().getAcademicProgram().getFaculty().getName(),
                    modality.getProgramDegreeModality().getAcademicProgram().getName(),
                    modality.getProgramDegreeModality().getAcademicProgram().getFaculty().getName(),
                    committeeMember.getName(),
                    committeeMember.getLastName(),
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern(
                            "d 'de' MMMM 'de' yyyy", java.util.Locale.forLanguageTag("es-CO"))),
                    event.getObservations() != null && !event.getObservations().isBlank()
                            ? "\n• Observaciones del Comité:\n  " + event.getObservations() + "\n"
                            : ""
            );

            Notification notification = Notification.builder()
                    .type(NotificationType.MODALITY_FINAL_APPROVED_BY_COMMITTEE)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
                    .triggeredBy(committeeMember)
                    .studentModality(modality)
                    .subject(subject)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);

            if (pdfPath != null) {
                try {
                    dispatcher.dispatchWithAttachment(notification, pdfPath, "ACTA_DE_APROBACION.pdf");
                    log.info("Acta simplificada enviada al estudiante {} (modalidad ID {})",
                            student.getId(), modality.getId());
                } catch (Exception e) {
                    log.error("Error enviando acta al estudiante {}: {}", student.getId(), e.getMessage());
                    dispatcher.dispatch(notification);
                }
            } else {
                dispatcher.dispatch(notification);
            }
        }

        // Actualizar estado del certificado a SENT si se generó correctamente
        if (certificate != null) {
            try {
                certificatePdfService.updateCertificateStatus(certificate.getId(), CertificateStatus.SENT);
            } catch (Exception e) {
                log.warn("No se pudo actualizar el estado del certificado: {}", e.getMessage());
            }
        }

        log.info("Notificaciones de aprobación final (comité) enviadas para modalidad ID {}",
                modality.getId());
    }


    @EventListener
    public void handleModalityRejectedByCommittee(ModalityRejectedByCommitteeEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User student = userRepository.findById(event.getStudentId())
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        User committeeMember = userRepository.findById(event.getCommitteeMemberId())
                .orElseThrow(() -> new RuntimeException("Miembro del comité no encontrado"));

        String subject = "IMPORTANTE: Modalidad de Grado NO APROBADA - Decisión del Comité";

        String message = """
                Estimado/a %s,
                
                Recibe un cordial saludo.
                
                Te informamos que después de la evaluación realizada por el Comité de Currículo del Programa, 
                tu modalidad de grado:
                
                "%s"
                
                NO ha sido aprobada.
                
                 INFORMACIÓN DEL PROCESO:
                
                • Programa académico: %s
                • Estado: NO APROBADO
                • Fecha: %s
                
                 MOTIVO DE LA DECISIÓN:
                
                %s
                
                 OPCIONES DISPONIBLES:
                
                Aunque esta modalidad no fue aprobada, tienes las siguientes alternativas para continuar 
                con tu proceso de grado:
                
                1. Iniciar una nueva modalidad de grado: Puedes seleccionar otra modalidad diferente 
                   que se ajuste mejor a tu perfil académico y profesional
                
                2. Recibir asesoría académica: Solicita una reunión con la jefatura de tu programa 
                   para recibir orientación sobre las mejores opciones para ti
                
                3. Revisar requisitos: Asegúrate de cumplir con todos los requisitos académicos y 
                   administrativos para la nueva modalidad que elijas
                
                 PRÓXIMOS PASOS:
                
                • Comunícate con la Jefatura de Programa para recibir asesoría personalizada
                • Solicita retroalimentación detallada sobre los aspectos a mejorar
                • Revisa las diferentes modalidades de grado disponibles en tu programa
                • Evalúa con tu director académico (en caso de tener) cuál opción se ajusta mejor a ti.
                
                📌 IMPORTANTE:
                
                Este resultado NO afecta tu expediente académico de manera permanente. Es una oportunidad 
                para replantear tu estrategia y elegir una modalidad más adecuada a tus fortalezas.
                
                Te invitamos a no desanimarte y a buscar el apoyo necesario para continuar exitosamente 
                con tu proceso de grado. El equipo académico está disponible para orientarte.
                
                Para cualquier duda o aclaración, por favor comunícate con:
                
                • Jefatura de Programa: %s
                • Comité de Currículo del Programa
                • Secretaría Académica de tu facultad
                
                Recuerda que el objetivo del comité es garantizar la calidad académica y el éxito de 
                nuestros estudiantes en su proceso de graduación.
                
                Cordialmente,
                
                Comité de Currículo del Programa
                Sistema de Gestión Académica
                Universidad Surcolombiana
                """.formatted(
                student.getName(),
                modality.getProgramDegreeModality().getDegreeModality().getName(),
                modality.getAcademicProgram().getName(),
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                event.getReason() != null ? event.getReason() : "No se proporcionó motivo específico",
                committeeMember.getName() + " " + committeeMember.getLastName()
        );

        Notification notification = Notification.builder()
                .type(NotificationType.MODALITY_REJECTED_BY_COMMITTEE)
                .recipientType(NotificationRecipientType.STUDENT)
                .recipient(student)
                .triggeredBy(committeeMember)
                .studentModality(modality)
                .subject(subject)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        dispatcher.dispatch(notification);
    }

    @EventListener
    public void onSeminarStarted(SeminarStartedEvent event) {
        String subject = "Inicio de Seminario: " + event.getSeminarName();

        String body = String.format("""
                Estimado/a %s,
                
                Le informamos que el seminario "%s" ha iniciado oficialmente.
                
                Detalles del seminario:
                - Nombre: %s
                - Programa: %s
                - Fecha de inicio: %s
                - Intensidad horaria: %d horas
                
                Es importante que esté atento/a a las indicaciones y horarios del seminario.
                Le recordamos que la asistencia es obligatoria (mínimo 80%% de la intensidad horaria).
                
                Cualquier duda o consulta, puede comunicarse con la jefatura del programa.
                
                Cordialmente,
                Sistema de Gestión de Modalidades de Grado - SIGMA
                %s
                Universidad Surcolombiana
                """,
                event.getRecipientName(),
                event.getSeminarName(),
                event.getSeminarName(),
                event.getProgramName(),
                event.getStartDate(),
                event.getTotalHours(),
                event.getProgramName()
        );

        User recipient = userRepository.findByEmail(event.getRecipientEmail()).orElse(null);

        Notification notification = Notification.builder()
                .recipient(recipient)
                .subject(subject)
                .message(body)
                .type(NotificationType.SEMINAR_STARTED)
                .recipientType(NotificationRecipientType.STUDENT)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        dispatcher.dispatch(notification);
    }

    @EventListener
    public void onSeminarCancelled(SeminarCancelledEvent event) {
        String subject = "Cancelación de Seminario: " + event.getSeminarName();

        String body = String.format("""
                Estimado/a %s,
                
                Le informamos que el seminario "%s" ha sido CANCELADO.
                
                Detalles del seminario:
                - Nombre: %s
                - Programa: %s
                - Fecha de cancelación: %s
                %s
                
                La inscripción al seminario ha sido suspendida automáticamente.
                Podrá inscribirse a otro seminario disponible cuando lo desee.
                
                Lamentamos los inconvenientes que esto pueda causar.
                
                Cordialmente,
                Sistema de Gestión de Modalidades de Grado - SIGMA
                %s
                Universidad Surcolombiana
                """,
                event.getRecipientName(),
                event.getSeminarName(),
                event.getSeminarName(),
                event.getProgramName(),
                event.getCancelledDate(),
                event.getReason() != null ? "\nMotivo: " + event.getReason() : "",
                event.getProgramName()
        );

        User recipient = userRepository.findByEmail(event.getRecipientEmail()).orElse(null);

        Notification notification = Notification.builder()
                .recipient(recipient)
                .subject(subject)
                .message(body)
                .type(NotificationType.SEMINAR_CANCELLED)
                .recipientType(NotificationRecipientType.STUDENT)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        dispatcher.dispatch(notification);
    }

    @EventListener
    public void handleModalityApprovedByExaminers(ModalityApprovedByExaminers event) {

        StudentModality modality = studentModalityRepository
                .findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User examiner = userRepository
                .findById(event.getExaminerUserId())
                .orElseThrow(() -> new RuntimeException("Jurado no encontrado"));

        var members = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(
                        modality.getId(),
                        MemberStatus.ACTIVE
                );

        String subject = "Notificación oficial – Modalidad aprobada por jurado evaluador";

        String messageTemplate = """
            Estimado/a %s,

            Reciba un cordial saludo.

            Por medio de la presente se le informa que la siguiente modalidad de grado:

            ───────────────────────────────
            "%s"
            ───────────────────────────────

            ha sido APROBADA por el jurado evaluador designado.

            Programa académico:
            %s

            Estado actual del proceso:
            PROPUESTA APROBADA POR JURADO

            Fecha de aprobación:
            %s

    
            En consecuencia, la modalidad continúa con el desarrollo
            normal del procedimiento académico conforme a los
            lineamientos institucionales vigentes.

            Esta notificación es generada automáticamente por el
            Sistema de Gestión Académica como constancia de la decisión registrada.

            Sistema de Gestión Académica – SIGMA
            Universidad Surcolombiana
            """;

        for (var member : members) {

            User student = member.getStudent();

            String personalizedMessage = String.format(
                    messageTemplate,
                    student.getName(),
                    modality.getProgramDegreeModality().getDegreeModality().getName(),
                    modality.getAcademicProgram().getName(),
                    LocalDateTime.now()

            );

            Notification notification = Notification.builder()
                    .type(NotificationType.MODALITY_APPROVED_BY_EXAMINERS)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
                    .triggeredBy(examiner)
                    .studentModality(modality)
                    .subject(subject)
                    .message(personalizedMessage)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
        }
    }

    @EventListener
    public void onExaminersAssigned(ExaminersAssignedEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));
        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
        List<DefenseExaminer> examiners = modality.getDefenseExaminers();
        String jurados = examiners.stream()
                .map(e -> e.getExaminer().getName() + " " + e.getExaminer().getLastName() + " (" + translateExaminerType(e.getExaminerType()) + ")")
                .toList()
                .isEmpty() ? "-" : String.join(", ", examiners.stream()
                .map(e -> e.getExaminer().getName() + " " + e.getExaminer().getLastName() + " (" + translateExaminerType(e.getExaminerType()) + ")")
                .toList());
        String subject = "Asignación de jurados evaluadores a tu modalidad de grado";
        String messageTemplate = """
            Estimado/a %s,

            Te informamos que se han asignado oficialmente los jurados evaluadores para tu modalidad de grado:

            Modalidad: %s
            Programa académico: %s
            Jurados asignados: %s
            Fecha de asignación: %s

            Puedes consultar el detalle y el avance del proceso académico en SIGMA.

            Cordialmente,
            Sistema de Gestión Académica
            """;
        for (StudentModalityMember member : members) {
            User student = member.getStudent();
            String message = String.format(messageTemplate,
                    student.getName() + " " + student.getLastName(),
                    modality.getProgramDegreeModality().getDegreeModality().getName(),
                    modality.getProgramDegreeModality().getAcademicProgram().getName(),
                    jurados,
                    LocalDateTime.now()
            );
            Notification notification = Notification.builder()
                    .type(NotificationType.EXAMINER_ASSIGNED)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
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
            case APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE -> "Aprobada por Comité de Currículo";
            case PROPOSAL_APPROVED -> "Propuesta aprobada";
            case PENDING_PROGRAM_HEAD_FINAL_REVIEW -> "Pendiente revisión final por Jefatura de Programa";
            case APPROVED_BY_PROGRAM_HEAD_FINAL_REVIEW -> "Documentos finales aprobados por Jefatura de Programa";
            case DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR -> "Sustentación solicitada por Director";
            case DEFENSE_SCHEDULED -> "Sustentación programada";
            case EXAMINERS_ASSIGNED -> "Jurados asignados";
            case READY_FOR_EXAMINERS -> "Lista para Jurados";
            case DOCUMENTS_APPROVED_BY_EXAMINERS -> "Documentos de propuesta aprobados por los jurados";
            case SECONDARY_DOCUMENTS_APPROVED_BY_EXAMINERS -> "Documentos finales aprobados por los jurados";
            case DOCUMENT_REVIEW_TIEBREAKER_REQUIRED -> "Revisión de documentos con desempate requerida";
            case EDIT_REQUESTED_BY_STUDENT -> "Edición de documento solicitado por estudiante";
            case CORRECTIONS_REQUESTED_EXAMINERS -> "Correcciones solicitadas por Jurados";
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
        if (distinction == null) return "Ninguna";
        return switch (distinction) {
            case NO_DISTINCTION -> "Sin distinción";
            case AGREED_APPROVED -> "Aprobado";
            case AGREED_MERITORIOUS -> "Meritorio";
            case AGREED_LAUREATE -> "Laureado";
            case AGREED_REJECTED -> "Reprobado";
            case DISAGREEMENT_PENDING_TIEBREAKER -> "Desacuerdo, pendiente desempate";
            case TIEBREAKER_APPROVED -> "Aprobado por desempate";
            case TIEBREAKER_MERITORIOUS -> "Meritorio por desempate";
            case TIEBREAKER_LAUREATE -> "Laureado por desempate";
            case TIEBREAKER_REJECTED -> "Reprobado por desempate";
            case REJECTED_BY_COMMITTEE -> "Rechazado por comité";
            case PENDING_COMMITTEE_MERITORIOUS -> "Mención Meritoria propuesta (pendiente del comité)";
            case PENDING_COMMITTEE_LAUREATE -> "Mención Laureada propuesta (pendiente del comité)";
            case TIEBREAKER_PENDING_COMMITTEE_MERITORIOUS -> "Mención Meritoria por desempate (pendiente del comité)";
            case TIEBREAKER_PENDING_COMMITTEE_LAUREATE -> "Mención Laureada por desempate (pendiente del comité)";
        };
    }

    /**
     * Notifica a todos los estudiantes miembros de la modalidad cuando un jurado
     * aprueba o rechaza su solicitud de edición de un documento.
     */
    @EventListener
    public void onDocumentEditResolved(DocumentEditResolvedEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        List<StudentModalityMember> members = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);

        boolean approved = event.isApproved();
        String subject = approved
                ? "Solicitud de edición de documento aprobada"
                : "Solicitud de edición de documento rechazada";

        for (StudentModalityMember member : members) {
            User student = member.getStudent();
            String message;
            if (approved) {
                message = """
                        Estimado/a %s,

                        Reciba un cordial saludo.

                        Le informamos que su solicitud de edición del siguiente documento
                        ha sido APROBADA por el jurado evaluador:

                        ───────────────────────────────
                        DOCUMENTO
                        ───────────────────────────────
                        "%s"

                        ───────────────────────────────
                        NOTAS DEL JURADO
                        ───────────────────────────────
                        %s

                        ───────────────────────────────
                        ACCIÓN REQUERIDA
                        ───────────────────────────────
                        Puede ingresar al sistema  y resubir el documento
                        con los cambios que consideró necesarios. Una vez resubido,
                        el jurado evaluará la nueva versión.

                        Esta notificación se genera automáticamente para efectos
                        de control y trazabilidad institucional.

                        Sistema de Gestión Académica – SIGMA
                        Universidad Surcolombiana
                        """.formatted(
                        student.getName(),
                        event.getDocumentName(),
                        event.getResolutionNotes() != null ? event.getResolutionNotes() : "Sin notas adicionales"
                );
            } else {
                message = """
                        Estimado/a %s,

                        Reciba un cordial saludo.

                        Le informamos que su solicitud de edición del siguiente documento
                        ha sido RECHAZADA por el jurado evaluador:

                        ───────────────────────────────
                        DOCUMENTO
                        ───────────────────────────────
                        "%s"

                        ───────────────────────────────
                        MOTIVO DEL RECHAZO
                        ───────────────────────────────
                        %s

                        ───────────────────────────────
                        INFORMACIÓN ADICIONAL
                        ───────────────────────────────
                        El documento permanece en su estado aprobado actual.
                        Si tiene dudas sobre esta decisión, puede comunicarse
                        con la Jefatura de Programa o el Director de Proyecto.

                        Esta notificación se genera automáticamente para efectos
                        de control y trazabilidad institucional.

                        Sistema de Gestión Académica – SIGMA
                        Universidad Surcolombiana
                        """.formatted(
                        student.getName(),
                        event.getDocumentName(),
                        event.getResolutionNotes() != null ? event.getResolutionNotes() : "Sin motivo registrado"
                );
            }

            Notification notification = Notification.builder()
                    .type(approved ? NotificationType.DOCUMENT_EDIT_APPROVED : NotificationType.DOCUMENT_EDIT_REJECTED)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
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

    private String translateExaminerType(com.SIGMA.USCO.Modalities.Entity.enums.ExaminerType type) {
        if (type == null) return "Jurado";
        return switch (type) {
            case PRIMARY_EXAMINER_1 -> "Jurado Principal 1";
            case PRIMARY_EXAMINER_2 -> "Jurado Principal 2";
            case TIEBREAKER_EXAMINER -> "Jurado de Desempate";
        };
    }
}









