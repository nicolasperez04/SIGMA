package com.SIGMA.USCO.Modalities.repository;

import com.SIGMA.USCO.Modalities.entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.entity.enums.ExaminerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DefenseExaminerRepository extends JpaRepository<DefenseExaminer, Long> {


    @Query("SELECT de FROM DefenseExaminer de " +
           "WHERE de.studentModality.id = :studentModalityId " +
           "ORDER BY de.examinerType ASC")
    List<DefenseExaminer> findByStudentModalityId(@Param("studentModalityId") Long studentModalityId);


    @Query("SELECT de FROM DefenseExaminer de " +
           "WHERE de.studentModality.id IN :studentModalityIds " +
           "ORDER BY de.examinerType ASC")
    List<DefenseExaminer> findByStudentModalityIdIn(@Param("studentModalityIds") List<Long> studentModalityIds);


    @Query("SELECT de FROM DefenseExaminer de " +
           "WHERE de.examiner.id = :examinerId " +
           "ORDER BY de.assignmentDate DESC")
    List<DefenseExaminer> findByExaminerId(@Param("examinerId") Long examinerId);


    @Query("SELECT de FROM DefenseExaminer de " +
           "WHERE de.examiner.id = :examinerId " +
           "AND de.examinerType = :examinerType " +
           "ORDER BY de.assignmentDate DESC")
    List<DefenseExaminer> findByExaminerIdAndType(
        @Param("examinerId") Long examinerId,
        @Param("examinerType") ExaminerType examinerType
    );


    boolean existsByStudentModalityIdAndExaminerType(Long studentModalityId, ExaminerType examinerType);


    boolean existsByStudentModalityIdAndExaminerId(Long studentModalityId, Long examinerId);


    long countByStudentModalityId(Long studentModalityId);


    @Query("SELECT de FROM DefenseExaminer de " +
           "WHERE de.studentModality.id = :studentModalityId " +
           "AND de.examinerType IN ('PRIMARY_EXAMINER_1', 'PRIMARY_EXAMINER_2') " +
           "ORDER BY de.examinerType ASC")
    List<DefenseExaminer> findPrimaryExaminersByStudentModalityId(@Param("studentModalityId") Long studentModalityId);


    @Query("SELECT de FROM DefenseExaminer de " +
           "WHERE de.studentModality.id = :studentModalityId " +
           "AND de.examinerType = 'TIEBREAKER_EXAMINER'")
    Optional<DefenseExaminer> findTiebreakerExaminerByStudentModalityId(@Param("studentModalityId") Long studentModalityId);


    Optional<DefenseExaminer> findByStudentModalityIdAndExaminerType(
        Long studentModalityId,
        ExaminerType examinerType
    );


    @Query("SELECT COUNT(de) FROM DefenseExaminer de " +
           "WHERE de.examiner.id = :examinerId " +
           "AND de.studentModality.status IN :statuses")
    long countPendingByExaminerIdAndStatuses(
        @Param("examinerId") Long examinerId,
        @Param("statuses") List<String> statuses
    );


    @Query("SELECT de FROM DefenseExaminer de " +
           "WHERE de.examiner.id = :examinerId " +
           "AND de.studentModality.status IN :statuses " +
           "ORDER BY de.assignmentDate DESC")
    List<DefenseExaminer> findByExaminerIdAndModalityStatuses(
        @Param("examinerId") Long examinerId,
        @Param("statuses") List<String> statuses
    );


    @Query("SELECT CASE WHEN COUNT(sm) > 0 THEN true ELSE false END " +
           "FROM StudentModality sm " +
           "WHERE sm.id = :studentModalityId " +
           "AND sm.projectDirector.id = :userId")
    boolean isProjectDirectorOfStudentModality(
        @Param("studentModalityId") Long studentModalityId,
        @Param("userId") Long userId
    );


    @Query("SELECT de FROM DefenseExaminer de " +
           "WHERE de.assignedBy.id = :assignedByUserId " +
           "ORDER BY de.assignmentDate DESC")
    List<DefenseExaminer> findByAssignedByUserId(@Param("assignedByUserId") Long assignedByUserId);


    @Query("SELECT de FROM DefenseExaminer de " +
           "WHERE de.studentModality.id = :studentModalityId " +
           "AND de.examiner.id = :examinerId")
    Optional<DefenseExaminer> findByStudentModalityIdAndExaminerId(
        @Param("studentModalityId") Long studentModalityId,
        @Param("examinerId") Long examinerId
    );
}

