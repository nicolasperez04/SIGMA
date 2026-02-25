package com.SIGMA.USCO.Modalities.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModalityListDTO {

    private Long studentModalityId;
    private String studentName;
    private String studentEmail;
    private String modalityName;
    private String currentStatus;
    private String currentStatusDescription;
    private LocalDateTime lastUpdatedAt;
    private boolean hasPendingActions;
    private LocalDateTime defenseDate;
    private String defenseLocation;

}
