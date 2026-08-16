package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;

/**
 * Shape de aprobaciones de modalidad (approveModalityByProgramHead,
 * approveModalityByCommittee, approveModalityByExaminers):
 * {approved, newStatus, message}.
 */
public record ApproveModalityResponse(boolean approved, ModalityProcessStatus newStatus, String message) {
}