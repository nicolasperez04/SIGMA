package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;

/**
 * Shape de reviewStudentDocument (jefatura de programa):
 * {message, documentId, newStatus}.
 */
public record ReviewStudentDocumentResponse(String message, Long documentId, DocumentStatus newStatus) {
}