package com.SIGMA.USCO.Modalities.dto.response;

import java.util.List;

/**
 * Respuesta de validateAllDocumentsUploaded: claves exactas del Map anterior
 * {canContinue, missingDocuments}.
 */
public record ValidateAllDocumentsUploadedResponse(
        boolean canContinue,
        List<String> missingDocuments) {
}