package com.SIGMA.USCO.Modalities.dto.response;

public record DirectorAssignmentResponse(boolean success, Long studentModalityId, String directorAssigned,
                                         String message) {
}