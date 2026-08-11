package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.Entity.DefenseEvaluationCriteria;
import com.SIGMA.USCO.Modalities.Entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.Entity.ModalityProcessStatusHistory;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.Entity.enums.DefenseRubricType;
import com.SIGMA.USCO.Modalities.Entity.enums.ExaminerType;
import com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.ProposedMention;
import com.SIGMA.USCO.Modalities.Repository.DefenseEvaluationCriteriaRepository;
import com.SIGMA.USCO.Modalities.Repository.DefenseExaminerRepository;
import com.SIGMA.USCO.Modalities.Repository.ModalityProcessStatusHistoryRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Modalities.dto.DefenseEvaluationCriteriaDTO;
import com.SIGMA.USCO.Modalities.dto.ExaminerEvaluationDTO;
import com.SIGMA.USCO.Modalities.dto.ModalityListDTO;
import com.SIGMA.USCO.Modalities.dto.DefenseProposalDTO;
import com.SIGMA.USCO.Modalities.dto.ScheduleDefenseDTO;
import com.SIGMA.USCO.Modalities.dto.response.FinalDefenseResponse;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.Entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.documents.entity.RequiredDocument;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import com.SIGMA.USCO.documents.repository.RequiredDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.event.ModalityEvent;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.security.SecurityUtils;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import com.SIGMA.USCO.common.exception.ForbiddenException;
import com.SIGMA.USCO.common.exception.ValidationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefenseModalityService {

    private final DefenseExaminerRepository defenseExaminerRepository;
    private final DefenseEvaluationCriteriaRepository defenseEvaluationCriteriaRepository;
    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final ModalityProcessStatusHistoryRepository historyRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;
    private final RequiredDocumentRepository requiredDocumentRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public Map<String, Object> scheduleDefense(Long studentModalityId, ScheduleDefenseDTO request) {
        User projectDirector = SecurityUtils.getCurrentUser();
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
        studentModality.setDefenseDate(request.getDefenseDate());
        studentModality.setDefenseLocation(request.getDefenseLocation());
        studentModality.setStatus(ModalityProcessStatus.DEFENSE_SCHEDULED);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);
        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.DEFENSE_SCHEDULED)
                        .changeDate(LocalDateTime.now())
                        .responsible(projectDirector)
                        .observations(
                                "Director de proyecto programó la sustentación para el "
                                        + request.getDefenseDate()
                                        + " en "
                                        + request.getDefenseLocation()
                        )
                        .build()
        );
        // Notificar al estudiante líder
        User student = studentModality.getLeader();
        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.DEFENSE_SCHEDULED, studentModality.getId(), student.getId(), Map.of(
                        ModalityEvent.KEY_DEFENSE_DATE, request.getDefenseDate(),
                        ModalityEvent.KEY_DEFENSE_LOCATION, request.getDefenseLocation()
                ))
        );
        // Notificar a los jurados asociados
        List<DefenseExaminer> examiners = defenseExaminerRepository.findByStudentModalityId(studentModalityId);
        for (DefenseExaminer examinerAssignment : examiners) {
            User examiner = examinerAssignment.getExaminer();
            applicationEventPublisher.publishEvent(
                    new ModalityEvent(NotificationType.DEFENSE_SCHEDULED, studentModality.getId(), examiner.getId(), Map.of(
                            ModalityEvent.KEY_DEFENSE_DATE, request.getDefenseDate(),
                            ModalityEvent.KEY_DEFENSE_LOCATION, request.getDefenseLocation()
                    ))
            );
        }
        // Notificar al director (ya se hace arriba, pero si se requiere explícitamente)
        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.DEFENSE_SCHEDULED, studentModality.getId(), projectDirector.getId(), Map.of(
                        ModalityEvent.KEY_DEFENSE_DATE, request.getDefenseDate(),
                        ModalityEvent.KEY_DEFENSE_LOCATION, request.getDefenseLocation()
                ))
        );
        return (
                Map.of(
                        "success", true,
                        "studentModalityId", studentModalityId,
                        "defenseDate", request.getDefenseDate(),
                        "defenseLocation", request.getDefenseLocation(),
                        "newStatus", ModalityProcessStatus.DEFENSE_SCHEDULED,
                        "message", "Sustentación programada correctamente por el director de proyecto"
                )
        );
    }

    @Transactional
    public Map<String, Object> getPendingDefenseProposals() {

        User committeeMember = SecurityUtils.getCurrentUser();

        List<Long> academicProgramIds = programAuthorityRepository
                .findByUser_Id(committeeMember.getId())
                .stream()
                .filter(pa -> pa.getRole() == ProgramRole.PROGRAM_CURRICULUM_COMMITTEE)
                .map(pa -> pa.getAcademicProgram().getId())
                .toList();

        if (academicProgramIds.isEmpty()) {
            throw new ForbiddenException("El usuario no tiene el rol de PROGRAM_CURRICULUM_COMMITTEE");
        }

        List<StudentModality> pendingProposals = studentModalityRepository
                .findByStatusAndProgramDegreeModality_AcademicProgram_IdIn(
                        ModalityProcessStatus.DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR,
                        academicProgramIds
                );

        List<DefenseProposalDTO> proposals = pendingProposals.stream()
                .map(sm -> {
                    User student = sm.getLeader();
                    User director = sm.getProjectDirector();

                    String studentCode = null;
                    Optional<StudentProfile> profile = studentProfileRepository.findByUserId(student.getId());
                    if (profile.isPresent()) {
                        studentCode = profile.get().getStudentCode();
                    }

                    return DefenseProposalDTO.builder()
                            .studentModalityId(sm.getId())
                            .studentName(student.getName() + " " + student.getLastName())
                            .studentEmail(student.getEmail())
                            .studentCode(studentCode)
                            .modalityName(sm.getProgramDegreeModality().getDegreeModality().getName())
                            .academicProgram(sm.getProgramDegreeModality().getAcademicProgram().getName())
                            .faculty(sm.getProgramDegreeModality().getAcademicProgram().getFaculty().getName())
                            .projectDirectorId(director != null ? director.getId() : null)
                            .projectDirectorName(director != null ? director.getName() + " " + director.getLastName() : "No asignado")
                            .projectDirectorEmail(director != null ? director.getEmail() : null)
                            .proposedDefenseDate(sm.getDefenseDate())
                            .proposedDefenseLocation(sm.getDefenseLocation())
                            .proposalSubmittedAt(sm.getUpdatedAt())
                            .currentStatus(sm.getStatus().name())
                            .statusDescription(ModalityServiceUtils.describeModalityStatus(sm.getStatus()))
                            .build();
                })
                .toList();

        return (
                Map.of(
                        "success", true,
                        "totalProposals", proposals.size(),
                        "proposals", proposals
                )
        );
    }

    @Transactional
    public Map<String, Object> approveDefenseProposal(Long studentModalityId) {

        User committeeMember = SecurityUtils.getCurrentUser();

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
            throw new ForbiddenException("No tiene permiso para aprobar sustentaciones en este programa académico");
        }

        if (studentModality.getStatus() != ModalityProcessStatus.DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR) {
            throw new ValidationException("La modalidad no tiene una propuesta de sustentación pendiente de aprobación");
        }

        if (studentModality.getDefenseDate() == null ||
                studentModality.getDefenseLocation() == null ||
                studentModality.getDefenseLocation().isBlank()) {
            throw new ValidationException("No hay fecha y lugar propuestos para aprobar");
        }

        LocalDateTime approvedDate = studentModality.getDefenseDate();
        String approvedLocation = studentModality.getDefenseLocation();

        studentModality.setStatus(ModalityProcessStatus.DEFENSE_SCHEDULED);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.DEFENSE_SCHEDULED)
                        .changeDate(LocalDateTime.now())
                        .responsible(committeeMember)
                        .observations(
                                String.format(
                                        "Comité de currículo aprobó la propuesta del director de proyecto. " +
                                        "Sustentación programada para el %s en %s",
                                        approvedDate,
                                        approvedLocation
                                )
                        )
                        .build()
        );

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.DEFENSE_SCHEDULED, studentModality.getId(), committeeMember.getId(), Map.of(
                        ModalityEvent.KEY_DEFENSE_DATE, approvedDate,
                        ModalityEvent.KEY_DEFENSE_LOCATION, approvedLocation
                ))
        );

        return (
                Map.of(
                        "success", true,
                        "studentModalityId", studentModalityId,
                        "defenseDate", approvedDate,
                        "defenseLocation", approvedLocation,
                        "newStatus", ModalityProcessStatus.DEFENSE_SCHEDULED,
                        "action", "APROBADA",
                        "message", "Propuesta de sustentación aprobada correctamente"
                )
        );
    }

    @Transactional
    public Map<String, Object> rescheduleDefense(Long studentModalityId, ScheduleDefenseDTO request) {

        User committeeMember = SecurityUtils.getCurrentUser();

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
            throw new ForbiddenException("No tiene permiso para reprogramar sustentaciones en este programa académico");
        }

        if (studentModality.getStatus() != ModalityProcessStatus.DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR &&
                studentModality.getStatus() != ModalityProcessStatus.PROPOSAL_APPROVED) {
            throw new ValidationException("La modalidad no se encuentra en estado válido para reprogramar sustentación");
        }

        if (request.getDefenseDate() == null ||
                request.getDefenseLocation() == null ||
                request.getDefenseLocation().isBlank()) {

            throw new ValidationException("Debe ingresar fecha y lugar válidos para la reprogramación");
        }

        LocalDateTime originalProposedDate = studentModality.getDefenseDate();
        String originalProposedLocation = studentModality.getDefenseLocation();
        boolean hadProposal = studentModality.getStatus() == ModalityProcessStatus.DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR;

        studentModality.setDefenseDate(request.getDefenseDate());
        studentModality.setDefenseLocation(request.getDefenseLocation());
        studentModality.setStatus(ModalityProcessStatus.DEFENSE_SCHEDULED);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        String observation;
        if (hadProposal && originalProposedDate != null && originalProposedLocation != null) {
            observation = String.format(
                    "Comité de currículo reprogramó la sustentación. " +
                    "Propuesta original del director: %s en %s. " +
                    "Nueva programación: %s en %s",
                    originalProposedDate,
                    originalProposedLocation,
                    request.getDefenseDate(),
                    request.getDefenseLocation()
            );
        } else {
            observation = String.format(
                    "Comité de currículo programó la sustentación para el %s en %s",
                    request.getDefenseDate(),
                    request.getDefenseLocation()
            );
        }

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.DEFENSE_SCHEDULED)
                        .changeDate(LocalDateTime.now())
                        .responsible(committeeMember)
                        .observations(observation)
                        .build()
        );

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.DEFENSE_SCHEDULED, studentModality.getId(), committeeMember.getId(), Map.of(
                        ModalityEvent.KEY_DEFENSE_DATE, request.getDefenseDate(),
                        ModalityEvent.KEY_DEFENSE_LOCATION, request.getDefenseLocation()
                ))
        );

        return (
                Map.of(
                        "success", true,
                        "studentModalityId", studentModalityId,
                        "defenseDate", request.getDefenseDate(),
                        "defenseLocation", request.getDefenseLocation(),
                        "newStatus", ModalityProcessStatus.DEFENSE_SCHEDULED,
                        "action", hadProposal ? "REPROGRAMADA" : "PROGRAMADA",
                        "hadProposal", hadProposal,
                        "message", hadProposal ? "Sustentación reprogramada correctamente" : "Sustentación programada correctamente"
                )
        );
    }

    @Transactional
    public Map<String, Object> assignExaminers(Long studentModalityId, ScheduleDefenseDTO request) {

        User committeeMember = SecurityUtils.getCurrentUser();

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

        if (request.getPrimaryExaminer1Id() == null &&
                request.getPrimaryExaminer2Id() == null &&
                request.getTiebreakerExaminerId() == null) {
            throw new ValidationException("Debe proporcionar al menos un jurado para asignar");
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

        if (request.getPrimaryExaminer1Id() != null) {
            User examiner1 = userRepository.findById(request.getPrimaryExaminer1Id())
                    .orElseThrow(() -> new NotFoundException("Jurado principal 1 no encontrado"));

            boolean hasExaminerRole = examiner1.getRoles().stream()
                    .anyMatch(role -> role.getName().equals("EXAMINER"));

            if (!hasExaminerRole) {
                throw new ValidationException("El usuario seleccionado como jurado principal 1 no tiene el rol EXAMINER");
            }

            if (studentModality.getProjectDirector() != null &&
                    studentModality.getProjectDirector().getId().equals(examiner1.getId())) {
                throw new ValidationException("El director del proyecto no puede ser jurado de la misma modalidad");
            }

            defenseExaminerRepository
                    .findByStudentModalityIdAndExaminerType(studentModalityId, ExaminerType.PRIMARY_EXAMINER_1)
                    .ifPresent(defenseExaminerRepository::delete);

            DefenseExaminer defenseExaminer = DefenseExaminer.builder()
                    .studentModality(studentModality)
                    .examiner(examiner1)
                    .examinerType(ExaminerType.PRIMARY_EXAMINER_1)
                    .assignmentDate(LocalDateTime.now())
                    .assignedBy(committeeMember)
                    .build();

            defenseExaminerRepository.save(defenseExaminer);
            examinerAssignmentMessages.add("Jurado Principal 1: " + examiner1.getName() + " " + examiner1.getLastName());
        }

        if (request.getPrimaryExaminer2Id() != null) {
            User examiner2 = userRepository.findById(request.getPrimaryExaminer2Id())
                    .orElseThrow(() -> new NotFoundException("Jurado principal 2 no encontrado"));

            boolean hasExaminerRole = examiner2.getRoles().stream()
                    .anyMatch(role -> role.getName().equals("EXAMINER"));

            if (!hasExaminerRole) {
                throw new ValidationException("El usuario seleccionado como jurado principal 2 no tiene el rol EXAMINER");
            }

            if (studentModality.getProjectDirector() != null &&
                    studentModality.getProjectDirector().getId().equals(examiner2.getId())) {
                throw new ValidationException("El director del proyecto no puede ser jurado de la misma modalidad");
            }

            defenseExaminerRepository
                    .findByStudentModalityIdAndExaminerType(studentModalityId, ExaminerType.PRIMARY_EXAMINER_2)
                    .ifPresent(defenseExaminerRepository::delete);

            DefenseExaminer defenseExaminer = DefenseExaminer.builder()
                    .studentModality(studentModality)
                    .examiner(examiner2)
                    .examinerType(ExaminerType.PRIMARY_EXAMINER_2)
                    .assignmentDate(LocalDateTime.now())
                    .assignedBy(committeeMember)
                    .build();

            defenseExaminerRepository.save(defenseExaminer);
            examinerAssignmentMessages.add("Jurado Principal 2: " + examiner2.getName() + " " + examiner2.getLastName());
        }

        if (request.getTiebreakerExaminerId() != null) {
            User examiner3 = userRepository.findById(request.getTiebreakerExaminerId())
                    .orElseThrow(() -> new NotFoundException("Jurado de desempate no encontrado"));

            boolean hasExaminerRole = examiner3.getRoles().stream()
                    .anyMatch(role -> role.getName().equals("EXAMINER"));

            if (!hasExaminerRole) {
                throw new ValidationException("El usuario seleccionado como jurado de desempate no tiene el rol EXAMINER");
            }

            if (studentModality.getProjectDirector() != null &&
                    studentModality.getProjectDirector().getId().equals(examiner3.getId())) {
                throw new ValidationException("El director del proyecto no puede ser jurado de la misma modalidad");
            }

            defenseExaminerRepository
                    .findByStudentModalityIdAndExaminerType(studentModalityId, ExaminerType.TIEBREAKER_EXAMINER)
                    .ifPresent(defenseExaminerRepository::delete);

            DefenseExaminer defenseExaminer = DefenseExaminer.builder()
                    .studentModality(studentModality)
                    .examiner(examiner3)
                    .examinerType(ExaminerType.TIEBREAKER_EXAMINER)
                    .assignmentDate(LocalDateTime.now())
                    .assignedBy(committeeMember)
                    .build();

            defenseExaminerRepository.save(defenseExaminer);
            examinerAssignmentMessages.add("Jurado de Desempate: " + examiner3.getName() + " " + examiner3.getLastName());
        }

        studentModality.setStatus(ModalityProcessStatus.EXAMINERS_ASSIGNED);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        String observationMessage = "Jurados asignados por el comité de currículo:\n" +
                String.join("\n", examinerAssignmentMessages);

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.EXAMINERS_ASSIGNED)
                        .changeDate(LocalDateTime.now())
                        .responsible(committeeMember)
                        .observations(observationMessage)
                        .build()
        );

        // Notificar a los jurados asignados, estudiantes y director
        applicationEventPublisher.publishEvent(new ModalityEvent(NotificationType.EXAMINER_ASSIGNED, studentModalityId, committeeMember.getId(), Map.of()));

        return (
                Map.of(
                        "success", true,
                        "studentModalityId", studentModalityId,
                        "newStatus", ModalityProcessStatus.EXAMINERS_ASSIGNED,
                        "examinersAssigned", examinerAssignmentMessages,
                        "message", "Jurados asignados correctamente a la sustentación"
                )
        );
    }

    @Transactional
    public Map<String, Object> registerFinalDefenseEvaluation(Long studentModalityId, ExaminerEvaluationDTO evaluationDTO) {

        User examiner = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        DefenseExaminer defenseExaminer = defenseExaminerRepository
                .findByStudentModalityIdAndExaminerId(studentModalityId, examiner.getId())
                .orElseThrow(() -> new NotFoundException(
                        "No está asignado como jurado de esta sustentación"
                ));

        if (defenseEvaluationCriteriaRepository.existsByDefenseExaminerId(defenseExaminer.getId())) {
            throw new ValidationException("Ya ha registrado su evaluación para esta sustentación");
        }

        if (studentModality.getStatus() != ModalityProcessStatus.DEFENSE_COMPLETED &&
                studentModality.getStatus() != ModalityProcessStatus.READY_FOR_DEFENSE &&
                studentModality.getStatus() != ModalityProcessStatus.EXAMINERS_ASSIGNED &&
                studentModality.getStatus() != ModalityProcessStatus.UNDER_EVALUATION_PRIMARY_EXAMINERS &&
                studentModality.getStatus() != ModalityProcessStatus.UNDER_EVALUATION_TIEBREAKER &&
                studentModality.getStatus() != ModalityProcessStatus.DEFENSE_SCHEDULED &&
                studentModality.getStatus() != ModalityProcessStatus.DISAGREEMENT_REQUIRES_TIEBREAKER) {

            throw new ValidationException("La modalidad no está en estado válido para registrar evaluaciones");
        }

        // Punto 3: El jurado de desempate SOLO puede evaluar si hay desacuerdo entre primarios
        if (defenseExaminer.getExaminerType() == ExaminerType.TIEBREAKER_EXAMINER &&
                studentModality.getStatus() != ModalityProcessStatus.DISAGREEMENT_REQUIRES_TIEBREAKER) {
            throw new ValidationException("El jurado de desempate solo puede evaluar cuando existe desacuerdo entre los jurados principales (un jurado aprueba y el otro rechaza).");
        }

        // Los jurados primarios no pueden evaluar si ya hay desacuerdo resuelto al desempate
        if (defenseExaminer.getExaminerType() != ExaminerType.TIEBREAKER_EXAMINER &&
                studentModality.getStatus() == ModalityProcessStatus.DISAGREEMENT_REQUIRES_TIEBREAKER) {
            throw new ValidationException("Existe desacuerdo entre los jurados principales. Solo el jurado de desempate puede evaluar en este momento.");
        }

        // Validar nota
        if (evaluationDTO.getGrade() == null || evaluationDTO.getGrade() < 0.0 || evaluationDTO.getGrade() > 5.0) {
            throw new ValidationException("La calificación debe estar entre 0.0 y 5.0");
        }

        // Construir la entidad DefenseEvaluationCriteria con toda la información
        DefenseEvaluationCriteria.DefenseEvaluationCriteriaBuilder criteriaBuilder =
                DefenseEvaluationCriteria.builder()
                        .defenseExaminer(defenseExaminer)
                        .grade(evaluationDTO.getGrade())
                        .observations(evaluationDTO.getObservations())
                        .isFinalDecision(false)
                        .evaluatedAt(LocalDateTime.now());

        DefenseRubricType expectedRubricType = resolveDefenseRubricType(studentModality);
        DefenseEvaluationCriteriaDTO criteriaDTO = evaluationDTO.getEvaluationCriteria();

        if (criteriaDTO == null) {
            throw new ValidationException("Debe enviar la rúbrica de evaluación en el campo evaluationCriteria.");
        }

        if (criteriaDTO.getRubricType() != null && criteriaDTO.getRubricType() != expectedRubricType) {
            throw new ValidationException("El tipo de rúbrica enviado no coincide con la modalidad evaluada.");
        }

        if (expectedRubricType == DefenseRubricType.ENTREPRENEURSHIP) {
            if (criteriaDTO.getEntrepreneurshipPresentationSupportMaterial() == null
                    || criteriaDTO.getEntrepreneurshipCoherentBusinessObjectives() == null
                    || criteriaDTO.getEntrepreneurshipMethodologyTechnicalApproach() == null
                    || criteriaDTO.getEntrepreneurshipAnalyticalCreativeCapacity() == null
                    || criteriaDTO.getEntrepreneurshipDefenseSustentation() == null) {
                throw new ValidationException("Para la modalidad de Emprendimiento y fortalecimiento de empresa debe enviar los 5 criterios específicos de la rúbrica empresarial.");
            }

            criteriaBuilder
                    .rubricType(DefenseRubricType.ENTREPRENEURSHIP)
                    .entrepreneurshipPresentationSupportMaterial(criteriaDTO.getEntrepreneurshipPresentationSupportMaterial())
                    .entrepreneurshipCoherentBusinessObjectives(criteriaDTO.getEntrepreneurshipCoherentBusinessObjectives())
                    .entrepreneurshipMethodologyTechnicalApproach(criteriaDTO.getEntrepreneurshipMethodologyTechnicalApproach())
                    .entrepreneurshipAnalyticalCreativeCapacity(criteriaDTO.getEntrepreneurshipAnalyticalCreativeCapacity())
                    .entrepreneurshipDefenseSustentation(criteriaDTO.getEntrepreneurshipDefenseSustentation())
                    .proposedMention(criteriaDTO.getProposedMention() != null
                            ? criteriaDTO.getProposedMention()
                            : ProposedMention.NONE)
                    // Se mapean también a la rúbrica estándar para mantener compatibilidad histórica
                    // con reportes/queries existentes que leen los 5 campos legacy.
                    .domainAndClarity(criteriaDTO.getEntrepreneurshipCoherentBusinessObjectives())
                    .synthesisAndCommunication(criteriaDTO.getEntrepreneurshipPresentationSupportMaterial())
                    .argumentationAndResponse(criteriaDTO.getEntrepreneurshipDefenseSustentation())
                    .innovationAndImpact(criteriaDTO.getEntrepreneurshipAnalyticalCreativeCapacity())
                    .professionalPresentation(criteriaDTO.getEntrepreneurshipMethodologyTechnicalApproach());
        } else {
            if (criteriaDTO.getDomainAndClarity() == null
                    || criteriaDTO.getSynthesisAndCommunication() == null
                    || criteriaDTO.getArgumentationAndResponse() == null
                    || criteriaDTO.getInnovationAndImpact() == null
                    || criteriaDTO.getProfessionalPresentation() == null) {
                throw new ValidationException("Para esta modalidad debe enviar los 5 criterios estándar de la rúbrica.");
            }

            criteriaBuilder
                    .rubricType(DefenseRubricType.STANDARD)
                    .domainAndClarity(criteriaDTO.getDomainAndClarity())
                    .synthesisAndCommunication(criteriaDTO.getSynthesisAndCommunication())
                    .argumentationAndResponse(criteriaDTO.getArgumentationAndResponse())
                    .innovationAndImpact(criteriaDTO.getInnovationAndImpact())
                    .professionalPresentation(criteriaDTO.getProfessionalPresentation())
                    .proposedMention(criteriaDTO.getProposedMention() != null
                            ? criteriaDTO.getProposedMention()
                            : ProposedMention.NONE);
        }

        DefenseEvaluationCriteria evaluation = criteriaBuilder.build();
        defenseEvaluationCriteriaRepository.save(evaluation);

        if (defenseExaminer.getExaminerType() == ExaminerType.TIEBREAKER_EXAMINER) {
            return processTiebreakerEvaluation(studentModality, evaluation, examiner);
        } else {

            return processPrimaryExaminerEvaluation(studentModality, evaluation, examiner);
        }
    }

    @Transactional
    public Map<String, Object> getFinalDefenseEvaluationForExaminer(Long studentModalityId) {
        User examiner = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        DefenseExaminer defenseExaminer = defenseExaminerRepository
                .findByStudentModalityIdAndExaminerId(studentModalityId, examiner.getId())
                .orElseThrow(() -> new NotFoundException(
                        "No está asignado como jurado de esta sustentación"
                ));

        DefenseEvaluationCriteria evaluation = defenseEvaluationCriteriaRepository
                .findByDefenseExaminerId(defenseExaminer.getId())
                .orElse(null);

        if (evaluation == null) {
            return (
                    Map.of(
                            "success", false,
                            "message", "No hay evaluación registrada para este jurado en esta modalidad"
                    )
            );
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("evaluationId", evaluation.getId());
        response.put("grade", evaluation.getGrade());
        response.put("approved", evaluation.getGrade() != null && evaluation.getGrade() >= 3.5);
        response.put("observations", evaluation.getObservations());
        response.put("evaluationDate", evaluation.getEvaluatedAt());
        response.put("isFinalDecision", evaluation.getIsFinalDecision());
        response.put("examinerType", defenseExaminer.getExaminerType());

        response.put("evaluationCriteria", buildDefenseCriteriaResponse(evaluation));

        return (response);
    }

    private Map<String, Object> processPrimaryExaminerEvaluation(StudentModality studentModality, DefenseEvaluationCriteria currentEvaluation, User examiner) {

        if (studentModality.getStatus() == ModalityProcessStatus.DEFENSE_COMPLETED) {
            studentModality.setStatus(ModalityProcessStatus.UNDER_EVALUATION_PRIMARY_EXAMINERS);
            studentModality.setUpdatedAt(LocalDateTime.now());
            studentModalityRepository.save(studentModality);
        }

        boolean bothEvaluated = defenseEvaluationCriteriaRepository
                .bothPrimaryExaminersHaveEvaluated(studentModality.getId());

        if (!bothEvaluated) {

            return (
                    Map.of(
                            "success", true,
                            "message", "Evaluación registrada correctamente. Esperando evaluación del otro jurado principal.",
                            "grade", currentEvaluation.getGrade(),
                            "approved", currentEvaluation.getGrade() >= 3.5
                    )
            );
        }

        List<DefenseEvaluationCriteria> primaryEvaluations = defenseEvaluationCriteriaRepository
                .findPrimaryEvaluationsByStudentModalityId(studentModality.getId());

        boolean hasConsensus = defenseEvaluationCriteriaRepository
                .primaryExaminersHaveConsensus(studentModality.getId());

        if (hasConsensus) {
            return applyFinalResultWithConsensus(studentModality, primaryEvaluations, examiner);
        } else {

            return requestTiebreakerExaminer(studentModality, primaryEvaluations, examiner);
        }
    }

    private Map<String, Object> applyFinalResultWithConsensus(StudentModality studentModality, List<DefenseEvaluationCriteria> primaryEvaluations, User examiner) {

        // La nota final es el promedio de las dos notas de los jurados principales (punto 4)
        Double averageGrade = defenseEvaluationCriteriaRepository
                .calculateAverageGradeOfPrimaryExaminers(studentModality.getId());

        primaryEvaluations.forEach(eval -> {
            eval.setIsFinalDecision(true);
            defenseEvaluationCriteriaRepository.save(eval);
        });

        // La aprobación se determina por nota: >= 3.5 = aprobado, < 3.5 = reprobado
        boolean approved = averageGrade != null && averageGrade >= 3.5;

        AcademicDistinction distinction;
        ModalityProcessStatus finalStatus;
        boolean pendingDistinctionReview = false;

        if (!approved) {
            distinction = AcademicDistinction.AGREED_REJECTED;
            finalStatus = ModalityProcessStatus.GRADED_FAILED;
        } else {
            // La mención solo se propone si AMBOS jurados coinciden unánimemente
            ProposedMention mention1 = primaryEvaluations.get(0).getProposedMention();
            ProposedMention mention2 = primaryEvaluations.get(1).getProposedMention();

            if (mention1 != null && mention2 != null && mention1 == mention2
                    && mention1 == ProposedMention.LAUREATE) {
                // Los jurados PROPONEN la mención Laureada → el comité debe decidir
                distinction = AcademicDistinction.PENDING_COMMITTEE_LAUREATE;
                finalStatus = ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW;
                pendingDistinctionReview = true;
            } else if (mention1 != null && mention2 != null && mention1 == mention2
                    && mention1 == ProposedMention.MERITORIOUS) {
                // Los jurados PROPONEN la mención Meritoria → el comité debe decidir
                distinction = AcademicDistinction.PENDING_COMMITTEE_MERITORIOUS;
                finalStatus = ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW;
                pendingDistinctionReview = true;
            } else {
                distinction = AcademicDistinction.AGREED_APPROVED;
                finalStatus = ModalityProcessStatus.GRADED_APPROVED;
            }
        }

        studentModality.setStatus(finalStatus);
        studentModality.setAcademicDistinction(distinction);
        studentModality.setFinalGrade(averageGrade);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        // Construir la observación con los argumentos de los jurados sobre la mención
        String mentionNotes = primaryEvaluations.stream()
                .filter(e -> e.getObservations() != null && !e.getObservations().isBlank())
                .map(e -> "Jurado " + (e.getDefenseExaminer().getExaminerType() != null
                        ? e.getDefenseExaminer().getExaminerType().name() : "") + ": " + e.getObservations())
                .collect(Collectors.joining(" | "));

        String observations;
        if (pendingDistinctionReview) {
            observations = String.format(
                    "CONSENSO entre jurados principales. Calificación final (promedio): %.2f. " +
                    "Resultado: APROBADO. Los jurados proponen la distinción: %s. " +
                    "PENDIENTE DE REVISIÓN por el Comité de Currículo. Argumentos: %s",
                    averageGrade,
                    ModalityServiceUtils.translateAcademicDistinction(distinction),
                    mentionNotes.isBlank() ? "Sin argumentos adicionales" : mentionNotes
            );
        } else {
            observations = String.format(
                    "CONSENSO entre jurados principales. Calificación final (promedio): %.2f. " +
                    "Resultado: %s. Distinción: %s",
                    averageGrade,
                    approved ? "APROBADO" : "REPROBADO",
                    ModalityServiceUtils.translateAcademicDistinction(distinction)
            );
        }

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(finalStatus)
                        .changeDate(LocalDateTime.now())
                        .responsible(examiner)
                        .observations(observations)
                        .build()
        );

        // Publicar siempre: incluso si la distinción queda pendiente de comité,
        // el estudiante debe recibir correo y acta de aprobación inicial.
        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.DEFENSE_COMPLETED, studentModality.getId(), examiner.getId(), Map.of(
                        ModalityEvent.KEY_FINAL_STATUS, finalStatus,
                        ModalityEvent.KEY_ACADEMIC_DISTINCTION, distinction,
                        ModalityEvent.KEY_OBSERVATIONS, observations
                ))
        );

        String message;
        if (pendingDistinctionReview) {
            message = "¡Felicitaciones! Tu modalidad de grado ha sido aprobada por consenso de los jurados. Los jurados han propuesto una distinción honorífica (" +
                    ModalityServiceUtils.translateAcademicDistinction(distinction) + "). El Comité de Currículo debe revisar y decidir si acepta o rechaza la distinción.";
        } else {
            message = approved ? "¡Felicitaciones! Tu modalidad de grado ha sido aprobada por consenso de los jurados." : "Tu modalidad de grado ha sido reprobada por consenso de los jurados.";
        }

        return (
                Map.of(
                        "exito", true,
                        "consenso", true,
                        "estadoFinal", finalStatus.name(),
                        "distincionAcademica", ModalityServiceUtils.translateAcademicDistinction(distinction),
                        "calificacionFinal", averageGrade,
                        "distincionPendienteRevision", pendingDistinctionReview,
                        "mensaje", message
                )
        );
    }

    private Map<String, Object> requestTiebreakerExaminer(StudentModality studentModality, List<DefenseEvaluationCriteria> primaryEvaluations, User examiner) {

        studentModality.setStatus(ModalityProcessStatus.DISAGREEMENT_REQUIRES_TIEBREAKER);
        studentModality.setAcademicDistinction(AcademicDistinction.DISAGREEMENT_PENDING_TIEBREAKER);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        String observations = String.format(
                "DESACUERDO entre jurados principales. Jurado 1: %s (%.2f). Jurado 2: %s (%.2f). " +
                "Se requiere asignar un tercer jurado para desempatar.",
                primaryEvaluations.get(0).getGrade() >= 3.5 ? "APROBADO" : "REPROBADO",
                primaryEvaluations.get(0).getGrade(),
                primaryEvaluations.get(1).getGrade() >= 3.5 ? "APROBADO" : "REPROBADO",
                primaryEvaluations.get(1).getGrade()
        );

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.DISAGREEMENT_REQUIRES_TIEBREAKER)
                        .changeDate(LocalDateTime.now())
                        .responsible(examiner)
                        .observations(observations)
                        .build()
        );

        return (
                Map.of(
                        "success", true,
                        "hasConsensus", false,
                        "requiresTiebreaker", true,
                        "status", ModalityProcessStatus.DISAGREEMENT_REQUIRES_TIEBREAKER,
                        "message", "No hay consenso entre los jurados principales. Se requiere asignar un tercer jurado para desempatar."
                )
        );
    }

    private Map<String, Object> processTiebreakerEvaluation(StudentModality studentModality, DefenseEvaluationCriteria tiebreakerEvaluation, User examiner) {

        tiebreakerEvaluation.setIsFinalDecision(true);
        defenseEvaluationCriteriaRepository.save(tiebreakerEvaluation);

        // La aprobación se determina por nota: >= 3.5 = aprobado (punto 2 y 3)
        // La nota final es la del jurado de desempate (punto 5)
        double tiebreakerGrade = tiebreakerEvaluation.getGrade();
        boolean approved = tiebreakerGrade >= 3.5;

        AcademicDistinction distinction;
        ModalityProcessStatus finalStatus;
        boolean pendingDistinctionReview = false;

        if (!approved) {
            distinction = AcademicDistinction.TIEBREAKER_REJECTED;
            finalStatus = ModalityProcessStatus.GRADED_FAILED;
        } else {
            // La mención la determina el proposedMention del jurado de desempate
            ProposedMention tiebreakerMention = tiebreakerEvaluation.getProposedMention();
            if (tiebreakerMention == ProposedMention.LAUREATE) {
                // El jurado de desempate PROPONE la mención Laureada → el comité debe decidir
                distinction = AcademicDistinction.TIEBREAKER_PENDING_COMMITTEE_LAUREATE;
                finalStatus = ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW;
                pendingDistinctionReview = true;
            } else if (tiebreakerMention == ProposedMention.MERITORIOUS) {
                // El jurado de desempate PROPONE la mención Meritoria → el comité debe decidir
                distinction = AcademicDistinction.TIEBREAKER_PENDING_COMMITTEE_MERITORIOUS;
                finalStatus = ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW;
                pendingDistinctionReview = true;
            } else {
                distinction = AcademicDistinction.TIEBREAKER_APPROVED;
                finalStatus = ModalityProcessStatus.GRADED_APPROVED;
            }
        }

        // La nota final en studentModality es la del tercer jurado (punto 5)
        studentModality.setStatus(finalStatus);
        studentModality.setAcademicDistinction(distinction);
        studentModality.setFinalGrade(tiebreakerGrade);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        String observations;
        if (pendingDistinctionReview) {
            String mentionNote = tiebreakerEvaluation.getObservations() != null
                    ? tiebreakerEvaluation.getObservations() : "Sin argumentos adicionales";
            observations = String.format(
                    "DESEMPATE resuelto por tercer jurado. Calificación final: %.2f. " +
                    "Resultado: APROBADO. El jurado de desempate propone la distinción: %s. " +
                    "PENDIENTE DE REVISIÓN por el Comité de Currículo. Argumento: %s",
                    tiebreakerGrade,
                    ModalityServiceUtils.translateProposedDistinction(distinction),
                    mentionNote
            );
        } else {
            observations = String.format(
                    "DESEMPATE resuelto por tercer jurado. Calificación final: %.2f. " +
                    "Resultado: %s. Distinción: %s",
                    tiebreakerGrade,
                    approved ? "APROBADO" : "REPROBADO",
                    ModalityServiceUtils.translateProposedDistinction(distinction)
            );
        }

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(finalStatus)
                        .changeDate(LocalDateTime.now())
                        .responsible(examiner)
                        .observations(observations)
                        .build()
        );

        // Publicar siempre: si queda pendiente de comité también se debe enviar acta inicial.
        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.DEFENSE_COMPLETED, studentModality.getId(), examiner.getId(), Map.of(
                        ModalityEvent.KEY_FINAL_STATUS, finalStatus,
                        ModalityEvent.KEY_ACADEMIC_DISTINCTION, distinction,
                        ModalityEvent.KEY_OBSERVATIONS, observations
                ))
        );

        String message;
        if (pendingDistinctionReview) {
            message = "Modalidad APROBADA por decisión del jurado de desempate. El jurado ha PROPUESTO la distinción (" +
                    ModalityServiceUtils.translateProposedDistinction(distinction) + "). El Comité de Currículo debe revisar y decidir si acepta o rechaza la distinción.";
        } else {
            message = approved ? "Modalidad APROBADA por decisión del jurado de desempate"
                    : "Modalidad REPROBADA por decisión del jurado de desempate";
        }

        return (
                Map.of(
                        "success", true,
                        "isTiebreaker", true,
                        "finalStatus", finalStatus,
                        "academicDistinction", distinction,
                        "finalGrade", tiebreakerGrade,
                        "pendingDistinctionReview", pendingDistinctionReview,
                        "message", message
                )
        );
    }

    @Transactional(readOnly = true)
    public FinalDefenseResponse getFinalDefenseResult(Long studentModalityId) {

        User user = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        Long academicProgramId =
                studentModality
                        .getProgramDegreeModality()
                        .getAcademicProgram()
                        .getId();

        boolean authorized =
                programAuthorityRepository
                        .existsByUser_IdAndAcademicProgram_IdAndRoleIn(
                                user.getId(),
                                academicProgramId,
                                List.of(
                                        ProgramRole.PROGRAM_HEAD,
                                        ProgramRole.PROGRAM_CURRICULUM_COMMITTEE
                                )
                        );

        if (!authorized) {
            throw new ForbiddenException("No tiene permiso para consultar el resultado final de esta modalidad");
        }

        if (studentModality.getStatus() != ModalityProcessStatus.GRADED_APPROVED &&
                studentModality.getStatus() != ModalityProcessStatus.GRADED_FAILED) {

            throw new ValidationException("La modalidad aún no tiene un resultado final registrado");
        }

        ModalityProcessStatus finalStatus = studentModality.getStatus();

        ModalityProcessStatusHistory history =
                historyRepository
                        .findTopByStudentModalityAndStatusOrderByChangeDateDesc(
                                studentModality,
                                finalStatus
                        )
                        .orElseThrow(() ->
                                new NotFoundException("No se encontró historial de evaluación final")
                        );

        List<DefenseExaminer> defenseExaminers = defenseExaminerRepository
                .findByStudentModalityId(studentModalityId);

        List<FinalDefenseResponse.ExaminerEvaluationDetail> examinerEvaluations = defenseExaminers.stream()
                .map(defenseExaminer -> {
                    DefenseEvaluationCriteria evaluation = defenseEvaluationCriteriaRepository
                            .findByDefenseExaminerId(defenseExaminer.getId())
                            .orElse(null);

                    if (evaluation == null) {
                        return null;
                    }

                    FinalDefenseResponse.CriteriaDetail criteriaDetail = buildFinalDefenseCriteriaDetail(evaluation);

                    return FinalDefenseResponse.ExaminerEvaluationDetail.builder()
                            .examinerName(defenseExaminer.getExaminer().getName() + " " +
                                        defenseExaminer.getExaminer().getLastName())
                            .examinerType(defenseExaminer.getExaminerType().name())
                            .grade(evaluation.getGrade())
                            .approved(evaluation.getGrade() != null && evaluation.getGrade() >= 3.5)
                            .observations(evaluation.getObservations())
                            .evaluationDate(evaluation.getEvaluatedAt())
                            .isFinalDecision(evaluation.getIsFinalDecision())
                            .evaluationCriteria(criteriaDetail)
                            .build();
                })
                .filter(detail -> detail != null)
                .toList();

        boolean hasConsensus = studentModality.getAcademicDistinction() != null &&
                              (studentModality.getAcademicDistinction().name().startsWith("AGREED_"));

        boolean wasTiebreaker = studentModality.getAcademicDistinction() != null &&
                               (studentModality.getAcademicDistinction().name().startsWith("TIEBREAKER_"));

        return (
                FinalDefenseResponse.builder()
                        .studentModalityId(studentModality.getId())
                        .studentName(
                                studentModality.getLeader().getName() + " " +
                                        studentModality.getLeader().getLastName()
                        )
                        .studentEmail(studentModality.getLeader().getEmail())
                        .finalStatus(finalStatus)
                        .approved(finalStatus == ModalityProcessStatus.GRADED_APPROVED)
                        .academicDistinction(studentModality.getAcademicDistinction())
                        .finalGrade(studentModality.getFinalGrade())
                        .observations(history.getObservations())
                        .evaluationDate(history.getChangeDate())
                        .evaluatedBy(
                                history.getResponsible() != null
                                        ? history.getResponsible().getName()
                                        : "Comité de currículo de programa"
                        )
                        .hasConsensus(hasConsensus)
                        .wasTiebreaker(wasTiebreaker)
                        .examinerEvaluations(examinerEvaluations)
                        .build()
        );
    }

    private DefenseRubricType resolveDefenseRubricType(StudentModality studentModality) {
        String modalityName = studentModality.getProgramDegreeModality().getDegreeModality().getName();
        String normalizedName = ModalityServiceUtils.normalizeText(modalityName);
        if ("emprendimiento y fortalecimiento de empresa".equals(normalizedName)) {
            return DefenseRubricType.ENTREPRENEURSHIP;
        }
        return DefenseRubricType.STANDARD;
    }

    private Map<String, Object> buildDefenseCriteriaResponse(DefenseEvaluationCriteria evaluation) {
        if (evaluation == null) {
            return null;
        }

        Map<String, Object> criteriaMap = new LinkedHashMap<>();
        DefenseRubricType rubricType = evaluation.getRubricType() != null
                ? evaluation.getRubricType()
                : DefenseRubricType.STANDARD;

        criteriaMap.put("id", evaluation.getId());
        criteriaMap.put("rubricType", rubricType.name());
        criteriaMap.put("proposedMention", evaluation.getProposedMention());
        criteriaMap.put("evaluatedAt", evaluation.getEvaluatedAt());

        if (rubricType == DefenseRubricType.ENTREPRENEURSHIP) {
            criteriaMap.put("entrepreneurshipPresentationSupportMaterial", evaluation.getEntrepreneurshipPresentationSupportMaterial());
            criteriaMap.put("entrepreneurshipCoherentBusinessObjectives", evaluation.getEntrepreneurshipCoherentBusinessObjectives());
            criteriaMap.put("entrepreneurshipMethodologyTechnicalApproach", evaluation.getEntrepreneurshipMethodologyTechnicalApproach());
            criteriaMap.put("entrepreneurshipAnalyticalCreativeCapacity", evaluation.getEntrepreneurshipAnalyticalCreativeCapacity());
            criteriaMap.put("entrepreneurshipDefenseSustentation", evaluation.getEntrepreneurshipDefenseSustentation());
        } else {
            criteriaMap.put("domainAndClarity", evaluation.getDomainAndClarity());
            criteriaMap.put("synthesisAndCommunication", evaluation.getSynthesisAndCommunication());
            criteriaMap.put("argumentationAndResponse", evaluation.getArgumentationAndResponse());
            criteriaMap.put("innovationAndImpact", evaluation.getInnovationAndImpact());
            criteriaMap.put("professionalPresentation", evaluation.getProfessionalPresentation());
        }

        return criteriaMap;
    }

    private FinalDefenseResponse.CriteriaDetail buildFinalDefenseCriteriaDetail(DefenseEvaluationCriteria evaluation) {
        if (evaluation == null) {
            return null;
        }

        return FinalDefenseResponse.CriteriaDetail.builder()
                .rubricType(evaluation.getRubricType() != null ? evaluation.getRubricType() : DefenseRubricType.STANDARD)
                .domainAndClarity(evaluation.getDomainAndClarity())
                .synthesisAndCommunication(evaluation.getSynthesisAndCommunication())
                .argumentationAndResponse(evaluation.getArgumentationAndResponse())
                .innovationAndImpact(evaluation.getInnovationAndImpact())
                .professionalPresentation(evaluation.getProfessionalPresentation())
                .entrepreneurshipPresentationSupportMaterial(evaluation.getEntrepreneurshipPresentationSupportMaterial())
                .entrepreneurshipCoherentBusinessObjectives(evaluation.getEntrepreneurshipCoherentBusinessObjectives())
                .entrepreneurshipMethodologyTechnicalApproach(evaluation.getEntrepreneurshipMethodologyTechnicalApproach())
                .entrepreneurshipAnalyticalCreativeCapacity(evaluation.getEntrepreneurshipAnalyticalCreativeCapacity())
                .entrepreneurshipDefenseSustentation(evaluation.getEntrepreneurshipDefenseSustentation())
                .proposedMention(evaluation.getProposedMention())
                .evaluatedAt(evaluation.getEvaluatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Object getMyFinalDefenseResult() {

        User student = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository
                .findByStudent(student)
                .orElseThrow(() -> new NotFoundException(
                        "No se encontró una modalidad asociada al estudiante"
                ));

        if (studentModality.getStatus() != ModalityProcessStatus.GRADED_APPROVED &&
                studentModality.getStatus() != ModalityProcessStatus.GRADED_FAILED) {

            return (
                    Map.of(
                            "hasResult", false,
                            "message", "Tu modalidad aún no tiene un resultado final"
                    )
            );
        }

        ModalityProcessStatus finalStatus = studentModality.getStatus();

        ModalityProcessStatusHistory history = historyRepository
                .findTopByStudentModalityAndStatusOrderByChangeDateDesc(
                        studentModality,
                        finalStatus
                )
                .orElseThrow(() -> new NotFoundException(
                        "No se encontró historial de evaluación final"
                ));

        List<DefenseExaminer> defenseExaminers = defenseExaminerRepository
                .findByStudentModalityId(studentModality.getId());

        List<FinalDefenseResponse.ExaminerEvaluationDetail> examinerEvaluations = defenseExaminers.stream()
                .map(defenseExaminer -> {
                    DefenseEvaluationCriteria evaluation = defenseEvaluationCriteriaRepository
                            .findByDefenseExaminerId(defenseExaminer.getId())
                            .orElse(null);

                    if (evaluation == null) {
                        return null;
                    }

                    return FinalDefenseResponse.ExaminerEvaluationDetail.builder()
                            .examinerName(defenseExaminer.getExaminer().getName() + " " +
                                        defenseExaminer.getExaminer().getLastName())
                            .examinerType(defenseExaminer.getExaminerType().name())
                            .grade(evaluation.getGrade())
                            .approved(evaluation.getGrade() != null && evaluation.getGrade() >= 3.5)
                            .observations(evaluation.getObservations())
                            .evaluationDate(evaluation.getEvaluatedAt())
                            .isFinalDecision(evaluation.getIsFinalDecision())
                            .evaluationCriteria(buildFinalDefenseCriteriaDetail(evaluation))
                            .build();
                })
                .filter(detail -> detail != null)
                .toList();

        boolean hasConsensus = studentModality.getAcademicDistinction() != null &&
                              (studentModality.getAcademicDistinction().name().startsWith("AGREED_"));

        boolean wasTiebreaker = studentModality.getAcademicDistinction() != null &&
                               (studentModality.getAcademicDistinction().name().startsWith("TIEBREAKER_"));

        return (
                FinalDefenseResponse.builder()
                        .studentModalityId(studentModality.getId())
                        .studentName(student.getName() + " " + student.getLastName())
                        .studentEmail(student.getEmail())
                        .finalStatus(finalStatus)
                        .approved(finalStatus == ModalityProcessStatus.GRADED_APPROVED)
                        .academicDistinction(studentModality.getAcademicDistinction())
                        .finalGrade(studentModality.getFinalGrade())
                        .observations(history.getObservations())
                        .evaluationDate(history.getChangeDate())
                        .evaluatedBy(
                                history.getResponsible() != null
                                        ? history.getResponsible().getName()
                                        : "Comité de currículo de programa"
                        )
                        .hasConsensus(hasConsensus)
                        .wasTiebreaker(wasTiebreaker)
                        .examinerEvaluations(examinerEvaluations)
                        .build()
        );
    }

    // =========================================================================
    // GESTIÓN DE DISTINCIONES HONORÍFICAS PROPUESTAS POR JURADOS
    // =========================================================================

    /**
     * Lista las modalidades en las que los jurados han propuesto unánimemente
     * una distinción honorífica (Meritoria o Laureada) y que están pendientes
     * de revisión y decisión por parte del Comité de Currículo.
     *
     * Solo el comité del programa académico correspondiente puede ver estas modalidades.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPendingDistinctionProposals() {
        User committeeMember = SecurityUtils.getCurrentUser();

        List<Long> programIds = programAuthorityRepository
                .findByUser_Id(committeeMember.getId())
                .stream()
                .filter(pa -> pa.getRole() == ProgramRole.PROGRAM_CURRICULUM_COMMITTEE)
                .map(pa -> pa.getAcademicProgram().getId())
                .toList();

        if (programIds.isEmpty()) {
            throw new ForbiddenException("El usuario no tiene el rol de Comité de Currículo en ningún programa académico.");
        }

        // Buscar modalidades con estado PENDING_DISTINCTION_COMMITTEE_REVIEW en los programas del comité
        List<StudentModality> pendingModalities = studentModalityRepository
                .findByStatusAndProgramDegreeModality_AcademicProgram_IdIn(
                        ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW,
                        programIds
                );

        List<Map<String, Object>> result = pendingModalities.stream()
                .sorted(Comparator.comparing(StudentModality::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(sm -> {
                    User leader = sm.getLeader();
                    StudentProfile leaderProfile = studentProfileRepository.findByUserId(leader.getId()).orElse(null);

                    // Obtener las evaluaciones de los jurados para ver los argumentos
                    List<DefenseExaminer> examiners = defenseExaminerRepository.findByStudentModalityId(sm.getId());
                    List<Map<String, Object>> examinerDetails = examiners.stream()
                            .map(de -> {
                                DefenseEvaluationCriteria eval = defenseEvaluationCriteriaRepository
                                        .findByDefenseExaminerId(de.getId())
                                        .orElse(null);
                                Map<String, Object> examinerMap = new LinkedHashMap<>();
                                examinerMap.put("examinerId", de.getExaminer().getId());
                                examinerMap.put("examinerName", de.getExaminer().getName() + " " + de.getExaminer().getLastName());
                                examinerMap.put("examinerType", de.getExaminerType() != null ? de.getExaminerType().name() : null);
                                examinerMap.put("proposedMention", eval != null ? (eval.getProposedMention() != null ? eval.getProposedMention().name() : "NONE") : null);
                                examinerMap.put("grade", eval != null ? eval.getGrade() : null);
                                examinerMap.put("observations", eval != null ? eval.getObservations() : null);
                                return examinerMap;
                            })
                            .toList();

                    // Traducir la distinción propuesta
                    String proposedDistinctionLabel = ModalityServiceUtils.translateProposedDistinction(sm.getAcademicDistinction());

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("studentModalityId", sm.getId());
                    row.put("studentId", leader.getId());
                    row.put("studentName", leader.getName() + " " + leader.getLastName());
                    row.put("studentEmail", leader.getEmail());
                    row.put("studentCode", leaderProfile != null ? leaderProfile.getStudentCode() : null);
                    row.put("modalityName", sm.getProgramDegreeModality().getDegreeModality().getName());
                    row.put("academicProgram", sm.getAcademicProgram().getName());
                    row.put("finalGrade", sm.getFinalGrade());
                    row.put("currentStatus", sm.getStatus().name());
                    row.put("proposedDistinction", sm.getAcademicDistinction() != null ? sm.getAcademicDistinction().name() : null);
                    row.put("proposedDistinctionLabel", proposedDistinctionLabel);
                    row.put("lastUpdatedAt", sm.getUpdatedAt());
                    row.put("examinerEvaluations", examinerDetails);
                    row.put("projectDirector", sm.getProjectDirector() != null
                            ? sm.getProjectDirector().getName() + " " + sm.getProjectDirector().getLastName()
                            : null);
                    return row;
                })
                .collect(Collectors.toList());

        return (Map.of(
                "success", true,
                "totalPending", result.size(),
                "pendingDistinctionProposals", result
        ));
    }

    /**
     * El Comité de Currículo ACEPTA la distinción honorífica propuesta por los jurados.
     * La modalidad pasa a GRADED_APPROVED con la distinción confirmada.
     *
     * @param studentModalityId ID de la modalidad
     * @param notes             Notas/observaciones del comité al aceptar (opcional)
     */
    @Transactional
    public Map<String, Object> acceptDistinctionProposal(Long studentModalityId, String notes) {
        User committeeMember = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        Long academicProgramId = studentModality.getProgramDegreeModality().getAcademicProgram().getId();

        boolean authorized = programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                committeeMember.getId(), academicProgramId, ProgramRole.PROGRAM_CURRICULUM_COMMITTEE);

        if (!authorized) {
            throw new ForbiddenException("No tiene permiso para revisar distinciones en este programa académico.");
        }

        if (studentModality.getStatus() != ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW) {
            throw new ValidationException("La modalidad no está en estado de revisión de distinción por el comité.");
        }

        // Convertir la distinción propuesta en la distinción definitiva aceptada
        AcademicDistinction proposedDistinction = studentModality.getAcademicDistinction();
        AcademicDistinction confirmedDistinction = resolveAcceptedDistinction(proposedDistinction);

        if (confirmedDistinction == null) {
            throw new ValidationException("No se puede determinar la distinción a confirmar. Estado de distinción inválido: " + proposedDistinction);
        }

        studentModality.setStatus(ModalityProcessStatus.GRADED_APPROVED);
        studentModality.setAcademicDistinction(confirmedDistinction);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        String observations = String.format(
                "El Comité de Currículo ACEPTÓ la distinción honorífica propuesta por los jurados. " +
                "Distinción propuesta: %s → Distinción confirmada: %s. %s",
                ModalityServiceUtils.translateAcademicDistinction(proposedDistinction),
                ModalityServiceUtils.translateAcademicDistinction(confirmedDistinction),
                notes != null && !notes.isBlank() ? "Observaciones del comité: " + notes : ""
        );

        historyRepository.save(ModalityProcessStatusHistory.builder()
                .studentModality(studentModality)
                .status(ModalityProcessStatus.GRADED_APPROVED)
                .changeDate(LocalDateTime.now())
                .responsible(committeeMember)
                .observations(observations)
                .build());

        // Notificar resultado final definitivo
        applicationEventPublisher.publishEvent(new ModalityEvent(NotificationType.DEFENSE_COMPLETED, studentModality.getId(), committeeMember.getId(), Map.of(
                ModalityEvent.KEY_FINAL_STATUS, ModalityProcessStatus.GRADED_APPROVED,
                ModalityEvent.KEY_ACADEMIC_DISTINCTION, confirmedDistinction,
                ModalityEvent.KEY_OBSERVATIONS, observations
        )));

        return (Map.of(
                "success", true,
                "studentModalityId", studentModalityId,
                "newStatus", ModalityProcessStatus.GRADED_APPROVED,
                "confirmedDistinction", confirmedDistinction,
                "message", "Distinción honorífica aceptada correctamente. La modalidad queda APROBADA con distinción " +
                        ModalityServiceUtils.translateProposedDistinction(confirmedDistinction) + "."
        ));
    }

    /**
     * El Comité de Currículo RECHAZA la distinción honorífica propuesta por los jurados.
     * La modalidad pasa a GRADED_APPROVED sin distinción especial (AGREED_APPROVED o TIEBREAKER_APPROVED).
     *
     * @param studentModalityId ID de la modalidad
     * @param reason            Razón del rechazo (obligatorio)
     */
    @Transactional
    public Map<String, Object> rejectDistinctionProposal(Long studentModalityId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Debe proporcionar una razón para rechazar la distinción propuesta.");
        }

        User committeeMember = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        Long academicProgramId = studentModality.getProgramDegreeModality().getAcademicProgram().getId();

        boolean authorized = programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                committeeMember.getId(), academicProgramId, ProgramRole.PROGRAM_CURRICULUM_COMMITTEE);

        if (!authorized) {
            throw new ForbiddenException("No tiene permiso para revisar distinciones en este programa académico.");
        }

        if (studentModality.getStatus() != ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW) {
            throw new ValidationException("La modalidad no está en estado de revisión de distinción por el comité.");
        }

        // Al rechazar, la distinción se convierte en aprobada sin mención especial
        AcademicDistinction proposedDistinction = studentModality.getAcademicDistinction();
        AcademicDistinction rejectedDistinction = resolveRejectedDistinction(proposedDistinction);

        studentModality.setStatus(ModalityProcessStatus.GRADED_APPROVED);
        studentModality.setAcademicDistinction(rejectedDistinction);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        String observations = String.format(
                "El Comité de Currículo RECHAZÓ la distinción honorífica propuesta por los jurados. " +
                "Distinción propuesta: %s → Distinción final: %s (sin mención especial). " +
                "Razón del rechazo: %s",
                ModalityServiceUtils.translateAcademicDistinction(proposedDistinction),
                ModalityServiceUtils.translateAcademicDistinction(rejectedDistinction),
                reason
        );

        historyRepository.save(ModalityProcessStatusHistory.builder()
                .studentModality(studentModality)
                .status(ModalityProcessStatus.GRADED_APPROVED)
                .changeDate(LocalDateTime.now())
                .responsible(committeeMember)
                .observations(observations)
                .build());

        // Notificar resultado final definitivo sin mención
        applicationEventPublisher.publishEvent(new ModalityEvent(NotificationType.DEFENSE_COMPLETED, studentModality.getId(), committeeMember.getId(), Map.of(
                ModalityEvent.KEY_FINAL_STATUS, ModalityProcessStatus.GRADED_APPROVED,
                ModalityEvent.KEY_ACADEMIC_DISTINCTION, rejectedDistinction,
                ModalityEvent.KEY_OBSERVATIONS, observations
        )));

        return (Map.of(
                "success", true,
                "studentModalityId", studentModalityId,
                "newStatus", ModalityProcessStatus.GRADED_APPROVED,
                "finalDistinction", rejectedDistinction,
                "reason", reason,
                "message", "Distinción honorífica rechazada. La modalidad queda APROBADA sin distinción especial."
        ));
    }

    /**
     * Resuelve cuál es la distinción definitiva al ACEPTAR la propuesta de los jurados.
     */
    private AcademicDistinction resolveAcceptedDistinction(AcademicDistinction proposed) {
        if (proposed == null) return null;
        return switch (proposed) {
            case PENDING_COMMITTEE_MERITORIOUS -> AcademicDistinction.AGREED_MERITORIOUS;
            case PENDING_COMMITTEE_LAUREATE -> AcademicDistinction.AGREED_LAUREATE;
            case TIEBREAKER_PENDING_COMMITTEE_MERITORIOUS -> AcademicDistinction.TIEBREAKER_MERITORIOUS;
            case TIEBREAKER_PENDING_COMMITTEE_LAUREATE -> AcademicDistinction.TIEBREAKER_LAUREATE;
            default -> null;
        };
    }

    /**
     * Resuelve cuál es la distinción definitiva al RECHAZAR la propuesta de los jurados.
     * La modalidad queda aprobada sin mención especial.
     */
    private AcademicDistinction resolveRejectedDistinction(AcademicDistinction proposed) {
        if (proposed == null) return AcademicDistinction.AGREED_APPROVED;
        return switch (proposed) {
            case TIEBREAKER_PENDING_COMMITTEE_MERITORIOUS, TIEBREAKER_PENDING_COMMITTEE_LAUREATE ->
                    AcademicDistinction.TIEBREAKER_APPROVED;
            default -> AcademicDistinction.AGREED_APPROVED;
        };
    }

    @Transactional
    public Map<String, Object> modalityReadyForDefenseByDirector(Long studentModalityId) {
        User projectDirector = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        // Validación de documentos subidos (excepto para "Emprendimiento y fortalecimiento de empresa")
        String modalidadNombre = studentModality.getProgramDegreeModality().getDegreeModality().getName();
        if (!modalidadNombre.equalsIgnoreCase("Emprendimiento y fortalecimiento de empresa")) {
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
        studentModality.setStatus(ModalityProcessStatus.PENDING_PROGRAM_HEAD_FINAL_REVIEW);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.PENDING_PROGRAM_HEAD_FINAL_REVIEW)
                        .changeDate(LocalDateTime.now())
                        .responsible(projectDirector)
                        .observations("Director de proyecto notificó a jefatura que los documentos finales están listos para revisión previa a la sustentación")
                        .build()
        );

        // Notificar a jefatura de programa (no a jurados - eso lo hará jefatura en el paso siguiente)
        applicationEventPublisher.publishEvent(
            new ModalityEvent(NotificationType.DIRECTOR_NOTIFIES_PROGRAM_HEAD_FINAL_REVIEW, studentModality.getId(), projectDirector.getId(), Map.of())
        );

        return (
                Map.of(
                        "success", true,
                        "studentModalityId", studentModalityId,
                        "newStatus", ModalityProcessStatus.PENDING_PROGRAM_HEAD_FINAL_REVIEW,
                        "message", "Jefatura de programa ha sido notificada para revisar los documentos finales. Una vez aprobados, jefatura notificará a los jurados."
                )
        );
    }

    /**
     * Método para que jefatura de programa apruebe los documentos finales y notifique a los jurados.
     * Paso intermedio entre la notificación del director y la revisión de jurados.
     */
    @Transactional
    public Map<String, Object> programHeadApprovesAndNotifiesExaminers(Long studentModalityId) {
        User programHead = SecurityUtils.getCurrentUser();

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
                modalityName.equalsIgnoreCase("Emprendimiento y fortalecimiento de empresa");

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
        studentModality.setStatus(ModalityProcessStatus.READY_FOR_DEFENSE);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.READY_FOR_DEFENSE)
                        .changeDate(LocalDateTime.now())
                        .responsible(programHead)
                        .observations("Jefatura de programa aprobó todos los documentos y notificó a los jurados para revisión de la sustentación")
                        .build()
        );

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

        return (
                Map.of(
                        "success", true,
                        "studentModalityId", studentModalityId,
                        "newStatus", ModalityProcessStatus.READY_FOR_DEFENSE,
                        "message", "Todos los documentos fueron validados. Jurados notificados para revisión de la sustentación."
                )
        );
    }

    @Transactional
    public Map<String, Object> examinerFinalReviewCompleted(Long studentModalityId) {
        User examiner = SecurityUtils.getCurrentUser();

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
        studentModality.setStatus(ModalityProcessStatus.FINAL_REVIEW_COMPLETED);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        // Registrar en historial
        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.FINAL_REVIEW_COMPLETED)
                        .changeDate(LocalDateTime.now())
                        .responsible(examiner)
                        .observations("Jurado finalizó la revisión de documentos. Modalidad lista para programación de sustentación.")
                        .build()
        );

        // Notificar al director de proyecto
        User projectDirector = studentModality.getProjectDirector();
        if (projectDirector != null) {
            applicationEventPublisher.publishEvent(
                    new ModalityEvent(NotificationType.EXAMINER_FINAL_REVIEW_COMPLETED, studentModality.getId(), projectDirector.getId(), Map.of(
                            ModalityEvent.KEY_PROJECT_DIRECTOR_ID, projectDirector.getId()
                    ))
            );
        }

        return (
                Map.of(
                        "success", true,
                        "studentModalityId", studentModalityId,
                        "newStatus", ModalityProcessStatus.FINAL_REVIEW_COMPLETED,
                        "message", "Revisión final completada por el jurado. Se notificó al director para programar la sustentación."
                )
        );
    }

    /**
     * Devuelve un calendario de próximas sustentaciones para el jurado autenticado.
     * Solo incluye modalidades en estado DEFENSE_SCHEDULED, ordenadas por fecha de defensa ascendente.
     */
    @Transactional(readOnly = true)
    public List<ModalityListDTO> getExaminerDefenseCalendar() {
        User examiner = SecurityUtils.getCurrentUser();

        // Buscar todas las modalidades asignadas al jurado en estado DEFENSE_SCHEDULED
        List<StudentModality> modalities = studentModalityRepository.findForExaminerWithStatus(
                examiner.getId(),
                List.of(ModalityProcessStatus.DEFENSE_SCHEDULED)
        );

        // Filtrar solo las que tienen fecha de defensa futura o igual a hoy
        LocalDateTime now = LocalDateTime.now();
        List<ModalityListDTO> calendar = modalities.stream()
                .filter(sm -> sm.getDefenseDate() != null && !sm.getDefenseDate().isBefore(now))
                .sorted(Comparator.comparing(StudentModality::getDefenseDate))
                .map(sm -> {
                    List<StudentModalityMember> activeMembers = studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                            sm.getId(), MemberStatus.ACTIVE);
                    String studentNames = activeMembers.stream()
                            .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName())
                            .collect(Collectors.joining(", "));
                    String studentEmails = activeMembers.stream()
                            .map(m -> m.getStudent().getEmail())
                            .collect(Collectors.joining(", "));
                    return ModalityListDTO.builder()
                            .studentModalityId(sm.getId())
                            .studentName(studentNames)
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

    @Transactional
    public Map<String, Object> getExaminerTypeForModality(Long studentModalityId) {
        User examiner = SecurityUtils.getCurrentUser();

        DefenseExaminer defenseExaminer = defenseExaminerRepository
                .findByStudentModalityIdAndExaminerId(studentModalityId, examiner.getId())
                .orElse(null);

        if (defenseExaminer == null) {
            throw new ForbiddenException("No está asignado como jurado a esta modalidad");
        }

        return (Map.of(
            "success", true,
            "examinerType", defenseExaminer.getExaminerType().name()
        ));
    }

    @Transactional
    public Map<String, Object> getExaminerEvaluationForModality(Long studentModalityId) {
        User examiner = SecurityUtils.getCurrentUser();

        DefenseExaminer defenseExaminer = defenseExaminerRepository
                .findByStudentModalityIdAndExaminerId(studentModalityId, examiner.getId())
                .orElse(null);

        if (defenseExaminer == null) {
            throw new ForbiddenException("No está asignado como jurado a esta modalidad");
        }

        DefenseEvaluationCriteria evaluation = defenseEvaluationCriteriaRepository
                .findByDefenseExaminerId(defenseExaminer.getId())
                .orElse(null);

        if (evaluation == null) {
            return (Map.of(
                "success", false,
                "message", "No ha registrado evaluación para esta modalidad"
            ));
        }

        ExaminerEvaluationDTO dto = ExaminerEvaluationDTO.builder()
                .grade(evaluation.getGrade())
                .observations(evaluation.getObservations())
                .evaluationDate(evaluation.getEvaluatedAt())
                .build();

        return (Map.of(
            "success", true,
            "evaluation", dto
        ));
    }
}
