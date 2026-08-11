package com.SIGMA.USCO.academic.dto;

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
public class ProgramDTO {

    private Long id;

    @NotBlank(message = "El nombre del programa es obligatorio.")
    private String name;

    @NotBlank(message = "El código del programa es obligatorio.")
    private String code;

    private Long totalCredits;
    private String description;

    @NotNull(message = "La facultad es obligatoria.")
    private Long facultyId;
    private boolean active;

}
