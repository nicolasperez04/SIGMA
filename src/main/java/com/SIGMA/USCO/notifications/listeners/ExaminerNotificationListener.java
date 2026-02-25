package com.SIGMA.USCO.notifications.listeners;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.notifications.entity.Notification;
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.service.NotificationDispatcherService;
import com.SIGMA.USCO.notifications.repository.NotificationRepository;
import com.SIGMA.USCO.Modalities.Entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.Repository.DefenseExaminerRepository;
import com.SIGMA.USCO.notifications.event.DefenseReadyByDirectorEvent;
import com.SIGMA.USCO.notifications.event.DefenseScheduledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;

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

    public void notifyExaminersAssignment(Long studentModalityId) {
        StudentModality modality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        List<DefenseExaminer> examiners = defenseExaminerRepository.findByStudentModalityId(studentModalityId);

        // Obtener todos los estudiantes asociados a la modalidad desde los miembros
        List<String> studentsList = modality.getMembers() != null ?
            modality.getMembers().stream()
                .map(member -> {
                    User student = member.getStudent();
                    return student.getName() + " " + student.getLastName();
                })
                .toList() : List.of();
        String studentsString = studentsList.isEmpty() ? "-" : String.join(", ", studentsList);

        for (DefenseExaminer examinerAssignment : examiners) {
            User examiner = examinerAssignment.getExaminer();
            String subject = "Asignación como Juez en Modalidad de Grado";
            String message = String.format("""
                Estimado/a %s %s,

                Le informamos que ha sido asignado como jurado en la modalidad de grado:

                Modalidad: %s
                Programa académico: %s
                Estudiante(s): %s
                Director de proyecto: %s %s
                Fecha de asignación: %s

                Por favor, recuerde revisar y aprobar los documentos correspondientes a la modalidad en el sistema SIGMA.

                Cordialmente,
                Sistema de Gestión Académica
                """,
                examiner.getName(),
                examiner.getLastName(),
                examinerAssignment.getExaminerType().name().replace('_', ' '),
                modality.getProgramDegreeModality().getDegreeModality().getName(),
                modality.getAcademicProgram().getName(),
                studentsString,
                modality.getProjectDirector() != null ? modality.getProjectDirector().getName() : "-",
                modality.getProjectDirector() != null ? modality.getProjectDirector().getLastName() : "-"
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

        String subject = "Notificación de modalidad lista para sustentación";

        String message = """
            Estimado/a %s %s,
            
            Reciba un cordial saludo.
            
            Le informamos que la siguiente modalidad de grado ha sido marcada oficialmente como lista para sustentación por parte del Director de Proyecto:
            
            Estudiantes:
            %s
            
            Modalidad de grado:
            "%s"
            
            A partir de este momento, el proceso se encuentra disponible para su revisión en calidad de jurado evaluador.
            
            Le solicitamos ingresar a la plataforma institucional para:
            - Revisar la documentación final presentada.
            - Verificar el cumplimiento de los requisitos académicos.
            - Continuar con las etapas correspondientes al proceso de sustentación.
            
            Agradecemos su disposición y compromiso con el proceso evaluativo.
            
            Cordialmente,
            Sistema de Gestión Académica
            """.formatted(
                examiner.getName(),
                examiner.getLastName(),
                miembros,
                modality.getProgramDegreeModality().getDegreeModality().getName()
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

        String subject = "Aprobación final de documentos – Puede programar la sustentación";

        String message = """
            Estimado/a %s %s,
            
            Reciba un cordial saludo.
            
            Le informamos que el jurado evaluador ha aprobado la totalidad de los documentos requeridos para la siguiente modalidad de grado:
            
            Estudiantes:
            %s
            
            Modalidad de grado:
            "%s"
            
            Con esta aprobación, el proceso académico cumple los requisitos necesarios para avanzar a la etapa de sustentación.
            
            En su calidad de Director/a de Proyecto, ahora puede:
            - Programar la fecha y hora de la sustentación.
            - Definir el lugar correspondiente.
            - Continuar con la gestión formal del cierre del proceso.
            
            Le invitamos a ingresar al sistema para realizar la programación y dar continuidad al procedimiento institucional.
            
            Cordialmente,
            Sistema de Gestión Académica
            """.formatted(
                director.getName(),
                director.getLastName(),
                miembros,
                modality.getProgramDegreeModality().getDegreeModality().getName()
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

        List<DefenseExaminer> examiners = defenseExaminerRepository.findByStudentModalityId(event.getStudentModalityId());
        for (DefenseExaminer examinerAssignment : examiners) {
            User examiner = examinerAssignment.getExaminer();
            String subject = "Sustentación programada – Modalidad de Grado";
            String message = String.format(
                """
                         Estimado/a %s %s,
                        
                                Reciba un cordial saludo.
                        
                                Le informamos que ha sido programada la sustentación de la siguiente modalidad de grado:
                        
                                Modalidad:
                                "%s"
                        
                                Fecha y hora:
                                %s
                        
                                Lugar:
                                %s
                        
                                Director/a asignado/a:
                                %s
                        
                                Estudiantes asociados:
                                %s
                        
                                En su calidad de jurado evaluador, le solicitamos ingresar al sistema SIGMA para revisar la documentación final, verificar los lineamientos académicos y prepararse para la jornada de sustentación.
                        
                                Agradecemos su compromiso con el proceso evaluativo.
                        
                                Cordialmente,
                                Sistema de Gestión Académica
                """,
                examiner.getName(),
                examiner.getLastName(),
                modality.getProgramDegreeModality().getDegreeModality().getName(),
                event.getDefenseDate(),
                event.getDefenseLocation(),
                modality.getProjectDirector() != null ? modality.getProjectDirector().getName() + " " + modality.getProjectDirector().getLastName() : "No asignado",
                modality.getMembers() != null && !modality.getMembers().isEmpty() ? modality.getMembers().stream().map(member -> member.getStudent().getName() + " " + member.getStudent().getLastName()).reduce((a, b) -> a + ", " + b).orElse("") : modality.getLeader().getName() + " " + modality.getLeader().getLastName()
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
                         Estimado/a %s,
                        
                                Reciba un cordial saludo.
                        
                                Le informamos que la sustentación de su modalidad de grado ha sido programada con los siguientes detalles:
                        
                                Modalidad:
                                "%s"
                        
                                Fecha y hora:
                                %s
                        
                                Lugar:
                                %s
                        
                                Director/a asignado/a:
                                %s
                        
                                Le recomendamos presentarse con la debida antelación y cumplir con los lineamientos académicos establecidos para la sustentación.
                        
                                Puede consultar la información completa en el sistema SIGMA.
                        
                                Cordialmente,
                                Sistema de Gestión Académica
                """,
                student.getName(),
                modality.getProgramDegreeModality().getDegreeModality().getName(),
                event.getDefenseDate(),
                event.getDefenseLocation(),
                modality.getProjectDirector() != null ? modality.getProjectDirector().getName() + " " + modality.getProjectDirector().getLastName() : "No asignado"
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
}
