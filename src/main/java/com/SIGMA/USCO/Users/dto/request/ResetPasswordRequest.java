package com.SIGMA.USCO.Users.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    private String token;
    @NotBlank(message = "La nueva contraseña es obligatoria.")
    @Size(min = 6, max = 60, message = "La contraseña debe tener entre 6 y 60 caracteres.")
    private String newPassword;
}
