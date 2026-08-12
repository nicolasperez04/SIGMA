package com.SIGMA.USCO.Users.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExaminerProgramItem {

    private Long programAuthorityId;
    private Long academicProgramId;
    private String academicProgramName;
    private Long facultyId;
    private String facultyName;
}
