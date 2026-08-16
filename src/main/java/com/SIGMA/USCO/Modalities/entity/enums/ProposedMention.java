package com.SIGMA.USCO.Modalities.entity.enums;

/**
 * Mención honorífica que el jurado puede proponer para el trabajo evaluado.
 * Solo aplica cuando la calificación es aprobatoria.
 *
 * Según Acuerdo 071 de 2023, Artículo 10:
 *   - MERITORIOUS  → Impacto positivo en al menos una dimensión (académica, social, económica y/o tecnológica).
 *                    Otorgada por Consejo de Facultad a solicitud unánime del jurado.
 *   - LAUREATE     → Cumple condiciones de Meritorio Y evidencia carácter innovador o aporte al estado del arte.
 *                    Otorgada por Consejo Académico, a solicitud del Consejo de Facultad.
 *   - NONE         → No se propone ninguna mención especial.
 */
public enum ProposedMention {

    /** Sin mención especial */
    NONE,

    /** Mención Meritoria */
    MERITORIOUS,

    /** Mención Laureada */
    LAUREATE
}

