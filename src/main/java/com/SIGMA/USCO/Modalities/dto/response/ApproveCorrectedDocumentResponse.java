package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;

/**
 * Shape de approveCorrectedDocument: {success, message, documentId,
 * newDocumentStatus, newModalityStatus}.
 */
public record ApproveCorrectedDocumentResponse(boolean success, String message, Long documentId,
                                               DocumentStatus newDocumentStatus,
                                               ModalityProcessStatus newModalityStatus) {
}