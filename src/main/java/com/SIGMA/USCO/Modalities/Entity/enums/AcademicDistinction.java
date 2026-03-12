package com.SIGMA.USCO.Modalities.Entity.enums;


public enum AcademicDistinction {

    NO_DISTINCTION,
    AGREED_APPROVED,
    AGREED_MERITORIOUS,
    AGREED_LAUREATE,
    AGREED_REJECTED,
    DISAGREEMENT_PENDING_TIEBREAKER,
    TIEBREAKER_APPROVED,
    TIEBREAKER_MERITORIOUS,
    TIEBREAKER_LAUREATE,
    TIEBREAKER_REJECTED,
    REJECTED_BY_COMMITTEE,

    /**
     * Los jurados principales propusieron unánimemente mención Meritoria.
     * Pendiente de decisión del comité de currículo.
     */
    PENDING_COMMITTEE_MERITORIOUS,

    /**
     * Los jurados principales propusieron unánimemente mención Laureada.
     * Pendiente de decisión del comité de currículo.
     */
    PENDING_COMMITTEE_LAUREATE,

    /**
     * El jurado de desempate propuso mención Meritoria.
     * Pendiente de decisión del comité de currículo.
     */
    TIEBREAKER_PENDING_COMMITTEE_MERITORIOUS,

    /**
     * El jurado de desempate propuso mención Laureada.
     * Pendiente de decisión del comité de currículo.
     */
    TIEBREAKER_PENDING_COMMITTEE_LAUREATE
}
