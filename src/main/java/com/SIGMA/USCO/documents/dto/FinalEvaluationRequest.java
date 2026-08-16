package com.SIGMA.USCO.documents.dto;

import com.SIGMA.USCO.documents.entity.enums.ProposalAspectGrade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalEvaluationRequest {

    // ponytail: sin @NotNull por campo — la rúbrica requerida depende de la modalidad
    // (STANDARD exige los 7 campos estándar; PRÁCTICA PROFESIONAL exige solo los 5 de práctica,
    // ver DocumentWorkflowService.validateFinalEvaluationByRubric). Un @NotNull estático rompería
    // el flujo de Práctica Profesional (el frontend envía solo los 5 campos de práctica), así que
    // la validación queda exclusivamente en el service (rubric-aware, mensajes únicos).
    private ProposalAspectGrade summary;

    private ProposalAspectGrade introduction;

    private ProposalAspectGrade materialsAndMethods;

    private ProposalAspectGrade resultsAndDiscussion;

    private ProposalAspectGrade conclusions;

    private ProposalAspectGrade bibliographyReferences;

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
