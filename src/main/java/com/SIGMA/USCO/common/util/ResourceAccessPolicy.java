package com.SIGMA.USCO.common.util;

import com.SIGMA.USCO.Modalities.Entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Repository.DefenseExaminerRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.Entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
}
