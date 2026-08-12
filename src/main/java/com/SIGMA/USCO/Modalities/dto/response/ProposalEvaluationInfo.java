package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.documents.entity.enums.ProposalAspectGrade;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Sub-mapa "proposalEvaluation" de las respuestas de evaluación del jurado.
 * `id` es opcional: se emite solo cuando aplica (review de jurado), se omite
 * en los GET de consulta para mantener el wire byte-idéntico.
 */
@Data
@Builder
public class ProposalEvaluationInfo {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long id;

    private ProposalAspectGrade summary;
    private ProposalAspectGrade backgroundJustification;
    private ProposalAspectGrade problemStatement;
    private ProposalAspectGrade objectives;
    private ProposalAspectGrade methodology;
    private ProposalAspectGrade bibliographyReferences;
    private ProposalAspectGrade documentOrganization;
    private LocalDateTime evaluatedAt;
}
