package com.SIGMA.USCO.academic.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.springframework.stereotype.Service;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProgramDegreeModalityRequest {

    @NotNull(message = "El programa académico es obligatorio.")
    private Long academicProgramId;

    @NotNull(message = "La modalidad es obligatoria.")
    private Long degreeModalityId;

    @NotNull(message = "Los créditos requeridos son obligatorios.")
    @Positive(message = "Los créditos requeridos deben ser mayores a cero.")
    private Long creditsRequired;

    /**
     * Indica si esta modalidad requiere el proceso completo de sustentación
     * (director de proyecto, jurados, sustentación y evaluación).
     * Por defecto true. Si es false, el comité decide directamente.
     */
    @Builder.Default
    private boolean requiresDefenseProcess = true;

}


