package com.SIGMA.USCO.academic.repository;

import com.SIGMA.USCO.academic.entity.AcademicProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AcademicProgramRepository extends JpaRepository<AcademicProgram, Long> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<AcademicProgram> findByActiveTrue();

    List<AcademicProgram> findByFaculty_IdAndActiveTrue(Long facultyId);

    @Query("SELECT p FROM AcademicProgram p WHERE p.active = true AND p.faculty.active = true ORDER BY p.faculty.id, p.id")
    List<AcademicProgram> findActiveProgramsWithActiveFaculty();
}
