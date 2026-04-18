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
import com.SIGMA.USCO.notifications.event.CancellationApprovedEvent;
import com.SIGMA.USCO.notifications.event.CancellationRejectedEvent;
import com.SIGMA.USCO.notifications.event.CancellationRequestedEvent;
import com.SIGMA.USCO.notifications.event.CorrectionApprovedEvent;
import com.SIGMA.USCO.notifications.event.CorrectionDeadlineExpiredEvent;
import com.SIGMA.USCO.notifications.event.CorrectionDeadlineReminderEvent;
import com.SIGMA.USCO.notifications.event.CorrectionRejectedFinalEvent;
import com.SIGMA.USCO.notifications.event.CorrectionResubmittedEvent;
import com.SIGMA.USCO.notifications.event.DefenseScheduledEvent;
import com.SIGMA.USCO.notifications.event.DirectorAssignedEvent;
import com.SIGMA.USCO.notifications.event.DocumentCorrectionsRequestedEvent;
import com.SIGMA.USCO.notifications.event.DocumentEditResolvedEvent;
import com.SIGMA.USCO.notifications.event.ExaminersAssignedEvent;
import com.SIGMA.USCO.notifications.event.FinalDefenseResultEvent;
import com.SIGMA.USCO.notifications.event.ModalityApprovedByCommitteeEvent;
import com.SIGMA.USCO.notifications.event.ModalityApprovedByExaminers;
import com.SIGMA.USCO.notifications.event.ModalityApprovedByProgramHead;
import com.SIGMA.USCO.notifications.event.ModalityClosedByCommitteeEvent;
import com.SIGMA.USCO.notifications.event.ModalityFinalApprovedByCommitteeEvent;
import com.SIGMA.USCO.notifications.event.ModalityInvitationAcceptedEvent;
import com.SIGMA.USCO.notifications.event.ModalityInvitationRejectedEvent;
import com.SIGMA.USCO.notifications.event.ModalityInvitationSentEvent;
import com.SIGMA.USCO.notifications.event.ModalityRejectedByCommitteeEvent;
import com.SIGMA.USCO.notifications.event.SeminarCancelledEvent;
import com.SIGMA.USCO.notifications.event.SeminarStartedEvent;
import com.SIGMA.USCO.notifications.event.StudentModalityStarted;
import com.SIGMA.USCO.notifications.repository.NotificationRepository;
import com.SIGMA.USCO.notifications.service.AcademicCertificatePdfService;
import com.SIGMA.USCO.notifications.service.NotificationDispatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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
                Estimado(a) %s:

                Reciba un cordial saludo.

                Nos permitimos informarle que su modalidad de grado ha sido registrada e iniciada oficialmente en el sistema institucional. A continuación, se relaciona la información correspondiente:

                Modalidad de grado: "%s".
                Estado actual del proceso: %s.

                Actualmente, la modalidad se encuentra en etapa de revisión y evaluación por parte de la Jefatura de Programa y del Comité de Currículo correspondiente.

                Se recomienda consultar periódicamente el sistema y mantenerse atento(a) a las notificaciones institucionales, ya que a través de este medio se comunicarán solicitudes, observaciones o decisiones relacionadas con su proceso académico.

                Atentamente,

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
            
            // Determinar quién solicita las correcciones
            String requestedByText;
            if (event.getRequestedBy() == NotificationRecipientType.PROGRAM_HEAD) {
                requestedByText = "la Jefatura de Programa y/o Coordinación de Modalidades";
            } else if (event.getRequestedBy() == NotificationRecipientType.EXAMINER) {
                requestedByText = "un jurado evaluador";
            } else {
                requestedByText = "el Comité de Currículo del Programa";
            }

            String message = """
        Estimado(a) %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que %s ha solicitado la realización de correcciones en uno de los documentos asociados a su modalidad de grado, en el marco del proceso de revisión académica.

        A continuación, se detalla la información correspondiente:

        Documento: "%s".
        Observaciones registradas: %s.

        En este sentido, se solicita ingresar a la plataforma institucional, revisar detalladamente las observaciones indicadas y efectuar los ajustes correspondientes, con el fin de dar continuidad al proceso académico dentro de los plazos establecidos.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso.

        Atentamente,

        Sistema de Gestión Académica
        """.formatted(
                    student.getName(),
                    requestedByText,
                    document.getDocumentConfig().getDocumentName(),
                    event.getObservations() != null && !event.getObservations().isBlank()
                            ? event.getObservations()
                            : "No se registraron observaciones adicionales."
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
        String degreeModalityName = sm.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = sm.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        for (var member : members) {
            User student = member.getStudent();
            String message = """
        Estimado(a) %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que su solicitud de cancelación de la modalidad de grado ha sido registrada correctamente en el sistema institucional.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".

        La solicitud será evaluada inicialmente por el director del proyecto y, posteriormente, por el Comité de Currículo del programa académico correspondiente.

        Una vez se emita una decisión oficial, esta le será notificada oportunamente a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.

        Atentamente,

        Sistema de Gestión Académica
        """.formatted(
                    student.getName(),
                    modalidadInfo
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
        String degreeModalityName = sm.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = sm.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        for (var member : members) {
            User student = member.getStudent();
            String message = """
        Estimado(a) %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que el Comité de Currículo del programa académico ha aprobado oficialmente su solicitud de cancelación de la modalidad de grado.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".

        En consecuencia, la modalidad queda cerrada de manera oficial y el proceso académico asociado finaliza a partir de la fecha en que se emite la presente decisión.

        En caso de requerir orientación adicional o información complementaria sobre su situación académica, podrá comunicarse con la Jefatura de Programa.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica – SIGMA
        """.formatted(
                    student.getName(),
                    modalidadInfo
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
        String degreeModalityName = sm.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = sm.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        for (var member : members) {
            User student = member.getStudent();
            String message = """
        Estimado(a) %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que el Comité de Currículo del programa académico ha decidido no aprobar su solicitud de cancelación de la modalidad de grado.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".
        Motivo de la decisión: %s.

        En consecuencia, la modalidad de grado continúa activa bajo las condiciones previamente establecidas dentro del proceso académico.

        En caso de requerir mayor claridad sobre la presente decisión o desear orientación adicional, podrá comunicarse con la Jefatura de Programa.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        """.formatted(
                    student.getName(),
                    modalidadInfo,
                    event.getReason() != null && !event.getReason().isBlank()
                            ? event.getReason()
                            : "No se especifican motivos adicionales."
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
        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        String studentSubject =
                "Sustentación programada – Modalidad de Grado";

        for (var member : members) {
            User student = member.getStudent();
            String studentMessage = """
        Estimado(a) %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que la sustentación correspondiente a su modalidad de grado ha sido programada, conforme al proceso académico establecido.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".
        Fecha y hora: %s.
        Lugar: %s.
        Director asignado: %s.

        De acuerdo con la normativa institucional vigente, deberá realizar la divulgación pública de su proyecto con al menos tres (3) días hábiles de anticipación a la fecha programada para la sustentación, en los espacios definidos por el programa académico.

        Se recomienda presentarse con la debida antelación y cumplir estrictamente con los lineamientos académicos establecidos para el desarrollo de la sesión de sustentación.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.

        Atentamente,

        Sistema de Gestión Académica
        """.formatted(
                    student.getName(),
                    modalidadInfo,
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
        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        String studentSubject =
                "Director de proyecto asignado – Modalidad de grado";
        for (var member : members) {
            User student = member.getStudent();
            String studentMessage = """
        Estimado(a) %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que ha sido designado oficialmente un Director de Proyecto para su modalidad de grado, conforme a los lineamientos académicos vigentes.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".
        Director asignado: %s.
        Correo electrónico: %s.

        A partir de este momento, el director asignado actuará como su orientador académico principal durante el desarrollo de la modalidad de grado y será responsable del seguimiento y acompañamiento del proceso.

        Se recomienda establecer contacto oportunamente con el director, con el fin de coordinar las actividades iniciales y definir el plan de trabajo correspondiente.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.

        Atentamente,

        Sistema de Gestión Académica
        """.formatted(
                    student.getName(),
                    modalidadInfo,
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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleDefenseResult(FinalDefenseResultEvent event){

        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                        .orElseThrow();

        boolean approved = event.getFinalStatus() == ModalityProcessStatus.GRADED_APPROVED;
        boolean approvedPendingCommitteeReview = event.getFinalStatus() == ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW;
        boolean shouldSendCertificate = approved || approvedPendingCommitteeReview;

        String studentSubject = shouldSendCertificate
                ? (approvedPendingCommitteeReview
                    ? "Resultado de sustentación – Modalidad aprobada (distinción en revisión del comité)"
                    : "Resultado final de sustentación – Modalidad aprobada")
                : "Resultado final de sustentación – Modalidad no aprobada";

        // Obtener todos los miembros activos
        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
            modality.getId(), MemberStatus.ACTIVE
        );

        // Fallback para modalidades individuales sin registros activos en StudentModalityMember
        if (members == null || members.isEmpty()) {
            StudentModalityMember syntheticLeaderMember = StudentModalityMember.builder()
                    .studentModality(modality)
                    .student(modality.getLeader())
                    .status(MemberStatus.ACTIVE)
                    .isLeader(true)
                    .build();
            members = List.of(syntheticLeaderMember);
        }

        // GENERAR EL PDF UNA SOLA VEZ (FUERA DEL LOOP)
        AcademicCertificate certificate = null;
        Path pdfPath = null;
        if (shouldSendCertificate) {
            try {
                log.info("Generando acta de aprobación para la modalidad ID: {}", modality.getId());

                boolean isComplete = isCompleteModality(modality);
                if (isComplete) {
                    certificate = certificatePdfService.generateCertificate(modality);
                } else {
                    certificate = certificatePdfService.generateCertificateForCommitteeApproval(modality);
                }
                pdfPath = certificatePdfService.getCertificatePath(modality.getId());
                log.info("Acta PDF generada exitosamente para la modalidad ID: {}", modality.getId());
            } catch (Exception e) {
                log.error("Error generando acta PDF para modalidad ID {}: {}", modality.getId(), e.getMessage(), e);
            }
        }

        // ENVIAR CORREO CON ACTA SOLO AL LÍDER (evita múltiples adjuntos)
        User leader = modality.getLeader();
        if (leader != null) {
            String leaderMessage = shouldSendCertificate
                    ? buildApprovedStudentMessage(leader, modality, event)
                    : buildRejectedStudentMessage(leader, modality, event);

            Notification leaderNotification = Notification.builder()
                    .type(NotificationType.DEFENSE_COMPLETED)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(leader)
                    .triggeredBy(null)
                    .studentModality(modality)
                    .subject(studentSubject)
                    .message(leaderMessage)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(leaderNotification);

            if (shouldSendCertificate && pdfPath != null) {
                try {
                    dispatcher.dispatchWithAttachment(
                            leaderNotification,
                            pdfPath,
                            "ACTA_DE_APROBACION.pdf"
                    );
                    log.info("Acta enviada al líder {} (modalidad ID {})", leader.getId(), modality.getId());
                } catch (Exception e) {
                    log.error("Error enviando acta al líder {}: {}", leader.getId(), e.getMessage());
                    dispatcher.dispatch(leaderNotification);
                }
            } else {
                dispatcher.dispatch(leaderNotification);
            }
        }

        // ENVIAR NOTIFICACIÓN SIN ACTA A LOS DEMÁS MIEMBROS (si la modalidad es grupal)
        for (StudentModalityMember member : members) {
            // Si ya es el líder, saltarlo (ya recibió el correo con acta)
            if (leader != null && member.getStudent().getId().equals(leader.getId())) {
                continue;
            }

            User student = member.getStudent();
            String studentMessage = shouldSendCertificate
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

            // Sin adjunto para los demás miembros (el líder ya tiene el acta)
            dispatcher.dispatch(notification);
            log.info("Notificación enviada al miembro {} (modalidad ID {}, sin acta adjunta)", student.getId(), modality.getId());
        }

        // MARCAR COMO ENVIADA UNA SOLA VEZ
        if (certificate != null && shouldSendCertificate) {
            try {
                certificatePdfService.updateCertificateStatus(certificate.getId(), CertificateStatus.SENT);
                log.info("Estado del certificado actualizado a SENT para modalidad ID: {}", modality.getId());
            } catch (Exception e) {
                log.error("Error actualizando estado del certificado: {}", e.getMessage());
            }
        }
    }

    private String buildApprovedStudentMessage(User student, StudentModality modality, FinalDefenseResultEvent event) {
        String observaciones = localizeObservations(event.getObservations());
        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        return """
            Estimado(a) %s:

            Reciba un cordial saludo.

            Nos permitimos informarle que, una vez realizada la sustentación y evaluado el resultado por los jurados designados, ha aprobado oficialmente la modalidad de grado.

            A continuación, se relaciona la información correspondiente:

            Modalidad de grado: "%s".
            Mención académica: %s.
            Observaciones registradas: %s.

            Se adjunta a este correo el acta de aprobación en formato PDF, documento oficial que certifica la culminación satisfactoria de su modalidad de grado, conforme a la normatividad académica vigente.

            Para finalizar su proceso académico, deberá comunicarse con la Jefatura de Programa, con el fin de adelantar los trámites administrativos correspondientes.

            Reciba un reconocimiento institucional por este importante logro académico.

            Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.

            Atentamente,

            Sistema de Gestión Académica
            Universidad Surcolombiana
            """.formatted(
                student.getName(),
                modalidadInfo,
                translateAcademicDistinction(event.getAcademicDistinction()),
                observaciones != null && !observaciones.isBlank() ? observaciones : "No se registran observaciones."
        );
    }

    private String buildRejectedStudentMessage(User student, StudentModality modality, FinalDefenseResultEvent event) {
        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        return """
            Estimado(a) %s:

            Reciba un cordial saludo.

            Nos permitimos informarle que, una vez realizada la sustentación y evaluado el resultado por los jurados designados, no se ha determinado la aprobación de la modalidad de grado en la presente oportunidad.

            A continuación, se relaciona la información correspondiente:

            Modalidad de grado: "%s".
            Observaciones de los jurados: %s.

            De acuerdo con la normativa académica vigente, se recomienda revisar detenidamente las observaciones consignadas y establecer comunicación con el Director de Proyecto, así como con la Jefatura de Programa, con el fin de definir las acciones a seguir dentro del proceso académico.

            Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.

            Atentamente,

            Sistema de Gestión Académica – SIGMA
            Universidad Surcolombiana
            """.formatted(
                student.getName(),
                modalidadInfo,
                event.getObservations() != null && !event.getObservations().isBlank()
                        ? event.getObservations()
                        : "No se registran observaciones adicionales."
        );
    }

    @EventListener
    public void ModalityApprovedByCommittee(ModalityApprovedByCommitteeEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
            modality.getId(),
            MemberStatus.ACTIVE
        );
        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        String subject = "Modalidad de grado aprobada – Comité de Currículo";
        for (var member : members) {
            User student = member.getStudent();
            String message = """
        Estimado(a) %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que la modalidad de grado ha sido aprobada oficialmente por el Comité de Currículo del programa académico.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".
        Estado del proceso: Propuesta aprobada por el Comité de Currículo.
        Director de Proyecto: %s.
        Fecha de aprobación: %s.

        En virtud de esta decisión, la modalidad de grado continúa con la siguiente etapa del proceso académico, correspondiente a la evaluación y aprobación por parte del jurado designado.

        Se recomienda mantenerse atento(a) a las notificaciones del sistema institucional y conservar comunicación permanente con el Director de Proyecto y la Jefatura de Programa, con el fin de garantizar el adecuado desarrollo y seguimiento del proceso.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                    student.getName(),
                    modalidadInfo,
                    modality.getProjectDirector() != null
                            ? modality.getProjectDirector().getName() + " " +
                            modality.getProjectDirector().getLastName()
                            : "No se registra director asignado.",
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
        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        String subject = "Modalidad de grado aprobada – Jefatura de Programa y/o Coordinación de Modalidades";
        for (var member : members) {
            User student = member.getStudent();
            String message = """
        Estimado(a) %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que la modalidad de grado ha sido aprobada oficialmente por la Jefatura de Programa y/o la coordinación de modalidades del programa académico.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".
        Estado del proceso: Aprobada por la Jefatura de Programa.

        En virtud de esta decisión, la modalidad de grado continuará con la etapa de evaluación por parte del Comité de Currículo del programa académico, instancia encargada de emitir la decisión correspondiente para la continuidad del proceso.

        Se recomienda mantenerse atento(a) a las notificaciones del sistema institucional y conservar comunicación con la Jefatura de Programa, en caso de requerir información adicional o aclaraciones relacionadas con el trámite.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                    student.getName(),
                    modalidadInfo
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
        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        String subject = "Recordatorio oficial – Plazo de correcciones (%d días restantes)"
                .formatted(event.getDaysRemaining());
        for (var member : members) {
            User student = member.getStudent();
            String message = """
        Estimado(a) %s:

        Reciba un cordial saludo.

        Nos permitimos recordarle que actualmente presenta correcciones pendientes asociadas a su modalidad de grado, conforme al proceso de revisión académica.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".
        Días restantes: %d.
        Fecha límite de entrega: %s.

        En este sentido, es indispensable realizar las correcciones solicitadas y efectuar la carga de la versión ajustada del documento antes de la fecha indicada. En caso de no cumplir con el plazo establecido, el sistema podrá proceder con la cancelación automática de la modalidad, de conformidad con la normativa académica vigente.

        Para realizar la carga del documento, deberá seguir el siguiente procedimiento:

        1. Realizar las correcciones indicadas en el documento.
        2. Ingresar a la plataforma institucional.
        3. Acceder al módulo "Mis Documentos".
        4. Seleccionar el documento correspondiente y cargar la versión corregida.

        En caso de presentar alguna dificultad o requerir orientación adicional, podrá comunicarse a la mayor brevedad con la Jefatura de Programa.

        Este mensaje constituye una notificación automática generada como recordatorio preventivo y para efectos de control y trazabilidad del proceso académico.

        Atentamente,

        Sistema de Gestión Académica – SIGMA
        Universidad Surcolombiana
        """.formatted(
                    student.getName(),
                    modalidadInfo,
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
        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        String subject = "Notificación oficial – Cancelación automática de modalidad por vencimiento de plazo";
        for (var member : members) {
            User student = member.getStudent();
            String message = """
            Estimado(a) %s:

            Reciba un cordial saludo.

            Nos permitimos informarle que la modalidad de grado relacionada a continuación ha sido cancelada de manera automática, debido al vencimiento del plazo establecido para la entrega de las correcciones solicitadas, sin que se haya efectuado la carga del documento ajustado dentro del término reglamentario.

            A continuación, se relaciona la información correspondiente:

            Modalidad de grado: "%s".
            Fecha de solicitud de correcciones: %s.
            Plazo máximo otorgado: 30 días calendario.
            Estado final del proceso: Cancelada.

            La presente decisión se adopta de conformidad con la normativa académica vigente y el reglamento institucional aplicable a las modalidades de grado.

            Para dar continuidad a su proceso académico, deberá postular una nueva modalidad de grado e iniciar nuevamente el procedimiento desde su etapa inicial, cumpliendo con los requisitos y tiempos establecidos por el programa académico.

            Se recomienda comunicarse con la Jefatura de Programa, con el fin de recibir orientación sobre los pasos a seguir.

            Este mensaje constituye una notificación automática generada como constancia del cierre del proceso y para efectos de control y trazabilidad institucional.

            Atentamente,

            Sistema de Gestión Académica
            Universidad Surcolombiana
            """.formatted(
                    student.getName(),
                    modalidadInfo,
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
        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        String subject = "Notificación oficial – Documento corregido recibido";
        for (var member : members) {
            User student = member.getStudent();
            String message = """
            Estimado(a) %s:

            Reciba un cordial saludo.

            Nos permitimos informarle que la carga del documento corregido ha sido registrada correctamente en el Sistema de Gestión Académica, en el marco del proceso de revisión de su modalidad de grado.

            A continuación, se relaciona la información correspondiente:

            Modalidad de grado: "%s".
            Nombre del archivo: %s.
            Fecha de envío: %s.
            Estado del proceso: Correcciones enviadas – pendiente de revisión.

            El documento será evaluado por las instancias académicas competentes. Una vez finalizada la revisión, le será notificado el resultado correspondiente a través de la plataforma institucional.

            Se recomienda permanecer atento(a) a las comunicaciones emitidas por el sistema, con el fin de garantizar la adecuada continuidad del proceso académico.

            Este mensaje constituye una notificación automática generada como constancia del registro de la nueva versión del documento y para efectos de control y trazabilidad institucional.

            Atentamente,

            Sistema de Gestión Académica
            Universidad Surcolombiana
            """.formatted(
                    student.getName(),
                    modalidadInfo,
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
        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        String subject = "Notificación oficial – Correcciones aprobadas";
        for (var member : members) {
            User student = member.getStudent();
            String message = """
            Estimado(a) %s:

            Reciba un cordial saludo.

            Nos permitimos informarle que las correcciones remitidas han sido aprobadas por el jurado evaluador, en el marco del proceso de revisión académica de su modalidad de grado.

            A continuación, se relaciona la información correspondiente:

            Modalidad de grado: "%s".
            Documento evaluado: %s.
            Estado del proceso: Correcciones aprobadas.

            En virtud de esta decisión, la modalidad de grado continúa con el desarrollo normal del proceso académico, conforme a las disposiciones institucionales vigentes.

            La siguiente actuación dentro del proceso será notificada oportunamente a través de la plataforma institucional.

            Este mensaje constituye una notificación automática generada como constancia de la decisión registrada y para efectos de control y trazabilidad institucional.

            Atentamente,

            Sistema de Gestión Académica
            Universidad Surcolombiana
            """.formatted(
                    student.getName(),
                    modalidadInfo,
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
        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        String subject = "Notificación oficial – Cancelación de modalidad por rechazo definitivo de correcciones";
        for (var member : members) {
            User student = member.getStudent();
            String message = """
            Estimado(a) %s:

            Reciba un cordial saludo.

            Nos permitimos informarle que, como resultado de la evaluación realizada por el jurado designado, no se ha determinado la aprobación de uno o más documentos asociados a su modalidad de grado. En consecuencia, se ha dispuesto la cancelación definitiva del proceso académico correspondiente.

            A continuación, se relaciona la información pertinente:

            Modalidad de grado: "%s".
            Documento evaluado: %s.
            Estado final del proceso: Rechazado – modalidad cancelada.
            Motivo registrado: %s.

            La presente decisión se adopta de conformidad con la normativa académica vigente aplicable a las modalidades de grado.

            Para dar continuidad a su proceso académico, deberá postular una nueva modalidad de grado e iniciar nuevamente el procedimiento desde su etapa inicial, cumpliendo con los requisitos y términos establecidos por el programa académico.

            Se recomienda comunicarse con la Jefatura de Programa, con el fin de recibir orientación sobre las alternativas disponibles.

            Este mensaje constituye una notificación automática generada como constancia del cierre definitivo del proceso y para efectos de control y trazabilidad institucional.

            Atentamente,

            Sistema de Gestión Académica – SIGMA
            Universidad Surcolombiana
            """.formatted(
                    student.getName(),
                    modalidadInfo,
                    event.getDocumentName(),
                    event.getReason() != null && !event.getReason().isBlank()
                            ? event.getReason()
                            : "No se registran motivos adicionales."
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
        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        String subject = "Notificación oficial – Cierre de modalidad por decisión del Comité de Currículo";
        for (var member : members) {
            User student = member.getStudent();
            String message = """
            Estimado(a) %s:

            Reciba un cordial saludo.

            Nos permitimos informarle que el Comité de Currículo del programa académico ha decidido el cierre de la modalidad de grado, conforme a sus competencias y a la normativa académica vigente.

            A continuación, se relaciona la información correspondiente:

            Modalidad de grado: "%s".
            Programa académico: %s.
            Estado del proceso: Modalidad cerrada.
            Decisión adoptada por: %s %s.
            Fecha de registro de la decisión: %s.
            Motivo del cierre: %s.

            La presente decisión se adopta de conformidad con la normativa académica vigente y las disposiciones institucionales aplicables.

            Para dar continuidad a su proceso académico, se recomienda solicitar orientación ante la Jefatura de Programa, con el fin de recibir asesoría sobre las alternativas disponibles y, en caso de ser procedente, iniciar una nueva modalidad de grado conforme al reglamento institucional.

            Este mensaje constituye una notificación automática generada como constancia de la decisión registrada y para efectos de control y trazabilidad institucional.

            Atentamente,

            Sistema de Gestión Académica
            Universidad Surcolombiana
                """.formatted(
                    student.getName(),
                    modalidadInfo,
                    modality.getAcademicProgram().getName(),
                    committeeMember.getName(),
                    committeeMember.getLastName(),
                    LocalDateTime.now().toString(),
                    event.getReason() != null && !event.getReason().isBlank()
                            ? event.getReason()
                            : "No se registran motivos adicionales."
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

        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }

        String subject = "Invitación para unirte a una modalidad de grado grupal – SIGMA";

        String message = """
                Estimado(a) %s:

                Reciba un cordial saludo.

                Nos permitimos informarle que ha recibido una invitación para integrarse a una modalidad de grado en la modalidad grupal, conforme a los lineamientos académicos vigentes.

                A continuación, se relaciona la información correspondiente:

                Modalidad de grado: "%s".
                Programa académico: %s.
                Invitación realizada por: %s.
                Fecha de invitación: %s.

                La presente invitación tiene como propósito vincularle a un equipo de trabajo para el desarrollo conjunto de la modalidad de grado. En caso de aceptar, adquirirá los compromisos académicos correspondientes y participará de manera colaborativa en las actividades y entregables definidos dentro del proceso.

                Se recuerda que, de acuerdo con la normativa institucional, solo es posible estar vinculado(a) a una modalidad de grado a la vez.

                Para gestionar la invitación, deberá ingresar a la plataforma institucional, dirigirse a la sección de invitaciones o notificaciones, revisar la información correspondiente y registrar su decisión de aceptación o rechazo.

                Se recomienda establecer comunicación previa con %s, con el fin de asegurar la alineación de expectativas, responsabilidades y objetivos del proyecto académico.

                Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.

                Atentamente,

                Sistema de Gestión Académica – SIGMA
                Universidad Surcolombiana
                """.formatted(
                invitee.getName(),
                modalidadInfo,
                modality.getAcademicProgram().getName(),
                inviter.getName() + " " + inviter.getLastName(),
                LocalDateTime.now().toString(),
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

        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }

        String subject = "Un estudiante aceptó tu invitación a la modalidad grupal – SIGMA";

        String message = """
                Estimado(a) %s:

                Reciba un cordial saludo.

                Nos permitimos informarle que un estudiante ha aceptado su invitación para integrarse a la modalidad de grado en la modalidad grupal.

                A continuación, se relaciona la información correspondiente:

                Estudiante: %s.
                Modalidad de grado: "%s".
                Programa académico: %s.
                Fecha de aceptación: %s.

                En consecuencia, el estudiante mencionado ha sido vinculado formalmente a su grupo de trabajo, adquiriendo los derechos y responsabilidades establecidos para el desarrollo de la modalidad de grado.

                Se recomienda coordinar con los integrantes del grupo la asignación de roles, la definición de responsabilidades y la planificación de las actividades académicas, con el fin de garantizar el adecuado desarrollo del proceso.

                Así mismo, se sugiere establecer mecanismos de comunicación efectivos y realizar seguimiento permanente a los avances del proyecto, conforme a los lineamientos institucionales.

                Este mensaje constituye una notificación automática generada como constancia de la vinculación del estudiante y para efectos de control y trazabilidad del proceso académico.

                Atentamente,

                Sistema de Gestión Académica – SIGMA
                Universidad Surcolombiana
                """.formatted(
                leader.getName(),
                acceptedBy.getName() + " " + acceptedBy.getLastName(),
                modalidadInfo,
                modality.getAcademicProgram().getName(),
                LocalDateTime.now().toString()
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

        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }

        String subject = "Un estudiante rechazó tu invitación a la modalidad grupal – SIGMA";

        String message = """
                Estimado(a) %s:

                Reciba un cordial saludo.

                Nos permitimos informarle que un estudiante ha registrado el rechazo de la invitación para integrarse a la modalidad de grado en la modalidad grupal.

                A continuación, se relaciona la información correspondiente:

                Estudiante: %s.
                Modalidad de grado: "%s".
                Programa académico: %s.
                Fecha de rechazo: %s.

                En consecuencia, el estudiante mencionado no ha sido vinculado al grupo de trabajo asociado a la modalidad de grado.

                En caso de requerir la conformación o ajuste del grupo, podrá gestionar nuevas invitaciones a estudiantes que cumplan con las condiciones establecidas, o continuar con el desarrollo de la modalidad conforme a la estructura actual del equipo.

                Se recuerda que el número máximo de integrantes permitidos para la modalidad es de %d estudiante(s), incluido usted. Actualmente, el grupo cuenta con %d integrante(s) activo(s).

                Se recomienda coordinar con los integrantes actuales del grupo y definir las acciones pertinentes para garantizar la continuidad y adecuado desarrollo del proceso académico.

                Este mensaje constituye una notificación automática generada como constancia del registro de la decisión y para efectos de control y trazabilidad institucional.

                Atentamente,

                Sistema de Gestión Académica – SIGMA
                Universidad Surcolombiana
                """.formatted(
                leader.getName(),
                rejectedBy.getName() + " " + rejectedBy.getLastName(),
                modalidadInfo,
                modality.getAcademicProgram().getName(),
                LocalDateTime.now().toString(),
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

        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }

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
                    Estimado(a) %s %s:

                    Reciba un cordial saludo.

                    Nos permitimos informarle que la modalidad de grado relacionada a continuación ha sido aprobada de manera definitiva por el Comité de Currículo del programa académico, conforme a la normativa institucional vigente.

                    A continuación, se relaciona la información correspondiente:

                    Modalidad de grado: "%s".
                    Programa académico: %s.
                    Facultad: %s.
                    Decisión adoptada por: %s %s (Comité de Currículo).
                    Fecha de aprobación: %s.
                    %s
                    Se adjunta a este correo el acta de aprobación en formato PDF, documento oficial que certifica la culminación satisfactoria de su proceso académico.

                    Para la finalización de su proceso de grado, deberá comunicarse con la Jefatura de Programa, con el fin de adelantar los trámites administrativos correspondientes.

                    Reciba un reconocimiento institucional por este logro académico.

                    Este mensaje constituye una notificación automática generada como constancia de la decisión registrada y para efectos de control y trazabilidad institucional.

                    Atentamente,

                    Comité de Currículo del Programa Académico
                    Sistema de Gestión Académica
                    Universidad Surcolombiana
                    """.formatted(
                    student.getName(),
                    student.getLastName(),
                    modalidadInfo,
                    modality.getProgramDegreeModality().getAcademicProgram().getName(),
                    modality.getProgramDegreeModality().getAcademicProgram().getFaculty().getName(),
                    committeeMember.getName(),
                    committeeMember.getLastName(),
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern(
                            "d 'de' MMMM 'de' yyyy", java.util.Locale.forLanguageTag("es-CO"))),
                    event.getObservations() != null && !event.getObservations().isBlank()
                            ? "Observaciones del Comité: " + event.getObservations() + ".\n\n"
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

        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }

        String subject = "IMPORTANTE: Modalidad de Grado NO APROBADA - Decisión del Comité";

        String message = """
                Estimado(a) %s:

                Reciba un cordial saludo.

                Nos permitimos informarle que, una vez realizada la evaluación por parte del Comité de Currículo del programa académico, no se ha determinado la aprobación de la modalidad de grado.

                A continuación, se relaciona la información correspondiente:

                Modalidad de grado: "%s".
                Programa académico: %s.
                Estado del proceso: No aprobado.
                Fecha de la decisión: %s.
                Motivo de la decisión: %s.

                La presente decisión se adopta de conformidad con la normativa académica vigente y las disposiciones institucionales aplicables.

                Para dar continuidad a su proceso académico, podrá postular una nueva modalidad de grado o solicitar orientación ante la Jefatura de Programa, con el fin de definir las alternativas disponibles conforme a su situación académica.

                Se recomienda revisar los requisitos establecidos para las modalidades de grado y, en caso de requerirlo, solicitar retroalimentación adicional que le permita fortalecer una nueva postulación.

                Este mensaje constituye una notificación automática generada como constancia de la decisión registrada y para efectos de control y trazabilidad institucional.

                Atentamente,

                Comité de Currículo del Programa Académico
                Sistema de Gestión Académica
                Universidad Surcolombiana
                """.formatted(
                student.getName(),
                modalidadInfo,
                modality.getAcademicProgram().getName(),
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                event.getReason() != null && !event.getReason().isBlank()
                        ? event.getReason()
                        : "No se registran motivos adicionales."
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
        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        String subject = "Asignación de jurados evaluadores a tu modalidad de grado";
        String messageTemplate = """
            Estimado(a) %s:

            Reciba un cordial saludo.

            Nos permitimos informarle que han sido designados oficialmente los jurados evaluadores para su modalidad de grado, conforme al proceso académico establecido.

            A continuación, se relaciona la información correspondiente:

            Modalidad de grado: "%s".
            Programa académico: %s.
            Jurados asignados: %s.
            Fecha de asignación: %s.

            Los jurados designados serán responsables de la evaluación académica de su trabajo, conforme a los lineamientos institucionales vigentes.

            Se recomienda consultar periódicamente la plataforma institucional, con el fin de hacer seguimiento al estado y avance del proceso académico.

            Este mensaje constituye una notificación automática generada como constancia de la asignación realizada y para efectos de control y trazabilidad institucional.

            Atentamente,

            Sistema de Gestión Académica
            Universidad Surcolombiana
            """;

        for (StudentModalityMember member : members) {
            User student = member.getStudent();
            String message = String.format(messageTemplate,
                    student.getName() + " " + student.getLastName(),
                    modalidadInfo,
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
     * Determina si la modalidad requiere acta completa (sustentación/jurados/director)
     * o simplificada (comité).
     */
    private boolean isCompleteModality(StudentModality modality) {
        boolean hasDefenseDate = modality.getDefenseDate() != null;
        boolean hasExaminers = modality.getDefenseExaminers() != null && !modality.getDefenseExaminers().isEmpty();
        boolean hasDirector = modality.getProjectDirector() != null;
        return hasDefenseDate || hasExaminers || hasDirector;
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
        Estimado(a) %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que la solicitud de edición del documento ha sido aprobada por el jurado evaluador, conforme al proceso de revisión académica.

        A continuación, se relaciona la información correspondiente:

        Documento: "%s".
        Observaciones del jurado: %s.

        En virtud de esta decisión, podrá ingresar a la plataforma institucional y realizar la carga de la versión actualizada del documento. Una vez registrada, la nueva versión será objeto de evaluación por parte del jurado designado.

        Este mensaje constituye una notificación automática generada como constancia de la decisión adoptada y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica – SIGMA
        Universidad Surcolombiana
        """.formatted(
                        student.getName(),
                        event.getDocumentName(),
                        event.getResolutionNotes() != null && !event.getResolutionNotes().isBlank()
                                ? event.getResolutionNotes()
                                : "No se registran observaciones adicionales."
                );
            } else {
                message = """
        Estimado(a) %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que la solicitud de edición del documento no ha sido aprobada por el jurado evaluador, conforme al proceso de revisión académica.

        A continuación, se relaciona la información correspondiente:

        Documento: "%s".
        Motivo de la decisión: %s.

        En consecuencia, el documento conserva su estado actual dentro del proceso académico. En caso de requerir aclaraciones adicionales, podrá comunicarse con la Jefatura de Programa o con el Director de Proyecto.

        Este mensaje constituye una notificación automática generada como constancia de la decisión adoptada y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica – SIGMA
        Universidad Surcolombiana
        """.formatted(
                        student.getName(),
                        event.getDocumentName(),
                        event.getResolutionNotes() != null && !event.getResolutionNotes().isBlank()
                                ? event.getResolutionNotes()
                                : "No se registran motivos adicionales."
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

    private String localizeObservations(String observations) {
        if (observations == null) return "Ninguna";

        // Crear mapa de traducciones para enums
        java.util.Map<String, String> translations = java.util.Map.ofEntries(
            // Tipos de jurados
            java.util.Map.entry("PRIMARY_EXAMINER_1", "Jurado Principal 1"),
            java.util.Map.entry("PRIMARY_EXAMINER_2", "Jurado Principal 2"),
            java.util.Map.entry("TIEBREAKER_EXAMINER", "Jurado de Desempate"),
            
            // Estados de distinción
            java.util.Map.entry("PENDING_COMMITTEE_MERITORIOUS", "Mención Meritoria propuesta (pendiente del comité)"),
            java.util.Map.entry("PENDING_COMMITTEE_LAUREATE", "Mención Laureada propuesta (pendiente del comité)"),
            java.util.Map.entry("TIEBREAKER_PENDING_COMMITTEE_MERITORIOUS", "Mención Meritoria por desempate (pendiente del comité)"),
            java.util.Map.entry("TIEBREAKER_PENDING_COMMITTEE_LAUREATE", "Mención Laureada por desempate (pendiente del comité)"),
            java.util.Map.entry("NO_DISTINCTION", "Sin distinción"),
            java.util.Map.entry("AGREED_APPROVED", "Aprobado"),
            java.util.Map.entry("AGREED_MERITORIOUS", "Meritorio"),
            java.util.Map.entry("AGREED_LAUREATE", "Laureado"),
            java.util.Map.entry("AGREED_REJECTED", "Reprobado"),
            java.util.Map.entry("DISAGREEMENT_PENDING_TIEBREAKER", "Desacuerdo, pendiente desempate"),
            java.util.Map.entry("TIEBREAKER_APPROVED", "Aprobado por desempate"),
            java.util.Map.entry("TIEBREAKER_MERITORIOUS", "Meritorio por desempate"),
            java.util.Map.entry("TIEBREAKER_LAUREATE", "Laureado por desempate"),
            java.util.Map.entry("TIEBREAKER_REJECTED", "Reprobado por desempate"),
            java.util.Map.entry("REJECTED_BY_COMMITTEE", "Rechazado por comité")
        );

        // Aplicar todas las traducciones
        String result = observations;
        for (java.util.Map.Entry<String, String> entry : translations.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        // Traducir distinción en formato "Distinción propuesta: X → Distinción confirmada: Y"
        if (result.contains("Distinción propuesta:") && result.contains("Distinción confirmada:")) {
            try {
                String regex = "Distinción propuesta: ([A-Z_]+) → Distinción confirmada: ([A-Z_]+)";
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(result);
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
                    result = result.replace(
                        "Distinción propuesta: " + propuesta + " → Distinción confirmada: " + confirmada,
                        "Distinción propuesta: " + propuestaLabel + " → Distinción confirmada: " + confirmadaLabel
                    );
                }
            } catch (Exception e) {
                // Ignorar error de formateo
            }
        }

        return result;
    }
}


