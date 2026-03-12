package com.SIGMA.USCO.Users.dto.request;

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

    private String name;
    private String lastName;
    private String email;
    private String password;
    private String roleName; // PROGRAM_HEAD, PROJECT_DIRECTOR, PROGRAM_CURRICULUM_COMMITTEE, EXAMINER

    /** Requerido para roles vinculados a un solo programa (PROGRAM_HEAD, PROJECT_DIRECTOR, PROGRAM_CURRICULUM_COMMITTEE) */
    private Long academicProgramId;

    /** Solo para EXAMINER: lista de IDs de programas académicos a los que se asociará el jurado */
    private List<Long> academicProgramIds;

}

