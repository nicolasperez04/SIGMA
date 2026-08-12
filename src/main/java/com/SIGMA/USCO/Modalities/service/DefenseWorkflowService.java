package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.Entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Entity.enums.ExaminerType;
import com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.DefenseExaminerRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Modalities.dto.ModalityListDTO;
import com.SIGMA.USCO.Modalities.dto.DefenseProposalDTO;
import com.SIGMA.USCO.Modalities.dto.ScheduleDefenseDTO;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.Entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;
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
import com.SIGMA.USCO.common.util.TranslationUtils;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ModalityStatusTransition modalityStatusTransition;

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
        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.DEFENSE_SCHEDULED, projectDirector,
                "Director de proyecto programó la sustentación para el "
                        + request.getDefenseDate()
                        + " en "
                        + request.getDefenseLocation());
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

        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.DEFENSE_SCHEDULED, committeeMember,
                String.format(
                        "Comité de currículo aprobó la propuesta del director de proyecto. " +
                        "Sustentación programada para el %s en %s",
                        approvedDate,
                        approvedLocation
                ));

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

        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.DEFENSE_SCHEDULED, committeeMember, observation);

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
            examinerAssignmentMessages.add(TranslationUtils.translateExaminerType(defenseExaminer.getExaminerType()) + ": " + examiner1.getName() + " " + examiner1.getLastName());
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
            examinerAssignmentMessages.add(TranslationUtils.translateExaminerType(defenseExaminer.getExaminerType()) + ": " + examiner2.getName() + " " + examiner2.getLastName());
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
            examinerAssignmentMessages.add(TranslationUtils.translateExaminerType(defenseExaminer.getExaminerType()) + ": " + examiner3.getName() + " " + examiner3.getLastName());
        }

        String observationMessage = "Jurados asignados por el comité de currículo:\n" +
                String.join("\n", examinerAssignmentMessages);

        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.EXAMINERS_ASSIGNED, committeeMember, observationMessage);

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
        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.PENDING_PROGRAM_HEAD_FINAL_REVIEW, projectDirector,
                "Director de proyecto notificó a jefatura que los documentos finales están listos para revisión previa a la sustentación");

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

}
