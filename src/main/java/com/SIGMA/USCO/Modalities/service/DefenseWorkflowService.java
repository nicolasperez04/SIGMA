package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.entity.enums.ExaminerType;
import com.SIGMA.USCO.Modalities.entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.repository.DefenseExaminerRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Modalities.dto.ModalityListDTO;
import com.SIGMA.USCO.Modalities.dto.ScheduleDefenseRequest;
import com.SIGMA.USCO.Modalities.dto.response.DefenseScheduleResponse;
import com.SIGMA.USCO.Modalities.dto.response.DefenseWorkflowResponse;
import com.SIGMA.USCO.Modalities.dto.response.ExaminerAssignmentResponse;
import com.SIGMA.USCO.Modalities.dto.response.ExaminerTypeResponse;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.documents.entity.RequiredDocument;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import com.SIGMA.USCO.documents.repository.RequiredDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.Modalities.event.ModalityEvent;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.shared.util.TranslationUtils;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import com.SIGMA.USCO.common.exception.ForbiddenException;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.common.security.Roles;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefenseWorkflowService {

    private final DefenseExaminerRepository defenseExaminerRepository;
    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;
    private final RequiredDocumentRepository requiredDocumentRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ModalityStatusTransition modalityStatusTransition;

    @Transactional
    public DefenseScheduleResponse scheduleDefense(Long studentModalityId, ScheduleDefenseRequest request, User projectDirector) {
        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));
        if (studentModality.getProjectDirector() == null ||
                !studentModality.getProjectDirector().getId().equals(projectDirector.getId())) {
            throw new ForbiddenException("No tiene permiso para proponer sustentación. No es el director asignado a esta modalidad");
        }
        if (studentModality.getStatus() != ModalityProcessStatus.FINAL_REVIEW_COMPLETED) {
            throw new ValidationException("La modalidad no se encuentra en estado válido para proponer sustentación");
        }
        if (request.getDefenseDate() == null ||
                request.getDefenseLocation() == null ||
                request.getDefenseLocation().isBlank()) {
            throw new ValidationException("Debe ingresar fecha y lugar válidos para la sustentación propuesta");
        }
        if (request.getDefenseDate().isBefore(LocalDateTime.now())) {
            throw new ValidationException("La fecha de defensa debe ser en el futuro");
        }
        studentModality.setDefenseDate(request.getDefenseDate());
        studentModality.setDefenseLocation(request.getDefenseLocation());
        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.DEFENSE_SCHEDULED, projectDirector,
                "Director de proyecto programó la sustentación para el "
                        + request.getDefenseDate()
                        + " en "
                        + request.getDefenseLocation());
        // Notificar a estudiantes, jurados y jefes de programa. Los listeners hacen fan-out
        // a todos sus destinatarios, por lo que se publica un único evento (evita N×N).
        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.DEFENSE_SCHEDULED, studentModality.getId(), projectDirector.getId(), Map.of(
                        ModalityEvent.KEY_DEFENSE_DATE, request.getDefenseDate(),
                        ModalityEvent.KEY_DEFENSE_LOCATION, request.getDefenseLocation()
                ))
        );
        return new DefenseScheduleResponse(
                true,
                studentModalityId,
                request.getDefenseDate(),
                request.getDefenseLocation(),
                ModalityProcessStatus.DEFENSE_SCHEDULED,
                "Sustentación programada correctamente por el director de proyecto"
        );
    }

    @Transactional
    public ExaminerAssignmentResponse assignExaminers(Long studentModalityId, ScheduleDefenseRequest request, User committeeMember) {

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        Long academicProgramId = studentModality
                .getProgramDegreeModality()
                .getAcademicProgram()
                .getId();

        boolean authorized = programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                committeeMember.getId(),
                academicProgramId,
                ProgramRole.PROGRAM_CURRICULUM_COMMITTEE
        );

        if (!authorized) {
            throw new ForbiddenException("No tiene permiso para asignar jurados en este programa académico");
        }

        if (studentModality.getStatus() != ModalityProcessStatus.READY_FOR_EXAMINERS) {
            throw new ValidationException("La modalidad debe estar en estado 'Listo para jurados' para asignar jurados"); 
        }

        if (request.getPrimaryExaminer1Id() == null ||
                request.getPrimaryExaminer2Id() == null ||
                request.getTiebreakerExaminerId() == null) {
            throw new ValidationException("Debe asignar los dos jurados principales y el jurado de desempate");
        }

        List<Long> examinerIds = new ArrayList<>();
        if (request.getPrimaryExaminer1Id() != null) examinerIds.add(request.getPrimaryExaminer1Id());
        if (request.getPrimaryExaminer2Id() != null) examinerIds.add(request.getPrimaryExaminer2Id());
        if (request.getTiebreakerExaminerId() != null) examinerIds.add(request.getTiebreakerExaminerId());

        Set<Long> uniqueIds = new HashSet<>(examinerIds);
        if (uniqueIds.size() != examinerIds.size()) {
            throw new ValidationException("No se pueden asignar el mismo jurado más de una vez");
        }

        List<String> examinerAssignmentMessages = new ArrayList<>();

        assignExaminer(studentModality, request.getPrimaryExaminer1Id(), ExaminerType.PRIMARY_EXAMINER_1,
                "Jurado principal 1", committeeMember, examinerAssignmentMessages);

        assignExaminer(studentModality, request.getPrimaryExaminer2Id(), ExaminerType.PRIMARY_EXAMINER_2,
                "Jurado principal 2", committeeMember, examinerAssignmentMessages);

        assignExaminer(studentModality, request.getTiebreakerExaminerId(), ExaminerType.TIEBREAKER_EXAMINER,
                "Jurado de desempate", committeeMember, examinerAssignmentMessages);

        String observationMessage = "Jurados asignados por el comité de currículo:\n" +
                String.join("\n", examinerAssignmentMessages);

        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.EXAMINERS_ASSIGNED, committeeMember, observationMessage);

        // Notificar a los jurados asignados, estudiantes y director
        applicationEventPublisher.publishEvent(new ModalityEvent(NotificationType.EXAMINER_ASSIGNED, studentModalityId, committeeMember.getId(), Map.of()));

        return new ExaminerAssignmentResponse(
                true,
                studentModalityId,
                ModalityProcessStatus.EXAMINERS_ASSIGNED,
                examinerAssignmentMessages,
                "Jurados asignados correctamente a la sustentación"
        );
    }

    private void assignExaminer(StudentModality studentModality, Long examinerId, ExaminerType type, String label,
                                User committeeMember, List<String> examinerAssignmentMessages) {
        User examiner = userRepository.findById(examinerId)
                .orElseThrow(() -> new NotFoundException(label + " no encontrado"));

        boolean hasExaminerRole = examiner.getRoles().stream()
                .anyMatch(role -> role.getName().equals(Roles.ROLE_EXAMINER));

        if (!hasExaminerRole) {
            throw new ValidationException("El usuario seleccionado como " + label.toLowerCase(Locale.ROOT) + " no tiene el rol EXAMINER");
        }

        if (studentModality.getProjectDirector() != null &&
                studentModality.getProjectDirector().getId().equals(examiner.getId())) {
            throw new ValidationException("El director del proyecto no puede ser jurado de la misma modalidad");
        }

        if (!programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                examiner.getId(),
                studentModality.getAcademicProgram().getId(),
                ProgramRole.EXAMINER)) {
            throw new ValidationException("El examinador no pertenece al programa de la modalidad");
        }

        defenseExaminerRepository
                .findByStudentModalityIdAndExaminerType(studentModality.getId(), type)
                .ifPresent(defenseExaminerRepository::delete);

        DefenseExaminer defenseExaminer = DefenseExaminer.builder()
                .studentModality(studentModality)
                .examiner(examiner)
                .examinerType(type)
                .assignmentDate(LocalDateTime.now())
                .assignedBy(committeeMember)
                .build();

        defenseExaminerRepository.save(defenseExaminer);
        examinerAssignmentMessages.add(TranslationUtils.translateExaminerType(defenseExaminer.getExaminerType()) + ": " + examiner.getName() + " " + examiner.getLastName());
    }

    @Transactional
    public DefenseWorkflowResponse modalityReadyForDefenseByDirector(Long studentModalityId, User projectDirector) {

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        // Validación de documentos subidos (excepto para "Emprendimiento y fortalecimiento de empresa")
        String modalidadNombre = studentModality.getProgramDegreeModality().getDegreeModality().getName();
        if (!modalidadNombre.equalsIgnoreCase(ModalityServiceUtils.ENTREPRENEURSHIP_MODALITY_NAME)) {
            Long degreeModalityId = studentModality.getProgramDegreeModality().getDegreeModality().getId();
            List<RequiredDocument> mandatoryDocs = requiredDocumentRepository.findByModalityIdAndActiveTrueAndDocumentType(degreeModalityId, DocumentType.MANDATORY);
            List<RequiredDocument> secondaryDocs = requiredDocumentRepository.findByModalityIdAndActiveTrueAndDocumentType(degreeModalityId, DocumentType.SECONDARY);
            List<RequiredDocument> requiredDocs = new ArrayList<>();
            requiredDocs.addAll(mandatoryDocs);
            requiredDocs.addAll(secondaryDocs);
            List<StudentDocument> uploadedDocs = studentDocumentRepository.findByStudentModalityId(studentModalityId);
            for (RequiredDocument reqDoc : requiredDocs) {
                StudentDocument doc = uploadedDocs.stream()
                    .filter(d -> d.getDocumentConfig().getId().equals(reqDoc.getId()))
                    .findFirst()
                    .orElse(null);
                // Si no existe documento o está vacío (por ejemplo, fileName es null o vacío)
                if (doc == null || doc.getFileName() == null || doc.getFileName().isBlank()) {
                    throw new ValidationException("El estudiante debe subir todos los documentos para marcar la modalidad como lista para defensa");
                }
            }
        }

        if (studentModality.getProjectDirector() == null ||
                !studentModality.getProjectDirector().getId().equals(projectDirector.getId())) {
            throw new ForbiddenException("No tiene permiso para marcar la modalidad como lista para defensa. No es el director asignado a esta modalidad");
        }

        // Validar estado actual
        if (studentModality.getStatus() != ModalityProcessStatus.PROPOSAL_APPROVED &&
            studentModality.getStatus() != ModalityProcessStatus.DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR) {
            throw new ValidationException("La modalidad no se encuentra en estado válido para notificar a jefatura de programa");
        }

        // Cambiar estado al paso intermedio: jefatura debe revisar antes de notificar a jurados
        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.PENDING_PROGRAM_HEAD_FINAL_REVIEW, projectDirector,
                "Director de proyecto notificó a jefatura que los documentos finales están listos para revisión previa a la sustentación");

        // Notificar a jefatura de programa (no a jurados - eso lo hará jefatura en el paso siguiente)
        applicationEventPublisher.publishEvent(
            new ModalityEvent(NotificationType.DIRECTOR_NOTIFIES_PROGRAM_HEAD_FINAL_REVIEW, studentModality.getId(), projectDirector.getId(), Map.of())
        );

        return new DefenseWorkflowResponse(
                true,
                studentModalityId,
                ModalityProcessStatus.PENDING_PROGRAM_HEAD_FINAL_REVIEW,
                "Jefatura de programa ha sido notificada para revisar los documentos finales. Una vez aprobados, jefatura notificará a los jurados."
        );
    }

    /**
     * Método para que jefatura de programa apruebe los documentos finales y notifique a los jurados.
     * Paso intermedio entre la notificación del director y la revisión de jurados.
     */
    @Transactional
    public DefenseWorkflowResponse programHeadApprovesAndNotifiesExaminers(Long studentModalityId, User programHead) {

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        // Validar que sea jefatura de programa
        Long academicProgramId = studentModality.getAcademicProgram().getId();
        boolean isProgramHead = programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                programHead.getId(), academicProgramId, ProgramRole.PROGRAM_HEAD);
        if (!isProgramHead) {
            throw new ForbiddenException("Solo jefatura de programa puede aprobar y notificar a los jurados en este paso");
        }

        // Validar estado actual
        if (studentModality.getStatus() != ModalityProcessStatus.PENDING_PROGRAM_HEAD_FINAL_REVIEW) {
            throw new ValidationException("La modalidad no está en espera de revisión de jefatura de programa");
        }

        // Validar que TODOS los documentos SECONDARY estén aprobados por jefatura o en estado superior
        // EXCEPCIÓN: Para la modalidad "Emprendimiento y fortalecimiento de empresa", 
        // se permite avanzar sin validar que los documentos SECONDARY estén subidos
        String modalityName = studentModality.getProgramDegreeModality().getDegreeModality().getName();
        boolean isEmprendimientoModality = modalityName != null && 
                modalityName.equalsIgnoreCase(ModalityServiceUtils.ENTREPRENEURSHIP_MODALITY_NAME);

        List<Map<String, Object>> invalidDocuments = new ArrayList<>();

        // Solo validar documentos SECONDARY si NO es la modalidad especial
        if (!isEmprendimientoModality) {
            Long degreeModalityId = studentModality.getProgramDegreeModality().getDegreeModality().getId();
            List<RequiredDocument> secondaryDocs = requiredDocumentRepository
                    .findByModalityIdAndActiveTrueAndDocumentType(degreeModalityId, DocumentType.SECONDARY);
            List<StudentDocument> uploadedDocs = studentDocumentRepository.findByStudentModalityId(studentModalityId);

            for (RequiredDocument reqDoc : secondaryDocs) {
                StudentDocument doc = uploadedDocs.stream()
                        .filter(d -> d.getDocumentConfig().getId().equals(reqDoc.getId()))
                        .findFirst()
                        .orElse(null);

                // Validar que el documento exista
                if (doc == null) {
                    invalidDocuments.add(Map.of(
                            "documentName", reqDoc.getDocumentName(),
                            "status", "NOT_UPLOADED"
                    ));
                    continue;
                }

                DocumentStatus status = doc.getStatus();

                // Estados inválidos: PENDING, REJECTED_FOR_PROGRAM_HEAD_REVIEW, CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD
                if (status == DocumentStatus.PENDING ||
                    status == DocumentStatus.REJECTED_FOR_PROGRAM_HEAD_REVIEW ||
                    status == DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD) {
                    invalidDocuments.add(Map.of(
                            "documentName", reqDoc.getDocumentName(),
                            "currentStatus", status.name(),
                            "reason", "Documento no aprobado por jefatura o requiere correcciones"
                    ));
                }
            }

            // Si hay documentos inválidos, retornar error sin permitir avanzar
            if (!invalidDocuments.isEmpty()) {
                throw new ValidationException("No se puede notificar a los jurados. Existen documentos que no están aprobados por jefatura o requieren correcciones:");
            }
        }

        // Cambiar estado a READY_FOR_DEFENSE (jurados pueden revisar)
        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.READY_FOR_DEFENSE, programHead,
                "Jefatura de programa aprobó todos los documentos y notificó a los jurados para revisión de la sustentación");

        // Notificar a los jurados asignados
        List<DefenseExaminer> examiners = defenseExaminerRepository.findByStudentModalityId(studentModalityId);
        for (DefenseExaminer examinerAssignment : examiners) {
            User examiner = examinerAssignment.getExaminer();
            applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.READY_FOR_DEFENSE_REQUESTED, studentModality.getId(), programHead.getId(), Map.of(
                        ModalityEvent.KEY_EXAMINER_ID, examiner.getId(),
                        ModalityEvent.KEY_OBSERVATIONS, "",
                        ModalityEvent.KEY_REASON, ""
                ))
            );
        }

        // Notificar al director de proyecto y a los estudiantes que la modalidad está lista para sustentación
        applicationEventPublisher.publishEvent(
            new ModalityEvent(NotificationType.MODALITY_READY_FOR_DEFENSE, studentModality.getId(), programHead.getId(), Map.of())
        );

        return new DefenseWorkflowResponse(
                true,
                studentModalityId,
                ModalityProcessStatus.READY_FOR_DEFENSE,
                "Todos los documentos fueron validados. Jurados notificados para revisión de la sustentación."
        );
    }

    @Transactional
    public DefenseWorkflowResponse examinerFinalReviewCompleted(Long studentModalityId, User examiner) {

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        Long academicProgramId = studentModality.getAcademicProgram().getId();

        boolean isAuthorized =
                programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                        examiner.getId(),
                        academicProgramId,
                        ProgramRole.EXAMINER
                );

        if (!isAuthorized) {
            throw new ForbiddenException("No tienes permisos para finalizar la revisión como jurado en este programa académico");
        }

        // Validar que el jurado esté asignado a esta modalidad
        defenseExaminerRepository.findByStudentModalityIdAndExaminerId(studentModalityId, examiner.getId())
                .orElseThrow(() -> new ForbiddenException("No eres jurado asignado a esta modalidad"));

        // Validar estado actual
        if (studentModality.getStatus() != ModalityProcessStatus.READY_FOR_DEFENSE) {
            throw new ValidationException("La modalidad no está en estado válido para finalizar revisión de jurado");
        }

        // Validar que todos los documentos que requieren evaluación de propuesta estén aceptados por el jurado
        // Se validan documentos MANDATORY y SECONDARY que tengan requiresProposalEvaluation = true
        Long modalityId = studentModality.getProgramDegreeModality().getDegreeModality().getId();

        // Obtener documentos MANDATORY que requieren evaluación de propuesta
        List<RequiredDocument> mandatoryDocumentsWithEval =
                requiredDocumentRepository.findByModalityIdAndActiveTrueAndDocumentTypeAndRequiresProposalEvaluationTrue(
                        modalityId, DocumentType.MANDATORY);

        // Obtener documentos SECONDARY que requieren evaluación de propuesta
        List<RequiredDocument> secondaryDocumentsWithEval =
                requiredDocumentRepository.findByModalityIdAndActiveTrueAndDocumentTypeAndRequiresProposalEvaluationTrue(
                        modalityId, DocumentType.SECONDARY);

        // Combinar ambas listas
        List<RequiredDocument> documentsRequiringEvaluation = new ArrayList<>();
        documentsRequiringEvaluation.addAll(mandatoryDocumentsWithEval);
        documentsRequiringEvaluation.addAll(secondaryDocumentsWithEval);

        List<StudentDocument> uploadedDocuments =
                studentDocumentRepository.findByStudentModalityId(studentModalityId);
        Map<Long, StudentDocument> uploadedMap =
                uploadedDocuments.stream()
                        .collect(Collectors.toMap(
                                doc -> doc.getDocumentConfig().getId(),
                                doc -> doc
                        ));

        List<Map<String, Object>> invalidDocuments = new ArrayList<>();
        for (RequiredDocument required : documentsRequiringEvaluation) {
            StudentDocument uploaded = uploadedMap.get(required.getId());
            if (uploaded == null) {
                invalidDocuments.add(
                        Map.of(
                                "documentName", required.getDocumentName(),
                                "status", "NOT_UPLOADED"
                        )
                );
                continue;
            }
            if (uploaded.getStatus() != DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW) {
                invalidDocuments.add(
                        Map.of(
                                "documentName", required.getDocumentName(),
                                "status", uploaded.getStatus()
                        )
                );
            }
        }
        if (!invalidDocuments.isEmpty()) {
            throw new ValidationException("Para finalizar la revisión, todos los documentos que requieren evaluación deben estar aceptados por los jurados");
        }

        // Cambiar estado
        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.FINAL_REVIEW_COMPLETED, examiner,
                "Jurado finalizó la revisión de documentos. Modalidad lista para programación de sustentación.");

        // Notificar al director de proyecto
        User projectDirector = studentModality.getProjectDirector();
        if (projectDirector != null) {
            applicationEventPublisher.publishEvent(
                    new ModalityEvent(NotificationType.EXAMINER_FINAL_REVIEW_COMPLETED, studentModality.getId(), projectDirector.getId(), Map.of(
                            ModalityEvent.KEY_PROJECT_DIRECTOR_ID, projectDirector.getId()
                    ))
            );
        }

        return new DefenseWorkflowResponse(
                true,
                studentModalityId,
                ModalityProcessStatus.FINAL_REVIEW_COMPLETED,
                "Revisión final completada por el jurado. Se notificó al director para programar la sustentación."
        );
    }

    /**
     * Devuelve un calendario de próximas sustentaciones para el jurado autenticado.
     * Solo incluye modalidades en estado DEFENSE_SCHEDULED, ordenadas por fecha de defensa ascendente.
     */
    @Transactional(readOnly = true)
    public List<ModalityListDTO> getExaminerDefenseCalendar(User examiner) {

        // Buscar todas las modalidades asignadas al jurado en estado DEFENSE_SCHEDULED
        List<StudentModality> modalities = studentModalityRepository.findForExaminerWithStatus(
                examiner.getId(),
                List.of(ModalityProcessStatus.DEFENSE_SCHEDULED)
        );

        // Filtrar solo las que tienen fecha de defensa futura o igual a hoy
        LocalDateTime now = LocalDateTime.now();
        List<StudentModality> upcoming = modalities.stream()
                .filter(sm -> sm.getDefenseDate() != null && !sm.getDefenseDate().isBefore(now))
                .sorted(Comparator.comparing(StudentModality::getDefenseDate))
                .toList();

        List<Long> upcomingIds = upcoming.stream().map(StudentModality::getId).toList();
        Map<Long, List<StudentModalityMember>> membersByModality = upcomingIds.isEmpty() ? Map.of()
                : studentModalityMemberRepository.findByStudentModalityIdInAndStatus(upcomingIds, MemberStatus.ACTIVE)
                        .stream()
                        .collect(Collectors.groupingBy(m -> m.getStudentModality().getId()));

        List<ModalityListDTO> calendar = upcoming.stream()
                .map(sm -> {
                    List<StudentModalityMember> activeMembers = membersByModality.getOrDefault(sm.getId(), List.of());
                    String studentNames = activeMembers.stream()
                            .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName())
                            .collect(Collectors.joining(", "));
                    String studentEmails = activeMembers.stream()
                            .map(m -> m.getStudent().getEmail())
                            .collect(Collectors.joining(", "));
                    String studentLastNames = activeMembers.stream()
                            .map(m -> m.getStudent().getLastName())
                            .collect(Collectors.joining(", "));
                    return ModalityListDTO.builder()
                            .studentModalityId(sm.getId())
                            .studentName(studentNames)
                            .studentLastName(studentLastNames)
                            .studentEmail(studentEmails)
                            .modalityName(sm.getProgramDegreeModality().getDegreeModality().getName())
                            .currentStatus(sm.getStatus().name())
                            .currentStatusDescription(ModalityServiceUtils.describeModalityStatus(sm.getStatus()))
                            .defenseDate(sm.getDefenseDate())
                            .defenseLocation(sm.getDefenseLocation())
                            .lastUpdatedAt(sm.getUpdatedAt())
                            .build();
                })
                .toList();
        return (calendar);
    }

    @Transactional(readOnly = true)
    public ExaminerTypeResponse getExaminerTypeForModality(Long studentModalityId, User examiner) {

        DefenseExaminer defenseExaminer = defenseExaminerRepository
                .findByStudentModalityIdAndExaminerId(studentModalityId, examiner.getId())
                .orElse(null);

        if (defenseExaminer == null) {
            throw new ForbiddenException("No está asignado como jurado a esta modalidad");
        }

        return new ExaminerTypeResponse(
                true,
                defenseExaminer.getExaminerType().name()
        );
    }

}
