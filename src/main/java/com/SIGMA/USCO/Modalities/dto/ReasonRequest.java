package com.SIGMA.USCO.Modalities.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReasonRequest {

    @NotBlank(message = "La razón es obligatoria.")
    private String reason;
}