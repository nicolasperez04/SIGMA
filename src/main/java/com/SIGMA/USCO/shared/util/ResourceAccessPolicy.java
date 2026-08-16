package com.SIGMA.USCO.shared.util;

import com.SIGMA.USCO.Modalities.entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.repository.DefenseExaminerRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ResourceAccessPolicy {

    private final ProgramAuthorityRepository programAuthorityRepository;
    private final DefenseExaminerRepository defenseExaminerRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;

    public void requireProgramAuthority(User user, Long academicProgramId, ProgramRole role, String message) {
        boolean authorized = programAuthorityRepository
                .existsByUser_IdAndAcademicProgram_IdAndRole(user.getId(), academicProgramId, role);
        if (!authorized) {
            throw new ForbiddenException(message);
        }
    }

    public void requireProgramAuthorityIn(User user, Long academicProgramId, List<ProgramRole> roles, String message) {
        boolean authorized = programAuthorityRepository
                .existsByUser_IdAndAcademicProgram_IdAndRoleIn(user.getId(), academicProgramId, roles);
        if (!authorized) {
            throw new ForbiddenException(message);
        }
    }

    public void requireProjectDirector(StudentModality modality, User user, String message) {
        boolean isDirector = modality.getProjectDirector() != null
                && modality.getProjectDirector().getId().equals(user.getId());
        if (!isDirector) {
            throw new ForbiddenException(message);
        }
    }

    public DefenseExaminer requireAssignedExaminer(Long studentModalityId, User user, String message) {
        return defenseExaminerRepository
                .findByStudentModalityIdAndExaminerId(studentModalityId, user.getId())
                .orElseThrow(() -> new ForbiddenException(message));
    }

    public void requireLeader(StudentModality modality, User user, String message) {
        boolean isLeader = modality.getLeader() != null
                && modality.getLeader().getId().equals(user.getId());
        if (!isLeader) {
            throw new ForbiddenException(message);
        }
    }

    public void requireActiveMember(Long studentModalityId, User user, String message) {
        boolean active = studentModalityMemberRepository.isActiveMember(studentModalityId, user.getId());
        if (!active) {
            throw new ForbiddenException(message);
        }
    }

    /** Ejecuta un check capturando ForbiddenException; true si pasó, false si no. Permite OR de permisos. */
    public boolean tryRequire(Runnable check) {
        try {
            check.run();
            return true;
        } catch (ForbiddenException e) {
            return false;
        }
    }
}