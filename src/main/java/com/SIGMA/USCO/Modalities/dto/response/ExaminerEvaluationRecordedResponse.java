package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.dto.ExaminerEvaluationDTO;

/**
 * Shape de éxito de getExaminerEvaluationForModality: {success, evaluation}.
 */
public record ExaminerEvaluationRecordedResponse(Boolean success, ExaminerEvaluationDTO evaluation) {
}