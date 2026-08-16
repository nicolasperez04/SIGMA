package com.SIGMA.USCO.Modalities.dto.response;

import java.time.LocalDateTime;

/**
 * Respuesta de completeSeminar: claves exactas del Map anterior
 * {success, message, seminarId, seminarName, status, startDate, endDate, totalParticipants}.
 */
public record CompleteSeminarResponse(
        boolean success,
        String message,
        Long seminarId,
        String seminarName,
        String status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        int totalParticipants) {
}