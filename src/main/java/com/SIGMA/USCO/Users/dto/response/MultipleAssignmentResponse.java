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
public class MultipleAssignmentResponse {

    private boolean success;
    private Long examinerId;
    private String examinerName;
    private String examinerEmail;
    private List<ProgramAssignmentItem> programsAssigned;
    private List<SkippedProgramItem> programsSkipped;
    private int totalAssigned;
    private int totalSkipped;
}
