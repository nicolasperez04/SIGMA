package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;

public record CancellationRequestResponse(boolean success, String message, Long studentModalityId,
                                          ModalityProcessStatus newStatus) {
}