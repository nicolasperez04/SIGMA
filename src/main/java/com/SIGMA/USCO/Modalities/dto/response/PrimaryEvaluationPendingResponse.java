package com.SIGMA.USCO.Modalities.dto.response;

/**
 * Shape de registerFinalDefenseEvaluation cuando un jurado primario registró
 * su evaluación y falta la del otro: {success, message, grade, approved}.
 */
public record PrimaryEvaluationPendingResponse(Boolean success, String message, Double grade, Boolean approved) {
}