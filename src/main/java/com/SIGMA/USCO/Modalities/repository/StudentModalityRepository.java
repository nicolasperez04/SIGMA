package com.SIGMA.USCO.Modalities.repository;

import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Users.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentModalityRepository extends JpaRepository<StudentModality, Long> {


    List<StudentModality> findByStatusIn(List<ModalityProcessStatus> statuses);

    // Serializa escrituras concurrentes sobre la modalidad (p.ej. consenso de evaluación de defensa):
    // el segundo voto bloquea hasta que el primero commitea y ve su evaluación.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sm FROM StudentModality sm WHERE sm.id = :id")
    Optional<StudentModality> findWithLockingById(@Param("id") Long id);

    // Verifica si un estudiante tiene CUALQUIER modalidad como líder (sin importar el estado)
    boolean existsByLeaderId(Long leaderId);

    // Busca modalidades por líder y estado específico
    List<StudentModality> findByLeaderIdAndStatus(Long leaderId, ModalityProcessStatus status);

    // Historial completo de modalidades del estudiante (como líder o miembro), por fecha de creación desc
    @Query("""
        SELECT sm FROM StudentModality sm
        JOIN StudentModalityMember smm ON smm.studentModality.id = sm.id
        WHERE smm.student.id = :studentId
        ORDER BY sm.selectionDate DESC
        """)
    List<StudentModality> findHistoryByStudentIdOrderBySelectionDateDesc(@Param("studentId") Long studentId);

    // Obtiene la modalidad más reciente de un estudiante (como miembro)
    @Query("""
        SELECT sm FROM StudentModality sm
        JOIN StudentModalityMember smm ON smm.studentModality.id = sm.id
        WHERE smm.student.id = :studentId
        AND smm.status = 'ACTIVE'
        ORDER BY sm.updatedAt DESC
        LIMIT 1
        """)
    Optional<StudentModality> findTopByStudentIdOrderByUpdatedAtDesc(@Param("studentId") Long studentId);

    // Busca modalidad por estudiante (como miembro activo)
    @Query("""
        SELECT sm FROM StudentModality sm
        JOIN StudentModalityMember smm ON smm.studentModality.id = sm.id
        WHERE smm.student = :student
        AND smm.status = 'ACTIVE'
        """)
    Optional<StudentModality> findByStudent(@Param("student") User student);

    @Query("""
SELECT sm FROM StudentModality sm
WHERE sm.programDegreeModality.academicProgram.id IN :programIds
AND sm.status IN :statuses
AND (
    LOWER(sm.leader.name) LIKE LOWER(CONCAT('%', :name, '%'))
    OR LOWER(sm.leader.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
)
""")
    List<StudentModality> findForProgramHeadWithStatusAndName(
            List<Long> programIds,
            List<ModalityProcessStatus> statuses,
            String name
    );

    @Query("""
SELECT sm FROM StudentModality sm
WHERE sm.programDegreeModality.academicProgram.id IN :programIds
AND sm.status IN :statuses
""")
    List<StudentModality> findForProgramHeadWithStatus(
            List<Long> programIds,
            List<ModalityProcessStatus> statuses
    );

    @Query("""
SELECT sm FROM StudentModality sm
WHERE sm.programDegreeModality.academicProgram.id IN :programIds
AND (
    LOWER(sm.leader.name) LIKE LOWER(CONCAT('%', :name, '%'))
    OR LOWER(sm.leader.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
)
""")
    List<StudentModality> findForProgramHeadWithName(
            List<Long> programIds,
            String name
    );

    @Query("""
SELECT sm FROM StudentModality sm
WHERE sm.programDegreeModality.academicProgram.id IN :programIds
""")
    List<StudentModality> findForProgramHead(
            List<Long> programIds
    );


    List<StudentModality> findByStatusAndProgramDegreeModality_AcademicProgram_IdIn(
            ModalityProcessStatus status,
            List<Long> academicProgramIds
    );

    @Query("""
SELECT sm FROM StudentModality sm
WHERE sm.projectDirector.id = :directorId
AND sm.status IN :statuses
AND (
    LOWER(sm.leader.name) LIKE LOWER(CONCAT('%', :name, '%'))
    OR LOWER(sm.leader.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
)
""")
    List<StudentModality> findForProjectDirectorWithStatusAndName(
            Long directorId,
            List<ModalityProcessStatus> statuses,
            String name
    );

    @Query("""
SELECT sm FROM StudentModality sm
WHERE sm.projectDirector.id = :directorId
AND sm.status IN :statuses
""")
    List<StudentModality> findForProjectDirectorWithStatus(
            Long directorId,
            List<ModalityProcessStatus> statuses
    );

    @Query("""
SELECT sm FROM StudentModality sm
WHERE sm.projectDirector.id = :directorId
AND (
    LOWER(sm.leader.name) LIKE LOWER(CONCAT('%', :name, '%'))
    OR LOWER(sm.leader.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
)
""")
    List<StudentModality> findForProjectDirectorWithName(
            Long directorId,
            String name
    );

    @Query("""
SELECT sm FROM StudentModality sm
WHERE sm.projectDirector.id = :directorId
""")
    List<StudentModality> findForProjectDirector(
            Long directorId
    );

    @Query("""
SELECT DISTINCT sm FROM StudentModality sm
JOIN DefenseExaminer de ON de.studentModality.id = sm.id
WHERE de.examiner.id = :examinerId
AND sm.status IN :statuses
AND (
    LOWER(sm.leader.name) LIKE LOWER(CONCAT('%', :name, '%'))
    OR LOWER(sm.leader.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
)
ORDER BY sm.updatedAt DESC
""")
    List<StudentModality> findForExaminerWithStatusAndName(
            Long examinerId,
            List<ModalityProcessStatus> statuses,
            String name
    );

    @Query("""
SELECT DISTINCT sm FROM StudentModality sm
JOIN DefenseExaminer de ON de.studentModality.id = sm.id
WHERE de.examiner.id = :examinerId
AND sm.status IN :statuses
ORDER BY sm.updatedAt DESC
""")
    List<StudentModality> findForExaminerWithStatus(
            Long examinerId,
            List<ModalityProcessStatus> statuses
    );

    @Query("""
SELECT DISTINCT sm FROM StudentModality sm
JOIN DefenseExaminer de ON de.studentModality.id = sm.id
WHERE de.examiner.id = :examinerId
AND (
    LOWER(sm.leader.name) LIKE LOWER(CONCAT('%', :name, '%'))
    OR LOWER(sm.leader.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
)
ORDER BY sm.updatedAt DESC
""")
    List<StudentModality> findForExaminerWithName(
            Long examinerId,
            String name
    );

    @Query("""
SELECT DISTINCT sm FROM StudentModality sm
JOIN DefenseExaminer de ON de.studentModality.id = sm.id
WHERE de.examiner.id = :examinerId
ORDER BY sm.updatedAt DESC
""")
    List<StudentModality> findForExaminer(
            Long examinerId
    );

    // ==================== NUEVOS MÉTODOS PARA MODALIDADES GRUPALES ====================

    /**
     * Encuentra modalidades por el líder (estudiante que inició la modalidad)
     * @param leaderId ID del líder
     * @return Lista de modalidades donde el usuario es líder
     */
    List<StudentModality> findByLeaderId(Long leaderId);

    List<StudentModality> findByLeaderIdIn(List<Long> leaderIds);

    // Verifica si una modalidad del catálogo (DegreeModality) está en uso por alguna StudentModality
    // en un estado NO terminal (estados terminales = historial cerrado).
    boolean existsByProgramDegreeModality_DegreeModalityIdAndStatusNotIn(
            Long degreeModalityId,
            Collection<ModalityProcessStatus> statuses);

    /**
     * Encuentra modalidades con información completa de miembros (para modalidades grupales)
     * @param modalityId ID de la modalidad
     * @return Optional con la modalidad y sus miembros cargados
     */
    @Query("""
        SELECT DISTINCT sm FROM StudentModality sm
        LEFT JOIN FETCH sm.members m
        WHERE sm.id = :modalityId
        """)
    Optional<StudentModality> findByIdWithMembers(@Param("modalityId") Long modalityId);
}
