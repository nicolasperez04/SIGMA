package com.SIGMA.USCO.academic.dto;

import lombok.*;
import org.springframework.stereotype.Service;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProgramDegreeModalityRequest {

    private Long academicProgramId;

    private Long degreeModalityId;

    private Long creditsRequired;

    /**
     * Indica si esta modalidad requiere el proceso completo de sustentación
     * (director de proyecto, jurados, sustentación y evaluación).
     * Por defecto true. Si es false, el comité decide directamente.
     */
    @Builder.Default
    private boolean requiresDefenseProcess = true;

}


