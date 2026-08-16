package com.SIGMA.USCO.Modalities.entity.enums;

/**
 * Enum que representa la decisión individual de un juez sobre la sustentación de un estudiante.
 *
 * Cada juez debe decidir:
 * 1. Si aprueba o rechaza la modalidad
 * 2. Si aprueba, qué distinción académica merece (si aplica)
 *
 * NOTAS:
 * - Esta es la decisión individual de cada juez
 * - La decisión final se determina por consenso o desempate
 * - Cada decisión debe ir acompañada de una calificación numérica (0-5)
 */
public enum ExaminerDecision {

    APPROVED_NO_DISTINCTION,



    APPROVED_MERITORIOUS,


    APPROVED_LAUREATE,


    REJECTED
}

