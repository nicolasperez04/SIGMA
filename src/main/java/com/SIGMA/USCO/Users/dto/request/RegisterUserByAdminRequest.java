package com.SIGMA.USCO.Users.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserByAdminRequest {

    @NotBlank(message = "El nombre es obligatorio.")
    private String name;
    @NotBlank(message = "El apellido es obligatorio.")
    private String lastName;
    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "El correo no es válido.")
    private String email;
    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(min = 6, max = 60, message = "La contraseña debe tener entre 6 y 60 caracteres.")
    private String password;
    @NotBlank(message = "El rol es obligatorio.")
    private String roleName; // PROGRAM_HEAD, PROJECT_DIRECTOR, PROGRAM_CURRICULUM_COMMITTEE, EXAMINER

    /** Requerido para roles vinculados a un solo programa (PROGRAM_HEAD, PROJECT_DIRECTOR, PROGRAM_CURRICULUM_COMMITTEE) */
    private Long academicProgramId;

    /** Solo para EXAMINER: lista de IDs de programas académicos a los que se asociará el jurado */
    private List<Long> academicProgramIds;

}

