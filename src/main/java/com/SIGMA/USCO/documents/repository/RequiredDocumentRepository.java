package com.SIGMA.USCO.documents.repository;

import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import com.SIGMA.USCO.documents.entity.RequiredDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequiredDocumentRepository extends JpaRepository<RequiredDocument, Long> {
    List<RequiredDocument> findByModalityId(Long modalityId);

    List<RequiredDocument> findByModalityIdIn(List<Long> modalityIds);

    List<RequiredDocument> findByModalityIdAndActiveTrue(Long modalityId);

    List<RequiredDocument> findByModalityIdAndActive(Long modalityId, boolean active);

    List<RequiredDocument> findByModalityIdAndActiveTrueAndDocumentType(Long modalityId, DocumentType documentType);

    List<RequiredDocument> findByModalityIdAndActiveTrueAndDocumentTypeIn(Long modalityId, List<DocumentType> documentTypes);

    List<RequiredDocument> findByModalityIdAndActiveTrueAndDocumentTypeAndRequiresProposalEvaluationTrue(Long modalityId, DocumentType documentType);

    /** T5.12: verificación de pertenencia sin navegar la relación LAZY modality fuera de tx */
    boolean existsByIdAndModalityId(Long id, Long modalityId);

    // ponytail: unicidad de nombre de documento dentro de la misma modalidad (solo docs activos)
    boolean existsByModality_IdAndDocumentNameIgnoreCaseAndActiveTrue(Long modalityId, String documentName);

    boolean existsByModality_IdAndDocumentNameIgnoreCaseAndActiveTrueAndIdNot(Long modalityId, String documentName, Long id);
}



