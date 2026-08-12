package com.SIGMA.USCO.Modalities.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Shape de éxito de getMyFinalDocumentEvaluation (GET veredicto del jurado sobre el documento final).
 * El early-return success=false queda como Map.of en el servicio — no es este DTO.
 */
@Data
@Builder
public class ExaminerFinalDocumentEvaluationResponse {

    private Boolean success;
    private Long documentId;
    private String documentName;
    private String documentType;
    private String examinerName;
    private String examinerEmail;
    private String examinerType;
    private Boolean isTiebreaker;
    private String decision;
    private String decisionDescription;
    private String notes;
    private LocalDateTime reviewedAt;
    private FinalEvaluationInfo finalEvaluation;
    private String documentStatus;
    private String documentNotes;
}
