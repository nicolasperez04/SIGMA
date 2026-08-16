package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;

/**
 * Shape de registerFinalDefenseEvaluation cuando los jurados primarios
 * discrepan y se requiere jurado de desempate:
 * {success, hasConsensus, requiresTiebreaker, status, message}.
 */
public record TiebreakerRequiredResponse(Boolean success, Boolean hasConsensus, Boolean requiresTiebreaker,
                                         ModalityProcessStatus status, String message) {
}