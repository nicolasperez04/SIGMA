package com.SIGMA.USCO.documents.service;

import com.SIGMA.USCO.Modalities.Repository.DefenseExaminerRepository;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.documents.dto.ProposalEvaluationRequest;
import com.SIGMA.USCO.documents.dto.ProposalEvaluationResponseDTO;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import com.SIGMA.USCO.documents.entity.ProposalEvaluation;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.repository.ProposalEvaluationRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.security.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProposalEvaluationService {

    private final ProposalEvaluationRepository proposalEvaluationRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final UserRepository userRepository;
    private final DefenseExaminerRepository defenseExaminerRepository;

    /**
     * Registers the proposal evaluation for a MANDATORY document by the authenticated examiner.
     */
    @Transactional
    public ResponseEntity<?> submitProposalEvaluation(Long studentDocumentId, ProposalEvaluationRequest request) {

        User examiner = SecurityUtils.getCurrentUser();

        StudentDocument studentDocument = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        if (studentDocument.getDocumentConfig().getDocumentType() != DocumentType.MANDATORY) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Only MANDATORY documents (degree proposal) can be evaluated with this form"
            ));
        }

        Long studentModalityId = studentDocument.getStudentModality().getId();

        boolean isAssignedExaminer = defenseExaminerRepository
                .findByStudentModalityIdAndExaminerId(studentModalityId, examiner.getId())
                .isPresent();

        if (!isAssignedExaminer) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "You are not assigned as an examiner for the modality associated with this document"
            ));
        }

        if (proposalEvaluationRepository.existsByStudentDocumentIdAndExaminerId(studentDocumentId, examiner.getId())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "You have already submitted an evaluation for this document"
            ));
        }

        ProposalEvaluation evaluation = ProposalEvaluation.builder()
                .studentDocument(studentDocument)
                .examiner(examiner)
                .summary(request.getSummary())
                .backgroundJustification(request.getBackgroundJustification())
                .problemStatement(request.getProblemStatement())
                .objectives(request.getObjectives())
                .methodology(request.getMethodology())
                .bibliographyReferences(request.getBibliographyReferences())
                .documentOrganization(request.getDocumentOrganization())

                .evaluatedAt(LocalDateTime.now())
                .build();

        proposalEvaluationRepository.save(evaluation);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Proposal evaluation submitted successfully",
                "evaluationId", evaluation.getId(),
                "studentDocumentId", studentDocumentId
        ));
    }

    /**
     * Returns all proposal evaluations for a given document.
     */
    public ResponseEntity<?> getEvaluationsByDocument(Long studentDocumentId) {

        studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        List<ProposalEvaluationResponseDTO> evaluations = proposalEvaluationRepository
                .findByStudentDocumentId(studentDocumentId)
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return ResponseEntity.ok(evaluations);
    }

    /**
     * Returns the authenticated examiner's evaluation for a specific document.
     */
    public ResponseEntity<?> getMyEvaluationForDocument(Long studentDocumentId) {

        User examiner = SecurityUtils.getCurrentUser();

        ProposalEvaluation evaluation = proposalEvaluationRepository
                .findByStudentDocumentIdAndExaminerId(studentDocumentId, examiner.getId())
                .orElseThrow(() -> new RuntimeException(
                        "No evaluation found for this document and examiner"
                ));

        return ResponseEntity.ok(toResponseDTO(evaluation));
    }

    // ========================
    // Internal mapper
    // ========================

    private ProposalEvaluationResponseDTO toResponseDTO(ProposalEvaluation evaluation) {
        return ProposalEvaluationResponseDTO.builder()
                .evaluationId(evaluation.getId())
                .studentDocumentId(evaluation.getStudentDocument().getId())
                .documentName(evaluation.getStudentDocument().getDocumentConfig().getDocumentName())
                .evaluatorName(evaluation.getExaminer().getName() + " " + evaluation.getExaminer().getLastName())
                .evaluatorEmail(evaluation.getExaminer().getEmail())
                .summary(evaluation.getSummary().name())
                .backgroundJustification(evaluation.getBackgroundJustification().name())
                .problemStatement(evaluation.getProblemStatement().name())
                .objectives(evaluation.getObjectives().name())
                .methodology(evaluation.getMethodology().name())
                .bibliographyReferences(evaluation.getBibliographyReferences().name())
                .documentOrganization(evaluation.getDocumentOrganization().name())

                .evaluatedAt(evaluation.getEvaluatedAt())
                .build();
    }
}


