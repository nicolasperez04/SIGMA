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
public class AssignExaminerMultipleProgramsRequest {

    /** ID del usuario al que se le asignará el rol EXAMINER */
    private Long userId;

    /** Lista de IDs de programas académicos a los que se asociará el jurado */
    private List<Long> academicProgramIds;
}

