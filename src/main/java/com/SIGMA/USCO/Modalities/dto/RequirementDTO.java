package com.SIGMA.USCO.Modalities.dto;

import com.SIGMA.USCO.Modalities.entity.enums.RuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementDTO {

    private Long id;

    @NotBlank(message = "El nombre del requisito es obligatorio.")
    private String requirementName;
    private String description;

    @NotNull(message = "El tipo de regla es obligatorio para el requisito.")
    private RuleType ruleType;

    @NotBlank(message = "El valor esperado es obligatorio para el requisito.")
    private String expectedValue;
    private boolean active;


}
