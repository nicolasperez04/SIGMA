package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.Entity.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FinalDefenseResponse {

    private Long studentModalityId;

    private String studentName;
    private String studentEmail;

    private ModalityProcessStatus finalStatus;
    private boolean approved;

    private AcademicDistinction academicDistinction;

    private Double finalGrade;

    private String observations;

    private LocalDateTime evaluationDate;

    private String evaluatedBy;

    private boolean hasConsensus;

    private boolean wasTiebreaker;

    private List<ExaminerEvaluationDetail> examinerEvaluations;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExaminerEvaluationDetail {
        private String examinerName;
        private String examinerType;
        private Double grade;
        private boolean approved;
        private String observations;
        private LocalDateTime evaluationDate;
        private boolean isFinalDecision;

        /** Criterios de rúbrica (null si la modalidad no requiere formulario) */
        private CriteriaDetail evaluationCriteria;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CriteriaDetail {
        private DefenseRubricType rubricType;
        private CriteriaRating domainAndClarity;
        private CriteriaRating synthesisAndCommunication;
        private CriteriaRating argumentationAndResponse;
        private CriteriaRating innovationAndImpact;
        private CriteriaRating professionalPresentation;
        private CriteriaRating entrepreneurshipPresentationSupportMaterial;
        private CriteriaRating entrepreneurshipCoherentBusinessObjectives;
        private CriteriaRating entrepreneurshipMethodologyTechnicalApproach;
        private CriteriaRating entrepreneurshipAnalyticalCreativeCapacity;
        private CriteriaRating entrepreneurshipDefenseSustentation;
        private ProposedMention proposedMention;
        private LocalDateTime evaluatedAt;
    }
}
