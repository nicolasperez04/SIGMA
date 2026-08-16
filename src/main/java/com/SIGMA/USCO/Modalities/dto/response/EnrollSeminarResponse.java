package com.SIGMA.USCO.Modalities.dto.response;

import java.time.LocalDateTime;

/**
 * Respuesta de enrollInSeminar: claves exactas del Map anterior
 * {success, message, seminarName, enrollmentDate, currentParticipants, maxParticipants, availableSeats}.
 */
public record EnrollSeminarResponse(
        boolean success,
        String message,
        String seminarName,
        LocalDateTime enrollmentDate,
        Integer currentParticipants,
        Integer maxParticipants,
        int availableSeats) {
}