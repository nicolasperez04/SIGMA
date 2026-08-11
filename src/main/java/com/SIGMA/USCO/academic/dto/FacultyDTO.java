package com.SIGMA.USCO.academic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FacultyDTO {

    @NotBlank(message = "El nombre de la facultad es obligatorio.")
    private String name;

    @NotBlank(message = "El código de la facultad es obligatorio.")
    private String code;

    @NotBlank(message = "La descripción de la facultad es obligatoria.")
    private String description;

    //response

    private Long id;
    private boolean active;
    private List<ProgramDTO> academicPrograms;
}
