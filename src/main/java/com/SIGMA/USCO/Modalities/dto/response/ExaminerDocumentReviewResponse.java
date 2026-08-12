package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shape de éxito de reviewStudentDocumentByExaminer (POST/PUT review de jurado).
 * El early-return del consenso (consensusResult) NO es este DTO — queda como Map.
 * toMap() permite a reviewFinalDocumentByExaminer fusionar estas claves sin
 * cambiar el wire byte-idéntico.
 */
@Data
@Builder
public class ExaminerDocumentReviewResponse {

    private Boolean success;
    private Long documentId;
    private String documentName;
    private String examinerDecision;
    private DocumentStatus currentDocumentStatus;
    private String examinerName;
    private String examinerType;
    private String message;
    private ProposalEvaluationInfo proposalEvaluation;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", success);
        map.put("documentId", documentId);
        map.put("documentName", documentName);
        map.put("examinerDecision", examinerDecision);
        map.put("currentDocumentStatus", currentDocumentStatus);
        map.put("examinerName", examinerName);
        map.put("examinerType", examinerType);
        map.put("message", message);
        map.put("proposalEvaluation", proposalEvaluation);
        return map;
    }
}
