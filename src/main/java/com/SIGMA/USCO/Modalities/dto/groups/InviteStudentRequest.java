package com.SIGMA.USCO.Modalities.dto.groups;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteStudentRequest {

    @NotNull(message = "La modalidad es obligatoria.")
    private Long studentModalityId;
    @NotNull(message = "El estudiante a invitar es obligatorio.")
    private Long inviteeId;
}

