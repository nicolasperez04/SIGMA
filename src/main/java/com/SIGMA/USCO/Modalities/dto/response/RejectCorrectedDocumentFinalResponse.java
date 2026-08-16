package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;

/**
 * Shape de rejectCorrectedDocumentFinal: {success, message, documentId, finalStatus}.
 */
public record RejectCorrectedDocumentFinalResponse(boolean success, String message, Long documentId,
                                                   ModalityProcessStatus finalStatus) {
}