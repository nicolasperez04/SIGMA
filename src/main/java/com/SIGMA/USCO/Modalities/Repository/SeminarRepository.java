package com.SIGMA.USCO.Modalities.Repository;

import com.SIGMA.USCO.Modalities.Entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeminarRepository extends JpaRepository<Seminar, Long> {

    List<Seminar> findByAcademicProgramId(Long academicProgramId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Seminar s WHERE LOWER(s.name) = LOWER(:name) AND s.academicProgram.id = :programId")
    boolean existsByNameIgnoreCaseAndAcademicProgramId(@Param("name") String name, @Param("programId") Long programId);

    @Query("SELECT s FROM Seminar s WHERE s.active = true AND s.currentParticipants < s.maxParticipants AND s.academicProgram.id = :programId")
    List<Seminar> findActiveWithAvailableSeatsByProgram(@Param("programId") Long programId);

    @Query("SELECT s FROM Seminar s WHERE s.active = true AND s.currentParticipants < s.maxParticipants")
    List<Seminar> findAllActiveWithAvailableSeats();

    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM seminar_students WHERE seminar_id = :seminarId AND student_id = :studentId", nativeQuery = true)
    Integer isStudentEnrolledNative(@Param("seminarId") Long seminarId, @Param("studentId") Long studentId);

    default boolean isStudentEnrolled(Long seminarId, Long studentId) {
        Integer result = isStudentEnrolledNative(seminarId, studentId);
        return result != null && result > 0;
    }

    @Modifying
    @Query(value = "INSERT INTO seminar_students (seminar_id, student_id) VALUES (:seminarId, :studentId)", nativeQuery = true)
    void enrollStudent(@Param("seminarId") Long seminarId, @Param("studentId") Long studentId);

    @Query(value = """
        SELECT sp.* FROM student_profiles sp
        WHERE sp.user_id IN (
            SELECT ss.student_id FROM seminar_students ss WHERE ss.seminar_id = :seminarId
        )
    """, nativeQuery = true)
    List<com.SIGMA.USCO.academic.entity.StudentProfile> findEnrolledStudentsBySeminarId(@Param("seminarId") Long seminarId);

    List<Seminar> findByAcademicProgramIdOrderByCreatedAtDesc(Long academicProgramId);

    List<Seminar> findByAcademicProgramIdAndStatusOrderByCreatedAtDesc(Long academicProgramId, com.SIGMA.USCO.Modalities.Entity.enums.SeminarStatus status);

    List<Seminar> findByAcademicProgramIdAndActiveOrderByCreatedAtDesc(Long academicProgramId, boolean active);

    List<Seminar> findByAcademicProgramIdAndStatusAndActiveOrderByCreatedAtDesc(
            Long academicProgramId,
            com.SIGMA.USCO.Modalities.Entity.enums.SeminarStatus status,
            boolean active
    );

}








