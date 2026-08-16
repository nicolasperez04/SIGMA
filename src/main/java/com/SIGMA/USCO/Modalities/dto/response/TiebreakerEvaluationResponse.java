package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;

/**
 * Shape de registerFinalDefenseEvaluation cuando el jurado de desempate
 * registra su evaluación: {success, isTiebreaker, finalStatus,
 * academicDistinction, finalGrade, pendingDistinctionReview, message}.
 */
public record TiebreakerEvaluationResponse(Boolean success, Boolean isTiebreaker,
                                           ModalityProcessStatus finalStatus,
                                           AcademicDistinction academicDistinction, Double finalGrade,
                                           Boolean pendingDistinctionReview, String message) {
}