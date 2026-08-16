package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;

import java.time.LocalDateTime;

/**
 * Item de getStudentDocuments: claves exactas del Map anterior
 * {studentDocumentId, documentName, documentType, status, notes, uploadedAt, filePath}.
 * OJO: "filePath" es la ruta absoluta del filesystem (pendiente de decisión de negocio,
 * el contrato se preserva en esta fase).
 */
public record StudentDocumentResponse(
        Long studentDocumentId,
        String documentName,
        DocumentType documentType,
        DocumentStatus status,
        String notes,
        LocalDateTime uploadedAt,
        String filePath) {
}