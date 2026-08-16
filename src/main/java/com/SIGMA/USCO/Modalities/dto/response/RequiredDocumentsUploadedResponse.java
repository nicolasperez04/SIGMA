package com.SIGMA.USCO.Modalities.dto.response;

import java.util.List;

/**
 * Respuesta de validateAllRequiredDocumentsUploaded: claves exactas del Map anterior
 * {allDocumentsUploaded, totalRequired, totalUploaded, missingDocuments, missingCount}.
 */
public record RequiredDocumentsUploadedResponse(
        boolean allDocumentsUploaded,
        int totalRequired,
        int totalUploaded,
        List<MissingDocumentInfo> missingDocuments,
        int missingCount) {

    public record MissingDocumentInfo(
            Long documentId,
            String documentName,
            String documentType,
            String description) {
    }
}