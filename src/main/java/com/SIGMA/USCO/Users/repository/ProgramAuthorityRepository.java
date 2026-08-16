package com.SIGMA.USCO.Users.repository;

import com.SIGMA.USCO.Users.entity.ProgramAuthority;
import com.SIGMA.USCO.Users.entity.enums.ProgramRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgramAuthorityRepository extends JpaRepository<ProgramAuthority,Long> {

    boolean existsByUser_IdAndAcademicProgram_IdAndRole(
            Long userId,
            Long academicProgramId,
            ProgramRole role
    );

    List<ProgramAuthority> findByAcademicProgram_IdAndRole(
            Long academicProgramId,
            ProgramRole role
    );

    List<ProgramAuthority> findByUser_Id(Long userId);

    List<ProgramAuthority> findByRole(ProgramRole role);

    List<ProgramAuthority> findAllByUser_IdIn(List<Long> userIds);





    List<ProgramAuthority> findByUser_IdAndRole(Long userId, ProgramRole role);

    boolean existsByUser_IdAndAcademicProgram_Id(Long userId, Long academicProgramId);

    boolean existsByUser_IdAndAcademicProgram_IdAndRoleIn(Long id, Long academicProgramId, List<ProgramRole> programHead);
}
