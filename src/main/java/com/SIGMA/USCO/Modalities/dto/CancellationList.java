package com.SIGMA.USCO.Modalities.dto;

import com.SIGMA.USCO.Modalities.entity.StudentModality;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CancellationList {

    private Long studentModalityId;
    private String studentName;
    private String email;
    private String modalityName;
    private LocalDateTime requestDate;

}
