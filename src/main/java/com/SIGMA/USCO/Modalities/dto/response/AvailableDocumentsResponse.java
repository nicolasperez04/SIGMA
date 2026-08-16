package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de getAvailableDocumentsForStudent: claves exactas del Map anterior
 * {success, message, missingDocuments, studentModalityId, documents, statistics}.
 * success=false mientras falten documentos obligatorios; missingDocuments = nombres de los faltantes.
 * Los campos del item con @NON_NULL solo se serializan cuando el documento está subido.
 */
public record AvailableDocumentsResponse(
        boolean success,
        String message,
        List<String> missingDocuments,
        Long studentModalityId,
        List<AvailableDocumentDTO> documents,
        DocumentStatistics statistics) {

    public record AvailableDocumentDTO(
            Long requiredDocumentId,
            String documentName,
            String description,
            DocumentType documentType,
            String allowedFormat,
            Integer maxFileSizeMB,
            boolean uploaded,
            @JsonInclude(JsonInclude.Include.NON_NULL) Long studentDocumentId,
            @JsonInclude(JsonInclude.Include.NON_NULL) String fileName,
            @JsonInclude(JsonInclude.Include.NON_NULL) DocumentStatus status,
            @JsonInclude(JsonInclude.Include.NON_NULL) String notes,
            @JsonInclude(JsonInclude.Include.NON_NULL) LocalDateTime uploadDate) {
    }

    public record DocumentStatistics(
            long totalDocuments,
            long uploadedDocuments,
            long pendingDocuments,
            long mandatoryDocuments,
            long secondaryDocuments) {
    }
}