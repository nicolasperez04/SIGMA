package com.SIGMA.USCO.Modalities.dto.response;

import java.util.List;

/**
 * Respuesta de validateAllDocumentsAcceptedForCommittee: claves exactas del Map anterior
 * {allAccepted, notAcceptedDocuments, notAcceptedCount, totalRequired}.
 */
public record DocumentsAcceptedForCommitteeResponse(
        boolean allAccepted,
        List<NotAcceptedDocumentInfo> notAcceptedDocuments,
        int notAcceptedCount,
        int totalRequired) {

    public record NotAcceptedDocumentInfo(
            Long documentId,
            String documentName,
            String documentType,
            String currentStatus) {
    }
}