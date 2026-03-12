package com.SIGMA.USCO.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO principal para el reporte de trazabilidad completa de una modalidad individual.
 * Contiene toda la información relevante para que el comité tenga visibilidad total.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModalityTraceabilityReportDTO {

    // ── Metadatos del reporte ─────────────────────────────────────────────────
    private LocalDateTime generatedAt;
    private String generatedBy;
    private String reportTitle;

    // ── Información general de la modalidad ──────────────────────────────────
    private Long studentModalityId;
    private String modalityName;
    private String modalityType;         // INDIVIDUAL / GROUP
    private String academicProgramName;
    private String facultyName;
    private String currentStatus;
    private String currentStatusLabel;
    private LocalDateTime selectionDate;
    private LocalDateTime lastUpdated;
    private Long totalDaysInProcess;

    // ── Integrantes ───────────────────────────────────────────────────────────
    private List<MemberDetailDTO> members;

    // ── Director del proyecto ─────────────────────────────────────────────────
    private DirectorDetailDTO director;

    // ── Jurados ───────────────────────────────────────────────────────────────
    private List<ExaminerDetailDTO> examiners;

    // ── Documentos subidos ────────────────────────────────────────────────────
    private List<DocumentDetailDTO> documents;

    // ── Historial de estados (trazabilidad) ───────────────────────────────────
    private List<StatusHistoryEntryDTO> statusHistory;

    // ── Sustentación ─────────────────────────────────────────────────────────
    private DefenseInfoDTO defenseInfo;

    // ── Resultado final ───────────────────────────────────────────────────────
    private FinalResultDTO finalResult;

    // ── Resumen estadístico ───────────────────────────────────────────────────
    private TraceabilitySummaryDTO summary;

    // ─────────────────────────────────────────────────────────────────────────
    // DTOs internos
    // ─────────────────────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDetailDTO {
        private Long userId;
        private String fullName;
        private String email;
        private String studentCode;
        private Long semester;
        private Double gpa;
        private Boolean isLeader;
        private String memberStatus;
        private LocalDateTime joinedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectorDetailDTO {
        private Long userId;
        private String fullName;
        private String email;
        private Boolean assigned;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExaminerDetailDTO {
        private Long userId;
        private String fullName;
        private String email;
        private String examinerType;
        private String examinerTypeLabel;
        private LocalDateTime assignmentDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentDetailDTO {
        private Long documentId;
        private String documentName;
        private String documentType;       // MANDATORY / SECONDARY
        private String documentTypeLabel;
        private String fileName;
        private String currentStatus;
        private String currentStatusLabel;
        private LocalDateTime uploadDate;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusHistoryEntryDTO {
        private Long entryId;
        private String status;
        private String statusLabel;
        private LocalDateTime changeDate;
        private String responsibleName;
        private String responsibleEmail;
        private String observations;
        private Long daysInThisStatus;  // días que estuvo en este estado antes de cambiar
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefenseInfoDTO {
        private LocalDateTime defenseDate;
        private String defenseLocation;
        private Boolean defenseScheduled;
        private Boolean defenseCompleted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinalResultDTO {
        private Double finalGrade;
        private String academicDistinction;
        private String academicDistinctionLabel;
        private Boolean approved;
        private Boolean hasResult;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TraceabilitySummaryDTO {
        private int totalStatusChanges;
        private int totalDocumentsUploaded;
        private int totalMandatoryDocuments;
        private int totalSecondaryDocuments;
        private int approvedDocuments;
        private int pendingDocuments;
        private int rejectedDocuments;
        private Long totalDaysInProcess;
        private boolean directorAssigned;
        private boolean examinersAssigned;
        private int totalExaminers;
        private boolean defenseCompleted;
        private Boolean finalResultAvailable;
    }
}


