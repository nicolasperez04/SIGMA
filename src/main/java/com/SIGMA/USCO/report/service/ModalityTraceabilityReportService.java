package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.Modalities.Entity.*;
import com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.*;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
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
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final StudentDocumentRepository studentDocumentRepository;
    private final StudentProfileRepository studentProfileRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Método principal: genera el DTO completo a partir del ID de la modalidad
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ModalityTraceabilityReportDTO generateReport(Long studentModalityId) {

        StudentModality sm = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new RuntimeException(
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
                .currentStatusLabel(translateStatus(sm.getStatus()))
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
                        studentId, com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus.ACTIVE);

        if (memberships.isEmpty()) {
            throw new RuntimeException(
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
        List<StudentModalityMember> allMembers = studentModalityMemberRepository
                .findByStudentModalityId(sm.getId());

        return allMembers.stream().map(m -> {
            User student = m.getStudent();
            StudentProfile profile = studentProfileRepository.findByUserId(student.getId()).orElse(null);

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
        }).collect(Collectors.toList());
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
        return examiners.stream().map(e -> {
            String typeLabel = switch (e.getExaminerType() != null ? e.getExaminerType().name() : "") {
                case "PRIMARY_EXAMINER_1" -> "Jurado Principal 1";
                case "PRIMARY_EXAMINER_2" -> "Jurado Principal 2";
                case "TIEBREAKER_EXAMINER" -> "Jurado de Desempate";
                default -> "Jurado";
            };
            User examiner = e.getExaminer();
            return ModalityTraceabilityReportDTO.ExaminerDetailDTO.builder()
                    .userId(examiner.getId())
                    .fullName(examiner.getName() + " " + examiner.getLastName())
                    .email(examiner.getEmail())
                    .examinerType(e.getExaminerType() != null ? e.getExaminerType().name() : "")
                    .examinerTypeLabel(typeLabel)
                    .assignmentDate(e.getAssignmentDate())
                    .build();
        }).collect(Collectors.toList());
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
                    .currentStatusLabel(translateDocumentStatus(d.getStatus()))
                    .uploadDate(d.getUploadDate())
                    .notes(d.getNotes())
                    .build();
        }).collect(Collectors.toList());
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
                    .statusLabel(translateStatus(entry.getStatus()))
                    .changeDate(entry.getChangeDate())
                    .responsibleName(responsible != null
                            ? responsible.getName() + " " + responsible.getLastName()
                            : "Sistema")
                    .responsibleEmail(responsible != null ? responsible.getEmail() : "")
                    .observations(entry.getObservations())
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
                .academicDistinctionLabel(translateDistinction(sm.getAcademicDistinction()))
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

    // ─────────────────────────────────────────────────────────────────────────
    // Traductores de enums a etiquetas legibles
    // ─────────────────────────────────────────────────────────────────────────

    public String translateStatus(ModalityProcessStatus status) {
        if (status == null) return "Sin estado";
        return switch (status) {
            case MODALITY_SELECTED -> "Modalidad seleccionada";
            case UNDER_REVIEW_PROGRAM_HEAD -> "En revisión por Jefatura de Programa";
            case CORRECTIONS_REQUESTED_PROGRAM_HEAD -> "Correcciones solicitadas por Jefatura";
            case CORRECTIONS_SUBMITTED -> "Correcciones enviadas";
            case CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD -> "Correcciones enviadas a Jefatura";
            case CORRECTIONS_SUBMITTED_TO_COMMITTEE -> "Correcciones enviadas al Comité";
            case CORRECTIONS_SUBMITTED_TO_EXAMINERS -> "Correcciones enviadas a Jurados";
            case CORRECTIONS_APPROVED -> "Correcciones aprobadas";
            case CORRECTIONS_REJECTED_FINAL -> "Correcciones rechazadas (final)";
            case READY_FOR_PROGRAM_CURRICULUM_COMMITTEE -> "Lista para Comité de Currículo";
            case UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE -> "En revisión por Comité de Currículo";
            case CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE -> "Correcciones solicitadas por Comité";
            case READY_FOR_DIRECTOR_ASSIGNMENT -> "Lista para asignación de Director";
            case READY_FOR_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE -> "Lista para aprobación por Comité";
            case APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE -> "Aprobada por Comité de Currículo";
            case PROPOSAL_APPROVED -> "Propuesta aprobada";
            case PENDING_PROGRAM_HEAD_FINAL_REVIEW -> "Pendiente revisión final de Jefatura";
            case APPROVED_BY_PROGRAM_HEAD_FINAL_REVIEW -> "Documentos finales aprobados por Jefatura";
            case DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR -> "Sustentación solicitada por Director";
            case DEFENSE_SCHEDULED -> "Sustentación programada";
            case EXAMINERS_ASSIGNED -> "Jurados asignados";
            case READY_FOR_EXAMINERS -> "Lista para Jurados";
            case DOCUMENTS_APPROVED_BY_EXAMINERS -> "Documentos de propuesta aprobados por Jurados";
            case SECONDARY_DOCUMENTS_APPROVED_BY_EXAMINERS -> "Documentos finales aprobados por Jurados";
            case DOCUMENT_REVIEW_TIEBREAKER_REQUIRED -> "Se requiere Jurado de Desempate";
            case EDIT_REQUESTED_BY_STUDENT -> "Edición de documento solicitada";
            case CORRECTIONS_REQUESTED_EXAMINERS -> "Correcciones solicitadas por Jurados";
            case READY_FOR_DEFENSE -> "Lista para Sustentación";
            case FINAL_REVIEW_COMPLETED -> "Revisión final completada";
            case DEFENSE_COMPLETED -> "Sustentación realizada";
            case UNDER_EVALUATION_PRIMARY_EXAMINERS -> "En evaluación por Jurados principales";
            case DISAGREEMENT_REQUIRES_TIEBREAKER -> "Desacuerdo — requiere Jurado de Desempate";
            case UNDER_EVALUATION_TIEBREAKER -> "En evaluación por Jurado de Desempate";
            case EVALUATION_COMPLETED -> "Evaluación completada";
            case PENDING_DISTINCTION_COMMITTEE_REVIEW -> "Aprobada – Distinción honorífica pendiente del Comité";
            case GRADED_APPROVED -> "Aprobada";
            case GRADED_FAILED -> "No aprobada";
            case MODALITY_CLOSED -> "Modalidad cerrada";
            case SEMINAR_CANCELED -> "Seminario cancelado";
            case MODALITY_CANCELLED -> "Modalidad cancelada";
            case CANCELLATION_REQUESTED -> "Cancelación solicitada";
            case CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR -> "Cancelación aprobada por Director";
            case CANCELLATION_REJECTED_BY_PROJECT_DIRECTOR -> "Cancelación rechazada por Director";
            case CANCELLED_WITHOUT_REPROVAL -> "Cancelada sin reprobación";
            case CANCELLATION_REJECTED -> "Cancelación rechazada";
            case CANCELLED_BY_CORRECTION_TIMEOUT -> "Cancelada por vencimiento de correcciones";
        };
    }

    private String translateDocumentStatus(DocumentStatus status) {
        if (status == null) return "Pendiente";
        return switch (status) {
            case PENDING -> "Pendiente de revisión";
            case ACCEPTED_FOR_PROGRAM_HEAD_REVIEW -> "Aprobado por Jefatura";
            case REJECTED_FOR_PROGRAM_HEAD_REVIEW -> "Rechazado por Jefatura";
            case CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD -> "Correcciones solicitadas por Jefatura";
            case CORRECTION_RESUBMITTED -> "Corrección re-enviada";
            case ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW -> "Aprobado por Comité";
            case REJECTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW -> "Rechazado por Comité";
            case CORRECTIONS_REQUESTED_BY_PROGRAM_CURRICULUM_COMMITTEE -> "Correcciones solicitadas por Comité";
            case ACCEPTED_FOR_EXAMINER_REVIEW -> "Aprobado por Jurado";
            case REJECTED_FOR_EXAMINER_REVIEW -> "Rechazado por Jurado";
            case CORRECTIONS_REQUESTED_BY_EXAMINER -> "Correcciones solicitadas por Jurado";
            case EDIT_REQUESTED -> "Edición solicitada";
            case EDIT_REQUEST_APPROVED -> "Edición aprobada";
            case EDIT_REQUEST_REJECTED -> "Edición rechazada";
        };
    }

    private String translateDistinction(AcademicDistinction dist) {
        if (dist == null) return "Sin resultado";
        return switch (dist) {
            case NO_DISTINCTION -> "Sin distinción";
            case AGREED_APPROVED -> "Aprobado";
            case AGREED_MERITORIOUS -> "Mención Meritoria";
            case AGREED_LAUREATE -> "Mención Laureada";
            case AGREED_REJECTED -> "Reprobado";
            case DISAGREEMENT_PENDING_TIEBREAKER -> "Pendiente desempate";
            case TIEBREAKER_APPROVED -> "Aprobado (desempate)";
            case TIEBREAKER_MERITORIOUS -> "Mención Meritoria (desempate)";
            case TIEBREAKER_LAUREATE -> "Mención Laureada (desempate)";
            case TIEBREAKER_REJECTED -> "Reprobado (desempate)";
            case REJECTED_BY_COMMITTEE -> "Rechazado por Comité";
            case PENDING_COMMITTEE_MERITORIOUS -> "Mención Meritoria propuesta (pendiente del comité)";
            case PENDING_COMMITTEE_LAUREATE -> "Mención Laureada propuesta (pendiente del comité)";
            case TIEBREAKER_PENDING_COMMITTEE_MERITORIOUS -> "Mención Meritoria por desempate (pendiente del comité)";
            case TIEBREAKER_PENDING_COMMITTEE_LAUREATE -> "Mención Laureada por desempate (pendiente del comité)";
        };
    }
}

