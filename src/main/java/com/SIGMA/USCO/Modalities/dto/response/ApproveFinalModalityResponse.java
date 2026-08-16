package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;

/**
 * Shape de approveFinalModalityByCommittee: {success, studentModalityId,
 * previousStatus, newStatus, academicDistinction, finalGrade, approvedBy,
 * observations, message}. finalGrade es el literal "N/A" del Map original.
 */
public record ApproveFinalModalityResponse(boolean success, Long studentModalityId,
                                           ModalityProcessStatus previousStatus, ModalityProcessStatus newStatus,
                                           AcademicDistinction academicDistinction, String finalGrade,
                                           String approvedBy, String observations, String message) {
}