package com.SIGMA.USCO.Modalities.dto;

import com.SIGMA.USCO.documents.dto.FinalEvaluationRequest;
import com.SIGMA.USCO.documents.dto.ProposalEvaluationRequest;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentReviewDTO {

    @NotNull(message = "El estado del documento es obligatorio.")
    private DocumentStatus status;
    private String notes;

    /**
     * Opcional: calificación por aspectos de la propuesta de grado.
     * Solo aplica cuando el documento es de tipo MANDATORY.
     * Si se envía, se almacenará como ProposalEvaluation asociada al documento.
     */
    @Valid
    private ProposalEvaluationRequest proposalEvaluation;

    /**
     * Opcional: calificación por aspectos del documento final (SECONDARY).
     * Solo aplica cuando el documento es de tipo SECONDARY y requiresProposalEvaluation=true.
     */
    @Valid
    private FinalEvaluationRequest finalEvaluation;

}
