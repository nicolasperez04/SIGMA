package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Respuesta de getCorrectionDeadlineStatus. Cubre las 2 formas del Map anterior:
 * - sin corrección: {hasCorrectionRequest, currentStatus, message}
 * - con corrección: {hasCorrectionRequest, currentStatus, correctionRequestDate, correctionDeadline,
 *                    daysRemaining, isExpired, reminderSent}
 * Los campos con @NON_NULL solo se serializan en la rama que los produce.
 */
public record CorrectionDeadlineStatusResponse(
        boolean hasCorrectionRequest,
        ModalityProcessStatus currentStatus,
        @JsonInclude(JsonInclude.Include.NON_NULL) String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) LocalDateTime correctionRequestDate,
        @JsonInclude(JsonInclude.Include.NON_NULL) LocalDateTime correctionDeadline,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long daysRemaining,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean isExpired,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean reminderSent) {
}