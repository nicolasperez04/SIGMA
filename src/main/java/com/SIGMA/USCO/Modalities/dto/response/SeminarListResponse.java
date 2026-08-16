package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.dto.SeminarListDTO;

import java.util.List;

/**
 * Respuesta de listSeminarsForProgramHead: claves exactas del Map anterior
 * {success, seminars, total}.
 */
public record SeminarListResponse(
        boolean success,
        List<SeminarListDTO> seminars,
        int total) {
}