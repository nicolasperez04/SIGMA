package com.SIGMA.USCO.academic.repository;

import com.SIGMA.USCO.academic.entity.ProgramDegreeModality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProgramDegreeModalityRepository extends JpaRepository<ProgramDegreeModality, Long> {

    boolean existsByAcademicProgramIdAndDegreeModalityId(Long academicProgramId, Long degreeModalityId);

    Optional<ProgramDegreeModality> findByAcademicProgramIdAndDegreeModalityIdAndActiveTrue(Long academicProgramId, Long degreeModalityId);

    List<ProgramDegreeModality> findByAcademicProgramIdAndActiveTrue(Long academicProgramId);

    @Query("SELECT pdm FROM ProgramDegreeModality pdm WHERE " +
            "(:active IS NULL OR pdm.active = :active) AND " +
            "(:degreeModalityId IS NULL OR pdm.degreeModality.id = :degreeModalityId) AND " +
            "(:facultyId IS NULL OR pdm.academicProgram.faculty.id = :facultyId) AND " +
            "(:academicProgramId IS NULL OR pdm.academicProgram.id = :academicProgramId)")
    List<ProgramDegreeModality> findByFilters(
            @Param("active") Boolean active,
            @Param("degreeModalityId") Long degreeModalityId,
            @Param("facultyId") Long facultyId,
            @Param("academicProgramId") Long academicProgramId
    );
}
