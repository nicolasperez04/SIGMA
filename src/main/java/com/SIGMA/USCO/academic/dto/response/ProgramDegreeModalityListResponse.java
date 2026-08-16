package com.SIGMA.USCO.academic.dto.response;

import com.SIGMA.USCO.academic.dto.ProgramDegreeModalityDTO;

import java.util.List;

public record ProgramDegreeModalityListResponse(boolean success, List<ProgramDegreeModalityDTO> data, int count) {
}
