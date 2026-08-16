package com.SIGMA.USCO.Modalities.dto.response;

public record CancellationRejectedResponse(boolean success, String message, String restoredStatus,
                                           String restoredStatusDescription) {
}