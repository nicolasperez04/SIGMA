package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.entity.enums.ExaminerType;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Shape de éxito de getFinalDefenseEvaluationForExaminer:
 * {success, evaluationId, grade, approved, observations, evaluationDate,
 * isFinalDecision, examinerType, evaluationCriteria}.
 * evaluationCriteria conserva el Map anidado privado (rubrica empresarial vs
 * estándar con claves distintas) — no se tipa (YAGNI).
 */
public record ExaminerFinalEvaluationResponse(Boolean success, Long evaluationId, Double grade,
                                              Boolean approved, String observations,
                                              LocalDateTime evaluationDate, Boolean isFinalDecision,
                                              ExaminerType examinerType,
                                              Map<String, Object> evaluationCriteria) {
}