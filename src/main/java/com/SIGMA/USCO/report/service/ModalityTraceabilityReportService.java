package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.shared.util.TranslationUtils;
import com.SIGMA.USCO.Modalities.entity.*;
import com.SIGMA.USCO.Modalities.entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.repository.*;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.report.dto.ModalityTraceabilityReportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Servicio que genera los datos del reporte de trazabilidad completa de una modalidad individual.
 * Consolida toda la información: integrantes, director, jurados, documentos,
 * historial de estados y resultado final.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModalityTraceabilityReportService {

    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final ModalityProcessStatusHistoryRepository statusHistoryRepository;
    private final DefenseExaminerRepository defenseExaminerRepository;
    private final DefenseEvaluationCriteriaRepository defenseEvaluationCriteriaRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final StudentProfileRepository studentProfileRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Método principal: genera el DTO completo a partir del ID de la modalidad
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ModalityTraceabilityReportDTO generateReport(Long studentModalityId) {

        StudentModality sm = studentModalityRepository.findByIdWithMembers(studentModalityId)
                .orElseThrow(() -> new NotFoundException(
                        "Modalidad no encontrada con ID: " + studentModalityId));

        log.info("Generando reporte de trazabilidad para modalidad ID={}", studentModalityId);

        // ── Datos básicos ─────────────────────────────────────────────────────
        long totalDays = sm.getSelectionDate() != null
                ? ChronoUnit.DAYS.between(sm.getSelectionDate(), LocalDateTime.now())
                : 0L;

        // ── Integrantes ───────────────────────────────────────────────────────
        List<ModalityTraceabilityReportDTO.MemberDetailDTO> members = buildMembers(sm);

        // ── Director ──────────────────────────────────────────────────────────
        ModalityTraceabilityReportDTO.DirectorDetailDTO director = buildDirector(sm);

        // ── Jurados ───────────────────────────────────────────────────────────
        List<ModalityTraceabilityReportDTO.ExaminerDetailDTO> examiners = buildExaminers(studentModalityId);

        // ── Documentos ────────────────────────────────────────────────────────
        List<ModalityTraceabilityReportDTO.DocumentDetailDTO> documents = buildDocuments(studentModalityId);

        // ── Historial de estados ──────────────────────────────────────────────
        List<ModalityTraceabilityReportDTO.StatusHistoryEntryDTO> history = buildStatusHistory(studentModalityId);

        // ── Sustentación ──────────────────────────────────────────────────────
        ModalityTraceabilityReportDTO.DefenseInfoDTO defenseInfo = buildDefenseInfo(sm);

        // ── Resultado final ───────────────────────────────────────────────────
        ModalityTraceabilityReportDTO.FinalResultDTO finalResult = buildFinalResult(sm);

        // ── Resumen ───────────────────────────────────────────────────────────
        ModalityTraceabilityReportDTO.TraceabilitySummaryDTO summary =
                buildSummary(sm, documents, examiners, history, defenseInfo, finalResult, totalDays);

        return ModalityTraceabilityReportDTO.builder()
                .generatedAt(LocalDateTime.now())
                .generatedBy("SIGMA — Sistema de Información y Gestión Académica")
                .reportTitle("Reporte de Trazabilidad Completa — Modalidad #" + studentModalityId)
                .studentModalityId(studentModalityId)
                .modalityName(sm.getProgramDegreeModality().getDegreeModality().getName())
                .modalityType(sm.getModalityType() != null ? sm.getModalityType().name() : "INDIVIDUAL")
                .academicProgramName(sm.getProgramDegreeModality().getAcademicProgram().getName())
                .facultyName(sm.getProgramDegreeModality().getAcademicProgram().getFaculty().getName())
                .currentStatus(sm.getStatus() != null ? sm.getStatus().name() : "UNKNOWN")
                .currentStatusLabel(TranslationUtils.translateModalityProcessStatus(sm.getStatus()))
                .selectionDate(sm.getSelectionDate())
                .lastUpdated(sm.getUpdatedAt())
                .totalDaysInProcess(totalDays)
                .members(members)
                .director(director)
                .examiners(examiners)
                .documents(documents)
                .statusHistory(history)
                .defenseInfo(defenseInfo)
                .finalResult(finalResult)
                .summary(summary)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Método auxiliar: busca la modalidad por ID de estudiante
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ModalityTraceabilityReportDTO generateReportByStudentId(Long studentId) {
        // Busca la membresía activa del estudiante
        List<StudentModalityMember> memberships =
                studentModalityMemberRepository.findByStudentIdAndStatus(
                        studentId, com.SIGMA.USCO.Modalities.entity.enums.MemberStatus.ACTIVE);

        if (memberships.isEmpty()) {
            throw new NotFoundException(
                    "No se encontró una modalidad activa para el estudiante con ID: " + studentId);
        }

        // Tomamos la más reciente (en caso de que haya más de una, aunque no debería)
        StudentModalityMember membership = memberships.get(0);
        return generateReport(membership.getStudentModality().getId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Builders internos
    // ─────────────────────────────────────────────────────────────────────────

    private List<ModalityTraceabilityReportDTO.MemberDetailDTO> buildMembers(StudentModality sm) {
        List<StudentModalityMember> allMembers = sm.getMembers();
        Map<Long, StudentProfile> profs = ReportUtils.loadProfilesByUserIds(
                allMembers.stream().map(m -> m.getStudent().getId()).toList(),
                studentProfileRepository);

        return allMembers.stream().map(m -> {
            User student = m.getStudent();
            StudentProfile profile = profs.get(student.getId());

            return ModalityTraceabilityReportDTO.MemberDetailDTO.builder()
                    .userId(student.getId())
                    .fullName(student.getName() + " " + student.getLastName())
                    .email(student.getEmail())
                    .studentCode(profile != null ? profile.getStudentCode() : "N/A")
                    .semester(profile != null ? profile.getSemester() : null)
                    .gpa(profile != null ? profile.getGpa() : null)
                    .isLeader(m.getIsLeader())
                    .memberStatus(m.getStatus() != null ? m.getStatus().name() : "UNKNOWN")
                    .joinedAt(m.getJoinedAt())
                    .build();
        }).toList();
    }

    private ModalityTraceabilityReportDTO.DirectorDetailDTO buildDirector(StudentModality sm) {
        User director = sm.getProjectDirector();
        if (director == null) {
            return ModalityTraceabilityReportDTO.DirectorDetailDTO.builder()
                    .assigned(false)
                    .build();
        }
        return ModalityTraceabilityReportDTO.DirectorDetailDTO.builder()
                .userId(director.getId())
                .fullName(director.getName() + " " + director.getLastName())
                .email(director.getEmail())
                .assigned(true)
                .build();
    }

    private List<ModalityTraceabilityReportDTO.ExaminerDetailDTO> buildExaminers(Long modalityId) {
        List<DefenseExaminer> examiners = defenseExaminerRepository.findByStudentModalityId(modalityId);
        Map<Long, DefenseEvaluationCriteria> criteriaMap = ReportUtils.loadCriteriaByExaminerIds(
                examiners.stream().map(DefenseExaminer::getId).toList(),
                defenseEvaluationCriteriaRepository);

        return examiners.stream().map(e -> {
            String typeLabel = TranslationUtils.translateExaminerType(e.getExaminerType());
            User examiner = e.getExaminer();
            
            // Recuperar evaluación del jurado si existe
            ModalityTraceabilityReportDTO.ExaminerEvaluationDTO evaluationDTO = null;
            DefenseEvaluationCriteria eval = criteriaMap.get(e.getId());

            if (eval != null) {
                evaluationDTO = ModalityTraceabilityReportDTO.ExaminerEvaluationDTO.builder()
                        .domainAndClarity(eval.getDomainAndClarity() != null ? eval.getDomainAndClarity().name() : null)
                        .synthesisAndCommunication(eval.getSynthesisAndCommunication() != null ? eval.getSynthesisAndCommunication().name() : null)
                        .argumentationAndResponse(eval.getArgumentationAndResponse() != null ? eval.getArgumentationAndResponse().name() : null)
                        .innovationAndImpact(eval.getInnovationAndImpact() != null ? eval.getInnovationAndImpact().name() : null)
                        .professionalPresentation(eval.getProfessionalPresentation() != null ? eval.getProfessionalPresentation().name() : null)
                        .entrepreneurshipPresentationSupportMaterial(eval.getEntrepreneurshipPresentationSupportMaterial() != null ? eval.getEntrepreneurshipPresentationSupportMaterial().name() : null)
                        .entrepreneurshipCoherentBusinessObjectives(eval.getEntrepreneurshipCoherentBusinessObjectives() != null ? eval.getEntrepreneurshipCoherentBusinessObjectives().name() : null)
                        .entrepreneurshipMethodologyTechnicalApproach(eval.getEntrepreneurshipMethodologyTechnicalApproach() != null ? eval.getEntrepreneurshipMethodologyTechnicalApproach().name() : null)
                        .entrepreneurshipAnalyticalCreativeCapacity(eval.getEntrepreneurshipAnalyticalCreativeCapacity() != null ? eval.getEntrepreneurshipAnalyticalCreativeCapacity().name() : null)
                        .entrepreneurshipDefenseSustentation(eval.getEntrepreneurshipDefenseSustentation() != null ? eval.getEntrepreneurshipDefenseSustentation().name() : null)
                        .grade(eval.getGrade())
                        .proposedMention(eval.getProposedMention() != null ? eval.getProposedMention().name() : null)
                        .observations(eval.getObservations())
                        .isFinalDecision(eval.getIsFinalDecision())
                        .evaluatedAt(eval.getEvaluatedAt())
                        .rubricType(eval.getRubricType() != null ? eval.getRubricType().name() : null)
                        .build();
            }
            
            return ModalityTraceabilityReportDTO.ExaminerDetailDTO.builder()
                    .userId(examiner.getId())
                    .fullName(examiner.getName() + " " + examiner.getLastName())
                    .email(examiner.getEmail())
                    .examinerType(e.getExaminerType() != null ? e.getExaminerType().name() : "")
                    .examinerTypeLabel(typeLabel)
                    .assignmentDate(e.getAssignmentDate())
                    .evaluation(evaluationDTO)
                    .build();
        }).toList();
    }

    private List<ModalityTraceabilityReportDTO.DocumentDetailDTO> buildDocuments(Long modalityId) {
        List<StudentDocument> docs = studentDocumentRepository.findByStudentModalityId(modalityId);
        return docs.stream().map(d -> {
            DocumentType docType = d.getDocumentConfig().getDocumentType();
            String docTypeLabel = docType == DocumentType.MANDATORY ? "Obligatorio" : "Secundario";
            return ModalityTraceabilityReportDTO.DocumentDetailDTO.builder()
                    .documentId(d.getId())
                    .documentName(d.getDocumentConfig().getDocumentName())
                    .documentType(docType != null ? docType.name() : "")
                    .documentTypeLabel(docTypeLabel)
                    .fileName(d.getFileName())
                    .currentStatus(d.getStatus() != null ? d.getStatus().name() : "PENDING")
                    .currentStatusLabel(TranslationUtils.translateDocumentStatus(d.getStatus()))
                    .uploadDate(d.getUploadDate())
                    .notes(d.getNotes())
                    .build();
        }).toList();
    }

    private List<ModalityTraceabilityReportDTO.StatusHistoryEntryDTO> buildStatusHistory(Long modalityId) {
        List<ModalityProcessStatusHistory> history =
                statusHistoryRepository.findByStudentModalityIdOrderByChangeDateAsc(modalityId);

        List<ModalityTraceabilityReportDTO.StatusHistoryEntryDTO> result = new ArrayList<>();

        for (int i = 0; i < history.size(); i++) {
            ModalityProcessStatusHistory entry = history.get(i);

            // Calcular días en este estado
            long daysInState = 0L;
            if (entry.getChangeDate() != null) {
                LocalDateTime next = (i + 1 < history.size() && history.get(i + 1).getChangeDate() != null)
                        ? history.get(i + 1).getChangeDate()
                        : LocalDateTime.now();
                daysInState = ChronoUnit.DAYS.between(entry.getChangeDate(), next);
            }

            User responsible = entry.getResponsible();
            result.add(ModalityTraceabilityReportDTO.StatusHistoryEntryDTO.builder()
                    .entryId(entry.getId())
                    .status(entry.getStatus() != null ? entry.getStatus().name() : "")
                    .statusLabel(TranslationUtils.translateModalityProcessStatus(entry.getStatus()))
                    .changeDate(entry.getChangeDate())
                    .responsibleName(responsible != null
                            ? responsible.getName() + " " + responsible.getLastName()
                            : "Sistema")
                    .responsibleEmail(responsible != null ? responsible.getEmail() : "")
                    .observations(TranslationUtils.localizeObservations(entry.getObservations()))
                    .daysInThisStatus(daysInState)
                    .build());
        }
        return result;
    }

    private ModalityTraceabilityReportDTO.DefenseInfoDTO buildDefenseInfo(StudentModality sm) {
        return ModalityTraceabilityReportDTO.DefenseInfoDTO.builder()
                .defenseDate(sm.getDefenseDate())
                .defenseLocation(sm.getDefenseLocation())
                .defenseScheduled(sm.getDefenseDate() != null)
                .defenseCompleted(sm.getStatus() == ModalityProcessStatus.DEFENSE_COMPLETED
                        || sm.getStatus() == ModalityProcessStatus.EVALUATION_COMPLETED
                        || sm.getStatus() == ModalityProcessStatus.GRADED_APPROVED
                        || sm.getStatus() == ModalityProcessStatus.GRADED_FAILED
                        || sm.getStatus() == ModalityProcessStatus.MODALITY_CLOSED)
                .build();
    }

    private ModalityTraceabilityReportDTO.FinalResultDTO buildFinalResult(StudentModality sm) {
        boolean hasResult = sm.getFinalGrade() != null || sm.getAcademicDistinction() != null;
        boolean approved = false;
        if (sm.getAcademicDistinction() != null) {
            approved = sm.getAcademicDistinction() == AcademicDistinction.AGREED_APPROVED
                    || sm.getAcademicDistinction() == AcademicDistinction.AGREED_MERITORIOUS
                    || sm.getAcademicDistinction() == AcademicDistinction.AGREED_LAUREATE
                    || sm.getAcademicDistinction() == AcademicDistinction.TIEBREAKER_APPROVED
                    || sm.getAcademicDistinction() == AcademicDistinction.TIEBREAKER_MERITORIOUS
                    || sm.getAcademicDistinction() == AcademicDistinction.TIEBREAKER_LAUREATE;
        }
        return ModalityTraceabilityReportDTO.FinalResultDTO.builder()
                .finalGrade(sm.getFinalGrade())
                .academicDistinction(sm.getAcademicDistinction() != null
                        ? sm.getAcademicDistinction().name() : null)
                .academicDistinctionLabel(TranslationUtils.translateAcademicDistinction(sm.getAcademicDistinction()))
                .approved(approved)
                .hasResult(hasResult)
                .build();
    }

    private ModalityTraceabilityReportDTO.TraceabilitySummaryDTO buildSummary(
            StudentModality sm,
            List<ModalityTraceabilityReportDTO.DocumentDetailDTO> documents,
            List<ModalityTraceabilityReportDTO.ExaminerDetailDTO> examiners,
            List<ModalityTraceabilityReportDTO.StatusHistoryEntryDTO> history,
            ModalityTraceabilityReportDTO.DefenseInfoDTO defenseInfo,
            ModalityTraceabilityReportDTO.FinalResultDTO finalResult,
            long totalDays) {

        long mandatory = documents.stream()
                .filter(d -> "MANDATORY".equals(d.getDocumentType())).count();
        long secondary = documents.stream()
                .filter(d -> "SECONDARY".equals(d.getDocumentType())).count();
        long approved = documents.stream()
                .filter(d -> d.getCurrentStatus() != null &&
                        (d.getCurrentStatus().contains("ACCEPTED") || d.getCurrentStatus().contains("APPROVED")))
                .count();
        long pending = documents.stream()
                .filter(d -> "PENDING".equals(d.getCurrentStatus())).count();
        long rejected = documents.stream()
                .filter(d -> d.getCurrentStatus() != null && d.getCurrentStatus().contains("REJECTED"))
                .count();

        return ModalityTraceabilityReportDTO.TraceabilitySummaryDTO.builder()
                .totalStatusChanges(history.size())
                .totalDocumentsUploaded(documents.size())
                .totalMandatoryDocuments((int) mandatory)
                .totalSecondaryDocuments((int) secondary)
                .approvedDocuments((int) approved)
                .pendingDocuments((int) pending)
                .rejectedDocuments((int) rejected)
                .totalDaysInProcess(totalDays)
                .directorAssigned(sm.getProjectDirector() != null)
                .examinersAssigned(!examiners.isEmpty())
                .totalExaminers(examiners.size())
                .defenseCompleted(Boolean.TRUE.equals(defenseInfo.getDefenseCompleted()))
                .finalResultAvailable(finalResult.getHasResult())
                .build();
    }
}

