package com.SIGMA.USCO.Modalities.dto.response;

/**
 * Respuesta de requestDocumentEdit: claves exactas del Map anterior
 * {success, editRequestId, documentId, documentName, newDocumentStatus, newModalityStatus, message}.
 */
public record EditRequestCreatedResponse(
        boolean success,
        Long editRequestId,
        Long documentId,
        String documentName,
        String newDocumentStatus,
        String newModalityStatus,
        String message) {
}