package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Shape de reviewStudentDocumentByCommittee. Cubre las 2 formas del Map anterior;
 * newModalityStatus con @NON_NULL solo se serializa cuando el flujo lo produce:
 * - todos los MANDATORY aprobados: {success, documentId, documentName, newStatus,
 *   newModalityStatus, message}
 * - revisión simple:               {success, documentId, documentName, newStatus, message}
 */
public record CommitteeDocumentReviewResponse(boolean success, Long documentId, String documentName,
                                              DocumentStatus newStatus,
                                              @JsonInclude(JsonInclude.Include.NON_NULL) String newModalityStatus,
                                              String message) {
}