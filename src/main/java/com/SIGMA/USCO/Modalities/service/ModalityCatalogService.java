package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.Entity.DegreeModality;
import com.SIGMA.USCO.Modalities.Entity.ModalityRequirements;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityStatus;
import com.SIGMA.USCO.Modalities.Repository.DegreeModalityRepository;
import com.SIGMA.USCO.Modalities.Repository.ModalityRequirementsRepository;
import com.SIGMA.USCO.Modalities.dto.ModalityDTO;
import com.SIGMA.USCO.Modalities.dto.RequirementDTO;
import com.SIGMA.USCO.Modalities.dto.response.ProjectDirectorResponse;
import com.SIGMA.USCO.Users.Entity.ProgramAuthority;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.Entity.enums.ProgramRole;
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
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.notifications.event.ModalityEvent;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.listeners.ExaminerNotificationListener;
import com.SIGMA.USCO.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import com.SIGMA.USCO.notifications.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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

    public DegreeModality createModality(ModalityDTO request) {

        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre de la modalidad es obligatorio.");
        }

        if (request.getFacultyId() == null) {
            throw new IllegalArgumentException("La facultad es obligatoria.");
        }

        Faculty faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() ->
                        new IllegalArgumentException("La facultad no existe.")
                );

        if (degreeModalityRepository.existsByNameIgnoreCaseAndFacultyId(request.getName(), faculty.getId())) {
            throw new IllegalArgumentException("Ya existe una modalidad con ese nombre en esta facultad.");
        }

        DegreeModality modality = DegreeModality.builder()
                .name(request.getName().toUpperCase())
                .description(request.getDescription())
                .status(ModalityStatus.ACTIVE)
                .faculty(faculty)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return degreeModalityRepository.save(modality);
    }

    public ResponseEntity<?> updateModality(Long modalityId, ModalityDTO request) {

        if (!degreeModalityRepository.existsById(modalityId)) {
            return ResponseEntity.badRequest().body("La modalidad con ID " + modalityId + " no existe.");
        }

        DegreeModality modality = degreeModalityRepository.findById(modalityId).orElseThrow();

        Faculty faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() ->
                        new IllegalArgumentException("La facultad no existe.")
                );

        modality.setFaculty(faculty);
        modality.setName(request.getName());
        modality.setDescription(request.getDescription());
        modality.setStatus(request.getStatus());
        modality.setUpdatedAt(LocalDateTime.now());

        degreeModalityRepository.save(modality);

        return ResponseEntity.ok("Modalidad actualizada exitosamente");
    }
    public ResponseEntity<?> desactiveModality(Long modalityId) {

        if (!degreeModalityRepository.existsById(modalityId)) {
            return ResponseEntity.badRequest().body("La modalidad con ID " + modalityId + " no existe.");
        }

        DegreeModality modality = degreeModalityRepository.findById(modalityId).orElseThrow();

        modality.setStatus(ModalityStatus.INACTIVE);
        modality.setUpdatedAt(LocalDateTime.now());

        degreeModalityRepository.save(modality);

        return ResponseEntity.ok("Modalidad desactivada exitosamente");
    }
    public void createModalityRequirements(Long modalityId, List<RequirementDTO> requirements) {

        if (requirements == null || requirements.isEmpty()) {
            throw new IllegalArgumentException("La lista de requisitos no puede estar vacía.");
        }

        DegreeModality modality = degreeModalityRepository.findById(modalityId)
                .orElseThrow(() -> new IllegalArgumentException("La modalidad con ID " + modalityId + " no existe."));

        for (RequirementDTO req : requirements) {

            if (req.getRequirementName() == null || req.getRequirementName().isBlank()) {throw new IllegalArgumentException("El nombre del requisito es obligatorio.");}

            if (req.getRuleType() == null) {
                throw new IllegalArgumentException("El tipo de regla es obligatorio para el requisito: " + req.getRequirementName());
            }

            if (req.getExpectedValue() == null || req.getExpectedValue().isBlank()) {
                throw new IllegalArgumentException("El valor esperado es obligatorio para el requisito: " + req.getRequirementName());
            }

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
                        new IllegalArgumentException("La modalidad con ID " + modalityId + " no existe.")
                );

        ModalityRequirements requirement = modalityRequirementsRepository.findById(requirementId)
                .orElseThrow(() ->
                        new IllegalArgumentException("El requisito con ID " + requirementId + " no existe.")
                );

        if (!requirement.getModality().getId().equals(modality.getId())) {
            throw new IllegalArgumentException(
                    "El requisito no pertenece a la modalidad indicada."
            );
        }

        if (req.getRequirementName() != null) {
            if (req.getRequirementName().isBlank()) {
                throw new IllegalArgumentException("El nombre del requisito no puede estar vacío.");
            }
            requirement.setRequirementName(req.getRequirementName());
        }

        if (req.getDescription() != null) {
            requirement.setDescription(req.getDescription());
        }

        if (req.getRuleType() != null) {
            requirement.setRuleType(req.getRuleType());
        }

        if (req.getExpectedValue() != null) {
            if (req.getExpectedValue().isBlank()) {
                throw new IllegalArgumentException("El valor esperado no puede estar vacío.");
            }
            requirement.setExpectedValue(req.getExpectedValue());
        }

        requirement.setUpdatedAt(LocalDateTime.now());

        modalityRequirementsRepository.save(requirement);
    }

    public ResponseEntity<List<RequirementDTO>> getModalityRequirements(Long modalityId, Boolean active) {

        if (!degreeModalityRepository.existsById(modalityId)) {
            return ResponseEntity.badRequest().body(List.of());
        }

        List<ModalityRequirements> requirements;

        if (active != null) {
            requirements = modalityRequirementsRepository.findByModalityIdAndActive(modalityId, active);
        } else {
            requirements = modalityRequirementsRepository.findByModalityId(modalityId);
        }

        List<RequirementDTO> response = requirements.stream()
                .map(r -> RequirementDTO.builder()
                        .id(r.getId())
                        .requirementName(r.getRequirementName())
                        .description(r.getDescription())
                        .ruleType(r.getRuleType())
                        .expectedValue(r.getExpectedValue())
                        .active(r.isActive())
                        .build())
                .toList();

        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> deleteRequirement(Long requirementId) {

        ModalityRequirements requirement = modalityRequirementsRepository.findById(requirementId)
                .orElseThrow(() -> new RuntimeException("Requisito no encontrado"));

        requirement.setActive(false);
        requirement.setUpdatedAt(LocalDateTime.now());

        modalityRequirementsRepository.save(requirement);

        return ResponseEntity.ok("Requisito desactivado correctamente");
    }

    public ResponseEntity<?> activeRequirement (Long requirementId){
        ModalityRequirements requirement = modalityRequirementsRepository.findById(requirementId)
                .orElseThrow(() -> new RuntimeException("Requisito no encontrado"));

        requirement.setActive(true);
        requirement.setUpdatedAt(LocalDateTime.now());

        modalityRequirementsRepository.save(requirement);

        return ResponseEntity.ok("Requisito activado correctamente");

    }

    public ResponseEntity<List<ModalityDTO>> getAllModalities() {

        User user = SecurityUtils.getCurrentUser();

        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil académico no encontrado"));

        Long userProgramId = profile.getAcademicProgram().getId();

        List<DegreeModality> modalities = degreeModalityRepository.findByStatus(ModalityStatus.ACTIVE);

        List<ModalityDTO> modalityDTOs = modalities.stream().map(mod -> {

            Optional<ProgramDegreeModality> pdmOpt = programDegreeModalityRepository
                    .findByAcademicProgramIdAndDegreeModalityIdAndActiveTrue(userProgramId, mod.getId());

            Long creditsRequired = null;
            if (pdmOpt.isPresent() && pdmOpt.get().getCreditsRequired() != null) {
                creditsRequired = pdmOpt.get().getCreditsRequired();
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

        return ResponseEntity.ok(modalityDTOs);
    }

    public ResponseEntity<?> getModalityDetail(Long modalityId) {

        if (!degreeModalityRepository.existsById(modalityId)) {
            return ResponseEntity.badRequest().body("La modalidad con ID " + modalityId + " no existe.");
        }

        User user = SecurityUtils.getCurrentUser();

        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil académico no encontrado"));

        Long userProgramId = profile.getAcademicProgram().getId();

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
                .map(doc -> RequiredDocumentDTO.builder()
                        .id(doc.getId())
                        .modalityId(modalityId)
                        .documentName(doc.getDocumentName())
                        .description(doc.getDescription())
                        .allowedFormat(doc.getAllowedFormat())
                        .maxFileSizeMB(doc.getMaxFileSizeMB())
                        .documentType(doc.getDocumentType())
                        .build())
                .toList();

        DegreeModality modality = degreeModalityRepository.findById(modalityId).orElseThrow();

        ModalityDTO modalityDetail = ModalityDTO.builder()
                .id(modalityId)
                .name(modality.getName())
                .description(modality.getDescription())
                .facultyId(modality.getFaculty().getId())
                .facultyName(modality.getFaculty().getName())
                .requiredCredits(creditsRequired != null ? creditsRequired.doubleValue() : null)
                .requirements(requirements)
                .documents(documents)
                .build();

        return ResponseEntity.ok(modalityDetail);

    }

    public List<ProjectDirectorResponse> getProjectDirectors() {

        User currentUser = SecurityUtils.getCurrentUser();

        List<ProgramAuthority> committeeAuthorities = programAuthorityRepository
                .findByUser_IdAndRole(currentUser.getId(), ProgramRole.PROGRAM_CURRICULUM_COMMITTEE);

        if (committeeAuthorities.isEmpty()) {
            throw new RuntimeException("El usuario no tiene el rol de PROGRAM_CURRICULUM_COMMITTEE");
        }

        Set<Long> userProgramIds = committeeAuthorities.stream()
                .map(authority -> authority.getAcademicProgram().getId())
                .collect(Collectors.toSet());

        List<com.SIGMA.USCO.Users.Entity.ProgramAuthority> projectDirectorAuthorities = programAuthorityRepository
                .findByAcademicProgram_IdAndRole(userProgramIds.iterator().next(),
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
                .collect(Collectors.toList());
    }

    public List<ProjectDirectorResponse> getProgramHeads() {

        User currentUser = SecurityUtils.getCurrentUser();

        List<ProgramAuthority> committeeAuthorities = programAuthorityRepository
                .findByUser_IdAndRole(currentUser.getId(), ProgramRole.PROGRAM_CURRICULUM_COMMITTEE);

        if (committeeAuthorities.isEmpty()) {
            throw new RuntimeException("El usuario no tiene el rol de PROGRAM_CURRICULUM_COMMITTEE");
        }

        Set<Long> userProgramIds = committeeAuthorities.stream()
                .map(authority -> authority.getAcademicProgram().getId())
                .collect(Collectors.toSet());

        List<com.SIGMA.USCO.Users.Entity.ProgramAuthority> programHeadAuthorities = programAuthorityRepository
                .findByAcademicProgram_IdAndRole(userProgramIds.iterator().next(),
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
                .collect(Collectors.toList());
    }

    public List<ProjectDirectorResponse> getProgramCurriculumCommittee(Long academicProgramId, Long facultyId) {

        List<ProgramAuthority> committeeAuthorities = programAuthorityRepository.findAll()
                .stream()
                .filter(authority -> authority.getRole() == ProgramRole.PROGRAM_CURRICULUM_COMMITTEE)
                .toList();

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
                .collect(Collectors.toList());
    }

    public List<ProjectDirectorResponse> getExaminers(Long academicProgramId, Long facultyId) {

        List<User> examiners = userRepository.findAll()
                .stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName().equals("EXAMINER")))
                .toList();

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
                    .collect(Collectors.toList());
        }

        return examiners.stream()
                .map(examiner -> new ProjectDirectorResponse(
                        examiner.getId(),
                        examiner.getName(),
                        examiner.getLastName(),
                        examiner.getEmail()
                ))
                .distinct()
                .collect(Collectors.toList());
    }

    public List<ProjectDirectorResponse> getExaminersForCommittee() {

        User currentUser = SecurityUtils.getCurrentUser();

        List<ProgramAuthority> committeeAuthorities = programAuthorityRepository
                .findByUser_IdAndRole(currentUser.getId(), ProgramRole.PROGRAM_CURRICULUM_COMMITTEE);

        if (committeeAuthorities.isEmpty()) {
            throw new RuntimeException("El usuario no tiene el rol de PROGRAM_CURRICULUM_COMMITTEE");
        }

        Set<Long> userProgramIds = committeeAuthorities.stream()
                .map(authority -> authority.getAcademicProgram().getId())
                .collect(Collectors.toSet());

        List<User> allExaminers = userRepository.findAll()
                .stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName().equals("EXAMINER")))
                .toList();

        List<ProgramAuthority> examinerAuthorities = new ArrayList<>();

        for (Long programId : userProgramIds) {
            List<ProgramAuthority> programExaminers = programAuthorityRepository.findAll()
                    .stream()
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
                .collect(Collectors.toList());
    }
}

