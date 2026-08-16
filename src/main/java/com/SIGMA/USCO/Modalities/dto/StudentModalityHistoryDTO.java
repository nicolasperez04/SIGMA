package com.SIGMA.USCO.Modalities.dto;

import java.time.LocalDateTime;

/**
 * Item del historial de modalidades del estudiante (GET /students/modalities/history).
 * Contrato con el frontend: {studentModalityId, modalityId, modalityName, currentStatus, createdAt}.
 */
public record StudentModalityHistoryDTO(
        Long studentModalityId,
        Long modalityId,
        String modalityName,
        String currentStatus,
        LocalDateTime createdAt) {
}