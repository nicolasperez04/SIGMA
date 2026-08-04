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
import com.SIGMA.USCO.notifications.event.ModalityEvent;
import com.SIGMA.USCO.notifications.service.AcademicCertificatePdfService;
import com.SIGMA.USCO.notifications.service.NotificationBuilderHelper;
import com.SIGMA.USCO.notifications.service.NotificationDispatcherService;
import com.SIGMA.USCO.notifications.service.NotificationFactory;
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
    private final NotificationFactory notificationFactory;
    private final NotificationDispatcherService dispatcher;
    private final UserRepository userRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final AcademicCertificatePdfService certificatePdfService;

    @EventListener
    public void handleEvent(ModalityEvent event) {
        switch (event.getType()) {
            case MODALITY_STARTED -> handleModalityStarted(event);
            case DOCUMENT_CORRECTIONS_REQUESTED -> handleDocumentCorrectionsRequested(event);
            case MODALITY_CANCELLATION_REQUESTED -> handleCancellationRequested(event);
            case MODALITY_CANCELLATION_APPROVED -> handleCancellationApproved(event);
            case MODALITY_CANCELLATION_REJECTED -> handleCancellationRejected(event);
            case DEFENSE_SCHEDULED -> handleDefenseScheduled(event);
            case DIRECTOR_ASSIGNED -> handleDirectorAssigned(event);
            case DEFENSE_COMPLETED -> handleDefenseResult(event);
            case MODALITY_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE -> handleModalityApprovedByCommittee(event);
            case MODALITY_APPROVED_BY_PROGRAM_HEAD -> handleModalityApprovedByProgramHead(event);
            case CORRECTION_DEADLINE_REMINDER -> handleCorrectionDeadlineReminder(event);
            case CORRECTION_DEADLINE_EXPIRED -> handleCorrectionDeadlineExpired(event);
            case CORRECTION_RESUBMITTED -> handleCorrectionResubmitted(event);
            case CORRECTION_APPROVED -> handleCorrectionApproved(event);
            case CORRECTION_REJECTED_FINAL -> handleCorrectionRejectedFinal(event);
            case MODALITY_CLOSED_BY_COMMITTEE -> handleModalityClosedByCommittee(event);
            case MODALITY_INVITATION_RECEIVED -> handleModalityInvitationSent(event);
            case MODALITY_INVITATION_ACCEPTED -> handleModalityInvitationAccepted(event);
            case MODALITY_INVITATION_REJECTED -> handleModalityInvitationRejected(event);
            case MODALITY_FINAL_APPROVED_BY_COMMITTEE -> handleModalityFinalApprovedByCommittee(event);
            case MODALITY_REJECTED_BY_COMMITTEE -> handleModalityRejectedByCommittee(event);
            case SEMINAR_STARTED -> handleSeminarStarted(event);
            case SEMINAR_CANCELLED -> handleSeminarCancelled(event);
            case MODALITY_APPROVED_BY_EXAMINERS -> handleModalityApprovedByExaminers(event);
            case EXAMINER_ASSIGNED -> handleExaminersAssigned(event);
            case DOCUMENT_EDIT_APPROVED, DOCUMENT_EDIT_REJECTED -> handleDocumentEditResolved(event);
            default -> log.warn("Unhandled notification type: {}", event.getType());
        }
    }

    private void handleModalityStarted(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        User student = modality.getLeader();
        String subject = "Modalidad iniciada – SIGMA";
        String body = """
                Nos permitimos informarle que su modalidad de grado ha sido registrada e iniciada oficialmente en el sistema institucional. A continuación, se relaciona la información correspondiente:

                Modalidad de grado: "%s".
                Estado actual del proceso: %s.

                Actualmente, la modalidad se encuentra en etapa de revisión y evaluación por parte de la Jefatura de Programa y del Comité de Currículo correspondiente.

                Se recomienda consultar periódicamente el sistema y mantenerse atento(a) a las notificaciones institucionales, ya que a través de este medio se comunicarán solicitudes, observaciones o decisiones relacionadas con su proceso académico.
                """.formatted(
                NotificationBuilderHelper.buildModalityInfo(modality),
                TranslationUtils.translateModalityProcessStatus(modality.getStatus())
        );
        String message = NotificationMessageTemplates.greeting(student.getName()) + body + NotificationMessageTemplates.closing();
        notificationFactory.buildAndDispatch(NotificationType.MODALITY_STARTED, NotificationRecipientType.STUDENT, student, modality, subject, message);

    }


    private void handleDocumentCorrectionsRequested(ModalityEvent event) {
        StudentDocument document = studentDocumentRepository.findById(
                event.get(ModalityEvent.KEY_STUDENT_DOCUMENT_ID, Long.class)
        ).orElseThrow();
        StudentModality modality = document.getStudentModality();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String subject = "Correcciones solicitadas en documento académico – Acción requerida";
        NotificationRecipientType requestedBy = event.get(ModalityEvent.KEY_REQUESTED_BY, NotificationRecipientType.class);
        String observations = event.get(ModalityEvent.KEY_OBSERVATIONS, String.class);
        for (var member : members) {
            User student = member.getStudent();

            String requestedByText;
            if (requestedBy == NotificationRecipientType.PROGRAM_HEAD) {
                requestedByText = "la Jefatura de Programa y/o Coordinación de Modalidades";
            } else if (requestedBy == NotificationRecipientType.EXAMINER) {
                requestedByText = "un jurado evaluador";
            } else {
                requestedByText = "el Comité de Currículo del Programa";
            }

            String body = """
        Nos permitimos informarle que %s ha solicitado la realización de correcciones en uno de los documentos asociados a su modalidad de grado, en el marco del proceso de revisión académica.

        A continuación, se detalla la información correspondiente:

        Documento: "%s".
        Observaciones registradas: %s.

        En este sentido, se solicita ingresar a la plataforma institucional, revisar detalladamente las observaciones indicadas y efectuar los ajustes correspondientes, con el fin de dar continuidad al proceso académico dentro de los plazos establecidos.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso.
        """.formatted(
                    requestedByText,
                    document.getDocumentConfig().getDocumentName(),
                    observations != null && !observations.isBlank()
                            ? observations
                            : "No se registraron observaciones adicionales."
            );
            String message = NotificationMessageTemplates.greeting(student.getName()) + body + NotificationMessageTemplates.closing();
            notificationFactory.buildAndDispatch(NotificationType.DOCUMENT_CORRECTIONS_REQUESTED, NotificationRecipientType.STUDENT, student, modality, subject, message);

        }
    }


    private void handleCancellationRequested(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                sm.getId(),
                MemberStatus.ACTIVE
        );
        String subject = "Solicitud de cancelación registrada – Modalidad de grado";
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(sm);
        for (var member : members) {
            User student = member.getStudent();
            String body = """
        Nos permitimos informarle que su solicitud de cancelación de la modalidad de grado ha sido registrada correctamente en el sistema institucional.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".

        La solicitud será evaluada inicialmente por el director del proyecto y, posteriormente, por el Comité de Currículo del programa académico correspondiente.

        Una vez se emita una decisión oficial, esta le será notificada oportunamente a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.
        """.formatted(
                    modalidadInfo
            );
            String message = NotificationMessageTemplates.greeting(student.getName()) + body + NotificationMessageTemplates.closing();
            notificationFactory.buildAndDispatch(NotificationType.MODALITY_CANCELLATION_REQUESTED, NotificationRecipientType.STUDENT, student, sm, subject, message);

        }
    }


    private void handleCancellationApproved(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                sm.getId(),
                MemberStatus.ACTIVE
        );
        String subject = "Cancelación aprobada – Modalidad de grado";
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(sm);
        for (var member : members) {
            User student = member.getStudent();
            String body = """
        Nos permitimos informarle que el Comité de Currículo del programa académico ha aprobado oficialmente su solicitud de cancelación de la modalidad de grado.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".

        En consecuencia, la modalidad queda cerrada de manera oficial y el proceso académico asociado finaliza a partir de la fecha en que se emite la presente decisión.

        En caso de requerir orientación adicional o información complementaria sobre su situación académica, podrá comunicarse con la Jefatura de Programa.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad institucional.
        """.formatted(
                    modalidadInfo
            );
            String message = NotificationMessageTemplates.greeting(student.getName()) + body + NotificationMessageTemplates.closingSigma();
            notificationFactory.buildAndDispatch(NotificationType.MODALITY_CANCELLATION_APPROVED, NotificationRecipientType.STUDENT, student, sm, subject, message);

        }
    }


    private void handleCancellationRejected(ModalityEvent event) {
        StudentModality sm = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                sm.getId(),
                MemberStatus.ACTIVE
        );
        String subject = "Cancelación no aprobada – Modalidad de grado";
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(sm);
        String reason = event.get(ModalityEvent.KEY_REASON, String.class);
        for (var member : members) {
            User student = member.getStudent();
            String body = """
        Nos permitimos informarle que el Comité de Currículo del programa académico ha decidido no aprobar su solicitud de cancelación de la modalidad de grado.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".
        Motivo de la decisión: %s.

        En consecuencia, la modalidad de grado continúa activa bajo las condiciones previamente establecidas dentro del proceso académico.

        En caso de requerir mayor claridad sobre la presente decisión o desear orientación adicional, podrá comunicarse con la Jefatura de Programa.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad institucional.
        """.formatted(
                    modalidadInfo,
                    reason != null && !reason.isBlank()
                            ? reason
                            : "No se especifican motivos adicionales."
            );
            String message = NotificationMessageTemplates.greeting(student.getName()) + body + NotificationMessageTemplates.closing();
            notificationFactory.buildAndDispatch(NotificationType.MODALITY_CANCELLATION_REJECTED, NotificationRecipientType.STUDENT, student, sm, subject, message);

        }
    }


    private void handleDefenseScheduled(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        User director = modality.getProjectDirector();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String studentSubject = "Sustentación programada – Modalidad de Grado";
        Object defenseDate = event.get(ModalityEvent.KEY_DEFENSE_DATE, Object.class);
        String defenseLocation = event.get(ModalityEvent.KEY_DEFENSE_LOCATION, String.class);
        for (var member : members) {
            User student = member.getStudent();
            String body = """
        Nos permitimos informarle que la sustentación correspondiente a su modalidad de grado ha sido programada, conforme al proceso académico establecido.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".
        Fecha y hora: %s.
        Lugar: %s.
        Director asignado: %s.

        De acuerdo con la normativa institucional vigente, deberá realizar la divulgación pública de su proyecto con al menos tres (3) días hábiles de anticipación a la fecha programada para la sustentación, en los espacios definidos por el programa académico.

        Se recomienda presentarse con la debida antelación y cumplir estrictamente con los lineamientos académicos establecidos para el desarrollo de la sesión de sustentación.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.
        """.formatted(
                    modalidadInfo,
                    defenseDate,
                    defenseLocation,
                    director != null
                            ? director.getName() + " " + director.getLastName()
                            : "No asignado"
            );
            String studentMessage = NotificationMessageTemplates.greeting(student.getName()) + body + NotificationMessageTemplates.closing();
            notificationFactory.buildAndDispatch(NotificationType.DEFENSE_SCHEDULED, NotificationRecipientType.STUDENT, student, modality, studentSubject, studentMessage);

        }
    }


    private void handleDirectorAssigned(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        User director = userRepository.findById(event.get(ModalityEvent.KEY_DIRECTOR_ID, Long.class))
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String studentSubject = "Director de proyecto asignado – Modalidad de grado";
        for (var member : members) {
            User student = member.getStudent();
            String body = """
        Nos permitimos informarle que ha sido designado oficialmente un Director de Proyecto para su modalidad de grado, conforme a los lineamientos académicos vigentes.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".
        Director asignado: %s.
        Correo electrónico: %s.

        A partir de este momento, el director asignado actuará como su orientador académico principal durante el desarrollo de la modalidad de grado y será responsable del seguimiento y acompañamiento del proceso.

        Se recomienda establecer contacto oportunamente con el director, con el fin de coordinar las actividades iniciales y definir el plan de trabajo correspondiente.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.
        """.formatted(
                    modalidadInfo,
                    director.getName() + " " + director.getLastName(),
                    director.getEmail()
            );
            String studentMessage = NotificationMessageTemplates.greeting(student.getName()) + body + NotificationMessageTemplates.closing();
            notificationFactory.buildAndDispatch(NotificationType.DIRECTOR_ASSIGNED, NotificationRecipientType.STUDENT, student, modality, studentSubject, studentMessage);

        }
    }


    private void handleDefenseResult(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();

        ModalityProcessStatus finalStatus = event.get(ModalityEvent.KEY_FINAL_STATUS, ModalityProcessStatus.class);
        boolean approved = finalStatus == ModalityProcessStatus.GRADED_APPROVED;
        boolean approvedPendingCommitteeReview = finalStatus == ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW;
        boolean shouldSendCertificate = approved || approvedPendingCommitteeReview;

        String studentSubject = shouldSendCertificate
                ? (approvedPendingCommitteeReview
                ? "Resultado de sustentación – Modalidad aprobada (distinción en revisión del comité)"
                : "Resultado final de sustentación – Modalidad aprobada")
                : "Resultado final de sustentación – Modalidad no aprobada";

        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(), MemberStatus.ACTIVE
        );

        if (members == null || members.isEmpty()) {
            StudentModalityMember syntheticLeaderMember = StudentModalityMember.builder()
                    .studentModality(modality)
                    .student(modality.getLeader())
                    .status(MemberStatus.ACTIVE)
                    .isLeader(true)
                    .build();
            members = List.of(syntheticLeaderMember);
        }

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

        User leader = modality.getLeader();
        if (leader != null) {
            String leaderMessage = shouldSendCertificate
                    ? buildApprovedStudentMessage(leader, modality, event)
                    : buildRejectedStudentMessage(leader, modality, event);

            Notification leaderNotification = notificationFactory.buildAndSave(
                    NotificationType.DEFENSE_COMPLETED, NotificationRecipientType.STUDENT,
                    leader, null, modality, studentSubject, leaderMessage);

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

        for (StudentModalityMember member : members) {
            if (leader != null && member.getStudent().getId().equals(leader.getId())) {
                continue;
            }

            User student = member.getStudent();
            String studentMessage = shouldSendCertificate
                    ? buildApprovedStudentMessage(student, modality, event)
                    : buildRejectedStudentMessage(student, modality, event);

            notificationFactory.buildAndDispatch(
                    NotificationType.DEFENSE_COMPLETED, NotificationRecipientType.STUDENT,
                    student, null, modality, studentSubject, studentMessage);
            log.info("Notificación enviada al miembro {} (modalidad ID {}, sin acta adjunta)", student.getId(), modality.getId());
        }

        if (certificate != null && shouldSendCertificate) {
            try {
                certificatePdfService.updateCertificateStatus(certificate.getId(), CertificateStatus.SENT);
                log.info("Estado del certificado actualizado a SENT para modalidad ID: {}", modality.getId());
            } catch (Exception e) {
                log.error("Error actualizando estado del certificado: {}", e.getMessage());
            }
        }
    }


    private String buildApprovedStudentMessage(User student, StudentModality modality, ModalityEvent event) {
        String observaciones = TranslationUtils.localizeObservations(event.get(ModalityEvent.KEY_OBSERVATIONS, String.class));
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String body = """
            Nos permitimos informarle que, una vez realizada la sustentación y evaluado el resultado por los jurados designados, ha aprobado oficialmente la modalidad de grado.

            A continuación, se relaciona la información correspondiente:

            Modalidad de grado: "%s".
            Mención académica: %s.
            Observaciones registradas: %s.

            Se adjunta a este correo el acta de aprobación en formato PDF, documento oficial que certifica la culminación satisfactoria de su modalidad de grado, conforme a la normatividad académica vigente.

            Para finalizar su proceso académico, deberá comunicarse con la Jefatura de Programa, con el fin de adelantar los trámites administrativos correspondientes.

            Reciba un reconocimiento institucional por este importante logro académico.

            Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.
            """.formatted(
                modalidadInfo,
                TranslationUtils.translateAcademicDistinction(event.get(ModalityEvent.KEY_ACADEMIC_DISTINCTION, AcademicDistinction.class)),
                observaciones != null && !observaciones.isBlank() ? observaciones : "No se registran observaciones."
        );
        return NotificationMessageTemplates.greeting(student.getName()) + body + NotificationMessageTemplates.universityClosing();
    }

    private String buildRejectedStudentMessage(User student, StudentModality modality, ModalityEvent event) {
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String observations = event.get(ModalityEvent.KEY_OBSERVATIONS, String.class);
        String body = """
            Nos permitimos informarle que, una vez realizada la sustentación y evaluado el resultado por los jurados designados, no se ha determinado la aprobación de la modalidad de grado en la presente oportunidad.

            A continuación, se relaciona la información correspondiente:

            Modalidad de grado: "%s".
            Observaciones de los jurados: %s.

            De acuerdo con la normativa académica vigente, se recomienda revisar detenidamente las observaciones consignadas y establecer comunicación con el Director de Proyecto, así como con la Jefatura de Programa, con el fin de definir las acciones a seguir dentro del proceso académico.

            Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.
            """.formatted(
                modalidadInfo,
                observations != null && !observations.isBlank()
                        ? observations
                        : "No se registran observaciones adicionales."
        );
        return NotificationMessageTemplates.greeting(student.getName()) + body + NotificationMessageTemplates.closingSigma() + "\nUniversidad Surcolombiana";
    }


    private void handleModalityApprovedByCommittee(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Modalidad de grado aprobada – Comité de Currículo";
        for (var member : members) {
            User student = member.getStudent();
            String body = """
        Nos permitimos informarle que la modalidad de grado ha sido aprobada oficialmente por el Comité de Currículo del programa académico.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".
        Estado del proceso: Propuesta aprobada por el Comité de Currículo.
        Director de Proyecto: %s.
        Fecha de aprobación: %s.

        En virtud de esta decisión, la modalidad de grado continúa con la siguiente etapa del proceso académico, correspondiente a la evaluación y aprobación por parte del jurado designado.

        Se recomienda mantenerse atento(a) a las notificaciones del sistema institucional y conservar comunicación permanente con el Director de Proyecto y la Jefatura de Programa, con el fin de garantizar el adecuado desarrollo y seguimiento del proceso.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.
        """.formatted(
                    modalidadInfo,
                    modality.getProjectDirector() != null
                            ? modality.getProjectDirector().getName() + " " +
                            modality.getProjectDirector().getLastName()
                            : "No se registra director asignado.",
                    modality.getUpdatedAt()
            );
            String message = NotificationMessageTemplates.greeting(student.getName()) + body + NotificationMessageTemplates.universityClosing();
            notificationFactory.buildAndDispatch(NotificationType.MODALITY_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE, NotificationRecipientType.STUDENT, student, modality, subject, message);

        }
    }


    private void handleModalityApprovedByProgramHead(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId()).orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Modalidad de grado aprobada – Jefatura de Programa y/o Coordinación de Modalidades";
        for (var member : members) {
            User student = member.getStudent();
            String body = """
        Nos permitimos informarle que la modalidad de grado ha sido aprobada oficialmente por la Jefatura de Programa y/o la coordinación de modalidades del programa académico.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".
        Estado del proceso: Aprobada por la Jefatura de Programa.

        En virtud de esta decisión, la modalidad de grado continuará con la etapa de evaluación por parte del Comité de Currículo del programa académico, instancia encargada de emitir la decisión correspondiente para la continuidad del proceso.

        Se recomienda mantenerse atento(a) a las notificaciones del sistema institucional y conservar comunicación con la Jefatura de Programa, en caso de requerir información adicional o aclaraciones relacionadas con el trámite.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.
        """.formatted(
                    modalidadInfo
            );
            String message = NotificationMessageTemplates.greeting(student.getName()) + body + NotificationMessageTemplates.universityClosing();
            notificationFactory.buildAndDispatch(NotificationType.MODALITY_APPROVED_BY_PROGRAM_HEAD, NotificationRecipientType.STUDENT, student, modality, subject, message);

        }
    }


    private void handleCorrectionDeadlineReminder(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        Integer daysRemaining = event.get(ModalityEvent.KEY_DAYS_REMAINING, Integer.class, 0);
        Object deadline = event.get(ModalityEvent.KEY_DEADLINE, Object.class);
        String subject = "Recordatorio oficial – Plazo de correcciones (%d días restantes)"
                .formatted(daysRemaining);
        for (var member : members) {
            User student = member.getStudent();
            String body = """
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
        """.formatted(
                    modalidadInfo,
                    daysRemaining,
                    deadline
            );
            String message = NotificationMessageTemplates.greeting(student.getName()) + body + NotificationMessageTemplates.closingSigma() + "\nUniversidad Surcolombiana";
            notificationFactory.buildAndDispatch(NotificationType.CORRECTION_DEADLINE_REMINDER, NotificationRecipientType.STUDENT, student, modality, subject, message);

            log.info("Recordatorio de plazo de corrección enviado al estudiante {}", student.getId());
        }
    }


    private void handleCorrectionDeadlineExpired(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Notificación oficial – Cancelación automática de modalidad por vencimiento de plazo";
        Object requestDate = event.get(ModalityEvent.KEY_REQUEST_DATE, Object.class);
        for (var member : members) {
            User student = member.getStudent();
            String body = """
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
            """.formatted(
                    modalidadInfo,
                    requestDate
            );
            String message = NotificationMessageTemplates.greeting(student.getName()) + body + NotificationMessageTemplates.universityClosing();
            notificationFactory.buildAndDispatch(NotificationType.CORRECTION_DEADLINE_EXPIRED, NotificationRecipientType.STUDENT, student, modality, subject, message);

            log.info("Notificación de cancelación por vencimiento enviada al estudiante {}", student.getId());
        }
    }


    private void handleCorrectionResubmitted(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Notificación oficial – Documento corregido recibido";
        String documentName = event.get(ModalityEvent.KEY_DOCUMENT_NAME, String.class);
        for (var member : members) {
            User student = member.getStudent();
            String body = """
            Nos permitimos informarle que la carga del documento corregido ha sido registrada correctamente en el Sistema de Gestión Académica, en el marco del proceso de revisión de su modalidad de grado.

            A continuación, se relaciona la información correspondiente:

            Modalidad de grado: "%s".
            Nombre del archivo: %s.
            Fecha de envío: %s.
            Estado del proceso: Correcciones enviadas – pendiente de revisión.

            El documento será evaluado por las instancias académicas competentes. Una vez finalizada la revisión, le será notificado el resultado correspondiente a través de la plataforma institucional.

            Se recomienda permanecer atento(a) a las comunicaciones emitidas por el sistema, con el fin de garantizar la adecuada continuidad del proceso académico.

            Este mensaje constituye una notificación automática generada como constancia del registro de la nueva versión del documento y para efectos de control y trazabilidad institucional.
            """.formatted(
                    modalidadInfo,
                    documentName,
                    LocalDateTime.now().toLocalDate()
            );
            String message = NotificationMessageTemplates.greeting(student.getName()) + body + NotificationMessageTemplates.universityClosing();
            notificationFactory.buildAndDispatch(NotificationType.CORRECTION_RESUBMITTED, NotificationRecipientType.STUDENT, student, modality, subject, message);

            log.info("Notificación de resubmisión de corrección enviada al estudiante {}", student.getId());
        }
    }


    private void handleCorrectionApproved(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Notificación oficial – Correcciones aprobadas";
        String documentName = event.get(ModalityEvent.KEY_DOCUMENT_NAME, String.class);
        for (var member : members) {
            User student = member.getStudent();
            String body = """
            Nos permitimos informarle que las correcciones remitidas han sido aprobadas por el jurado evaluador, en el marco del proceso de revisión académica de su modalidad de grado.

            A continuación, se relaciona la información correspondiente:

            Modalidad de grado: "%s".
            Documento evaluado: %s.
            Estado del proceso: Correcciones aprobadas.

            En virtud de esta decisión, la modalidad de grado continúa con el desarrollo normal del proceso académico, conforme a las disposiciones institucionales vigentes.

            La siguiente actuación dentro del proceso será notificada oportunamente a través de la plataforma institucional.

            Este mensaje constituye una notificación automática generada como constancia de la decisión registrada y para efectos de control y trazabilidad institucional.
            """.formatted(
                    modalidadInfo,
                    documentName
            );
            String message = NotificationMessageTemplates.greeting(student.getName()) + body + NotificationMessageTemplates.universityClosing();
            notificationFactory.buildAndDispatch(NotificationType.CORRECTION_APPROVED, NotificationRecipientType.STUDENT, student, modality, subject, message);

            log.info("Notificación de aprobación de corrección enviada al estudiante {}", student.getId());
        }
    }


    private void handleCorrectionRejectedFinal(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Notificación oficial – Cancelación de modalidad por rechazo definitivo de correcciones";
        String documentName = event.get(ModalityEvent.KEY_DOCUMENT_NAME, String.class);
        String reason = event.get(ModalityEvent.KEY_REASON, String.class);
        for (var member : members) {
            User student = member.getStudent();
            String body = """
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
            """.formatted(
                    modalidadInfo,
                    documentName,
                    reason != null && !reason.isBlank()
                            ? reason
                            : "No se registran motivos adicionales."
            );
            String message = NotificationMessageTemplates.greeting(student.getName()) + body + NotificationMessageTemplates.closingSigma() + "\nUniversidad Surcolombiana";
            notificationFactory.buildAndDispatch(NotificationType.CORRECTION_REJECTED_FINAL, NotificationRecipientType.STUDENT,
                    student, modality, subject, message
            );

            log.info("Notificación de rechazo final de corrección enviada al estudiante {}", student.getId());
        }
    }


    private void handleModalityClosedByCommittee(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow();
        User committeeMember = userRepository.findById(event.get(ModalityEvent.KEY_COMMITTEE_MEMBER_ID, Long.class))
                .orElseThrow();
        var members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                modality.getId(),
                MemberStatus.ACTIVE
        );
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Notificación oficial – Cierre de modalidad por decisión del Comité de Currículo";
        String reason = event.get(ModalityEvent.KEY_REASON, String.class);
        for (var member : members) {
            User student = member.getStudent();
            String body = """
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
            """.formatted(
                    modalidadInfo,
                    modality.getAcademicProgram().getName(),
                    committeeMember.getName(),
                    committeeMember.getLastName(),
                    LocalDateTime.now().toString(),
                    reason != null && !reason.isBlank()
                            ? reason
                            : "No se registran motivos adicionales."
            );
            String message = NotificationMessageTemplates.greeting(student.getName()) + body + NotificationMessageTemplates.universityClosing();
            notificationFactory.buildAndDispatch(NotificationType.MODALITY_CLOSED_BY_COMMITTEE, NotificationRecipientType.STUDENT,
                    student, committeeMember, modality, subject, message
            );

            log.info("Notificación de cierre de modalidad por comité enviada al estudiante {}", student.getId());
        }
    }


    private void handleModalityInvitationSent(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User invitee = userRepository.findById(event.get(ModalityEvent.KEY_INVITEE_ID, Long.class))
                .orElseThrow(() -> new RuntimeException("Estudiante invitado no encontrado"));

        User inviter = userRepository.findById(event.get(ModalityEvent.KEY_INVITER_ID, Long.class))
                .orElseThrow(() -> new RuntimeException("Estudiante que invita no encontrado"));

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        Long invitationId = event.get(ModalityEvent.KEY_INVITATION_ID, Long.class);

        String subject = "Invitación para unirte a una modalidad de grado grupal – SIGMA";

        String body = """
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
                """.formatted(
                modalidadInfo,
                modality.getAcademicProgram().getName(),
                inviter.getName() + " " + inviter.getLastName(),
                LocalDateTime.now().toString(),
                inviter.getName()
        );
        String message = NotificationMessageTemplates.greeting(invitee.getName()) + body + NotificationMessageTemplates.closingSigma() + "\nUniversidad Surcolombiana";

        Notification notification = Notification.builder()
                .type(NotificationType.MODALITY_INVITATION_RECEIVED)
                .recipientType(NotificationRecipientType.STUDENT)
                .recipient(invitee)
                .triggeredBy(inviter)
                .studentModality(modality)
                .invitationId(invitationId)
                .subject(subject)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        notificationFactory.saveAndDispatch(notification);

        log.info("Notificación de invitación a modalidad grupal enviada al estudiante {} por el estudiante {}",
                invitee.getId(), inviter.getId());
    }


    private void handleModalityInvitationAccepted(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User acceptedBy = userRepository.findById(event.get(ModalityEvent.KEY_ACCEPTED_BY_ID, Long.class))
                .orElseThrow(() -> new RuntimeException("Estudiante que aceptó no encontrado"));

        User leader = userRepository.findById(event.get(ModalityEvent.KEY_LEADER_ID, Long.class))
                .orElseThrow(() -> new RuntimeException("Líder del grupo no encontrado"));

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String subject = "Un estudiante aceptó tu invitación a la modalidad grupal – SIGMA";

        String body = """
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
                """.formatted(
                acceptedBy.getName() + " " + acceptedBy.getLastName(),
                modalidadInfo,
                modality.getAcademicProgram().getName(),
                LocalDateTime.now().toString()
        );
        String message = NotificationMessageTemplates.greeting(leader.getName()) + body + NotificationMessageTemplates.closingSigma() + "\nUniversidad Surcolombiana";

        notificationFactory.buildAndDispatch(NotificationType.MODALITY_INVITATION_ACCEPTED, NotificationRecipientType.STUDENT,
                leader, acceptedBy, modality, subject, message
        );


        log.info("Notificación de aceptación de invitación enviada al líder {} por el estudiante {}",
                leader.getId(), acceptedBy.getId());
    }


    private void handleModalityInvitationRejected(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User rejectedBy = userRepository.findById(event.get(ModalityEvent.KEY_REJECTED_BY_ID, Long.class))
                .orElseThrow(() -> new RuntimeException("Estudiante que rechazó no encontrado"));

        User leader = userRepository.findById(event.get(ModalityEvent.KEY_LEADER_ID, Long.class))
                .orElseThrow(() -> new RuntimeException("Líder del grupo no encontrado"));

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String subject = "Un estudiante rechazó tu invitación a la modalidad grupal – SIGMA";

        String body = """
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
                """.formatted(
                rejectedBy.getName() + " " + rejectedBy.getLastName(),
                modalidadInfo,
                modality.getAcademicProgram().getName(),
                LocalDateTime.now().toString(),
                3,
                studentModalityMemberRepository.countByStudentModalityIdAndStatus(
                        modality.getId(),
                        MemberStatus.ACTIVE
                )
        );
        String message = NotificationMessageTemplates.greeting(leader.getName()) + body + NotificationMessageTemplates.closingSigma() + "\nUniversidad Surcolombiana";

        notificationFactory.buildAndDispatch(NotificationType.MODALITY_INVITATION_REJECTED, NotificationRecipientType.STUDENT,
                leader, rejectedBy, modality, subject, message
        );


        log.info("Notificación de rechazo de invitación enviada al líder {} por el estudiante {}",
                leader.getId(), rejectedBy.getId());
    }


    private void handleModalityFinalApprovedByCommittee(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User committeeMember = userRepository.findById(event.get(ModalityEvent.KEY_COMMITTEE_MEMBER_ID, Long.class))
                .orElseThrow(() -> new RuntimeException("Miembro del comité no encontrado"));

        List<StudentModalityMember> activeMembers = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String subject = "¡Felicitaciones! — Modalidad de Grado Aprobada por el Comité de Currículo";

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

        String observations = event.get(ModalityEvent.KEY_OBSERVATIONS, String.class);

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
                    observations != null && !observations.isBlank()
                            ? "Observaciones del Comité: " + observations + ".\n\n"
                            : ""
            );

            Notification notification = notificationFactory.buildAndSave(
                    NotificationType.MODALITY_FINAL_APPROVED_BY_COMMITTEE, NotificationRecipientType.STUDENT,
                    student, committeeMember, modality, subject, message);

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


    private void handleModalityRejectedByCommittee(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User student = userRepository.findById(event.get(ModalityEvent.KEY_STUDENT_ID, Long.class))
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        User committeeMember = userRepository.findById(event.get(ModalityEvent.KEY_COMMITTEE_MEMBER_ID, Long.class))
                .orElseThrow(() -> new RuntimeException("Miembro del comité no encontrado"));

        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);

        String subject = "IMPORTANTE: Modalidad de Grado NO APROBADA - Decisión del Comité";

        String reason = event.get(ModalityEvent.KEY_REASON, String.class);

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
                reason != null && !reason.isBlank()
                        ? reason
                        : "No se registran motivos adicionales."
        );

        notificationFactory.buildAndDispatch(NotificationType.MODALITY_REJECTED_BY_COMMITTEE, NotificationRecipientType.STUDENT,
                student, committeeMember, modality, subject, message
        );

    }


    private void handleSeminarStarted(ModalityEvent event) {
        String seminarName = event.get(ModalityEvent.KEY_SEMINAR_NAME, String.class);
        String recipientName = event.get(ModalityEvent.KEY_RECIPIENT_NAME, String.class);
        String recipientEmail = event.get(ModalityEvent.KEY_RECIPIENT_EMAIL, String.class);
        String programName = event.get(ModalityEvent.KEY_PROGRAM_NAME, String.class);
        Object startDate = event.get(ModalityEvent.KEY_START_DATE, Object.class);
        Integer totalHours = event.get(ModalityEvent.KEY_TOTAL_HOURS, Integer.class, 0);

        String subject = "Inicio de Seminario: " + seminarName;

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
                recipientName,
                seminarName,
                seminarName,
                programName,
                startDate,
                totalHours,
                programName
        );

        User recipient = userRepository.findByEmail(recipientEmail).orElse(null);

        if (recipient != null) {
            notificationFactory.buildAndDispatch(NotificationType.SEMINAR_STARTED, NotificationRecipientType.STUDENT,
                    recipient, null, null, subject, body
            );

        } else {
            log.warn("No se encontró usuario con email {} para notificación de inicio de seminario", recipientEmail);
        }
    }


    private void handleSeminarCancelled(ModalityEvent event) {
        String seminarName = event.get(ModalityEvent.KEY_SEMINAR_NAME, String.class);
        String recipientName = event.get(ModalityEvent.KEY_RECIPIENT_NAME, String.class);
        String recipientEmail = event.get(ModalityEvent.KEY_RECIPIENT_EMAIL, String.class);
        String programName = event.get(ModalityEvent.KEY_PROGRAM_NAME, String.class);
        Object cancelledDate = event.get(ModalityEvent.KEY_CANCELLED_DATE, Object.class);
        String reason = event.get(ModalityEvent.KEY_REASON, String.class);

        String subject = "Cancelación de Seminario: " + seminarName;

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
                recipientName,
                seminarName,
                seminarName,
                programName,
                cancelledDate,
                reason != null ? "\nMotivo: " + reason : "",
                programName
        );

        User recipient = userRepository.findByEmail(recipientEmail).orElse(null);

        if (recipient != null) {
            notificationFactory.buildAndDispatch(NotificationType.SEMINAR_CANCELLED, NotificationRecipientType.STUDENT,
                    recipient, null, null, subject, body
            );

        } else {
            log.warn("No se encontró usuario con email {} para notificación de cancelación de seminario", recipientEmail);
        }
    }


    private void handleModalityApprovedByExaminers(ModalityEvent event) {
        StudentModality modality = studentModalityRepository
                .findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User examiner = userRepository
                .findById(event.get(ModalityEvent.KEY_EXAMINER_ID, Long.class))
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

        String modalityName = modality.getProgramDegreeModality().getDegreeModality().getName();

        for (var member : members) {
            User student = member.getStudent();

            String personalizedMessage = String.format(
                    messageTemplate,
                    student.getName(),
                    modalityName,
                    modality.getAcademicProgram().getName(),
                    LocalDateTime.now()
            );

            notificationFactory.buildAndDispatch(NotificationType.MODALITY_APPROVED_BY_EXAMINERS, NotificationRecipientType.STUDENT,
                    student, examiner, modality, subject, personalizedMessage
            );

        }
    }


    private void handleExaminersAssigned(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));
        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
        List<DefenseExaminer> examiners = modality.getDefenseExaminers();
        String jurados = examiners.stream()
                .map(e -> e.getExaminer().getName() + " " + e.getExaminer().getLastName() + " (" + TranslationUtils.translateExaminerType(e.getExaminerType()) + ")")
                .toList()
                .isEmpty() ? "-" : String.join(", ", examiners.stream()
                .map(e -> e.getExaminer().getName() + " " + e.getExaminer().getLastName() + " (" + TranslationUtils.translateExaminerType(e.getExaminerType()) + ")")
                .toList());
        String modalidadInfo = NotificationBuilderHelper.buildModalityInfo(modality);
        String subject = "Asignación de jurados evaluadores a tu modalidad de grado";
        String bodyTemplate = """
            Nos permitimos informarle que han sido designados oficialmente los jurados evaluadores para su modalidad de grado, conforme al proceso académico establecido.

            A continuación, se relaciona la información correspondiente:

            Modalidad de grado: "%s".
            Programa académico: %s.
            Jurados asignados: %s.
            Fecha de asignación: %s.

            Los jurados designados serán responsables de la evaluación académica de su trabajo, conforme a los lineamientos institucionales vigentes.

            Se recomienda consultar periódicamente la plataforma institucional, con el fin de hacer seguimiento al estado y avance del proceso académico.

            Este mensaje constituye una notificación automática generada como constancia de la asignación realizada y para efectos de control y trazabilidad institucional.
            """;

        for (StudentModalityMember member : members) {
            User student = member.getStudent();
            String body = String.format(bodyTemplate,
                    modalidadInfo,
                    modality.getProgramDegreeModality().getAcademicProgram().getName(),
                    jurados,
                    LocalDateTime.now()
            );
            String message = NotificationMessageTemplates.greeting(student.getName() + " " + student.getLastName()) + body + NotificationMessageTemplates.universityClosing();

            notificationFactory.buildAndDispatch(NotificationType.EXAMINER_ASSIGNED, NotificationRecipientType.STUDENT,
                    student, modality, subject, message
            );

        }
    }


    private void handleDocumentEditResolved(ModalityEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        List<StudentModalityMember> members = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);

        boolean approved = Boolean.TRUE.equals(event.get(ModalityEvent.KEY_APPROVED, Boolean.class));
        NotificationType type = approved ? NotificationType.DOCUMENT_EDIT_APPROVED : NotificationType.DOCUMENT_EDIT_REJECTED;
        String subject = approved
                ? "Solicitud de edición de documento aprobada"
                : "Solicitud de edición de documento rechazada";

        String documentName = event.get(ModalityEvent.KEY_DOCUMENT_NAME, String.class);
        String resolutionNotes = event.get(ModalityEvent.KEY_RESOLUTION_NOTES, String.class);

        for (StudentModalityMember member : members) {
            User student = member.getStudent();
            String message;
            String closing = NotificationMessageTemplates.closingSigma() + "\nUniversidad Surcolombiana";
            if (approved) {
                String body = """
        Nos permitimos informarle que la solicitud de edición del documento ha sido aprobada por el jurado evaluador, conforme al proceso de revisión académica.

        A continuación, se relaciona la información correspondiente:

        Documento: "%s".
        Observaciones del jurado: %s.

        En virtud de esta decisión, podrá ingresar a la plataforma institucional y realizar la carga de la versión actualizada del documento. Una vez registrada, la nueva versión será objeto de evaluación por parte del jurado designado.

        Este mensaje constituye una notificación automática generada como constancia de la decisión adoptada y para efectos de control y trazabilidad institucional.
        """.formatted(
                        documentName,
                        resolutionNotes != null && !resolutionNotes.isBlank()
                                ? resolutionNotes
                                : "No se registran observaciones adicionales."
                );
                message = NotificationMessageTemplates.greeting(student.getName()) + body + closing;
            } else {
                String body = """
        Nos permitimos informarle que la solicitud de edición del documento no ha sido aprobada por el jurado evaluador, conforme al proceso de revisión académica.

        A continuación, se relaciona la información correspondiente:

        Documento: "%s".
        Motivo de la decisión: %s.

        En consecuencia, el documento conserva su estado actual dentro del proceso académico. En caso de requerir aclaraciones adicionales, podrá comunicarse con la Jefatura de Programa o con el Director de Proyecto.

        Este mensaje constituye una notificación automática generada como constancia de la decisión adoptada y para efectos de control y trazabilidad institucional.
        """.formatted(
                        documentName,
                        resolutionNotes != null && !resolutionNotes.isBlank()
                                ? resolutionNotes
                                : "No se registran motivos adicionales."
                );
                message = NotificationMessageTemplates.greeting(student.getName()) + body + closing;
            }

            notificationFactory.buildAndDispatch(type, NotificationRecipientType.STUDENT,
                    student, modality, subject, message
            );

        }
    }


    private boolean isCompleteModality(StudentModality modality) {
        boolean hasDefenseDate = modality.getDefenseDate() != null;
        boolean hasExaminers = modality.getDefenseExaminers() != null && !modality.getDefenseExaminers().isEmpty();
        boolean hasDirector = modality.getProjectDirector() != null;
        return hasDefenseDate || hasExaminers || hasDirector;
    }

}

