package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;

import java.util.List;

public record ExaminerAssignmentResponse(boolean success, Long studentModalityId, ModalityProcessStatus newStatus,
                                         List<String> examinersAssigned, String message) {
}