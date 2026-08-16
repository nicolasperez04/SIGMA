package com.SIGMA.USCO.Modalities.dto.response;

public record DirectorChangeResponse(boolean success, Long studentModalityId, DirectorInfo previousDirector,
                                     DirectorInfo newDirector, String reason, String message) {

    public record DirectorInfo(Long id, String email, String name) {
    }
}