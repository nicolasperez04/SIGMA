package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;

import java.time.LocalDateTime;

public record DefenseScheduleResponse(boolean success, Long studentModalityId, LocalDateTime defenseDate,
                                      String defenseLocation, ModalityProcessStatus newStatus, String message) {
}