package com.SIGMA.USCO.Users.service;

import com.SIGMA.USCO.Users.Entity.Permission;
import com.SIGMA.USCO.Users.Entity.ProgramAuthority;
import com.SIGMA.USCO.Users.Entity.Role;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.Entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.Entity.enums.Status;
import com.SIGMA.USCO.Users.dto.request.PermissionDTO;
import com.SIGMA.USCO.Users.dto.request.RegisterUserByAdminRequest;
import com.SIGMA.USCO.Users.dto.request.RoleRequest;
import com.SIGMA.USCO.Users.dto.request.UpdateUserRequest;
import com.SIGMA.USCO.Users.dto.response.RegisterUserResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;
    private final AcademicProgramRepository academicProgramRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorityAssignmentService authorityAssignmentService;


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

        List<StudentProfile> studentProfiles =
                userIds.isEmpty() ? List.of() : studentProfileRepository.findAllByUserIdIn(userIds);
        List<ProgramAuthority> allAuthorities =
                userIds.isEmpty() ? List.of() : programAuthorityRepository.findAllByUser_IdIn(userIds);

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
                List<ProgramAuthority> authorities = authoritiesByUser.getOrDefault(user.getId(), List.of());

                if (authorities.isEmpty()) {
                    userResponses.add(
                        toUserResponse(user, null, null)
                    );
                } else {
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

        if (isExaminer) {
            List<Long> programIds = request.getAcademicProgramIds();

            if ((programIds == null || programIds.isEmpty()) && request.getAcademicProgramId() != null) {
                programIds = List.of(request.getAcademicProgramId());
            }

            if (programIds == null || programIds.isEmpty()) {
                return RegisterUserResponse.builder()
                        .success(true)
                        .message("Usuario registrado exitosamente con el rol EXAMINER sin programas asociados. " +
                                 "Puede asociarlo a programas académicos posteriormente.")
                        .userId(user.getId())
                        .examinerName(user.getName() + " " + user.getLastName())
                        .build();
            }

            AuthorityAssignmentService.AssignmentResult result =
                    authorityAssignmentService.assignExaminerToPrograms(user, programIds);

            return RegisterUserResponse.builder()
                    .success(true)
                    .message("Usuario registrado exitosamente con el rol EXAMINER")
                    .userId(user.getId())
                    .examinerName(user.getName() + " " + user.getLastName())
                    .examinerEmail(user.getEmail())
                    .programsAssigned(result.assigned())
                    .programsSkipped(result.skipped())
                    .totalAssigned(result.assigned().size())
                    .totalSkipped(result.skipped().size())
                    .build();
        }

        if (requiresProgram) {
            AcademicProgram program = academicProgramRepository.findById(request.getAcademicProgramId())
                    .orElseThrow(() -> new NotFoundException("Programa académico no encontrado"));

            ProgramRole programRole = switch (roleName) {
                case "PROGRAM_HEAD"                   -> ProgramRole.PROGRAM_HEAD;
                case "PROJECT_DIRECTOR"               -> ProgramRole.PROJECT_DIRECTOR;
                case "PROGRAM_CURRICULUM_COMMITTEE"   -> ProgramRole.PROGRAM_CURRICULUM_COMMITTEE;
                default -> throw new ValidationException("Rol de programa no válido");
            };

            authorityAssignmentService.assignAuthorityChecked(user, program, roleName, programRole);

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
