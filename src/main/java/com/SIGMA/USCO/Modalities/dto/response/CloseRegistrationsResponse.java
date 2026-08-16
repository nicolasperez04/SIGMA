package com.SIGMA.USCO.Modalities.dto.response;

import java.time.LocalDateTime;

/**
 * Respuesta de closeRegistrations: claves exactas del Map anterior
 * {success, message, seminarId, seminarName, status, currentParticipants, maxParticipants, updatedAt}.
 */
public record CloseRegistrationsResponse(
        boolean success,
        String message,
        Long seminarId,
        String seminarName,
        String status,
        Integer currentParticipants,
        Integer maxParticipants,
        LocalDateTime updatedAt) {
}