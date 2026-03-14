package com.SIGMA.USCO.Modalities.dto;

import com.SIGMA.USCO.Modalities.Entity.enums.CriteriaRating;
import com.SIGMA.USCO.Modalities.Entity.enums.DefenseRubricType;
import com.SIGMA.USCO.Modalities.Entity.enums.ProposedMention;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para el formulario de evaluación por criterios de rúbrica que el jurado
 * debe completar durante la sustentación de la modalidad de grado.
 *
 * Criterios (valoración I / A / B / E):
 *   1. Dominio del tema y claridad conceptual          — ponderación 30 %
 *   2. Capacidad de síntesis y comunicación oral       — ponderación 15 %
 *   3. Argumentación y capacidad de respuesta          — ponderación 30 %
 *   4. Innovación, impacto y aplicación del trabajo    — ponderación 15 %
 *   5. Presentación profesional y material de apoyo    — ponderación 10 %
 *
 * Este DTO es OPCIONAL dentro de {@link ExaminerEvaluationDTO}: solo se envía
 * cuando la modalidad exige el formulario de rúbrica (ej. Pasantía).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefenseEvaluationCriteriaDTO {

    /**
     * Tipo de rúbrica enviada por el cliente.
     * Si no se envía, el backend la determina según la modalidad.
     */
    private DefenseRubricType rubricType;

    /**
     * Criterio 1 — Dominio del tema y claridad conceptual (30 %).
     * El estudiante demuestra comprensión integral del problema, fundamentos teóricos
     * y metodología empleada.
     */
    private CriteriaRating domainAndClarity;

    /**
     * Criterio 2 — Capacidad de síntesis y comunicación oral (15 %).
     * Expone la información de forma ordenada, coherente y ajustada al tiempo asignado.
     */
    private CriteriaRating synthesisAndCommunication;

    /**
     * Criterio 3 — Argumentación y capacidad de respuesta (30 %).
     * Responde de manera fundamentada y segura a las preguntas del jurado.
     */
    private CriteriaRating argumentationAndResponse;

    /**
     * Criterio 4 — Innovación, impacto y aplicación del trabajo (15 %).
     * Explica los aportes en términos de innovación, pertinencia y posible impacto.
     */
    private CriteriaRating innovationAndImpact;

    /**
     * Criterio 5 — Presentación profesional y manejo del material de apoyo (10 %).
     * Emplea adecuadamente recursos audiovisuales y mantiene actitud profesional.
     */
    private CriteriaRating professionalPresentation;

    /**
     * Mención que el jurado propone para el trabajo (solo si aprueba).
     * Valores: NONE, MERITORIOUS, LAUREATE.
     * Si no se envía, se asume NONE.
     */
    @Builder.Default
    private ProposedMention proposedMention = ProposedMention.NONE;

    // --- Criterios para modalidad: Emprendimiento y fortalecimiento de empresa ---

    /** Presentación y manejo del material de apoyo. */
    private CriteriaRating entrepreneurshipPresentationSupportMaterial;

    /** Formulación coherente de objetivos empresariales. */
    private CriteriaRating entrepreneurshipCoherentBusinessObjectives;

    /** Metodología y enfoque técnico. */
    private CriteriaRating entrepreneurshipMethodologyTechnicalApproach;

    /** Capacidad analítica con enfoque creativo. */
    private CriteriaRating entrepreneurshipAnalyticalCreativeCapacity;

    /** Defensa y sustentación de la propuesta empresarial. */
    private CriteriaRating entrepreneurshipDefenseSustentation;
}

