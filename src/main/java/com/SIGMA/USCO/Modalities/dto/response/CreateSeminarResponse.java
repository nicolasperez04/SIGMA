package com.SIGMA.USCO.Modalities.dto.response;

/**
 * Respuesta de createSeminar: claves exactas del Map anterior
 * {success, message, seminarId, programName, seminarName}.
 */
public record CreateSeminarResponse(
        boolean success,
        String message,
        Long seminarId,
        String programName,
        String seminarName) {
}