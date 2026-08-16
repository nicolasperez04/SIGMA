package com.SIGMA.USCO.Modalities.repository;

import com.SIGMA.USCO.Modalities.entity.ModalityInvitation;
import com.SIGMA.USCO.Modalities.entity.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la gestión de invitaciones a modalidades grupales.
 * Maneja el proceso de invitación y respuesta de estudiantes.
 */
@Repository
public interface ModalityInvitationRepository extends JpaRepository<ModalityInvitation, Long> {

    /**
     * Encuentra todas las invitaciones de un estudiante con un estado específico
     * @param inviteeId ID del estudiante invitado
     * @param status Estado de la invitación
     * @return Lista de invitaciones
     */
    List<ModalityInvitation> findByInviteeIdAndStatus(Long inviteeId, InvitationStatus status);

    /**
     * Encuentra todas las invitaciones pendientes de un estudiante
     * @param inviteeId ID del estudiante invitado
     * @return Lista de invitaciones pendientes
     */
    @Query("""
        SELECT i FROM ModalityInvitation i
        JOIN FETCH i.studentModality sm
        JOIN FETCH i.inviter inv
        WHERE i.invitee.id = :inviteeId
        AND i.status = 'PENDING'
        ORDER BY i.invitedAt DESC
        """)
    List<ModalityInvitation> findPendingInvitationsForStudent(@Param("inviteeId") Long inviteeId);

    /**
     * Encuentra todas las invitaciones de una modalidad con un estado específico
     * @param modalityId ID de la modalidad
     * @param status Estado de la invitación
     * @return Lista de invitaciones
     */
    List<ModalityInvitation> findByStudentModalityIdAndStatus(Long modalityId, InvitationStatus status);

    /**
     * Encuentra todas las invitaciones de una modalidad
     * @param modalityId ID de la modalidad
     * @return Lista de todas las invitaciones
     */
    List<ModalityInvitation> findByStudentModalityId(Long modalityId);

    /**
     * Cuenta las invitaciones de una modalidad por estado
     * @param modalityId ID de la modalidad
     * @param status Estado de la invitación
     * @return Cantidad de invitaciones
     */
    Long countByStudentModalityIdAndStatus(Long modalityId, InvitationStatus status);

    /**
     * Encuentra una invitación específica de una modalidad a un estudiante con un estado
     * @param modalityId ID de la modalidad
     * @param inviteeId ID del invitado
     * @param status Estado de la invitación
     * @return Optional con la invitación si existe
     */
    Optional<ModalityInvitation> findByStudentModalityIdAndInviteeIdAndStatus(
            Long modalityId,
            Long inviteeId,
            InvitationStatus status
    );

    /**
     * Verifica si existe una invitación entre una modalidad y un estudiante
     * @param modalityId ID de la modalidad
     * @param inviteeId ID del invitado
     * @return true si existe alguna invitación (cualquier estado)
     */
    boolean existsByStudentModalityIdAndInviteeId(Long modalityId, Long inviteeId);

    /**
     * Verifica si existe una invitación pendiente para un estudiante a una modalidad específica
     * @param modalityId ID de la modalidad
     * @param inviteeId ID del invitado
     * @return true si existe invitación pendiente
     */
    @Query("""
        SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
        FROM ModalityInvitation i
        WHERE i.studentModality.id = :modalityId
        AND i.invitee.id = :inviteeId
        AND i.status = 'PENDING'
        """)
    boolean hasPendingInvitation(@Param("modalityId") Long modalityId, @Param("inviteeId") Long inviteeId);

    /**
     * Encuentra todas las invitaciones enviadas por un usuario
     * @param inviterId ID del invitador
     * @return Lista de invitaciones enviadas
     */
    List<ModalityInvitation> findByInviterId(Long inviterId);

    /**
     * Encuentra todas las invitaciones enviadas por un usuario con un estado específico
     * @param inviterId ID del invitador
     * @param status Estado de la invitación
     * @return Lista de invitaciones
     */
    List<ModalityInvitation> findByInviterIdAndStatus(Long inviterId, InvitationStatus status);

    /**
     * Obtiene todas las invitaciones de una modalidad con detalles completos
     * @param modalityId ID de la modalidad
     * @return Lista de invitaciones con estudiantes cargados
     */
    @Query("""
        SELECT i FROM ModalityInvitation i
        JOIN FETCH i.invitee
        JOIN FETCH i.inviter
        WHERE i.studentModality.id = :modalityId
        ORDER BY i.invitedAt DESC
        """)
    List<ModalityInvitation> findInvitationsWithDetailsForModality(@Param("modalityId") Long modalityId);

    /**
     * Cuenta las invitaciones pendientes para un estudiante
     * @param inviteeId ID del estudiante
     * @return Cantidad de invitaciones pendientes
     */
    @Query("""
        SELECT COUNT(i) FROM ModalityInvitation i
        WHERE i.invitee.id = :inviteeId
        AND i.status = 'PENDING'
        """)
    Long countPendingInvitationsForStudent(@Param("inviteeId") Long inviteeId);

    /**
     * Encuentra invitaciones de un estudiante para un programa académico específico
     * @param inviteeId ID del estudiante invitado
     * @param programId ID del programa académico
     * @param status Estado de la invitación
     * @return Lista de invitaciones
     */
    @Query("""
        SELECT i FROM ModalityInvitation i
        WHERE i.invitee.id = :inviteeId
        AND i.studentModality.academicProgram.id = :programId
        AND i.status = :status
        """)
    List<ModalityInvitation> findByInviteeAndProgramAndStatus(
            @Param("inviteeId") Long inviteeId,
            @Param("programId") Long programId,
            @Param("status") InvitationStatus status
    );

    /**
     * Encuentra invitaciones pendientes que necesitan recordatorio (opcional para uso futuro)
     * @return Lista de invitaciones pendientes antiguas
     */
    @Query("""
        SELECT i FROM ModalityInvitation i
        WHERE i.status = 'PENDING'
        AND i.invitedAt < CURRENT_TIMESTAMP - 7 DAY
        ORDER BY i.invitedAt ASC
        """)
    List<ModalityInvitation> findPendingInvitationsNeedingReminder();
}


