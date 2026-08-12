package com.SIGMA.USCO.Modalities.dto;

import com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDefenseDTO {

    private LocalDateTime defenseDate;
    private String defenseLocation;
    private boolean approved;
    private AcademicDistinction academicDistinction;
    private String observations;


    @Positive
    private Long primaryExaminer1Id;
    @Positive
    private Long primaryExaminer2Id;
    @Positive
    private Long tiebreakerExaminerId;

}
