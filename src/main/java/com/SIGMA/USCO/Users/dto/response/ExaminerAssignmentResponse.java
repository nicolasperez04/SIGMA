package com.SIGMA.USCO.Users.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExaminerAssignmentResponse {

    private boolean success;
    private String message;
    private String examinerName;
    private String programName;
}
