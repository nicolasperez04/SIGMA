package com.SIGMA.USCO.Modalities.dto.response;

/**
 * Respuesta de cancelSeminar: claves exactas del Map anterior
 * {success, message, seminarId, seminarName, status, previouslyEnrolledStudents, emailsSent}.
 */
public record CancelSeminarResponse(
        boolean success,
        String message,
        Long seminarId,
        String seminarName,
        String status,
        int previouslyEnrolledStudents,
        int emailsSent) {
}