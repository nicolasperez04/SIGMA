package com.SIGMA.USCO.Modalities.Entity;

import com.SIGMA.USCO.Modalities.Entity.enums.CriteriaRating;
import com.SIGMA.USCO.Modalities.Entity.enums.DefenseRubricType;
import com.SIGMA.USCO.Modalities.Entity.enums.ProposedMention;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Almacena la evaluación completa que el jurado realiza durante la sustentación.
 * Incluye tanto la rúbrica de criterios (I/A/B/E) como la nota cuantitativa,
 * la decisión final, observaciones y la mención propuesta.
 *
 * Los 5 criterios evaluados (valoración I / A / B / E):
 *   1. Dominio del tema y claridad conceptual          (ponderación 30 %)
 *   2. Capacidad de síntesis y comunicación oral       (ponderación 15 %)
 *   3. Argumentación y capacidad de respuesta          (ponderación 30 %)
 *   4. Innovación, impacto y aplicación del trabajo    (ponderación 15 %)
 *   5. Presentación profesional y material de apoyo   (ponderación 10 %)
 *
 * Regla de aprobación (Acuerdo 071/2023):
 *   - Nota ≥ 3.5 Y
 *   - Sin "I" en criterios 1 ni 3
 *
 * Se asocia directamente con {@link DefenseExaminer}.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "defense_evaluation_criteria")
public class DefenseEvaluationCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Jurado que registra esta evaluación (relación principal) */
    @ManyToOne(optional = false)
    @JoinColumn(name = "defense_examiner_id", nullable = false)
    private DefenseExaminer defenseExaminer;

    @Enumerated(EnumType.STRING)
    @Column(name = "rubric_type", nullable = false, length = 100)
    @Builder.Default
    private DefenseRubricType rubricType = DefenseRubricType.STANDARD;

    // ── Criterios de rúbrica ──────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "domain_and_clarity", nullable = false, length = 100)
    private CriteriaRating domainAndClarity;

    @Enumerated(EnumType.STRING)
    @Column(name = "synthesis_and_communication", nullable = false, length = 100)
    private CriteriaRating synthesisAndCommunication;

    @Enumerated(EnumType.STRING)
    @Column(name = "argumentation_and_response", nullable = false, length = 100)
    private CriteriaRating argumentationAndResponse;

    @Enumerated(EnumType.STRING)
    @Column(name = "innovation_and_impact", nullable = false, length = 100)
    private CriteriaRating innovationAndImpact;

    @Enumerated(EnumType.STRING)
    @Column(name = "professional_presentation", nullable = false, length = 100)
    private CriteriaRating professionalPresentation;

    // ── Criterios de rúbrica: Emprendimiento y fortalecimiento de empresa ──

    @Enumerated(EnumType.STRING)
    @Column(name = "ent_presentation_support_material", length = 100)
    private CriteriaRating entrepreneurshipPresentationSupportMaterial;

    @Enumerated(EnumType.STRING)
    @Column(name = "ent_coherent_business_objectives", length = 100)
    private CriteriaRating entrepreneurshipCoherentBusinessObjectives;

    @Enumerated(EnumType.STRING)
    @Column(name = "ent_methodology_technical_approach", length = 100)
    private CriteriaRating entrepreneurshipMethodologyTechnicalApproach;

    @Enumerated(EnumType.STRING)
    @Column(name = "ent_analytical_creative_capacity", length = 100)
    private CriteriaRating entrepreneurshipAnalyticalCreativeCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "ent_defense_sustentation", length = 100)
    private CriteriaRating entrepreneurshipDefenseSustentation;

    // ── Mención propuesta ─────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "proposed_mention", nullable = false, length = 200)
    @Builder.Default
    private ProposedMention proposedMention = ProposedMention.NONE;

    // ── Calificación y decisión del jurado ────────────────────────────────────

    /** Nota cuantitativa proporcionada por el jurado (0.0 – 5.0) */
    @Column(name = "grade", nullable = false)
    private Double grade;


    /** Observaciones / retroalimentación del jurado */
    @Column(name = "observations", length = 2000)
    private String observations;

    /** Indica si esta evaluación es la decisión final (para el jurado de desempate o en consenso) */
    @Column(name = "is_final_decision", nullable = false)
    @Builder.Default
    private Boolean isFinalDecision = false;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;
}
