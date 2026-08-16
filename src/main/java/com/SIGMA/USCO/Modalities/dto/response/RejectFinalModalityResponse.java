package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;

/**
 * Shape de rejectFinalModalityByCommittee: {success, studentModalityId,
 * previousStatus, newStatus, rejectedBy, reason, message}.
 */
public record RejectFinalModalityResponse(boolean success, Long studentModalityId,
                                          ModalityProcessStatus previousStatus, ModalityProcessStatus newStatus,
                                          String rejectedBy, String reason, String message) {
}