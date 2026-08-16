package com.SIGMA.USCO.documents.dto;

import com.SIGMA.USCO.documents.entity.enums.ProposalAspectGrade;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposalEvaluationRequest {

    @NotNull(message = "Debe proporcionar calificaciones para todos los aspectos de la propuesta de grado")
    private ProposalAspectGrade summary;

    @NotNull(message = "Debe proporcionar calificaciones para todos los aspectos de la propuesta de grado")
    private ProposalAspectGrade backgroundJustification;

    @NotNull(message = "Debe proporcionar calificaciones para todos los aspectos de la propuesta de grado")
    private ProposalAspectGrade problemStatement;

    @NotNull(message = "Debe proporcionar calificaciones para todos los aspectos de la propuesta de grado")
    private ProposalAspectGrade objectives;

    @NotNull(message = "Debe proporcionar calificaciones para todos los aspectos de la propuesta de grado")
    private ProposalAspectGrade methodology;

    @NotNull(message = "Debe proporcionar calificaciones para todos los aspectos de la propuesta de grado")
    private ProposalAspectGrade bibliographyReferences;

    @NotNull(message = "Debe proporcionar calificaciones para todos los aspectos de la propuesta de grado")
    private ProposalAspectGrade documentOrganization;


}
