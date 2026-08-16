package com.SIGMA.USCO.Modalities.dto.response;

/**
 * Shape {success, message} de "no hay evaluación registrada" en
 * getFinalDefenseEvaluationForExaminer y getExaminerEvaluationForModality.
 */
public record ExaminerEvaluationNotFoundResponse(Boolean success, String message) {
}