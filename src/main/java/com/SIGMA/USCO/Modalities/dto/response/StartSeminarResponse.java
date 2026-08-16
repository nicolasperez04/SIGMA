package com.SIGMA.USCO.Modalities.dto.response;

import java.time.LocalDateTime;

/**
 * Respuesta de startSeminar: claves exactas del Map anterior
 * {success, message, seminarId, seminarName, status, startDate, enrolledStudents, emailsSent}.
 */
public record StartSeminarResponse(
        boolean success,
        String message,
        Long seminarId,
        String seminarName,
        String status,
        LocalDateTime startDate,
        int enrolledStudents,
        int emailsSent) {
}