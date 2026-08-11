package com.SIGMA.USCO.Users.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class assignAuthorityProgram {

    @NotNull(message = "El ID del usuario es obligatorio.")
    private Long userId;
    @NotNull(message = "El programa académico es obligatorio.")
    private Long academicProgramId;

}
