package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;

/**
 * Shape de rejectDistinctionProposal: {success, studentModalityId, newStatus,
 * finalDistinction, reason, message}.
 */
public record RejectDistinctionResponse(Boolean success, Long studentModalityId,
                                        ModalityProcessStatus newStatus,
                                        AcademicDistinction finalDistinction, String reason, String message) {
}