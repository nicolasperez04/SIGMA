package com.SIGMA.USCO.documents.dto;

import com.SIGMA.USCO.documents.entity.enums.ProposalAspectGrade;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalEvaluationRequest {

    @NotNull(message = "Summary grade is required")
    private ProposalAspectGrade summary;

    @NotNull(message = "Introduction grade is required")
    private ProposalAspectGrade introduction;

    @NotNull(message = "Materials and methods grade is required")
    private ProposalAspectGrade materialsAndMethods;

    @NotNull(message = "Results and discussion grade is required")
    private ProposalAspectGrade resultsAndDiscussion;

    @NotNull(message = "Conclusions grade is required")
    private ProposalAspectGrade conclusions;

    @NotNull(message = "Bibliography references grade is required")
    private ProposalAspectGrade bibliographyReferences;

    @NotNull(message = "Document organization grade is required")
    private ProposalAspectGrade documentOrganization;

    // Optional because it applies only when the modality includes a prototype/software deliverable.
    private ProposalAspectGrade prototypeOrSoftware;

    // Professional practice rubric fields
    private ProposalAspectGrade generalObjective;
    private ProposalAspectGrade activitiesObjectiveCoherence;
    private ProposalAspectGrade criticalActivitiesDescription;
    private ProposalAspectGrade practiceComplianceEvidence;
    private ProposalAspectGrade organizationAndWriting;
}

