package com.SIGMA.USCO.documents.repository;

import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import com.SIGMA.USCO.documents.entity.RequiredDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequiredDocumentRepository extends JpaRepository<RequiredDocument, Long> {
    List<RequiredDocument> findByModalityId(Long modalityId);

    List<RequiredDocument> findByModalityIdAndActiveTrue(Long modalityId);

    List<RequiredDocument> findByModalityIdAndActive(Long modalityId, boolean active);

    List<RequiredDocument> findByModalityIdAndActiveTrueAndDocumentType(Long modalityId, DocumentType documentType);

    List<RequiredDocument> findByModalityIdAndActiveTrueAndDocumentTypeIn(Long modalityId, List<DocumentType> documentTypes);

    List<RequiredDocument> findByModalityIdAndActiveTrueAndDocumentTypeAndRequiresProposalEvaluationTrue(Long modalityId, DocumentType documentType);
}



