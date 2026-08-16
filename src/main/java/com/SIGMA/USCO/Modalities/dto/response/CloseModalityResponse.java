package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;

/**
 * Shape de closeModalityByCommittee: {success, studentModalityId, previousStatus,
 * newStatus, closedBy, reason, message}.
 */
public record CloseModalityResponse(boolean success, Long studentModalityId,
                                    ModalityProcessStatus previousStatus, ModalityProcessStatus newStatus,
                                    String closedBy, String reason, String message) {
}