package com.SIGMA.USCO.Modalities.Entity.enums;

/**
 * Enum que representa todos los estados posibles del proceso de modalidad de grado de un estudiante.
 *
 * FLUJO COMPLETO (estados principales):
 * 1. MODALITY_SELECTED - Estudiante selecciona modalidad
 * 2. UNDER_REVIEW_PROGRAM_HEAD - Jefe de programa revisa
 * 3. READY_FOR_PROGRAM_CURRICULUM_COMMITTEE - Pasa al comité
 * 4. UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE - Comité revisa
 * 5. PROPOSAL_APPROVED - Propuesta aprobada
 * 6. DEFENSE_SCHEDULED - Sustentación programada
 * 7. EXAMINERS_ASSIGNED - Jueces asignados (NUEVO)
 * 8. DEFENSE_COMPLETED - Sustentación realizada
 * 9. UNDER_EVALUATION_PRIMARY_EXAMINERS - Jueces principales evalúan (NUEVO)
 * 10. EVALUATION_COMPLETED / DISAGREEMENT_REQUIRES_TIEBREAKER - Resultado (NUEVO)
 * 11. GRADED_APPROVED / GRADED_FAILED - Calificación final
 */
public enum ModalityProcessStatus {
    // ========== SELECCIÓN Y REVISIÓN INICIAL ==========
    MODALITY_SELECTED,
    UNDER_REVIEW_PROGRAM_HEAD,
    CORRECTIONS_REQUESTED_PROGRAM_HEAD,
    CORRECTIONS_SUBMITTED,

    CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD,


    CORRECTIONS_SUBMITTED_TO_COMMITTEE,


    CORRECTIONS_SUBMITTED_TO_EXAMINERS,

    CORRECTIONS_APPROVED,
    CORRECTIONS_REJECTED_FINAL,

    // ========== REVISIÓN DE COMITÉ ==========
    READY_FOR_PROGRAM_CURRICULUM_COMMITTEE,
    UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE,
    CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE,


    READY_FOR_DIRECTOR_ASSIGNMENT,

    READY_FOR_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE,
    APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE,



    PROPOSAL_APPROVED,

    // ========== REVISIÓN FINAL POR JEFATURA (PASO INTERMEDIO ANTES DE JURADOS) ==========
    /**
     * El director notificó a jefatura que los documentos finales están listos.
     * Jefatura de programa debe revisarlos antes de notificar a los jurados.
     */
    PENDING_PROGRAM_HEAD_FINAL_REVIEW,

    /**
     * Jefatura revisó los documentos finales y los aprobó.
     * Ahora jefatura notifica a los jurados para que revisen la documentación.
     */
    APPROVED_BY_PROGRAM_HEAD_FINAL_REVIEW,

    // ========== PROGRAMACIÓN DE SUSTENTACIÓN ==========
    DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR,
    DEFENSE_SCHEDULED,

    // ========== ASIGNACIÓN DE JUECES (NUEVO) ==========
    /**
     * Los jueces/examinadores han sido asignados a la sustentación
     * Se asignan 2 jueces principales en este estado
     * Próximo paso: DEFENSE_COMPLETED (realización de la sustentación)
     */
    EXAMINERS_ASSIGNED,
    READY_FOR_EXAMINERS,

    /**
     * Todos los documentos obligatorios (MANDATORY) de la propuesta han sido aprobados
     * por ambos jurados principales (o por el jurado de desempate si hubo conflicto).
     * La modalidad puede avanzar a PROPOSAL_APPROVED.
     */
    DOCUMENTS_APPROVED_BY_EXAMINERS,

    /**
     * Todos los documentos finales (SECONDARY) han sido aprobados por los jurados.
     * Estado intermedio antes de pasar a FINAL_REVIEW_COMPLETED.
     */
    SECONDARY_DOCUMENTS_APPROVED_BY_EXAMINERS,

    /**
     * Los 2 jurados principales emitieron decisiones distintas sobre un documento
     * (uno aprueba, el otro rechaza). Se requiere asignar jurado de desempate.
     */
    DOCUMENT_REVIEW_TIEBREAKER_REQUIRED,

    /**
     * El estudiante ha solicitado la edición de un documento MANDATORY que ya fue
     * aprobado por los jurados. La solicitud está pendiente de votación por los jurados.
     */
    EDIT_REQUESTED_BY_STUDENT,

    CORRECTIONS_REQUESTED_EXAMINERS,

    READY_FOR_DEFENSE,

    FINAL_REVIEW_COMPLETED,

    // ========== SUSTENTACIÓN Y EVALUACIÓN ==========
    /**
     * La sustentación fue realizada
     * Los jueces están listos para registrar sus evaluaciones
     * Próximo paso: UNDER_EVALUATION_PRIMARY_EXAMINERS
     */
    DEFENSE_COMPLETED,

    /**
     * Los jueces principales están evaluando la sustentación
     * Cada jurado registra su calificación y decisión de forma independiente
     * Próximo paso: EVALUATION_COMPLETED o DISAGREEMENT_REQUIRES_TIEBREAKER
     */
    UNDER_EVALUATION_PRIMARY_EXAMINERS,

    /**
     * Los 2 jueces principales NO llegaron a un acuerdo
     * Se requiere asignar un tercer jurado (desempate)
     * Próximo paso: UNDER_EVALUATION_TIEBREAKER (después de asignar el jurado)
     */
    DISAGREEMENT_REQUIRES_TIEBREAKER,

    /**
     * El jurado de desempate está evaluando
     * Su decisión será definitiva
     * Próximo paso: EVALUATION_COMPLETED
     */
    UNDER_EVALUATION_TIEBREAKER,

    /**
     * Evaluación completada por los jueces
     * Hay consenso o se resolvió el desempate
     * Próximo paso: GRADED_APPROVED o GRADED_FAILED
     */
    EVALUATION_COMPLETED,

    // ========== RESULTADO FINAL ==========
    /**
     * Los jurados propusieron una distinción honorífica (Meritoria o Laureada) de forma unánime.
     * El comité de currículo debe revisar y decidir si acepta o rechaza la distinción propuesta.
     * El estudiante ya fue APROBADO en calificación, pero la distinción está pendiente.
     */
    PENDING_DISTINCTION_COMMITTEE_REVIEW,

    GRADED_APPROVED,
    GRADED_FAILED,
    MODALITY_CLOSED,
    SEMINAR_CANCELED,

    // ========== CANCELACIONES ==========
    MODALITY_CANCELLED,
    CANCELLATION_REQUESTED,
    CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR,
    CANCELLATION_REJECTED_BY_PROJECT_DIRECTOR,
    CANCELLED_WITHOUT_REPROVAL,
    CANCELLATION_REJECTED,
    CANCELLED_BY_CORRECTION_TIMEOUT

}
