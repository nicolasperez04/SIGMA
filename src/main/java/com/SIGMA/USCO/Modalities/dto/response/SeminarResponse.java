package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.dto.SeminarResponseDTO;

import java.util.List;

/**
 * Respuesta de listActiveSeminarsWithSeats: claves exactas del Map anterior
 * {success, seminars}.
 */
public record SeminarResponse(
        boolean success,
        List<SeminarResponseDTO> seminars) {
}