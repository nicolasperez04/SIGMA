package com.SIGMA.USCO.Modalities.dto.response;

import java.util.List;

public record ProgramStudentsResponse(boolean success, Long academicProgramId, String academicProgramName,
                                      int totalStudents, List<StudentSummary> students) {

    public record StudentSummary(Long studentId, String studentCode, String name, String lastName, String fullName,
                                 String email, Long semester, Double gpa, Long approvedCredits, int totalModalities,
                                 Long activeModalityId, String activeModalityName, String activeModalityStatus,
                                 String activeModalityStatusDescription, String activeModalityDirector) {
    }
}