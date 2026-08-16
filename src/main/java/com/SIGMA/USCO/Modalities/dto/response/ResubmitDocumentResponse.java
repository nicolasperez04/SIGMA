package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;

/**
 * Respuesta de resubmitCorrectedDocument: claves exactas del Map anterior
 * {success, message, documentId, newStatus} (newStatus era el enum DocumentStatus).
 */
public record ResubmitDocumentResponse(
        boolean success,
        String message,
        Long documentId,
        DocumentStatus newStatus) {
}