package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.documents.dto.DocumentEditRequestResponseDTO;

import java.util.List;

/**
 * Respuesta de getMyDocumentEditRequestsByModality: claves exactas del Map anterior
 * {success, studentModalityId, totalRequests, editRequests}.
 */
public record ModalityEditRequestsResponse(
        boolean success,
        Long studentModalityId,
        int totalRequests,
        List<DocumentEditRequestResponseDTO> editRequests) {
}