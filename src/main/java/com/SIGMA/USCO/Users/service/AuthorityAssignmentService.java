package com.SIGMA.USCO.Users.service;

import com.SIGMA.USCO.Users.Entity.ProgramAuthority;
import com.SIGMA.USCO.Users.Entity.Role;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.Entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.dto.request.AssignExaminerMultipleProgramsRequest;
import com.SIGMA.USCO.Users.dto.request.assignAuthorityProgram;
import com.SIGMA.USCO.Users.dto.response.ExaminerAssignmentResponse;
import com.SIGMA.USCO.Users.dto.response.ExaminerProgramItem;
import com.SIGMA.USCO.Users.dto.response.ExaminerProgramsResponse;
import com.SIGMA.USCO.Users.dto.response.MultipleAssignmentResponse;
import com.SIGMA.USCO.Users.dto.response.ProgramAssignmentItem;
import com.SIGMA.USCO.Users.dto.response.SkippedProgramItem;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.Users.repository.RoleRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.repository.AcademicProgramRepository;
import com.SIGMA.USCO.common.exception.ConflictException;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthorityAssignmentService {

    private final UserRepository userRepository;
    private final AcademicProgramRepository academicProgramRepository;
    private final RoleRepository roleRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;

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

    @Transactional
    public void assignAuthority(Long userId, Long programId, String roleName, ProgramRole programRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        AcademicProgram program = academicProgramRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Programa académico no encontrado"));

        assignAuthorityChecked(user, program, roleName, programRole);
    }

    @Transactional
    public void assignAuthorityChecked(User user, AcademicProgram program, String roleName, ProgramRole programRole) {
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

        AssignmentResult result = assignExaminerToPrograms(user, request.getAcademicProgramIds());

        return MultipleAssignmentResponse.builder()
                .success(true)
                .examinerId(user.getId())
                .examinerName(user.getName() + " " + user.getLastName())
                .examinerEmail(user.getEmail())
                .programsAssigned(result.assigned())
                .programsSkipped(result.skipped())
                .totalAssigned(result.assigned().size())
                .totalSkipped(result.skipped().size())
                .build();
    }

    /**
     * Asocia al usuario EXAMINER dado a cada programa de la lista.
     * Los programas inexistentes o donde ya esté asociado se omiten (no generan error).
     */
    public AssignmentResult assignExaminerToPrograms(User user, List<Long> programIds) {

        List<ProgramAssignmentItem> assigned = new ArrayList<>();
        List<SkippedProgramItem> skipped = new ArrayList<>();

        for (Long programId : programIds) {

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

        return new AssignmentResult(assigned, skipped);
    }

    public record AssignmentResult(List<ProgramAssignmentItem> assigned, List<SkippedProgramItem> skipped) {}

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

}
