package com.SIGMA.USCO.Users.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExaminerProgramsResponse {

    private boolean success;
    private Long examinerId;
    private String examinerName;
    private String examinerEmail;
    private List<ExaminerProgramItem> programs;
}
