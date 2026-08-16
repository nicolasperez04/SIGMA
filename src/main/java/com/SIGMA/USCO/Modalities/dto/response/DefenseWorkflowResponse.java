package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;

public record DefenseWorkflowResponse(boolean success, Long studentModalityId, ModalityProcessStatus newStatus,
                                      String message) {
}