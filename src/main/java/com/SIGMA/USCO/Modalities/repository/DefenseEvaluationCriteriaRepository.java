package com.SIGMA.USCO.Modalities.repository;

import com.SIGMA.USCO.Modalities.entity.DefenseEvaluationCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DefenseEvaluationCriteriaRepository extends JpaRepository<DefenseEvaluationCriteria, Long> {

    /** Busca la evaluación de un jurado específico por su defenseExaminer ID */
    Optional<DefenseEvaluationCriteria> findByDefenseExaminerId(Long defenseExaminerId);

    /** Elimina las evaluaciones de varios jurados a la vez (FK defense_examiner_id — borrar antes que el DefenseExaminer) */
    void deleteByDefenseExaminerIdIn(List<Long> defenseExaminerIds);

    /** Busca la evaluación de varios jurados a la vez (batch) */
    List<DefenseEvaluationCriteria> findByDefenseExaminerIdIn(List<Long> defenseExaminerIds);

    /** Verifica si ya existe evaluación para un defenseExaminer */
    boolean existsByDefenseExaminerId(Long defenseExaminerId);

    /**
     * Obtiene todas las evaluaciones de una modalidad específica,
     * ordenadas por fecha de evaluación.
     */
    @Query("""
            SELECT dec FROM DefenseEvaluationCriteria dec
            JOIN dec.defenseExaminer de
            WHERE de.studentModality.id = :studentModalityId
            ORDER BY dec.evaluatedAt ASC
            """)
    List<DefenseEvaluationCriteria> findByStudentModalityId(@Param("studentModalityId") Long studentModalityId);

    /**
     * Obtiene las evaluaciones de los jurados primarios de una modalidad.
     */
    @Query("""
            SELECT dec FROM DefenseEvaluationCriteria dec
            JOIN dec.defenseExaminer de
            WHERE de.studentModality.id = :studentModalityId
            AND de.examinerType IN ('PRIMARY_EXAMINER_1', 'PRIMARY_EXAMINER_2')
            ORDER BY de.examinerType ASC
            """)
    List<DefenseEvaluationCriteria> findPrimaryEvaluationsByStudentModalityId(@Param("studentModalityId") Long studentModalityId);

    /**
     * Verifica si ambos jurados primarios han evaluado.
     */
    @Query("""
            SELECT CASE WHEN COUNT(dec) = 2 THEN true ELSE false END
            FROM DefenseEvaluationCriteria dec
            JOIN dec.defenseExaminer de
            WHERE de.studentModality.id = :studentModalityId
            AND de.examinerType IN ('PRIMARY_EXAMINER_1', 'PRIMARY_EXAMINER_2')
            """)
    boolean bothPrimaryExaminersHaveEvaluated(@Param("studentModalityId") Long studentModalityId);

    /**
     * Verifica si los jurados primarios tienen consenso por nota:
     * consenso = ambos aprueban (grade >= 3.5) O ambos rechazan (grade < 3.5).
     * Desacuerdo = uno aprueba y el otro rechaza.
     */
    @Query("""
            SELECT CASE
              WHEN (SUM(CASE WHEN dec.grade >= 3.5 THEN 1 ELSE 0 END) = 2) THEN true
              WHEN (SUM(CASE WHEN dec.grade < 3.5 THEN 1 ELSE 0 END) = 2) THEN true
              ELSE false
            END
            FROM DefenseEvaluationCriteria dec
            JOIN dec.defenseExaminer de
            WHERE de.studentModality.id = :studentModalityId
            AND de.examinerType IN ('PRIMARY_EXAMINER_1', 'PRIMARY_EXAMINER_2')
            """)
    boolean primaryExaminersHaveConsensus(@Param("studentModalityId") Long studentModalityId);

    /**
     * Calcula el promedio de notas de los jurados primarios.
     */
    @Query("""
            SELECT AVG(dec.grade) FROM DefenseEvaluationCriteria dec
            JOIN dec.defenseExaminer de
            WHERE de.studentModality.id = :studentModalityId
            AND de.examinerType IN ('PRIMARY_EXAMINER_1', 'PRIMARY_EXAMINER_2')
            """)
    Double calculateAverageGradeOfPrimaryExaminers(@Param("studentModalityId") Long studentModalityId);

    /**
     * Obtiene la evaluación del jurado de desempate.
     */
    @Query("""
            SELECT dec FROM DefenseEvaluationCriteria dec
            JOIN dec.defenseExaminer de
            WHERE de.studentModality.id = :studentModalityId
            AND de.examinerType = 'TIEBREAKER_EXAMINER'
            """)
    Optional<DefenseEvaluationCriteria> findTiebreakerEvaluationByStudentModalityId(@Param("studentModalityId") Long studentModalityId);

    /**
     * Verifica si el jurado de desempate ya evaluó.
     */
    @Query("""
            SELECT CASE WHEN COUNT(dec) > 0 THEN true ELSE false END
            FROM DefenseEvaluationCriteria dec
            JOIN dec.defenseExaminer de
            WHERE de.studentModality.id = :studentModalityId
            AND de.examinerType = 'TIEBREAKER_EXAMINER'
            """)
    boolean tiebreakerHasEvaluated(@Param("studentModalityId") Long studentModalityId);
}

