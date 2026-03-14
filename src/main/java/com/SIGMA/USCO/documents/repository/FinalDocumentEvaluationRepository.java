package com.SIGMA.USCO.documents.repository;

import com.SIGMA.USCO.documents.entity.FinalDocumentEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinalDocumentEvaluationRepository extends JpaRepository<FinalDocumentEvaluation, Long> {

    List<FinalDocumentEvaluation> findByStudentDocumentId(Long studentDocumentId);

    Optional<FinalDocumentEvaluation> findByStudentDocumentIdAndExaminerId(Long studentDocumentId, Long examinerId);

    boolean existsByStudentDocumentIdAndExaminerId(Long studentDocumentId, Long examinerId);
}

