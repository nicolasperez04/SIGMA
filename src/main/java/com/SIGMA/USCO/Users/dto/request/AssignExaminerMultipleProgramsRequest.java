package com.SIGMA.USCO.Users.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignExaminerMultipleProgramsRequest {

    /** ID del usuario al que se le asignará el rol EXAMINER */
    @NotNull(message = "El ID del usuario es obligatorio.")
    private Long userId;

    /** Lista de IDs de programas académicos a los que se asociará el jurado */
    @NotEmpty(message = "Debe proporcionar al menos un ID de programa académico.")
    private List<Long> academicProgramIds;
}

