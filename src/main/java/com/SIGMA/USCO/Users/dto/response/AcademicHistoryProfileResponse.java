package com.SIGMA.USCO.Users.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AcademicHistoryProfileResponse {

    private String message;
    private String programNameExtracted;
    private String academicProgramMatched;
    private String faculty;
    private Long approvedCredits;
    private Long programTotalCreditsInPdf;
    private Long programTotalCreditsInSigma;
    private Double gpa;
    private Long semester;
    private String semesterSource;
    private String pdfFilePath;
    private String pdfFileName;
    private Boolean pdfStored;
    private String pdfWarning;
}
