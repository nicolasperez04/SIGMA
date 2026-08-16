package com.SIGMA.USCO.academic.dto.response;

import com.SIGMA.USCO.academic.dto.FacultyDTO;

import java.util.List;

public record FacultyListResponse(List<FacultyDTO> faculties) {
}
