package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;

/**
 * Shape de acceptDistinctionProposal: {success, studentModalityId, newStatus,
 * confirmedDistinction, message}.
 */
public record AcceptDistinctionResponse(Boolean success, Long studentModalityId,
                                        ModalityProcessStatus newStatus,
                                        AcademicDistinction confirmedDistinction, String message) {
}