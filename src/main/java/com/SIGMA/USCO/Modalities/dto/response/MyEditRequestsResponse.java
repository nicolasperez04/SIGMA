package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.documents.dto.DocumentEditRequestResponseDTO;

import java.util.List;

/**
 * Respuesta de getMyDocumentEditRequests: claves exactas del Map anterior
 * {success, totalRequests, editRequests}.
 */
public record MyEditRequestsResponse(
        boolean success,
        int totalRequests,
        List<DocumentEditRequestResponseDTO> editRequests) {
}