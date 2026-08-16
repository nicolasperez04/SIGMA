package com.SIGMA.USCO.Modalities.dto.response;

/**
 * Shape de registerFinalDefenseEvaluation cuando los jurados primarios
 * alcanzan consenso. Claves legacy del Map original: {@code exito},
 * {@code consenso}, {@code estadoFinal}, {@code distincionAcademica},
 * {@code calificacionFinal}, {@code distincionPendienteRevision},
 * {@code mensaje}.
 * NOTA: {@code exito} es un typo histórico del Map ("success") que se preserva
 * tal cual para no romper el contrato JSON (ver inventario Fase 2).
 */
public record ConsensusEvaluationResponse(Boolean exito, Boolean consenso, String estadoFinal,
                                          String distincionAcademica, Double calificacionFinal,
                                          Boolean distincionPendienteRevision, String mensaje) {
}