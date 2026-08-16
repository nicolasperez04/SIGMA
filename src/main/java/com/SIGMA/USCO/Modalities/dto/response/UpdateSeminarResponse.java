package com.SIGMA.USCO.Modalities.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Respuesta de updateSeminar: claves exactas del Map anterior
 * {success, message, seminar}.
 */
public record UpdateSeminarResponse(
        boolean success,
        String message,
        SeminarSummary seminar) {

    /**
     * Sub-objeto seminar del Map anterior: {id, name, description, totalCost, minParticipants,
     * maxParticipants, currentParticipants, totalHours, status, active, updatedAt}.
     */
    public record SeminarSummary(
            Long id,
            String name,
            String description,
            BigDecimal totalCost,
            Integer minParticipants,
            Integer maxParticipants,
            Integer currentParticipants,
            Integer totalHours,
            String status,
            boolean active,
            LocalDateTime updatedAt) {
    }
}