package com.SIGMA.USCO.Modalities.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidationItemDTO {

    private String requirementName;

    private String requiredValue;

    private String studentValue;

    private boolean fulfilled;
}
