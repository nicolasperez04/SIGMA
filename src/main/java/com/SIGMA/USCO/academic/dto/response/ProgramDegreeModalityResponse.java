package com.SIGMA.USCO.academic.dto.response;

import com.SIGMA.USCO.academic.dto.ProgramDegreeModalityDTO;

public record ProgramDegreeModalityResponse(boolean success, String message, ProgramDegreeModalityDTO data) {
}
