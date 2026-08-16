package com.SIGMA.USCO.documents.repository;

import com.SIGMA.USCO.documents.entity.DocumentEditRequest;
import com.SIGMA.USCO.documents.entity.enums.DocumentEditRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentEditRequestRepository extends JpaRepository<DocumentEditRequest, Long> {

    List<DocumentEditRequest> findByStudentDocumentId(Long studentDocumentId);

    List<DocumentEditRequest> findByStudentDocumentIdAndStatus(Long studentDocumentId, DocumentEditRequestStatus status);

    Optional<DocumentEditRequest> findTopByStudentDocumentIdAndStatusOrderByCreatedAtDesc(
            Long studentDocumentId, DocumentEditRequestStatus status);

    boolean existsByStudentDocumentIdAndStatus(Long studentDocumentId, DocumentEditRequestStatus status);

    /** Todas las solicitudes creadas por un estudiante específico */
    List<DocumentEditRequest> findByRequesterId(Long requesterId);

    /** Solicitudes de un estudiante filtradas por estado */
    List<DocumentEditRequest> findByRequesterIdAndStatus(Long requesterId, DocumentEditRequestStatus status);

    /** Solicitudes asociadas a una modalidad (buscando a través del documento) */
    @EntityGraph(attributePaths = {"studentDocument", "requester", "studentDocument.documentConfig"})
    @Query("SELECT er FROM DocumentEditRequest er " +
           "WHERE er.studentDocument.studentModality.id = :studentModalityId " +
           "ORDER BY er.createdAt DESC")
    List<DocumentEditRequest> findByStudentModalityId(@Param("studentModalityId") Long studentModalityId);

    /** Solicitudes de una modalidad filtradas por estado */
    @Query("SELECT er FROM DocumentEditRequest er " +
           "WHERE er.studentDocument.studentModality.id = :studentModalityId " +
           "AND er.status = :status " +
           "ORDER BY er.createdAt DESC")
    List<DocumentEditRequest> findByStudentModalityIdAndStatus(
            @Param("studentModalityId") Long studentModalityId,
            @Param("status") DocumentEditRequestStatus status);

    /** Solicitudes de un estudiante en una modalidad específica */
    @Query("SELECT er FROM DocumentEditRequest er " +
           "WHERE er.requester.id = :requesterId " +
           "AND er.studentDocument.studentModality.id = :studentModalityId " +
           "ORDER BY er.createdAt DESC")
    List<DocumentEditRequest> findByRequesterIdAndStudentModalityId(
            @Param("requesterId") Long requesterId,
            @Param("studentModalityId") Long studentModalityId);
}

