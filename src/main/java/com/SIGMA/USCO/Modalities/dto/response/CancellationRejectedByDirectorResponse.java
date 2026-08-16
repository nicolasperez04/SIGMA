package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;

public record CancellationRejectedByDirectorResponse(boolean success, String message,
                                                     ModalityProcessStatus restoredStatus) {
}