package com.SIGMA.USCO.notifications.listeners;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.notifications.entity.Notification;
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.event.DefenseReadyByDirectorEvent;
import com.SIGMA.USCO.notifications.event.DefenseScheduledEvent;
import com.SIGMA.USCO.notifications.service.NotificationDispatcherService;
import com.SIGMA.USCO.notifications.repository.NotificationRepository;
import com.SIGMA.USCO.Modalities.Entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.Repository.DefenseExaminerRepository;
import com.SIGMA.USCO.notifications.event.DocumentEditRequestedEvent;
import com.SIGMA.USCO.notifications.event.DocumentEditResolvedEvent;
import com.SIGMA.USCO.notifications.event.DefenseReadyByDirectorEvent;
import com.SIGMA.USCO.notifications.event.DefenseScheduledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.notifications.event.FinalDefenseResultEvent;
import com.SIGMA.USCO.notifications.service.ExaminerCertificatePdfService;
import com.SIGMA.USCO.Modalities.Entity.ExaminerCertificate;
import com.SIGMA.USCO.Modalities.Entity.enums.CertificateStatus;
import com.SIGMA.USCO.Modalities.Repository.ExaminerCertificateRepository;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExaminerNotificationListener {

    private final DefenseExaminerRepository defenseExaminerRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDispatcherService dispatcher;
    private final UserRepository userRepository;
    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final ExaminerCertificatePdfService examinerCertificatePdfService;
    private final ExaminerCertificateRepository examinerCertificateRepository;

    @Async("notificationTaskExecutor")
    public void notifyExaminersAssignment(Long studentModalityId) {
        StudentModality modality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        List<DefenseExaminer> examiners = defenseExaminerRepository.findByStudentModalityId(studentModalityId);

        // Obtener todos los miembros ACTIVOS de la modalidad
        List<StudentModalityMember> activeMembers = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(studentModalityId, MemberStatus.ACTIVE);

        String studentsString = activeMembers.isEmpty()
                ? (modality.getLeader() != null
                        ? modality.getLeader().getName() + " " + modality.getLeader().getLastName()
                                + " (" + modality.getLeader().getEmail() + ")"
                        : "-")
                : activeMembers.stream()
                        .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName()
                                + " (" + m.getStudent().getEmail() + ")")
                        .collect(Collectors.joining("\n                        "));

        String directorName = modality.getProjectDirector() != null
                ? modality.getProjectDirector().getName() + " " + modality.getProjectDirector().getLastName()
                : "No asignado";

        String programName = modality.getProgramDegreeModality().getAcademicProgram().getName();
        String facultyName = modality.getProgramDegreeModality().getAcademicProgram().getFaculty() != null
                ? modality.getProgramDegreeModality().getAcademicProgram().getFaculty().getName()
                : "";

        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }

        for (DefenseExaminer examinerAssignment : examiners) {
            User examiner = examinerAssignment.getExaminer();

            String examinerRoleLabel = switch (examinerAssignment.getExaminerType()) {
                case PRIMARY_EXAMINER_1, PRIMARY_EXAMINER_2 -> "Jurado Principal";
                case TIEBREAKER_EXAMINER -> "Jurado de Desempate";
            };

            String subject = "Designación oficial como Jurado Evaluador – Modalidad de Grado";

            String message = """
        Estimado(a) %s %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que ha sido designado(a) oficialmente como %s en el proceso de evaluación de la modalidad de grado, conforme a las disposiciones académicas vigentes.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".
        Programa académico: %s.
        Facultad: %s.
        Estudiantes asociados: %s.
        Director de proyecto: %s.
        Fecha de asignación: %s.

        En el marco de esta designación, le corresponde realizar la evaluación académica de la modalidad de grado, conforme a los lineamientos institucionales establecidos y dentro de los plazos definidos por el programa académico.

        Podrá consultar la información completa de la modalidad y gestionar las actividades asociadas a su rol a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada como constancia de la designación realizada y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                    examiner.getName(),
                    examiner.getLastName(),
                    examinerRoleLabel,
                    modalidadInfo,
                    programName,
                    facultyName,
                    studentsString,
                    directorName,
                    LocalDateTime.now().toLocalDate().toString()
            );

            Notification notification = Notification.builder()
                    .type(NotificationType.EXAMINER_ASSIGNED)
                    .recipientType(NotificationRecipientType.EXAMINER)
                    .recipient(examiner)
                    .triggeredBy(null)
                    .studentModality(modality)
                    .subject(subject)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
        }

        // ── Construir resumen de jurados asignados para el mensaje de estudiantes y director ──
        String examinersListForOthers = examiners.stream()
                .map(e -> {
                    String roleLabel = switch (e.getExaminerType()) {
                        case PRIMARY_EXAMINER_1, PRIMARY_EXAMINER_2 -> "Jurado Principal";
                        case TIEBREAKER_EXAMINER -> "Jurado de Desempate";
                    };
                    return "- " + e.getExaminer().getName() + " " + e.getExaminer().getLastName()
                            + " (" + roleLabel + ")";
                })
                .collect(Collectors.joining("\n"));

        // ── Notificar a todos los estudiantes activos de la modalidad ──
        List<User> studentsToNotify = activeMembers.isEmpty()
                ? (modality.getLeader() != null ? List.of(modality.getLeader()) : List.of())
                : activeMembers.stream().map(StudentModalityMember::getStudent).toList();

        for (User student : studentsToNotify) {
            String studentSubject = "Jurados asignados a tu modalidad de grado – SIGMA";
            String studentMessage = """
        Estimado(a) %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que el Comité de Currículo del programa académico ha designado oficialmente los jurados evaluadores para su modalidad de grado, conforme a la normativa institucional vigente.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".
        Programa académico: %s.
        Facultad: %s.
        Director de proyecto: %s.
        Jurados asignados: %s.
        Fecha de asignación: %s.

        En virtud de esta designación, los jurados procederán con la evaluación de la documentación académica asociada a su modalidad de grado, conforme a los lineamientos establecidos.

        Se recomienda verificar que la documentación requerida se encuentre debidamente registrada en la plataforma institucional y mantenerse atento(a) a las comunicaciones emitidas durante el proceso.

        Este mensaje constituye una notificación automática generada como constancia de la asignación realizada y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                    student.getName(),
                    modalidadInfo,
                    programName,
                    facultyName,
                    directorName,
                    examinersListForOthers != null && !examinersListForOthers.isBlank()
                            ? examinersListForOthers
                            : "Pendiente de asignación",
                    LocalDateTime.now().toLocalDate().toString()
            );

            Notification studentNotification = Notification.builder()
                    .type(NotificationType.EXAMINER_ASSIGNED)
                    .recipientType(NotificationRecipientType.STUDENT)
                    .recipient(student)
                    .triggeredBy(null)
                    .studentModality(modality)
                    .subject(studentSubject)
                    .message(studentMessage)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(studentNotification);
            dispatcher.dispatch(studentNotification);
        }

        // ── Notificar al director de proyecto si está asignado ──
        User director = modality.getProjectDirector();
        if (director != null) {
            String directorSubject = "Jurados asignados a modalidad bajo su dirección – SIGMA";
            String directorMessage = """
        Estimado(a) %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que el Comité de Currículo del programa académico ha designado oficialmente los jurados evaluadores para la modalidad de grado bajo su dirección, conforme a la normativa institucional vigente.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".
        Programa académico: %s.
        Facultad: %s.
        Estudiantes asociados: %s.
        Jurados asignados: %s.
        Fecha de asignación: %s.

        En virtud de esta designación, los jurados iniciarán el proceso de revisión y evaluación de la documentación académica asociada a la modalidad de grado, conforme a los lineamientos institucionales establecidos.

        En su calidad de Director de Proyecto, se recomienda realizar el seguimiento académico correspondiente, con el fin de garantizar el cumplimiento de los requisitos y la adecuada atención a las observaciones que se deriven del proceso evaluativo.

        Podrá consultar el detalle completo a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada como constancia de la asignación realizada y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                    director.getName() + " " + director.getLastName(),
                    modalidadInfo,
                    programName,
                    facultyName,
                    studentsString,
                    (examinersListForOthers != null && !examinersListForOthers.isBlank())
                            ? examinersListForOthers
                            : "Pendiente de asignación",
                    LocalDateTime.now().toLocalDate().toString()
            );

            Notification directorNotification = Notification.builder()
                    .type(NotificationType.EXAMINER_ASSIGNED)
                    .recipientType(NotificationRecipientType.PROJECT_DIRECTOR)
                    .recipient(director)
                    .triggeredBy(null)
                    .studentModality(modality)
                    .subject(directorSubject)
                    .message(directorMessage)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(directorNotification);
            dispatcher.dispatch(directorNotification);
        }
    }

    @EventListener
    public void handleDefenseReadyByDirectorEvent(DefenseReadyByDirectorEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User examiner = userRepository.findById(event.getExaminerId())
                .orElseThrow(() -> new RuntimeException("Jurado no encontrado"));


        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
        String miembros = members.stream()
            .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName() + " (" + m.getStudent().getEmail() + ")")
            .collect(Collectors.joining(", "));

        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }

        String subject = "Notificación de modalidad lista para sustentación";

        String message = """
        Estimado(a) %s %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que la modalidad de grado relacionada a continuación ha sido registrada como lista para sustentación por parte del Director de Proyecto, conforme al proceso académico establecido.

        A continuación, se relaciona la información pertinente:

        Estudiantes asociados: %s.
        Modalidad de grado: "%s".

        En virtud de esta actuación, el proceso se encuentra disponible para su revisión en calidad de jurado evaluador, conforme a los lineamientos institucionales vigentes.

        Podrá consultar la documentación final presentada y realizar el proceso de evaluación correspondiente a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada como constancia del estado registrado y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                examiner.getName(),
                examiner.getLastName(),
                miembros,
                modalidadInfo
        );
        Notification notification = Notification.builder()
                .type(NotificationType.READY_FOR_DEFENSE_REQUESTED)
                .recipientType(NotificationRecipientType.EXAMINER)
                .recipient(examiner)
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
    public void handleExaminerFinalReviewCompletedEvent(
            com.SIGMA.USCO.notifications.event.ExaminerFinalReviewCompletedEvent event) {

        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User director = userRepository.findById(event.getProjectDirectorId())
                .orElseThrow(() -> new RuntimeException("Director de proyecto no encontrado"));

        // Obtener todos los miembros ACTIVOS de la modalidad
        List<StudentModalityMember> members = studentModalityMemberRepository.findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
        String miembros = members.stream()
            .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName() + " (" + m.getStudent().getEmail() + ")")
            .collect(Collectors.joining(", "));

        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }

        String subject = "Aprobación final de documentos – Puede programar la sustentación";

        String message = """
        Estimado(a) %s %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que el jurado evaluador ha aprobado la totalidad de los documentos requeridos para la modalidad de grado, conforme al proceso académico establecido.

        A continuación, se relaciona la información pertinente:

        Estudiantes asociados: %s.
        Modalidad de grado: "%s".

        En virtud de esta aprobación, el proceso académico cumple con los requisitos necesarios para avanzar a la etapa de sustentación.

        En su calidad de Director de Proyecto, corresponde continuar con la gestión académica asociada a la programación y desarrollo de la sustentación, conforme a los lineamientos institucionales vigentes.

        Podrá realizar las acciones correspondientes y consultar el detalle del proceso a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada como constancia del estado registrado y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                director.getName(),
                director.getLastName(),
                miembros,
                modalidadInfo
        );

        Notification notification = Notification.builder()
                .type(NotificationType.FINAL_APPROVED)
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
    public void handleDefenseScheduled(DefenseScheduledEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }

        List<DefenseExaminer> examiners = defenseExaminerRepository.findByStudentModalityId(event.getStudentModalityId());
        for (DefenseExaminer examinerAssignment : examiners) {
            User examiner = examinerAssignment.getExaminer();
            String subject = "Sustentación programada – Modalidad de Grado";
            String message = String.format(
                    """
                    Estimado(a) %s %s:
            
                    Reciba un cordial saludo.
            
                    Nos permitimos informarle que ha sido programada la sustentación correspondiente a la modalidad de grado, conforme al proceso académico establecido.
            
                    A continuación, se relaciona la información pertinente:
            
                    Modalidad de grado: "%s".
                    Fecha y hora de la sustentación: %s.
                    Lugar: %s.
                    Director de proyecto: %s.
                    Estudiantes asociados: %s.
            
                    En virtud de esta programación, la sustentación se desarrollará conforme a los lineamientos institucionales vigentes, en el marco del proceso de evaluación académica.
            
                    Podrá consultar la documentación final y el detalle del proceso a través de la plataforma institucional.
            
                    Este mensaje constituye una notificación automática generada como constancia de la programación registrada y para efectos de control y trazabilidad institucional.
            
                    Atentamente,
            
                    Sistema de Gestión Académica
                    Universidad Surcolombiana
                    """,
                    examiner.getName(),
                    examiner.getLastName(),
                    modalidadInfo,
                    event.getDefenseDate(),
                    event.getDefenseLocation(),
                    modality.getProjectDirector() != null
                            ? modality.getProjectDirector().getName() + " " + modality.getProjectDirector().getLastName()
                            : "Pendiente de asignación",
                    modality.getMembers() != null && !modality.getMembers().isEmpty()
                            ? modality.getMembers().stream()
                            .map(member -> member.getStudent().getName() + " " + member.getStudent().getLastName())
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("")
                            : modality.getLeader().getName() + " " + modality.getLeader().getLastName()
            );

            Notification notification = Notification.builder()
                    .type(NotificationType.DEFENSE_SCHEDULED)
                    .recipientType(NotificationRecipientType.EXAMINER)
                    .recipient(examiner)
                    .triggeredBy(null)
                    .studentModality(modality)
                    .subject(subject)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
            dispatcher.dispatch(notification);
        }

        // Notificar a todos los estudiantes asociados
        List<User> students;
        if (modality.getMembers() != null && !modality.getMembers().isEmpty()) {
            students = modality.getMembers().stream().map(member -> member.getStudent()).toList();
        } else {
            students = List.of(modality.getLeader());
        }
        for (User student : students) {
            String subject = "Sustentación programada – Modalidad de Grado";
            String message = String.format(
                    """
                    Estimado(a) %s:
            
                    Reciba un cordial saludo.
            
                    Nos permitimos informarle que la sustentación correspondiente a su modalidad de grado ha sido programada, conforme al proceso académico establecido.
            
                    A continuación, se relaciona la información pertinente:
            
                    Modalidad de grado: "%s".
                    Fecha y hora de la sustentación: %s.
                    Lugar: %s.
                    Director de proyecto: %s.
            
                    La sustentación se desarrollará conforme a los lineamientos institucionales vigentes.
            
                    Podrá consultar el detalle completo del proceso a través de la plataforma institucional.
            
                    Este mensaje constituye una notificación automática generada como constancia de la programación registrada y para efectos de control y trazabilidad institucional.
            
                    Atentamente,
            
                    Sistema de Gestión Académica
                    Universidad Surcolombiana
                    """,
                    student.getName(),
                    modalidadInfo,
                    event.getDefenseDate(),
                    event.getDefenseLocation(),
                    modality.getProjectDirector() != null
                            ? modality.getProjectDirector().getName() + " " + modality.getProjectDirector().getLastName()
                            : "Pendiente de asignación"
            );

            Notification notification = Notification.builder()
                    .type(NotificationType.DEFENSE_SCHEDULED)
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

    /**
     * Notifica a los jurados asignados a la modalidad cuando un estudiante solicita
     * editar un documento previamente aprobado.
     */
    @EventListener
    public void onDocumentEditRequested(DocumentEditRequestedEvent event) {
        StudentModality modality = studentModalityRepository.findById(event.getStudentModalityId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        List<DefenseExaminer> examiners = defenseExaminerRepository.findByStudentModalityId(event.getStudentModalityId());

        String studentNames = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE)
                .stream()
                .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName())
                .collect(Collectors.joining(", "));

        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }

        String subject = "Solicitud de edición de documento aprobado – Modalidad de grado";

        for (DefenseExaminer examinerAssignment : examiners) {
            User examiner = examinerAssignment.getExaminer();
            String message = """
        Estimado(a) %s %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que los estudiantes asociados a la modalidad de grado han registrado una solicitud de edición sobre un documento previamente aprobado, conforme al procedimiento académico establecido.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".
        Programa académico: "%s".
        Estudiantes asociados: %s.
        Documento: "%s".
        Identificador de la solicitud: %d.
        Motivo de la solicitud: %s.

        En virtud de esta solicitud, el caso se encuentra disponible para su revisión en calidad de jurado evaluador, conforme a los lineamientos institucionales vigentes.

        Podrá consultar el detalle de la solicitud y emitir el concepto correspondiente a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada como constancia del registro efectuado y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                    examiner.getName(),
                    examiner.getLastName(),
                    modalidadInfo,
                    modality.getProgramDegreeModality().getAcademicProgram().getName(),
                    (studentNames != null && !studentNames.isBlank()) ? studentNames : "No registrado",
                    event.getDocumentName(),
                    event.getEditRequestId(),
                    event.getReason() != null && !event.getReason().isBlank()
                            ? event.getReason()
                            : "No se registra motivo"
            );

            Notification notification = Notification.builder()
                    .type(NotificationType.DOCUMENT_EDIT_REQUESTED)
                    .recipientType(NotificationRecipientType.EXAMINER)
                    .recipient(examiner)
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

    /**
     * Genera y envía actas de participación a todos los jurados
     * cuando la sustentación es aprobada y completada.
     */
    @EventListener
    @Transactional
    public void onFinalDefenseApproved(FinalDefenseResultEvent event) {
        Long modalityId = event.getStudentModalityId();
        StudentModality modality = studentModalityRepository.findById(modalityId)
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));
        
        // Solo procesar si fue aprobada
        if (event.getFinalStatus() == null || 
            !event.getFinalStatus().name().contains("APPROVED")) {
            log.debug("Evento de defensa no es aprobatorio, se omite generación de actas para jurados");
            return;
        }

        log.info("Generando actas de participación para jurados de modalidad ID: {}", modalityId);

        // Obtener todos los jurados asignados
        List<DefenseExaminer> examiners = modality.getDefenseExaminers();
        if (examiners == null || examiners.isEmpty()) {
            log.warn("No hay jurados asignados para la modalidad ID: {}", modalityId);
            return;
        }

        // Generar y enviar acta a cada jurado
        for (DefenseExaminer examiner : examiners) {
            try {
                log.info("Generando acta para jurado {} en modalidad ID: {}", 
                    examiner.getExaminer().getId(), modalityId);

                // Generar el PDF del acta
                ExaminerCertificate certificate = examinerCertificatePdfService.generateExaminerCertificate(
                    modality, examiner
                );
                
                Path pdfPath = examinerCertificatePdfService.getCertificatePath(
                    modalityId, 
                    examiner.getExaminer().getId()
                );

                // Crear notificación para el jurado
                User examinerUser = examiner.getExaminer();
                String subject = "Acta de Participación – Modalidad de Grado Completada";
                
                String message = buildExaminerParticipationMessage(examinerUser, modality, examiner);
                
                Notification notification = Notification.builder()
                        .type(NotificationType.DEFENSE_COMPLETED)
                        .recipientType(NotificationRecipientType.EXAMINER)
                        .recipient(examinerUser)
                        .triggeredBy(null)
                        .studentModality(modality)
                        .subject(subject)
                        .message(message)
                        .createdAt(LocalDateTime.now())
                        .build();

                notificationRepository.save(notification);

                // Enviar notificación con el acta adjunta
                try {
                    dispatcher.dispatchWithAttachment(
                        notification,
                        pdfPath,
                        "ACTA_JURADO_" + certificate.getCertificateNumber() + ".pdf"
                    );
                    
                    // Actualizar estado del certificado
                    examinerCertificatePdfService.updateCertificateStatus(
                        certificate.getId(), 
                        CertificateStatus.SENT
                    );
                    
                    log.info("Acta enviada al jurado {} (modalidad ID {})", 
                        examinerUser.getId(), modalityId);
                } catch (Exception e) {
                    log.error("Error enviando acta al jurado {}: {}", 
                        examinerUser.getId(), e.getMessage());
                    // Intentar enviar sin adjunto como fallback
                    dispatcher.dispatch(notification);
                }

            } catch (Exception e) {
                log.error("Error generando acta para jurado {} en modalidad ID {}: {}", 
                    examiner.getExaminer().getId(), modalityId, e.getMessage(), e);
            }
        }

        log.info("Proceso de generación de actas para jurados completado para modalidad ID: {}", 
            modalityId);
    }

    /**
     * Construye el mensaje para el jurado sobre su participación
     */
    private String buildExaminerParticipationMessage(User examiner, StudentModality modality, DefenseExaminer defenseExaminer) {
        String examinerRole = switch (defenseExaminer.getExaminerType()) {
            case PRIMARY_EXAMINER_1, PRIMARY_EXAMINER_2 -> "Jurado Principal";
            case TIEBREAKER_EXAMINER -> "Jurado de Desempate";
        };
        
        String degreeModalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        String projectTitle = modality.getModalityTitle();
        String modalidadInfo = degreeModalityName;
        if (projectTitle != null && !projectTitle.isBlank()) {
            modalidadInfo += " – " + projectTitle;
        }
        
        List<StudentModalityMember> members = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
        String studentNames;
        if (!members.isEmpty()) {
            studentNames = members.stream()
                    .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName())
                    .collect(Collectors.joining(", "));
        } else {
            studentNames = modality.getLeader() != null 
                    ? modality.getLeader().getName() + " " + modality.getLeader().getLastName()
                    : "No registrado";
        }

        return """
        Estimado(a) %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que la sustentación correspondiente a la modalidad de grado en la cual usted participó en calidad de %s ha finalizado y su resultado ha sido registrado oficialmente en el sistema.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".
        Programa académico: %s.
        Facultad: %s.
        Resultado: APROBADA.
        Estudiantes asociados: %s.

        En el marco de este proceso, su participación como %s quedó registrada en las diferentes etapas de evaluación académica, conforme a los lineamientos institucionales vigentes.

        Se adjunta a la presente comunicación el acta de participación en formato PDF, documento oficial que certifica su intervención en el proceso evaluativo y que forma parte del registro institucional de control y trazabilidad académica.

        Este mensaje constituye una notificación automática generada como constancia del cierre del proceso.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                examiner.getName(),
                examinerRole,
                modalidadInfo,
                modality.getProgramDegreeModality().getAcademicProgram().getName(),
                modality.getProgramDegreeModality().getAcademicProgram().getFaculty().getName(),
                studentNames,
                examinerRole
        );
    }

}
