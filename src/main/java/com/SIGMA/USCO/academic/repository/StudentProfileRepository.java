package com.SIGMA.USCO.academic.repository;


import com.SIGMA.USCO.academic.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByUserId(Long userId);

    List<StudentProfile> findByAcademicProgramId(Long academicProgramId);

    List<StudentProfile> findAllByUserIdIn(List<Long> userIds);
}
