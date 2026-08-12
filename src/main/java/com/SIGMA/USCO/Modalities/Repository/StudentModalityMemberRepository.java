package com.SIGMA.USCO.Modalities.Repository;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la gestión de miembros de modalidades grupales.
 * Maneja la relación entre estudiantes y modalidades.
 */
@Repository
public interface StudentModalityMemberRepository extends JpaRepository<StudentModalityMember, Long> {

    /**
     * Encuentra todos los miembros de una modalidad con un estado específico
     * @param modalityId ID de la modalidad
     * @param status Estado del miembro (ACTIVE, LEFT, REMOVED)
     * @return Lista de miembros
     */
    List<StudentModalityMember> findByStudentModalityIdAndStatus(Long modalityId, MemberStatus status);

    /**
     * Encuentra todos los miembros de una modalidad (cualquier estado)
     * @param modalityId ID de la modalidad
     * @return Lista de todos los miembros
     */
    List<StudentModalityMember> findByStudentModalityId(Long modalityId);

    /**
     * Encuentra los miembros con un estado específico de varias modalidades (batch)
     * @param modalityIds IDs de las modalidades
     * @param status Estado del miembro (ACTIVE, LEFT, REMOVED)
     * @return Lista de miembros
     */
    List<StudentModalityMember> findByStudentModalityIdInAndStatus(List<Long> modalityIds, MemberStatus status);

    /**
     * Cuenta la cantidad de miembros activos de una modalidad
     * @param modalityId ID de la modalidad
     * @param status Estado del miembro
     * @return Cantidad de miembros
     */
    Long countByStudentModalityIdAndStatus(Long modalityId, MemberStatus status);

    /**
     * Encuentra todas las modalidades activas de un estudiante
     * @param studentId ID del estudiante
     * @param status Estado del miembro
     * @return Lista de membresías
     */
    List<StudentModalityMember> findByStudentIdAndStatus(Long studentId, MemberStatus status);

    /**
     * Encuentra un miembro específico en una modalidad
     * @param modalityId ID de la modalidad
     * @param studentId ID del estudiante
     * @return Optional con el miembro si existe
     */
    Optional<StudentModalityMember> findByStudentModalityIdAndStudentId(Long modalityId, Long studentId);

    /**
     * Verifica si un estudiante tiene alguna modalidad activa
     * @param studentId ID del estudiante
     * @param status Estado del miembro
     * @return true si tiene modalidad activa
     */
    boolean existsByStudentIdAndStatus(Long studentId, MemberStatus status);

    /**
     * Verifica si un estudiante es miembro de CUALQUIER modalidad (sin importar el estado)
     * @param studentId ID del estudiante
     * @return true si es miembro de alguna modalidad
     */
    boolean existsByStudentId(Long studentId);

    /**
     * Encuentra el líder de una modalidad
     * @param modalityId ID de la modalidad
     * @param isLeader Debe ser true
     * @return Optional con el líder
     */
    Optional<StudentModalityMember> findByStudentModalityIdAndIsLeader(Long modalityId, Boolean isLeader);

    /**
     * Verifica si un estudiante es líder de una modalidad
     * @param modalityId ID de la modalidad
     * @param studentId ID del estudiante
     * @param isLeader Debe ser true
     * @return true si es líder
     */
    boolean existsByStudentModalityIdAndStudentIdAndIsLeader(
            Long modalityId,
            Long studentId,
            Boolean isLeader
    );

    /**
     * Obtiene todos los miembros activos de una modalidad con información detallada
     * @param modalityId ID de la modalidad
     * @return Lista de miembros activos con datos del estudiante cargados
     */
    @Query("""
        SELECT m FROM StudentModalityMember m
        JOIN FETCH m.student s
        WHERE m.studentModality.id = :modalityId
        AND m.status = 'ACTIVE'
        ORDER BY m.isLeader DESC, m.joinedAt ASC
        """)
    List<StudentModalityMember> findActiveMembersWithStudentDetails(@Param("modalityId") Long modalityId);

    /**
     * Cuenta todas las modalidades activas de un estudiante
     * @param studentId ID del estudiante
     * @return Cantidad de modalidades activas
     */
    @Query("""
        SELECT COUNT(m) FROM StudentModalityMember m
        WHERE m.student.id = :studentId
        AND m.status = 'ACTIVE'
        """)
    Long countActiveModalitiesForStudent(@Param("studentId") Long studentId);

    /**
     * Verifica si un estudiante es miembro activo de una modalidad
     * @param modalityId ID de la modalidad
     * @param studentId ID del estudiante
     * @return true si es miembro activo
     */
    @Query("""
        SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
        FROM StudentModalityMember m
        WHERE m.studentModality.id = :modalityId
        AND m.student.id = :studentId
        AND m.status = 'ACTIVE'
        """)
    boolean isActiveMember(@Param("modalityId") Long modalityId, @Param("studentId") Long studentId);

    /**
     * Obtiene todos los miembros de modalidades de un programa académico específico
     * @param programId ID del programa académico
     * @param status Estado del miembro
     * @return Lista de miembros
     */
    @Query("""
        SELECT m FROM StudentModalityMember m
        WHERE m.studentModality.academicProgram.id = :programId
        AND m.status = :status
        """)
    List<StudentModalityMember> findByAcademicProgramAndStatus(
            @Param("programId") Long programId,
            @Param("status") MemberStatus status
    );

    /**
     * Obtiene todas las modalidades activas de un estudiante
     * @param studentId ID del estudiante
     * @return Lista de modalidades donde el estudiante es miembro activo
     */
    @Query("""
        SELECT m.studentModality FROM StudentModalityMember m
        WHERE m.student.id = :studentId
        AND m.status = 'ACTIVE'
        ORDER BY m.studentModality.updatedAt DESC
        """)
    List<StudentModality> findActiveModalitiesByUserId(@Param("studentId") Long studentId);
}

