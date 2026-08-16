package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.documents.dto.DocumentEditRequestResponseDTO;

/**
 * Respuesta de getDocumentEditRequestDetail: claves exactas del Map anterior
 * {success, editRequest}.
 */
public record EditRequestDetailResponse(
        boolean success,
        DocumentEditRequestResponseDTO editRequest) {
}