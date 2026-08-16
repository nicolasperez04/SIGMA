package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.dto.SeminarDetailDTO;

/**
 * Respuesta de getSeminarDetailForProgramHead: claves exactas del Map anterior
 * {success, seminar}.
 */
public record SeminarDetailResponse(
        boolean success,
        SeminarDetailDTO seminar) {
}