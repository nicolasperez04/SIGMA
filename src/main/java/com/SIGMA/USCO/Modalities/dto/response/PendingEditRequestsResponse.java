package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.documents.dto.DocumentEditRequestResponseDTO;

import java.util.List;

/**
 * Respuesta de getPendingEditRequestsForExaminer: claves exactas del Map anterior
 * {success, studentModalityId, examinerType, isTiebreaker, pendingEditRequests}.
 */
public record PendingEditRequestsResponse(
        boolean success,
        Long studentModalityId,
        String examinerType,
        boolean isTiebreaker,
        List<DocumentEditRequestResponseDTO> pendingEditRequests) {
}