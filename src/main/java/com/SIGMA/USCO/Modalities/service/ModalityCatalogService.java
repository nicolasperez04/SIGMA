package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.entity.DegreeModality;
import com.SIGMA.USCO.Modalities.entity.ModalityRequirements;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityStatus;
import com.SIGMA.USCO.Modalities.repository.DegreeModalityRepository;
import com.SIGMA.USCO.Modalities.repository.ModalityRequirementsRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Modalities.dto.ModalityDTO;
import com.SIGMA.USCO.Modalities.dto.RequirementDTO;
import com.SIGMA.USCO.Modalities.dto.response.ProjectDirectorResponse;
import com.SIGMA.USCO.Users.entity.ProgramAuthority;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.academic.entity.Faculty;
import com.SIGMA.USCO.academic.entity.ProgramDegreeModality;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.FacultyRepository;
import com.SIGMA.USCO.academic.repository.ProgramDegreeModalityRepository;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.documents.dto.RequiredDocumentDTO;
import com.SIGMA.USCO.documents.entity.enums.DocumentEditRequestStatus;
import com.SIGMA.USCO.documents.entity.enums.FinalDocumentRubricType;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import com.SIGMA.USCO.documents.entity.enums.EditRequestVoteDecision;
import com.SIGMA.USCO.documents.entity.enums.ExaminerDocumentDecision;
import com.SIGMA.USCO.documents.repository.RequiredDocumentRepository;
import com.SIGMA.USCO.common.exception.ConflictException;
import com.SIGMA.USCO.common.exception.ForbiddenException;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.common.exception.ValidationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModalityCatalogService {

    private final DegreeModalityRepository degreeModalityRepository;
    private final ModalityRequirementsRepository modalityRequirementsRepository;
    private final RequiredDocumentRepository requiredDocumentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final FacultyRepository facultyRepository;
    private final ProgramDegreeModalityRepository programDegreeModalityRepository;
    private final UserRepository userRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;
    private final StudentModalityRepository studentModalityRepository;

    @Transactional
    public ModalityDTO createModality(ModalityDTO request) {

        Faculty faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() ->
                        new NotFoundException("La facultad no existe.")
                );

        if (degreeModalityRepository.existsByNameIgnoreCaseAndFacultyId(request.getName(), faculty.getId())) {
            throw new ConflictException("Ya existe una modalidad con ese nombre en esta facultad.");
        }

        DegreeModality modality = DegreeModality.builder()
                .name(request.getName().toUpperCase())
                .description(request.getDescription())
                .status(ModalityStatus.ACTIVE)
                .faculty(faculty)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        degreeModalityRepository.save(modality);

        return toModalityDTO(modality);
    }

    private ModalityDTO toModalityDTO(DegreeModality modality) {
        return ModalityDTO.builder()
                .id(modality.getId())
                .facultyId(modality.getFaculty() != null ? modality.getFaculty().getId() : null)
                .facultyName(modality.getFaculty() != null ? modality.getFaculty().getName() : null)
                .name(modality.getName())
                .description(modality.getDescription())
                .status(modality.getStatus())
                .build();
    }

    public String updateModality(Long modalityId, ModalityDTO request) {

        DegreeModality modality = degreeModalityRepository.findById(modalityId)
                .orElseThrow(() -> new NotFoundException("La modalidad con ID " + modalityId + " no existe."));

        Faculty faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() ->
                        new NotFoundException("La facultad no existe.")
                );

        if (request.getStatus() == ModalityStatus.INACTIVE) {
            validateModalityNotInUse(modalityId);
        }

        modality.setFaculty(faculty);
        modality.setName(request.getName());
        modality.setDescription(request.getDescription());
        modality.setStatus(request.getStatus());
        modality.setUpdatedAt(LocalDateTime.now());

        degreeModalityRepository.save(modality);

        return "Modalidad actualizada exitosamente";
    }
    public String desactiveModality(Long modalityId) {

        DegreeModality modality = degreeModalityRepository.findById(modalityId)
                .orElseThrow(() -> new NotFoundException("La modalidad con ID " + modalityId + " no existe."));

        validateModalityNotInUse(modalityId);

        modality.setStatus(ModalityStatus.INACTIVE);
        modality.setUpdatedAt(LocalDateTime.now());

        degreeModalityRepository.save(modality);

        return "Modalidad desactivada exitosamente";
    }

    private void validateModalityNotInUse(Long degreeModalityId) {
        // ponytail: "en uso" = existe StudentModality con esa DegreeModality en un estado NO terminal;
        // GRADED_APPROVED / MODALITY_CLOSED / MODALITY_CANCELLED se consideran historial cerrado.
        boolean inUse = studentModalityRepository.existsByProgramDegreeModality_DegreeModalityIdAndStatusNotIn(
                degreeModalityId,
                List.of(ModalityProcessStatus.GRADED_APPROVED,
                        ModalityProcessStatus.MODALITY_CLOSED,
                        ModalityProcessStatus.MODALITY_CANCELLED));
        if (inUse) {
            throw new ValidationException("No se puede desactivar la modalidad porque tiene estudiantes en proceso.");
        }
    }
    @Transactional
    public void createModalityRequirements(Long modalityId, List<RequirementDTO> requirements) {

        if (requirements == null || requirements.isEmpty()) {
            throw new ValidationException("La lista de requisitos no puede estar vacía.");
        }

        DegreeModality modality = degreeModalityRepository.findById(modalityId)
                .orElseThrow(() -> new NotFoundException("La modalidad con ID " + modalityId + " no existe."));

        for (RequirementDTO req : requirements) {

            ModalityRequirements requirement = ModalityRequirements.builder()
                    .modality(modality)
                    .requirementName(req.getRequirementName())
                    .description(req.getDescription())
                    .ruleType(req.getRuleType())
                    .expectedValue(req.getExpectedValue())
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            modalityRequirementsRepository.save(requirement);
        }
    }

    public void updateModalityRequirement(Long modalityId, Long requirementId, RequirementDTO req) {

        DegreeModality modality = degreeModalityRepository.findById(modalityId)
                .orElseThrow(() ->
                        new NotFoundException("La modalidad con ID " + modalityId + " no existe.")
                );

        ModalityRequirements requirement = modalityRequirementsRepository.findById(requirementId)
                .orElseThrow(() ->
                        new NotFoundException("El requisito con ID " + requirementId + " no existe.")
                );

        if (!requirement.getModality().getId().equals(modality.getId())) {
            throw new ValidationException(
                    "El requisito no pertenece a la modalidad indicada."
            );
        }

        requirement.setRequirementName(req.getRequirementName());

        if (req.getDescription() != null) {
            requirement.setDescription(req.getDescription());
        }

        requirement.setRuleType(req.getRuleType());

        requirement.setExpectedValue(req.getExpectedValue());

        requirement.setUpdatedAt(LocalDateTime.now());

        modalityRequirementsRepository.save(requirement);
    }

    @Transactional(readOnly = true)
    public List<RequirementDTO> getModalityRequirements(Long modalityId, Boolean active) {

        if (!degreeModalityRepository.existsById(modalityId)) {
            throw new NotFoundException("La modalidad con ID " + modalityId + " no existe.");
        }

        List<ModalityRequirements> requirements;

        if (active != null) {
            requirements = modalityRequirementsRepository.findByModalityIdAndActive(modalityId, active);
        } else {
            requirements = modalityRequirementsRepository.findByModalityId(modalityId);
        }

        return requirements.stream()
                .map(r -> RequirementDTO.builder()
                        .id(r.getId())
                        .requirementName(r.getRequirementName())
                        .description(r.getDescription())
                        .ruleType(r.getRuleType())
                        .expectedValue(r.getExpectedValue())
                        .active(r.isActive())
                        .build())
                .toList();
    }
    public String deleteRequirement(Long requirementId) {

        ModalityRequirements requirement = modalityRequirementsRepository.findById(requirementId)
                .orElseThrow(() -> new NotFoundException("Requisito no encontrado"));

        requirement.setActive(false);
        requirement.setUpdatedAt(LocalDateTime.now());

        modalityRequirementsRepository.save(requirement);

        return "Requisito desactivado correctamente";
    }

    public String activeRequirement (Long requirementId){
        ModalityRequirements requirement = modalityRequirementsRepository.findById(requirementId)
                .orElseThrow(() -> new NotFoundException("Requisito no encontrado"));

        requirement.setActive(true);
        requirement.setUpdatedAt(LocalDateTime.now());

        modalityRequirementsRepository.save(requirement);

        return "Requisito activado correctamente";

    }

    @Transactional(readOnly = true)
    public List<ModalityDTO> getAllModalities(User user) {

        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Perfil académico no encontrado"));

        Long userProgramId = profile.getAcademicProgram().getId();

        List<DegreeModality> modalities = degreeModalityRepository.findByStatus(ModalityStatus.ACTIVE);

        Map<Long, ProgramDegreeModality> pdmByModalityId = programDegreeModalityRepository
                .findByAcademicProgramIdAndActiveTrue(userProgramId)
                .stream()
                .collect(Collectors.toMap(pdm -> pdm.getDegreeModality().getId(), pdm -> pdm));

        return modalities.stream().map(mod -> {

            ProgramDegreeModality pdm = pdmByModalityId.get(mod.getId());

            Long creditsRequired = null;
            if (pdm != null && pdm.getCreditsRequired() != null) {
                creditsRequired = pdm.getCreditsRequired();
            }

            return ModalityDTO.builder()
                    .id(mod.getId())
                    .name(mod.getName())
                    .facultyName(mod.getFaculty().getName())
                    .description(mod.getDescription())
                    .facultyId(mod.getFaculty().getId())
                    .status(mod.getStatus())
                    .requiredCredits(creditsRequired != null ? creditsRequired.doubleValue() : null)
                    .build();

        }).toList();
    }

    @Transactional(readOnly = true)
    public ModalityDTO getModalityDetail(Long modalityId, User user) {

        if (!degreeModalityRepository.existsById(modalityId)) {
            throw new NotFoundException("La modalidad con ID " + modalityId + " no existe.");
        }

        Optional<StudentProfile> profileOpt = studentProfileRepository.findByUserId(user.getId());
        Long userProgramId;
        if (profileOpt.isPresent()) {
            userProgramId = profileOpt.get().getAcademicProgram().getId();
        } else {
            List<ProgramAuthority> authorities = programAuthorityRepository.findByUser_Id(user.getId());
            if (authorities.isEmpty()) {
                throw new ForbiddenException("No tienes un programa asignado para consultar esta modalidad");
            }
            Set<Long> programIds = authorities.stream()
                    .map(authority -> authority.getAcademicProgram().getId())
                    .collect(Collectors.toSet());
            userProgramId = resolveContextProgram(programIds);
        }

        Optional<ProgramDegreeModality> pdmOpt = programDegreeModalityRepository
                .findByAcademicProgramIdAndDegreeModalityIdAndActiveTrue(userProgramId, modalityId);

        Long creditsRequired = null;
        if (pdmOpt.isPresent() && pdmOpt.get().getCreditsRequired() != null) {
            creditsRequired = pdmOpt.get().getCreditsRequired();
        }

        var requirements = modalityRequirementsRepository.findByModalityIdAndActiveTrue(modalityId)
                .stream()
                .map(req -> RequirementDTO.builder()
                        .id(req.getId())
                        .requirementName(req.getRequirementName())
                        .description(req.getDescription())
                        .expectedValue(req.getExpectedValue())
                        .ruleType(req.getRuleType())
                        .build())
                .toList();

        var documents = requiredDocumentRepository
                .findByModalityIdAndActiveTrueAndDocumentType(modalityId, DocumentType.MANDATORY)
                .stream()
                .map(RequiredDocumentDTO::from)
                .toList();

        DegreeModality modality = degreeModalityRepository.findById(modalityId).orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        return ModalityDTO.builder()
                .id(modalityId)
                .name(modality.getName())
                .description(modality.getDescription())
                .facultyId(modality.getFaculty().getId())
                .facultyName(modality.getFaculty().getName())
                .requiredCredits(creditsRequired != null ? creditsRequired.doubleValue() : null)
                .requirements(requirements)
                .documents(documents)
                .build();

    }

    @Transactional(readOnly = true)
    public List<ProjectDirectorResponse> getProjectDirectors(User currentUser) {

        List<ProgramAuthority> committeeAuthorities = programAuthorityRepository
                .findByUser_IdAndRole(currentUser.getId(), ProgramRole.PROGRAM_CURRICULUM_COMMITTEE);

        if (committeeAuthorities.isEmpty()) {
            throw new ForbiddenException("El usuario no tiene el rol de PROGRAM_CURRICULUM_COMMITTEE");
        }

        Set<Long> userProgramIds = committeeAuthorities.stream()
                .map(authority -> authority.getAcademicProgram().getId())
                .collect(Collectors.toSet());

        List<com.SIGMA.USCO.Users.entity.ProgramAuthority> projectDirectorAuthorities = programAuthorityRepository
                .findByAcademicProgram_IdAndRole(resolveContextProgram(userProgramIds),
                        ProgramRole.PROJECT_DIRECTOR
                );

        return projectDirectorAuthorities.stream()
                .map(authority -> new ProjectDirectorResponse(
                        authority.getUser().getId(),
                        authority.getUser().getName(),
                        authority.getUser().getLastName(),
                        authority.getUser().getEmail()
                ))
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectDirectorResponse> getProgramHeads(User currentUser) {

        List<ProgramAuthority> committeeAuthorities = programAuthorityRepository
                .findByUser_IdAndRole(currentUser.getId(), ProgramRole.PROGRAM_CURRICULUM_COMMITTEE);

        if (committeeAuthorities.isEmpty()) {
            throw new ForbiddenException("El usuario no tiene el rol de PROGRAM_CURRICULUM_COMMITTEE");
        }

        Set<Long> userProgramIds = committeeAuthorities.stream()
                .map(authority -> authority.getAcademicProgram().getId())
                .collect(Collectors.toSet());

        List<com.SIGMA.USCO.Users.entity.ProgramAuthority> programHeadAuthorities = programAuthorityRepository
                .findByAcademicProgram_IdAndRole(resolveContextProgram(userProgramIds),
                        ProgramRole.PROGRAM_HEAD
                );

        return programHeadAuthorities.stream()
                .map(authority -> new ProjectDirectorResponse(
                        authority.getUser().getId(),
                        authority.getUser().getName(),
                        authority.getUser().getLastName(),
                        authority.getUser().getEmail()
                ))
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectDirectorResponse> getProgramCurriculumCommittee(Long academicProgramId, Long facultyId, User user) {

        if (academicProgramId != null && !programAuthorityRepository.existsByUser_IdAndAcademicProgram_Id(
                user.getId(), academicProgramId)) {
            throw new ForbiddenException("No tienes autoridad sobre este programa académico");
        }

        List<ProgramAuthority> committeeAuthorities = programAuthorityRepository
                .findByRole(ProgramRole.PROGRAM_CURRICULUM_COMMITTEE);

        if (academicProgramId != null) {
            committeeAuthorities = committeeAuthorities.stream()
                    .filter(authority -> authority.getAcademicProgram().getId().equals(academicProgramId))
                    .toList();
        }

        if (facultyId != null) {
            committeeAuthorities = committeeAuthorities.stream()
                    .filter(authority -> authority.getAcademicProgram().getFaculty().getId().equals(facultyId))
                    .toList();
        }

        return committeeAuthorities.stream()
                .map(authority -> new ProjectDirectorResponse(
                        authority.getUser().getId(),
                        authority.getUser().getName(),
                        authority.getUser().getLastName(),
                        authority.getUser().getEmail()
                ))
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectDirectorResponse> getExaminers(Long academicProgramId, Long facultyId, User user) {

        if (academicProgramId != null && !programAuthorityRepository.existsByUser_IdAndAcademicProgram_Id(
                user.getId(), academicProgramId)) {
            throw new ForbiddenException("No tienes autoridad sobre este programa académico");
        }

        List<User> examiners = userRepository.findAllExaminers();

        if (academicProgramId != null || facultyId != null) {
            List<ProgramAuthority> examinerAuthorities = programAuthorityRepository.findAll()
                    .stream()
                    .filter(authority -> examiners.stream()
                            .anyMatch(examiner -> examiner.getId().equals(authority.getUser().getId())))
                    .toList();

            if (academicProgramId != null) {
                examinerAuthorities = examinerAuthorities.stream()
                        .filter(authority -> authority.getAcademicProgram().getId().equals(academicProgramId))
                        .toList();
            }

            if (facultyId != null) {
                examinerAuthorities = examinerAuthorities.stream()
                        .filter(authority -> authority.getAcademicProgram().getFaculty().getId().equals(facultyId))
                        .toList();
            }

            return examinerAuthorities.stream()
                    .map(authority -> new ProjectDirectorResponse(
                            authority.getUser().getId(),
                            authority.getUser().getName(),
                            authority.getUser().getLastName(),
                            authority.getUser().getEmail()
                    ))
                    .distinct()
                    .toList();
        }

        return examiners.stream()
                .map(examiner -> new ProjectDirectorResponse(
                        examiner.getId(),
                        examiner.getName(),
                        examiner.getLastName(),
                        examiner.getEmail()
                ))
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectDirectorResponse> getExaminersForCommittee(User currentUser) {

        List<ProgramAuthority> committeeAuthorities = programAuthorityRepository
                .findByUser_IdAndRole(currentUser.getId(), ProgramRole.PROGRAM_CURRICULUM_COMMITTEE);

        if (committeeAuthorities.isEmpty()) {
            throw new ForbiddenException("El usuario no tiene el rol de PROGRAM_CURRICULUM_COMMITTEE");
        }

        Set<Long> userProgramIds = committeeAuthorities.stream()
                .map(authority -> authority.getAcademicProgram().getId())
                .collect(Collectors.toSet());

        List<User> allExaminers = userRepository.findAllExaminers();

        List<ProgramAuthority> allAuthorities = programAuthorityRepository.findAll();

        List<ProgramAuthority> examinerAuthorities = new ArrayList<>();

        for (Long programId : userProgramIds) {
            List<ProgramAuthority> programExaminers = allAuthorities.stream()
                    .filter(authority -> authority.getAcademicProgram().getId().equals(programId))
                    .filter(authority -> allExaminers.stream()
                            .anyMatch(examiner -> examiner.getId().equals(authority.getUser().getId())))
                    .toList();

            examinerAuthorities.addAll(programExaminers);
        }

        return examinerAuthorities.stream()
                .map(authority -> new ProjectDirectorResponse(
                        authority.getUser().getId(),
                        authority.getUser().getName(),
                        authority.getUser().getLastName(),
                        authority.getUser().getEmail()
                ))
                .distinct()
                .toList();
    }

    /**
     * Programa de contexto del comité autenticado: único si solo pertenece a uno;
     * con varios, el de menor id (determinista).
     */
    private Long resolveContextProgram(Set<Long> programIds) {
        if (programIds.size() == 1) {
            return programIds.iterator().next();
        }
        // ponytail: contrato sin programa de contexto → primero determinista (antes arbitrario)
        return programIds.stream().sorted().findFirst().orElseThrow();
    }
}

