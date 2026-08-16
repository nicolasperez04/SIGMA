package com.SIGMA.USCO.Modalities.entity.enums;

/**
 * Valoración cualitativa usada en la rúbrica de evaluación de la sustentación.
 *
 * Escala cuantitativa equivalente (según rúbrica institucional):
 *   E (Excelente)    → 4.5 – 5.0
 *   B (Bueno)        → 3.6 – 4.4
 *   A (Aceptable)    → 3.0 – 3.5
 *   I (Insuficiente) → < 3.0
 */
public enum CriteriaRating {

    /** Insuficiente — calificación cuantitativa equivalente: < 3.0 */
    Insufficient,

    /** Aceptable — calificación cuantitativa equivalente: 3.0 – 3.5 */
    Acceptable,

    /** Bueno — calificación cuantitativa equivalente: 3.6 – 4.4 */
    Good,

    /** Excelente — calificación cuantitativa equivalente: 4.5 – 5.0 */
    Excellent
}

