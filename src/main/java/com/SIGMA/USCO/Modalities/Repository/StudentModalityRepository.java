package com.SIGMA.USCO.Modalities.Repository;

import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Users.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentModalityRepository extends JpaRepository<StudentModality, Long> {


    List<StudentModality> findByStatus(ModalityProcessStatus status);

    List<StudentModality> findByStatusIn(List<ModalityProcessStatus> statuses);

    // Verifica si un estudiante (miembro) tiene modalidades activas
    @Query("""
        SELECT CASE WHEN COUNT(smm) > 0 THEN true ELSE false END
        FROM StudentModalityMember smm
        WHERE smm.student.id = :studentId
        AND smm.status = 'ACTIVE'
        AND smm.studentModality.status IN :statuses
        """)
    boolean existsByStudentIdAndStatusIn(@Param("studentId") Long studentId, @Param("statuses") List<ModalityProcessStatus> statuses);

    // Verifica si un estudiante tiene modalidades activas como líder
    boolean existsByLeaderIdAndStatusIn(Long leaderId, List<ModalityProcessStatus> statuses);

    // Verifica si un estudiante tiene CUALQUIER modalidad como líder (sin importar el estado)
    boolean existsByLeaderId(Long leaderId);

    // Busca modalidades por líder y estado específico
    List<StudentModality> findByLeaderIdAndStatus(Long leaderId, ModalityProcessStatus status);

    /*
     * MÉTODOS OBSOLETOS COMENTADOS - Usaban 'student' que ya no existe en StudentModality
     * Ahora se usa 'leader' y StudentModalityMember para manejar miembros
     */

    /*
    boolean existsByStudent_IdAndProgramDegreeModality_DegreeModality_Id(
            Long studentId,
            Long modalityId
    );
    */

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

    /*
    List<StudentModality> findByStatusInAndStudent_NameContainingIgnoreCaseOrStatusInAndStudent_LastNameContainingIgnoreCase(
            List<ModalityProcessStatus> statuses1,
            String name1,
            List<ModalityProcessStatus> statuses2,
            String name2
    );

    List<StudentModality> findByStudent_NameContainingIgnoreCaseOrStudent_LastNameContainingIgnoreCase(
            String name1,
            String name2
    );
    */

    // Busca modalidad por estudiante (como miembro activo)
    @Query("""
        SELECT sm FROM StudentModality sm
        JOIN StudentModalityMember smm ON smm.studentModality.id = sm.id
        WHERE smm.student = :student
        AND smm.status = 'ACTIVE'
        """)
    Optional<StudentModality> findByStudent(@Param("student") User student);

    /*
    boolean existsByStudent_IdAndProgramDegreeModality_Id(Long id, Long id1);
    */

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

    /**
     * Encuentra modalidades por tipo (INDIVIDUAL o GROUP)
     * @param modalityType Tipo de modalidad
     * @return Lista de modalidades del tipo especificado
     */
    @Query("SELECT sm FROM StudentModality sm WHERE sm.modalityType = :modalityType")
    List<StudentModality> findByModalityType(@Param("modalityType") com.SIGMA.USCO.Modalities.Entity.enums.ModalityType modalityType);

    /**
     * Encuentra modalidades grupales por estado
     * @param statuses Lista de estados
     * @return Lista de modalidades grupales
     */
    @Query("""
        SELECT sm FROM StudentModality sm
        WHERE sm.modalityType = 'GROUP'
        AND sm.status IN :statuses
        """)
    List<StudentModality> findGroupModalitiesByStatus(@Param("statuses") List<ModalityProcessStatus> statuses);

    /**
     * Cuenta modalidades activas de un líder
     * @param leaderId ID del líder
     * @param statuses Lista de estados activos
     * @return Cantidad de modalidades
     */
    @Query("""
        SELECT COUNT(sm) FROM StudentModality sm
        WHERE sm.leader.id = :leaderId
        AND sm.status IN :statuses
        """)
    Long countActiveModalitiesByLeader(
            @Param("leaderId") Long leaderId,
            @Param("statuses") List<ModalityProcessStatus> statuses
    );

    /**
     * Verifica si un usuario es líder de una modalidad específica
     * @param modalityId ID de la modalidad
     * @param leaderId ID del líder
     * @return true si es el líder
     */
    @Query("""
        SELECT CASE WHEN COUNT(sm) > 0 THEN true ELSE false END
        FROM StudentModality sm
        WHERE sm.id = :modalityId
        AND sm.leader.id = :leaderId
        """)
    boolean isLeaderOfModality(@Param("modalityId") Long modalityId, @Param("leaderId") Long leaderId);

    /**
     * Obtiene modalidades del mismo programa académico que un estudiante
     * Útil para obtener estudiantes elegibles del mismo programa
     * @param programId ID del programa académico
     * @param modalityType Tipo de modalidad
     * @return Lista de modalidades
     */
    @Query("""
        SELECT sm FROM StudentModality sm
        WHERE sm.academicProgram.id = :programId
        AND sm.modalityType = :modalityType
        ORDER BY sm.selectionDate DESC
        """)
    List<StudentModality> findByAcademicProgramAndType(
            @Param("programId") Long programId,
            @Param("modalityType") com.SIGMA.USCO.Modalities.Entity.enums.ModalityType modalityType
    );

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

    /**
     * Encuentra modalidades con información completa de invitaciones
     * @param modalityId ID de la modalidad
     * @return Optional con la modalidad y sus invitaciones cargadas
     */
    @Query("""
        SELECT DISTINCT sm FROM StudentModality sm
        LEFT JOIN FETCH sm.invitations i
        WHERE sm.id = :modalityId
        """)
    Optional<StudentModality> findByIdWithInvitations(@Param("modalityId") Long modalityId);

    /**
     * Encuentra modalidades grupales de un programa con información de miembros
     * @param programId ID del programa académico
     * @return Lista de modalidades grupales con miembros
     */
    @Query("""
        SELECT DISTINCT sm FROM StudentModality sm
        LEFT JOIN FETCH sm.members m
        WHERE sm.academicProgram.id = :programId
        AND sm.modalityType = 'GROUP'
        ORDER BY sm.selectionDate DESC
        """)
    List<StudentModality> findGroupModalitiesByProgramWithMembers(@Param("programId") Long programId);

    /**
     * Busca modalidades por líder con filtros de nombre
     * @param leaderId ID del líder
     * @param statuses Lista de estados
     * @param name Nombre o apellido a buscar
     * @return Lista de modalidades
     */
    @Query("""
        SELECT sm FROM StudentModality sm
        WHERE sm.leader.id = :leaderId
        AND sm.status IN :statuses
        AND (
            LOWER(sm.leader.name) LIKE LOWER(CONCAT('%', :name, '%'))
            OR LOWER(sm.leader.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
        )
        ORDER BY sm.updatedAt DESC
        """)
    List<StudentModality> findByLeaderWithStatusAndName(
            @Param("leaderId") Long leaderId,
            @Param("statuses") List<ModalityProcessStatus> statuses,
            @Param("name") String name
    );
}
