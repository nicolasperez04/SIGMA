package com.SIGMA.USCO.Users.service;

import com.SIGMA.USCO.Modalities.Entity.DegreeModality;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityStatus;
import com.SIGMA.USCO.Modalities.Repository.DegreeModalityRepository;
import com.SIGMA.USCO.Modalities.Repository.ModalityRequirementsRepository;
import com.SIGMA.USCO.Modalities.dto.ModalityDTO;
import com.SIGMA.USCO.Modalities.dto.RequirementDTO;
import com.SIGMA.USCO.Users.Entity.Permission;
import com.SIGMA.USCO.Users.Entity.ProgramAuthority;
import com.SIGMA.USCO.Users.Entity.Role;
import com.SIGMA.USCO.Users.Entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.Entity.enums.Status;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.dto.request.AssignExaminerMultipleProgramsRequest;
import com.SIGMA.USCO.Users.dto.request.assignAuthorityProgram;
import com.SIGMA.USCO.Users.dto.request.PermissionDTO;
import com.SIGMA.USCO.Users.dto.request.RegisterUserByAdminRequest;
import com.SIGMA.USCO.Users.dto.request.RoleRequest;
import com.SIGMA.USCO.Users.dto.request.UpdateUserRequest;
import com.SIGMA.USCO.Users.dto.response.ExaminerAssignmentResponse;
import com.SIGMA.USCO.Users.dto.response.ExaminerProgramItem;
import com.SIGMA.USCO.Users.dto.response.ExaminerProgramsResponse;
import com.SIGMA.USCO.Users.dto.response.MultipleAssignmentResponse;
import com.SIGMA.USCO.Users.dto.response.ProgramAssignmentItem;
import com.SIGMA.USCO.Users.dto.response.RegisterUserResponse;
import com.SIGMA.USCO.Users.dto.response.SkippedProgramItem;
import com.SIGMA.USCO.Users.dto.response.UserResponse;
import com.SIGMA.USCO.Users.repository.PermissionRepository;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.Users.repository.RoleRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.AcademicProgramRepository;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.common.exception.ConflictException;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.common.web.PaginatedResponse;
import com.SIGMA.USCO.documents.dto.RequiredDocumentDTO;
import com.SIGMA.USCO.documents.repository.RequiredDocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class AdminService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final DegreeModalityRepository degreeModalityRepository;
    private final ModalityRequirementsRepository modalityRequirementsRepository;
    private final RequiredDocumentRepository requiredDocumentRepository;
    private final AcademicProgramRepository academicProgramRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PasswordEncoder passwordEncoder;


    @Transactional(readOnly = true)
    public List<RoleRequest> getRoles() {

        return roleRepository.findAll().stream().map(role -> RoleRequest.builder()
                                .id(role.getId())
                                .name(role.getName())
                                .permissionIds(
                                        role.getPermissions()
                                                .stream()
                                                .map(Permission::getId)
                                                .collect(Collectors.toSet()))
                                .build()
                        )
                        .toList();
    }

    public void createRole(RoleRequest request) {

        if (roleRepository.findByName(request.getName()).isPresent()) {
            throw new ConflictException("El rol ya existe.");
        }

        Set<Permission> permissions = Set.of();

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            permissions = permissionRepository.findAllById(request.getPermissionIds())
                    .stream().collect(Collectors.toSet());
        }

        Role role = Role.builder()
                .name(request.getName().toUpperCase())
                .permissions(permissions)
                .build();

        roleRepository.save(role);
    }

    @Transactional
    public void updateRole(Long id, RoleRequest request){
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Rol no encontrado"));

        Optional<Role> existingRole = roleRepository.findByNameIgnoreCase(request.getName());

        if (existingRole.isPresent() && !existingRole.get().getId().equals(id)) {
            throw new ConflictException("El rol ya existe.");
        }


        Set<Permission> permissions = Set.of();

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            permissions = permissionRepository.findAllById(request.getPermissionIds())
                    .stream().collect(Collectors.toSet());
        }

        role.setName(request.getName().toUpperCase());
        role.setPermissions(permissions);

        roleRepository.save(role);
    }

    @Transactional
    public void assignRoleToUser(UpdateUserRequest request){

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new NotFoundException("Rol no encontrado"));

        user.getRoles().clear();
        user.getRoles().add(role);
        user.setLastUpdateDate(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void changeUserStatus(UpdateUserRequest request){

        if (request.getStatus() == null) {
            throw new ValidationException("El estado debe ser ACTIVE o INACTIVE.");
        }

        Status newStatus = Status.valueOf(request.getStatus().toUpperCase());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        user.setStatus(newStatus);
        user.setLastUpdateDate(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void createPermission (PermissionDTO request){

        if (permissionRepository.findByName(request.getName()).isPresent()) {
            throw new ConflictException("El permiso ya existe.");
        }

        Permission permission = Permission.builder()
                .name(request.getName().toUpperCase())
                .build();

        permissionRepository.save(permission);
    }

    @Transactional(readOnly = true)
    public List<PermissionDTO> getPermissions() {

        return permissionRepository.findAll().stream().map(permission -> PermissionDTO.builder()
                                .id(permission.getId())
                                .name(permission.getName())
                                .build()
                        )
                        .toList();
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<UserResponse> getUsers(String status, String role, Long academicProgramId, Long facultyId, 
                                               String name, String lastName, String email, int page, int size) {

        Status userStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                userStatus = Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Estado inválido. Use ACTIVE o INACTIVE");
            }
        }

        // Filtros se ejecutan en la consulta para reducir el volumen traído desde BD
        Page<User> usersPage = userRepository.findUsersByFilters(
                userStatus,
                (role == null || role.isBlank()) ? null : role,
                academicProgramId,
                facultyId,
                (name == null || name.isBlank()) ? null : name,
                (lastName == null || lastName.isBlank()) ? null : lastName,
                (email == null || email.isBlank()) ? null : email,
                PageRequest.of(page, size)
        );

        List<User> users = usersPage.getContent();

        List<Long> userIds = users.stream().map(User::getId).toList();

        // Cargar perfiles y autoridades de los usuarios de la página en 2 consultas (evita N+1)
        List<StudentProfile> studentProfiles =
                userIds.isEmpty() ? List.of() : studentProfileRepository.findAllByUserIdIn(userIds);
        List<ProgramAuthority> allAuthorities =
                userIds.isEmpty() ? List.of() : 
programAuthorityRepository.findAllByUser_IdIn(userIds);

        Map<Long, StudentProfile> studentProfileByUser = studentProfiles.stream()
                .collect(Collectors.toMap(sp -> sp.getUser().getId(), sp -> sp, (a, b) -> a));
        Map<Long, List<ProgramAuthority>> authoritiesByUser = allAuthorities.stream()
                .collect(Collectors.groupingBy(pa -> pa.getUser().getId()));

        List<UserResponse> userResponses = new ArrayList<>();

        for (User user : users) {
            StudentProfile sp = studentProfileByUser.get(user.getId());

            if (sp != null) {
                userResponses.add(
                    toUserResponse(
                            user,
                            sp.getFaculty().getName(),
                            sp.getAcademicProgram().getName()
                    )
                );
            } else {
                // Si no es estudiante, usar sus ProgramAuthority
                List<ProgramAuthority> authorities = authoritiesByUser.getOrDefault(user.getId(), List.of());

                if (authorities.isEmpty()) {
                    // Usuario sin perfil de estudiante ni autoridades: mostrar sin facultad/programa
                    userResponses.add(
                        toUserResponse(user, null, null)
                    );
                } else {
                    // Crear un UserResponse para CADA autoridad
                    for (ProgramAuthority authority : authorities) {
                        userResponses.add(
                            toUserResponse(
                                    user,
                                    authority.getAcademicProgram().getFaculty().getName(),
                                    authority.getAcademicProgram().getName()
                            )
                        );
                    }
                }
            }
        }

        return PaginatedResponse.of(userResponses, page, size, usersPage.getTotalElements());
    }

    private UserResponse toUserResponse(User user, String faculty, String academicProgram) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .lastname(user.getLastName())
                .email(user.getEmail())
                .status(user.getStatus())
                .roles(
                        user.getRoles().stream()
                                .map(Role::getName)
                                .collect(Collectors.toSet())
                )
                .faculty(faculty)
                .academicProgram(academicProgram)
                .createdDate(user.getCreationDate())
                .build();
    }

    @Transactional
    public void desactiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (user.getStatus() == Status.INACTIVE) {
            throw new ValidationException("El usuario ya está inactivo.");
        }

        user.setStatus(Status.INACTIVE);
        user.setLastUpdateDate(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void assignProgramHead(assignAuthorityProgram request){
        assignAuthority(request.getUserId(), request.getAcademicProgramId(), "PROGRAM_HEAD", ProgramRole.PROGRAM_HEAD);
    }

    @Transactional
    public void assignProjectDirector(assignAuthorityProgram request){
        assignAuthority(request.getUserId(), request.getAcademicProgramId(), "PROJECT_DIRECTOR", ProgramRole.PROJECT_DIRECTOR);
    }

    @Transactional
    public void assignCommittee(assignAuthorityProgram request){
        assignAuthority(request.getUserId(), request.getAcademicProgramId(), "PROGRAM_CURRICULUM_COMMITTEE", ProgramRole.PROGRAM_CURRICULUM_COMMITTEE);
    }

    @Transactional
    public void assignExaminer(assignAuthorityProgram request){
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        AcademicProgram program = academicProgramRepository.findById(request.getAcademicProgramId())
                .orElseThrow(() -> new NotFoundException("Programa académico no encontrado"));

        boolean alreadyAssigned = programAuthorityRepository
                .existsByUser_IdAndAcademicProgram_IdAndRole(user.getId(), program.getId(), ProgramRole.EXAMINER);
        if (alreadyAssigned) {
            throw new ConflictException("El jurado ya está asociado a este programa académico");
        }

        assignAuthorityChecked(user, program, "EXAMINER", ProgramRole.EXAMINER);
    }

    private void assignAuthority(Long userId, Long programId, String roleName, ProgramRole programRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        AcademicProgram program = academicProgramRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Programa académico no encontrado"));

        assignAuthorityChecked(user, program, roleName, programRole);
    }

    private void assignAuthorityChecked(User user, AcademicProgram program, String roleName, ProgramRole programRole) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new NotFoundException("Rol " + roleName + " no encontrado"));

        if (!user.getRoles().contains(role)) {
            user.getRoles().add(role);
            userRepository.save(user);
        }

        ProgramAuthority authority = ProgramAuthority.builder()
                .user(user)
                .academicProgram(program)
                .role(programRole)
                .build();

        programAuthorityRepository.save(authority);
    }

    /**
     * Asocia un jurado (EXAMINER) existente a un programa académico adicional.
     * Un jurado puede estar vinculado a múltiples programas académicos.
     */
    @Transactional
    public ExaminerAssignmentResponse assignExaminerToAdditionalProgram(assignAuthorityProgram request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        boolean hasExaminerRole = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals("EXAMINER"));
        if (!hasExaminerRole) {
            throw new ValidationException("El usuario no tiene el rol EXAMINER");
        }

        AcademicProgram program = academicProgramRepository.findById(request.getAcademicProgramId())
                .orElseThrow(() -> new NotFoundException("Programa académico no encontrado"));

        boolean alreadyAssigned = programAuthorityRepository
                .existsByUser_IdAndAcademicProgram_IdAndRole(user.getId(), program.getId(), ProgramRole.EXAMINER);
        if (alreadyAssigned) {
            throw new ValidationException("El jurado ya está asociado al programa: " + program.getName());
        }

        assignAuthorityChecked(user, program, "EXAMINER", ProgramRole.EXAMINER);

        return ExaminerAssignmentResponse.builder()
                .success(true)
                .message("Jurado vinculado correctamente al programa: " + program.getName())
                .examinerName(user.getName() + " " + user.getLastName())
                .programName(program.getName())
                .build();
    }

    /**
     * Desvincula un jurado (EXAMINER) de un programa académico específico.
     */
    @Transactional
    public ExaminerAssignmentResponse removeExaminerFromProgram(Long userId, Long academicProgramId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        AcademicProgram program = academicProgramRepository.findById(academicProgramId)
                .orElseThrow(() -> new NotFoundException("Programa académico no encontrado"));

        List<ProgramAuthority> authorities = programAuthorityRepository
                .findByAcademicProgram_IdAndRole(academicProgramId, ProgramRole.EXAMINER)
                .stream()
                .filter(a -> a.getUser().getId().equals(userId))
                .toList();

        if (authorities.isEmpty()) {
            throw new ValidationException("El jurado no está asociado al programa: " + program.getName());
        }

        programAuthorityRepository.deleteAll(authorities);

        return ExaminerAssignmentResponse.builder()
                .success(true)
                .message("Jurado desvinculado correctamente del programa: " + program.getName())
                .examinerName(user.getName() + " " + user.getLastName())
                .programName(program.getName())
                .build();
    }

    /**
     * Asocia un usuario con el rol EXAMINER a múltiples programas académicos en una sola operación.
     * Si el usuario aún no tiene el rol EXAMINER, se lo asigna automáticamente.
     * Los programas donde ya esté asociado se omiten (no generan error).
     */
    @Transactional
    public MultipleAssignmentResponse assignExaminerToMultiplePrograms(AssignExaminerMultipleProgramsRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Role examinerRole = roleRepository.findByName("EXAMINER")
                .orElseThrow(() -> new NotFoundException("Rol EXAMINER no encontrado"));

        // Asignar el rol EXAMINER si el usuario aún no lo tiene
        if (user.getRoles().stream().noneMatch(r -> r.getName().equals("EXAMINER"))) {
            user.getRoles().add(examinerRole);
            userRepository.save(user);
        }

        List<ProgramAssignmentItem> assigned = new ArrayList<>();
        List<SkippedProgramItem> skipped = new ArrayList<>();

        for (Long programId : request.getAcademicProgramIds()) {

            AcademicProgram program = academicProgramRepository.findById(programId)
                    .orElse(null);

            if (program == null) {
                skipped.add(SkippedProgramItem.builder()
                        .academicProgramId(programId)
                        .reason("Programa académico no encontrado")
                        .build());
                continue;
            }

            boolean alreadyAssigned = programAuthorityRepository
                    .existsByUser_IdAndAcademicProgram_IdAndRole(user.getId(), programId, ProgramRole.EXAMINER);

            if (alreadyAssigned) {
                skipped.add(SkippedProgramItem.builder()
                        .academicProgramId(programId)
                        .academicProgramName(program.getName())
                        .reason("El jurado ya estaba asociado a este programa")
                        .build());
                continue;
            }

            assignAuthorityChecked(user, program, "EXAMINER", ProgramRole.EXAMINER);

            assigned.add(ProgramAssignmentItem.builder()
                    .academicProgramId(program.getId())
                    .academicProgramName(program.getName())
                    .facultyName(program.getFaculty().getName())
                    .build());
        }

        return MultipleAssignmentResponse.builder()
                .success(true)
                .examinerId(user.getId())
                .examinerName(user.getName() + " " + user.getLastName())
                .examinerEmail(user.getEmail())
                .programsAssigned(assigned)
                .programsSkipped(skipped)
                .totalAssigned(assigned.size())
                .totalSkipped(skipped.size())
                .build();
    }

    /**
     * Retorna todos los programas académicos a los que está asociado un jurado.
     */
    @Transactional(readOnly = true)
    public ExaminerProgramsResponse getExaminerPrograms(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        boolean hasExaminerRole = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals("EXAMINER"));
        if (!hasExaminerRole) {
            throw new ValidationException("El usuario no tiene el rol EXAMINER");
        }

        List<ProgramAuthority> authorities = programAuthorityRepository
                .findByUser_IdAndRole(userId, ProgramRole.EXAMINER);

        List<ExaminerProgramItem> programs = authorities.stream()
                .map(a -> ExaminerProgramItem.builder()
                        .programAuthorityId(a.getId())
                        .academicProgramId(a.getAcademicProgram().getId())
                        .academicProgramName(a.getAcademicProgram().getName())
                        .facultyId(a.getAcademicProgram().getFaculty().getId())
                        .facultyName(a.getAcademicProgram().getFaculty().getName())
                        .build())
                .toList();

        return ExaminerProgramsResponse.builder()
                .success(true)
                .examinerId(user.getId())
                .examinerName(user.getName() + " " + user.getLastName())
                .examinerEmail(user.getEmail())
                .programs(programs)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ModalityDTO> getModalities(ModalityStatus status) {

        List<DegreeModality> modalities;

        if (status != null) {
            modalities = degreeModalityRepository.findByStatus(status);
        } else {
            modalities = degreeModalityRepository.findAll();
        }

        List<ModalityDTO> response = modalities.stream()
                .map(mod -> ModalityDTO.builder()
                        .id(mod.getId())
                        .name(mod.getName())
                        .description(mod.getDescription())
                        .status(mod.getStatus())


                        .facultyId(mod.getFaculty().getId())
                        .facultyName(mod.getFaculty().getName())


                        .requirements(
                                modalityRequirementsRepository.findByModalityId(mod.getId())
                                        .stream()
                                        .map(req -> RequirementDTO.builder()
                                                .id(req.getId())
                                                .requirementName(req.getRequirementName())
                                                .description(req.getDescription())
                                                .ruleType(req.getRuleType())
                                                .expectedValue(req.getExpectedValue())
                                                .active(req.isActive())
                                                .build())
                                        .toList()
                        )


                        .documents(
                                requiredDocumentRepository.findByModalityId(mod.getId())
                                        .stream()
                                        .map(doc -> RequiredDocumentDTO.builder()
                                                .id(doc.getId())
                                                .modalityId( doc.getModality().getId())
                                                .documentName(doc.getDocumentName())
                                                .description(doc.getDescription())
                                                .allowedFormat(doc.getAllowedFormat())
                                                .maxFileSizeMB(doc.getMaxFileSizeMB())
                                                .documentType(doc.getDocumentType())
                                                .active(doc.isActive())
                                                .requiresProposalEvaluation(doc.isRequiresProposalEvaluation())
                                                .build())
                                        .toList()
                        )

                        .build()
                )
                .toList();

        return response;
    }

    @Transactional
    public RegisterUserResponse registerUserByAdmin(RegisterUserByAdminRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (!email.endsWith("@usco.edu.co")) {
            throw new ValidationException("El correo debe ser institucional con dominio @usco.edu.co");
        }

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Este correo ya está registrado en el sistema");
        }

        String roleName = request.getRoleName().toUpperCase();
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new NotFoundException("El rol " + roleName + " no existe en el sistema"));

        boolean requiresProgram = roleName.equals("PROGRAM_HEAD") ||
                roleName.equals("PROJECT_DIRECTOR") ||
                roleName.equals("PROGRAM_CURRICULUM_COMMITTEE");

        boolean isExaminer = roleName.equals("EXAMINER");

        if (requiresProgram && request.getAcademicProgramId() == null) {
            throw new ValidationException("El rol " + roleName + " requiere que se especifique un programa académico");
        }

        // Crear y guardar el usuario
        User user = User.builder()
                .name(request.getName())
                .lastName(request.getLastName())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(role))
                .status(Status.ACTIVE)
                .creationDate(LocalDateTime.now())
                .lastUpdateDate(LocalDateTime.now())
                .build();

        userRepository.save(user);

        // ── EXAMINER: asociar a múltiples programas ──────────────────────────
        if (isExaminer) {
            List<Long> programIds = request.getAcademicProgramIds();

            // Compatibilidad: si no envían la lista pero sí el id singular, usarlo
            if ((programIds == null || programIds.isEmpty()) && request.getAcademicProgramId() != null) {
                programIds = List.of(request.getAcademicProgramId());
            }

            if (programIds == null || programIds.isEmpty()) {
                // Sin programas: el jurado se registra sin asociación de programa
                return RegisterUserResponse.builder()
                        .success(true)
                        .message("Usuario registrado exitosamente con el rol EXAMINER sin programas asociados. " +
                                 "Puede asociarlo a programas académicos posteriormente.")
                        .userId(user.getId())
                        .examinerName(user.getName() + " " + user.getLastName())
                        .build();
            }

            List<ProgramAssignmentItem> assigned = new ArrayList<>();
            List<SkippedProgramItem> skipped  = new ArrayList<>();

            for (Long programId : programIds) {
                AcademicProgram program = academicProgramRepository.findById(programId).orElse(null);

                if (program == null) {
                    skipped.add(SkippedProgramItem.builder()
                            .academicProgramId(programId)
                            .reason("Programa académico no encontrado")
                            .build());
                    continue;
                }

                boolean alreadyAssigned = programAuthorityRepository
                        .existsByUser_IdAndAcademicProgram_IdAndRole(user.getId(), programId, ProgramRole.EXAMINER);

                if (alreadyAssigned) {
                    skipped.add(SkippedProgramItem.builder()
                            .academicProgramId(programId)
                            .academicProgramName(program.getName())
                            .reason("El jurado ya estaba asociado a este programa")
                            .build());
                    continue;
                }

                assignAuthority(user.getId(), program.getId(), "EXAMINER", ProgramRole.EXAMINER);

                assigned.add(ProgramAssignmentItem.builder()
                        .academicProgramId(program.getId())
                        .academicProgramName(program.getName())
                        .facultyName(program.getFaculty().getName())
                        .build());
            }

            return RegisterUserResponse.builder()
                    .success(true)
                    .message("Usuario registrado exitosamente con el rol EXAMINER")
                    .userId(user.getId())
                    .examinerName(user.getName() + " " + user.getLastName())
                    .examinerEmail(user.getEmail())
                    .programsAssigned(assigned)
                    .programsSkipped(skipped)
                    .totalAssigned(assigned.size())
                    .totalSkipped(skipped.size())
                    .build();
        }

        // ── Otros roles: un solo programa obligatorio ────────────────────────
        if (requiresProgram) {
            AcademicProgram program = academicProgramRepository.findById(request.getAcademicProgramId())
                    .orElseThrow(() -> new NotFoundException("Programa académico no encontrado"));

            ProgramRole programRole = switch (roleName) {
                case "PROGRAM_HEAD"                   -> ProgramRole.PROGRAM_HEAD;
                case "PROJECT_DIRECTOR"               -> ProgramRole.PROJECT_DIRECTOR;
                case "PROGRAM_CURRICULUM_COMMITTEE"   -> ProgramRole.PROGRAM_CURRICULUM_COMMITTEE;
                default -> throw new ValidationException("Rol de programa no válido");
            };

            assignAuthority(user.getId(), request.getAcademicProgramId(), roleName, programRole);

            return RegisterUserResponse.builder()
                    .success(true)
                    .message("Usuario registrado exitosamente con el rol " + roleName +
                             " y asignado al programa académico: " + program.getName())
                    .userId(user.getId())
                    .build();
        }

        return RegisterUserResponse.builder()
                .success(true)
                .message("Usuario registrado exitosamente con el rol " + roleName)
                .userId(user.getId())
                .build();
    }

}
