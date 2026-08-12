package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.documents.entity.enums.ProposalAspectGrade;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Sub-mapa "finalEvaluation" de las respuestas de evaluación de documento final.
 * Los campos de rúbrica de práctica profesional solo se emiten cuando aplica
 * (rubricType = PROFESSIONAL_PRACTICE), replicando buildFinalEvaluationInfo.
 */
@Data
@Builder
public class FinalEvaluationInfo {

    private Long id;
    private String rubricType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ProposalAspectGrade generalObjective;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ProposalAspectGrade activitiesObjectiveCoherence;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ProposalAspectGrade criticalActivitiesDescription;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ProposalAspectGrade practiceComplianceEvidence;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ProposalAspectGrade organizationAndWriting;

    private ProposalAspectGrade summary;
    private ProposalAspectGrade introduction;
    private ProposalAspectGrade materialsAndMethods;
    private ProposalAspectGrade resultsAndDiscussion;
    private ProposalAspectGrade conclusions;
    private ProposalAspectGrade bibliographyReferences;
    private ProposalAspectGrade documentOrganization;
    private ProposalAspectGrade prototypeOrSoftware;
    private LocalDateTime evaluatedAt;
}
