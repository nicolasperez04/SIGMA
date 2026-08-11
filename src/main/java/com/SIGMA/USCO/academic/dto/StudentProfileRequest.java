package com.SIGMA.USCO.academic.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileRequest {

    @NotNull(message = "El programa académico es obligatorio.")
    private Long academicProgramId;

    @NotNull(message = "La facultad es obligatoria.")
    private Long facultyId;

    @NotNull(message = "Los créditos aprobados son obligatorios.")
    @Min(value = 0, message = "Los créditos aprobados no pueden ser negativos.")
    private Long approvedCredits;

    @NotNull(message = "El promedio es obligatorio.")
    @DecimalMin(value = "0.0", message = "El promedio debe estar entre 0.0 y 5.0.")
    @DecimalMax(value = "5.0", message = "El promedio debe estar entre 0.0 y 5.0.")
    private Double gpa;

    @Min(value = 1, message = "El semestre debe estar entre 1 y 10.")
    @Max(value = 10, message = "El semestre debe estar entre 1 y 10.")
    private Long semester;

    private String studentCode;
}
