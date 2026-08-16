package com.SIGMA.USCO.Modalities.dto.response;

public record StartGroupModalityResponse(boolean eligible, Long studentModalityId, String studentModalityName,
                                         String modalityType, String message) {
}