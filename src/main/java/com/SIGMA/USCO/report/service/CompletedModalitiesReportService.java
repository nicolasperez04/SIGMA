package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Modalities.service.ModalityServiceUtils;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.report.dto.*;
import com.SIGMA.USCO.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CompletedModalitiesReportService {

    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;


    @Transactional(readOnly = true)
    public CompletedModalitiesReportDTO generateCompletedModalitiesReport(CompletedModalitiesFilterDTO filters) {
        long startTime = System.currentTimeMillis();

        // Obtener usuario autenticado y su programa
        String userEmail = SecurityUtils.getCurrentUser().getEmail();
        AcademicProgram userProgram = ReportUtils.getAuthenticatedUserProgram(programAuthorityRepository);

        // Obtener modalidades completadas (aprobadas o fallidas)
        List<ModalityProcessStatus> completedStatuses = Arrays.asList(
                ModalityProcessStatus.GRADED_APPROVED,
                ModalityProcessStatus.GRADED_FAILED
        );

        List<StudentModality> completedModalities = studentModalityRepository
                .findByStatusIn(completedStatuses).stream()
                .filter(m -> m.getAcademicProgram().getId().equals(userProgram.getId()))
                .collect(Collectors.toList());

        // Aplicar filtros adicionales
        completedModalities = applyCompletedFilters(completedModalities, filters);

        // Generar filtros aplicados
        CompletedModalitiesReportDTO.AppliedFiltersDTO appliedFilters = buildCompletedFilters(filters);

        // Construir detalles de modalidades completadas
        List<CompletedModalitiesReportDTO.CompletedModalityDetailDTO> modalityDetails =
                buildCompletedModalityDetails(completedModalities);

        // Generar resumen ejecutivo
        CompletedModalitiesReportDTO.ExecutiveSummaryDTO executiveSummary =
                buildCompletedExecutiveSummary(modalityDetails, completedModalities);

        // Generar estadísticas generales
        CompletedModalitiesReportDTO.GeneralStatisticsDTO generalStatistics =
                buildCompletedGeneralStatistics(modalityDetails, completedModalities);

        // Generar análisis por resultado
        CompletedModalitiesReportDTO.ResultAnalysisDTO resultAnalysis =
                buildResultAnalysis(modalityDetails, completedModalities);

        // Generar análisis por tipo de modalidad
        List<CompletedModalitiesReportDTO.ModalityTypeAnalysisDTO> modalityTypeAnalysis =
                buildModalityTypeAnalysis(completedModalities);

        // Generar análisis temporal
        CompletedModalitiesReportDTO.TemporalAnalysisDTO temporalAnalysis =
                buildTemporalAnalysis(completedModalities);

        // Generar desempeño de directores
        CompletedModalitiesReportDTO.DirectorPerformanceDTO directorPerformance =
                buildDirectorPerformance(completedModalities);

        // Generar análisis de distinciones
        CompletedModalitiesReportDTO.DistinctionAnalysisDTO distinctionAnalysis =
                buildDistinctionAnalysis(completedModalities);

        // Aplicar ordenamiento
        modalityDetails = applySortingCompleted(modalityDetails, filters);

        // Calcular tiempo de generación
        long endTime = System.currentTimeMillis();
        long generationTime = endTime - startTime;

        // Construir metadata
        ReportMetadataDTO metadata = ReportMetadataDTO.builder()
                .reportVersion("1.0")
                .reportType("COMPLETED_MODALITIES_REPORT")
                .generatedBySystem("SIGMA - Sistema de Gestión de Modalidades de Grado")
                .totalRecords(modalityDetails.size())
                .generationTimeMs(generationTime)
                .exportFormat("JSON")
                .build();

        return CompletedModalitiesReportDTO.builder()
                .generatedAt(LocalDateTime.now())
                .generatedBy(userEmail + " (" + userProgram.getName() + ")")
                .academicProgramId(userProgram.getId())
                .academicProgramName(userProgram.getName())
                .academicProgramCode(userProgram.getCode())
                .appliedFilters(appliedFilters)
                .executiveSummary(executiveSummary)
                .completedModalities(modalityDetails)
                .generalStatistics(generalStatistics)
                .resultAnalysis(resultAnalysis)
                .modalityTypeAnalysis(modalityTypeAnalysis)
                .temporalAnalysis(temporalAnalysis)
                .directorPerformance(directorPerformance)
                .distinctionAnalysis(distinctionAnalysis)
                .metadata(metadata)
                .build();
    }

    /**
     * Aplica filtros a modalidades completadas
     */
    private List<StudentModality> applyCompletedFilters(List<StudentModality> modalities,
                                                        CompletedModalitiesFilterDTO filters) {
        if (filters == null) {
            return modalities;
        }

        return modalities.stream()
                .filter(m -> ReportUtils.filterByModalityTypes(m, filters.getModalityTypes()))
                .filter(m -> filterByResults(m, filters.getResults()))
                .filter(m -> filterByYearSemester(m, filters.getYear(), filters.getSemester()))
                .filter(m -> filterByGradeRange(m, filters.getMinGrade(), filters.getMaxGrade()))
                .filter(m -> filterByDistinction(m, filters.getOnlyWithDistinction(), filters.getDistinctionType()))
                .filter(m -> filterByDirectorId(m, filters.getDirectorId()))
                .filter(m -> ReportUtils.filterByModalityTypeFilter(m, filters.getModalityTypeFilter()))
                .collect(Collectors.toList());
    }

    private boolean filterByResults(StudentModality modality, List<String> results) {
        if (results == null || results.isEmpty()) return true;
        String result = modality.getStatus() == ModalityProcessStatus.GRADED_APPROVED ? "SUCCESS" : "FAILED";
        return results.contains(result);
    }

    private boolean filterByYearSemester(StudentModality modality, Integer year, Integer semester) {
        if (modality.getUpdatedAt() == null) return year == null && semester == null;

        if (year != null && modality.getUpdatedAt().getYear() != year) return false;
        if (semester != null && ReportUtils.getSemesterFromDate(modality.getUpdatedAt()) != semester) return false;

        return true;
    }

    private boolean filterByGradeRange(StudentModality modality, Double minGrade, Double maxGrade) {
        if (minGrade == null && maxGrade == null) return true;
        if (modality.getFinalGrade() == null) return false;

        if (minGrade != null && modality.getFinalGrade() < minGrade) return false;
        if (maxGrade != null && modality.getFinalGrade() > maxGrade) return false;

        return true;
    }

    private boolean filterByDistinction(StudentModality modality, Boolean onlyWithDistinction, String distinctionType) {
        if (onlyWithDistinction != null && onlyWithDistinction) {
            if (modality.getAcademicDistinction() == null) return false;
        }

        if (distinctionType != null && !distinctionType.isEmpty()) {
            if (modality.getAcademicDistinction() == null) return false;
            return modality.getAcademicDistinction().name().equals(distinctionType);
        }

        return true;
    }

    private boolean filterByDirectorId(StudentModality modality, Long directorId) {
        if (directorId == null) return true;
        return modality.getProjectDirector() != null &&
               modality.getProjectDirector().getId().equals(directorId);
    }

    /**
     * Construye filtros aplicados
     */
    private CompletedModalitiesReportDTO.AppliedFiltersDTO buildCompletedFilters(CompletedModalitiesFilterDTO filters) {
        if (filters == null) {
            return CompletedModalitiesReportDTO.AppliedFiltersDTO.builder()
                    .hasFilters(false)
                    .filterDescription("Sin filtros - Mostrando todas las modalidades completadas")
                    .build();
        }

        List<String> filterParts = new ArrayList<>();

        if (filters.getModalityTypes() != null && !filters.getModalityTypes().isEmpty()) {
            filterParts.add("Modalidades: " + String.join(", ", filters.getModalityTypes()));
        }

        if (filters.getResults() != null && !filters.getResults().isEmpty()) {
            filterParts.add("Resultados: " + String.join(", ", filters.getResults()));
        }

        if (filters.getYear() != null) {
            filterParts.add("Año: " + filters.getYear());
        }

        if (filters.getSemester() != null) {
            filterParts.add("Semestre: " + filters.getSemester());
        }

        if (filters.getOnlyWithDistinction() != null && filters.getOnlyWithDistinction()) {
            filterParts.add("Solo con distinción académica");
        }

        if (filters.getDistinctionType() != null) {
            filterParts.add("Distinción: " + filters.getDistinctionType());
        }

        String description = filterParts.isEmpty() ?
                "Sin filtros aplicados" :
                String.join(" | ", filterParts);

        return CompletedModalitiesReportDTO.AppliedFiltersDTO.builder()
                .modalityTypes(filters.getModalityTypes())
                .results(filters.getResults())
                .year(filters.getYear())
                .semester(filters.getSemester())
                .includeDistinctions(filters.getOnlyWithDistinction())
                .filterDescription(description)
                .hasFilters(!filterParts.isEmpty())
                .build();
    }

    /**
     * Construye detalles de modalidades completadas
     */
    private List<CompletedModalitiesReportDTO.CompletedModalityDetailDTO> buildCompletedModalityDetails(
            List<StudentModality> modalities) {

        List<CompletedModalitiesReportDTO.CompletedModalityDetailDTO> details = new ArrayList<>();

        for (StudentModality modality : modalities) {
            List<StudentModalityMember> members = studentModalityMemberRepository
                    .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);

            List<CompletedModalitiesReportDTO.StudentInfoDTO> students = new ArrayList<>();
            for (StudentModalityMember member : members) {
                User user = member.getStudent();
                StudentProfile profile = studentProfileRepository.findById(user.getId()).orElse(null);

                if (user != null) {
                    students.add(CompletedModalitiesReportDTO.StudentInfoDTO.builder()
                            .studentId(user.getId())
                            .studentCode(profile != null ? profile.getStudentCode() : null)
                            .fullName(user.getName() + " " + user.getLastName())
                            .email(user.getEmail())
                            .cumulativeGPA(profile != null ? profile.getGpa() : null)
                            .completedCredits(profile != null && profile.getApprovedCredits() != null ?
                                    profile.getApprovedCredits().intValue() : null)
                            .isLeader(member.getIsLeader())
                            .build());
                }
            }

            // Calcular días de completitud
            Integer completionDays = null;
            if (modality.getSelectionDate() != null && modality.getUpdatedAt() != null) {
                completionDays = (int) ChronoUnit.DAYS.between(
                        modality.getSelectionDate(), modality.getUpdatedAt());
            }

            // Determinar resultado
            String result = modality.getStatus() == ModalityProcessStatus.GRADED_APPROVED ? "SUCCESS" : "FAILED";

            // Obtener examinadores
            List<String> examiners = new ArrayList<>();
            // Aquí podrías agregar lógica para obtener examinadores si tienes esa relación

            // Determinar periodo
            Integer year = modality.getUpdatedAt() != null ? modality.getUpdatedAt().getYear() : null;
            Integer semester = modality.getUpdatedAt() != null ?
                    ReportUtils.getSemesterFromDate(modality.getUpdatedAt()) : null;

            details.add(CompletedModalitiesReportDTO.CompletedModalityDetailDTO.builder()
                    .modalityId(modality.getId())
                    .modalityType(modality.getProgramDegreeModality().getDegreeModality().getName())
                    .modalityTypeName(modality.getProgramDegreeModality().getDegreeModality().getName())
                    .result(result)
                    .completionDate(modality.getUpdatedAt())
                    .completionDays(completionDays)
                    .finalGrade(modality.getFinalGrade())
                    .gradeDescription(describeGrade(modality.getFinalGrade()))
                    .academicDistinction(ModalityServiceUtils.translateAcademicDistinction(modality.getAcademicDistinction()))
                    .students(students)
                    .studentCount(students.size())
                    .isGroup(students.size() > 1)
                    .directorName(modality.getProjectDirector() != null ?
                            modality.getProjectDirector().getName() + " " +
                            modality.getProjectDirector().getLastName() : null)
                    .directorEmail(modality.getProjectDirector() != null ?
                            modality.getProjectDirector().getEmail() : null)
                    .selectionDate(modality.getSelectionDate())
                    .defenseDate(modality.getDefenseDate())
                    .defenseLocation(modality.getDefenseLocation())
                    .examiners(examiners)
                    .year(year)
                    .semester(semester)
                    .periodLabel(year != null && semester != null ? year + "-" + semester : null)
                    .observations(generateCompletedObservations(modality))
                    .build());
        }

        return details;
    }

    private String describeGrade(Double grade) {
        if (grade == null) return "Sin calificar";
        if (grade >= 4.5) return "Sobresaliente";
        if (grade >= 4.0) return "Excelente";
        if (grade >= 3.5) return "Bueno";
        if (grade >= 3.0) return "Aprobado";
        return "Reprobado";
    }

    private String generateCompletedObservations(StudentModality modality) {
        List<String> observations = new ArrayList<>();

        if (modality.getAcademicDistinction() != null) {
            observations.add("Con distinción académica: " + ModalityServiceUtils.translateAcademicDistinction(modality.getAcademicDistinction()));
        }

        if (modality.getFinalGrade() != null && modality.getFinalGrade() >= 4.5) {
            observations.add("Calificación sobresaliente");
        }

        if (modality.getSelectionDate() != null && modality.getUpdatedAt() != null) {
            long days = ChronoUnit.DAYS.between(modality.getSelectionDate(), modality.getUpdatedAt());
            if (days <= 180) {
                observations.add("Completada en tiempo óptimo");
            } else if (days > 365) {
                observations.add("Tiempo de completitud extendido");
            }
        }

        return observations.isEmpty() ? null : String.join(" | ", observations);
    }

    /**
     * Construye resumen ejecutivo
     */
    private CompletedModalitiesReportDTO.ExecutiveSummaryDTO buildCompletedExecutiveSummary(
            List<CompletedModalitiesReportDTO.CompletedModalityDetailDTO> details,
            List<StudentModality> modalities) {

        long successful = details.stream().filter(d -> "SUCCESS".equals(d.getResult())).count();
        long failed = details.stream().filter(d -> "FAILED".equals(d.getResult())).count();

        double successRate = details.size() > 0 ? (successful * 100.0) / details.size() : 0.0;
        double failureRate = details.size() > 0 ? (failed * 100.0) / details.size() : 0.0;

        long withDistinction = details.stream()
                .filter(d -> d.getAcademicDistinction() != null)
                .count();

        double avgGrade = details.stream()
                .filter(d -> d.getFinalGrade() != null)
                .mapToDouble(CompletedModalitiesReportDTO.CompletedModalityDetailDTO::getFinalGrade)
                .average()
                .orElse(0.0);

        double avgDays = details.stream()
                .filter(d -> d.getCompletionDays() != null)
                .mapToInt(CompletedModalitiesReportDTO.CompletedModalityDetailDTO::getCompletionDays)
                .average()
                .orElse(0.0);

        int totalStudents = details.stream()
                .mapToInt(CompletedModalitiesReportDTO.CompletedModalityDetailDTO::getStudentCount)
                .sum();

        Set<String> uniqueDirectors = details.stream()
                .filter(d -> d.getDirectorName() != null)
                .map(CompletedModalitiesReportDTO.CompletedModalityDetailDTO::getDirectorName)
                .collect(Collectors.toSet());

        Map<String, Integer> quickStats = new LinkedHashMap<>();
        quickStats.put("Total Completadas", details.size());
        quickStats.put("Exitosas", (int) successful);
        quickStats.put("Fallidas", (int) failed);
        quickStats.put("Con Distinción", (int) withDistinction);

        return CompletedModalitiesReportDTO.ExecutiveSummaryDTO.builder()
                .totalCompleted(details.size())
                .totalSuccessful((int) successful)
                .totalFailed((int) failed)
                .successRate(Math.round(successRate * 100.0) / 100.0)
                .failureRate(Math.round(failureRate * 100.0) / 100.0)
                .withDistinction((int) withDistinction)
                .averageGrade(Math.round(avgGrade * 100.0) / 100.0)
                .averageCompletionDays(Math.round(avgDays * 100.0) / 100.0)
                .totalStudents(totalStudents)
                .uniqueDirectors(uniqueDirectors.size())
                .quickStats(quickStats)
                .build();
    }

    /**
     * Construye estadísticas generales
     */
    private CompletedModalitiesReportDTO.GeneralStatisticsDTO buildCompletedGeneralStatistics(
            List<CompletedModalitiesReportDTO.CompletedModalityDetailDTO> details,
            List<StudentModality> modalities) {

        long approved = details.stream().filter(d -> "SUCCESS".equals(d.getResult())).count();
        long failed = details.size() - approved;
        double approvalRate = details.size() > 0 ? (approved * 100.0) / details.size() : 0.0;

        // Tiempos
        List<Integer> completionDays = details.stream()
                .filter(d -> d.getCompletionDays() != null)
                .map(CompletedModalitiesReportDTO.CompletedModalityDetailDTO::getCompletionDays)
                .sorted()
                .collect(Collectors.toList());

        double avgDays = completionDays.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        Integer fastestDays = completionDays.isEmpty() ? null : completionDays.get(0);
        Integer slowestDays = completionDays.isEmpty() ? null : completionDays.get(completionDays.size() - 1);
        Double medianDays = calculateMedian(completionDays);

        // Calificaciones
        List<Double> grades = details.stream()
                .filter(d -> d.getFinalGrade() != null)
                .map(CompletedModalitiesReportDTO.CompletedModalityDetailDTO::getFinalGrade)
                .sorted()
                .collect(Collectors.toList());

        double avgGrade = grades.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        Double highestGrade = grades.isEmpty() ? null : grades.get(grades.size() - 1);
        Double lowestGrade = grades.isEmpty() ? null : grades.get(0);
        Double medianGrade = calculateMedianDouble(grades);

        // Distinciones
        long meritorious = modalities.stream()
                .filter(m -> m.getAcademicDistinction() != null && (
                        m.getAcademicDistinction() == com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction.AGREED_MERITORIOUS ||
                        m.getAcademicDistinction() == com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction.TIEBREAKER_MERITORIOUS))
                .count();
        long laureate = modalities.stream()
                .filter(m -> m.getAcademicDistinction() != null && (
                        m.getAcademicDistinction() == com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction.AGREED_LAUREATE ||
                        m.getAcademicDistinction() == com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction.TIEBREAKER_LAUREATE))
                .count();
        long withoutDistinction = modalities.size() - meritorious - laureate;

        // Por tipo
        long individual = details.stream().filter(d -> !d.getIsGroup()).count();
        long group = details.size() - individual;

        // Distribuciones
        Map<String, Integer> byType = details.stream()
                .collect(Collectors.groupingBy(
                        CompletedModalitiesReportDTO.CompletedModalityDetailDTO::getModalityTypeName,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        Map<String, Integer> byResult = details.stream()
                .collect(Collectors.groupingBy(
                        CompletedModalitiesReportDTO.CompletedModalityDetailDTO::getResult,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        Map<String, Integer> byDistinction = new HashMap<>();
        byDistinction.put("MERITORIOUS", (int) meritorious);
        byDistinction.put("LAUREATE", (int) laureate);
        byDistinction.put("WITHOUT", (int) withoutDistinction);

        return CompletedModalitiesReportDTO.GeneralStatisticsDTO.builder()
                .totalCompleted(details.size())
                .approved((int) approved)
                .failed((int) failed)
                .approvalRate(Math.round(approvalRate * 100.0) / 100.0)
                .averageCompletionDays(Math.round(avgDays * 100.0) / 100.0)
                .fastestCompletionDays(fastestDays)
                .slowestCompletionDays(slowestDays)
                .medianCompletionDays(medianDays)
                .averageGrade(Math.round(avgGrade * 100.0) / 100.0)
                .highestGrade(highestGrade)
                .lowestGrade(lowestGrade)
                .medianGrade(medianGrade)
                .withMeritorious((int) meritorious)
                .withLaudeate((int) laureate)
                .withoutDistinction((int) withoutDistinction)
                .individualModalities((int) individual)
                .groupModalities((int) group)
                .byModalityType(byType)
                .byResult(byResult)
                .byDistinction(byDistinction)
                .build();
    }

    private Double calculateMedian(List<Integer> values) {
        if (values.isEmpty()) return null;
        int size = values.size();
        if (size % 2 == 0) {
            return (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
        } else {
            return values.get(size / 2).doubleValue();
        }
    }

    private Double calculateMedianDouble(List<Double> values) {
        if (values.isEmpty()) return null;
        int size = values.size();
        if (size % 2 == 0) {
            return (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
        } else {
            return values.get(size / 2);
        }
    }

    /**
     * Construye análisis por resultado
     */
    private CompletedModalitiesReportDTO.ResultAnalysisDTO buildResultAnalysis(
            List<CompletedModalitiesReportDTO.CompletedModalityDetailDTO> details,
            List<StudentModality> modalities) {

        List<CompletedModalitiesReportDTO.CompletedModalityDetailDTO> successful = details.stream()
                .filter(d -> "SUCCESS".equals(d.getResult()))
                .collect(Collectors.toList());

        List<CompletedModalitiesReportDTO.CompletedModalityDetailDTO> failed = details.stream()
                .filter(d -> "FAILED".equals(d.getResult()))
                .collect(Collectors.toList());

        double successRate = details.size() > 0 ? (successful.size() * 100.0) / details.size() : 0.0;
        double failureRate = details.size() > 0 ? (failed.size() * 100.0) / details.size() : 0.0;

        double avgSuccessGrade = successful.stream()
                .filter(d -> d.getFinalGrade() != null)
                .mapToDouble(CompletedModalitiesReportDTO.CompletedModalityDetailDTO::getFinalGrade)
                .average()
                .orElse(0.0);

        double avgSuccessDays = successful.stream()
                .filter(d -> d.getCompletionDays() != null)
                .mapToInt(CompletedModalitiesReportDTO.CompletedModalityDetailDTO::getCompletionDays)
                .average()
                .orElse(0.0);

        double avgFailureGrade = failed.stream()
                .filter(d -> d.getFinalGrade() != null)
                .mapToDouble(CompletedModalitiesReportDTO.CompletedModalityDetailDTO::getFinalGrade)
                .average()
                .orElse(0.0);

        double avgFailureDays = failed.stream()
                .filter(d -> d.getCompletionDays() != null)
                .mapToInt(CompletedModalitiesReportDTO.CompletedModalityDetailDTO::getCompletionDays)
                .average()
                .orElse(0.0);

        // Factores de éxito
        List<String> successFactors = new ArrayList<>();
        if (avgSuccessGrade >= 4.0) {
            successFactors.add("Alta calificación promedio");
        }
        if (avgSuccessDays < 365) {
            successFactors.add("Tiempo de completitud óptimo");
        }
        long withDistinction = successful.stream()
                .filter(d -> d.getAcademicDistinction() != null)
                .count();
        if (withDistinction > 0) {
            successFactors.add("Presencia de distinciones académicas");
        }

        // Razones de fallo
        List<String> failureReasons = new ArrayList<>();
        if (!failed.isEmpty()) {
            if (avgFailureGrade < 3.0) {
                failureReasons.add("Calificaciones por debajo del mínimo");
            }
            if (avgFailureDays > 450) {
                failureReasons.add("Tiempo de completitud excesivamente prolongado");
            }
        }

        // Veredicto
        String verdict = successRate >= 80 ? "EXCELLENT" :
                        successRate >= 60 ? "GOOD" :
                        successRate >= 40 ? "REGULAR" : "NEEDS_IMPROVEMENT";

        // Recomendaciones
        List<String> recommendations = new ArrayList<>();
        if (successRate < 70) {
            recommendations.add("Implementar seguimiento más cercano a estudiantes en proceso");
        }
        if (avgFailureDays > 400) {
            recommendations.add("Establecer hitos intermedios para control de avance");
        }
        if (failed.size() > successful.size() * 0.3) {
            recommendations.add("Revisar criterios de evaluación y apoyo académico");
        }

        return CompletedModalitiesReportDTO.ResultAnalysisDTO.builder()
                .successfulCount(successful.size())
                .successRate(Math.round(successRate * 100.0) / 100.0)
                .averageSuccessGrade(Math.round(avgSuccessGrade * 100.0) / 100.0)
                .averageSuccessCompletionDays(Math.round(avgSuccessDays * 100.0) / 100.0)
                .successFactors(successFactors)
                .failedCount(failed.size())
                .failureRate(Math.round(failureRate * 100.0) / 100.0)
                .averageFailureGrade(Math.round(avgFailureGrade * 100.0) / 100.0)
                .averageFailureCompletionDays(Math.round(avgFailureDays * 100.0) / 100.0)
                .failureReasons(failureReasons)
                .performanceVerdict(verdict)
                .recommendations(recommendations)
                .build();
    }

    /**
     * Construye análisis por tipo de modalidad
     */
    private List<CompletedModalitiesReportDTO.ModalityTypeAnalysisDTO> buildModalityTypeAnalysis(
            List<StudentModality> modalities) {

        Map<String, List<StudentModality>> groupedByType = modalities.stream()
                .collect(Collectors.groupingBy(m -> m.getProgramDegreeModality().getDegreeModality().getName()));

        return groupedByType.entrySet().stream()
                .map(entry -> {
                    String typeName = entry.getKey();
                    List<StudentModality> typeModalities = entry.getValue();

                    long successful = typeModalities.stream()
                            .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED)
                            .count();

                    long failed = typeModalities.size() - successful;
                    double successRate = typeModalities.size() > 0 ?
                            (successful * 100.0) / typeModalities.size() : 0.0;

                    double avgGrade = typeModalities.stream()
                            .filter(m -> m.getFinalGrade() != null)
                            .mapToDouble(StudentModality::getFinalGrade)
                            .average()
                            .orElse(0.0);

                    double avgDays = typeModalities.stream()
                            .filter(m -> m.getSelectionDate() != null && m.getUpdatedAt() != null)
                            .mapToLong(m -> ChronoUnit.DAYS.between(m.getSelectionDate(), m.getUpdatedAt()))
                            .average()
                            .orElse(0.0);

                    long withDistinction = typeModalities.stream()
                            .filter(m -> m.getAcademicDistinction() != null)
                            .count();

                    String performance = successRate >= 80 ? "EXCELLENT" :
                                       successRate >= 60 ? "GOOD" :
                                       successRate >= 40 ? "REGULAR" : "POOR";

                    return CompletedModalitiesReportDTO.ModalityTypeAnalysisDTO.builder()
                            .modalityType(typeName)
                            .totalCompleted(typeModalities.size())
                            .successful((int) successful)
                            .failed((int) failed)
                            .successRate(Math.round(successRate * 100.0) / 100.0)
                            .averageGrade(Math.round(avgGrade * 100.0) / 100.0)
                            .averageCompletionDays(Math.round(avgDays * 100.0) / 100.0)
                            .withDistinction((int) withDistinction)
                            .performance(performance)
                            .build();
                })
                .sorted(Comparator.comparing(CompletedModalitiesReportDTO.ModalityTypeAnalysisDTO::getTotalCompleted).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Construye análisis temporal
     */
    private CompletedModalitiesReportDTO.TemporalAnalysisDTO buildTemporalAnalysis(
            List<StudentModality> modalities) {

        Map<String, List<StudentModality>> groupedByPeriod = modalities.stream()
                .filter(m -> m.getUpdatedAt() != null)
                .collect(Collectors.groupingBy(m -> {
                    int year = m.getUpdatedAt().getYear();
                    int semester = ReportUtils.getSemesterFromDate(m.getUpdatedAt());
                    return year + "-" + semester;
                }));

        List<CompletedModalitiesReportDTO.PeriodDataDTO> periodData = groupedByPeriod.entrySet().stream()
                .map(entry -> {
                    String period = entry.getKey();
                    String[] parts = period.split("-");
                    int year = Integer.parseInt(parts[0]);
                    int semester = Integer.parseInt(parts[1]);

                    List<StudentModality> periodModalities = entry.getValue();

                    long successful = periodModalities.stream()
                            .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED)
                            .count();

                    long failed = periodModalities.size() - successful;
                    double successRate = periodModalities.size() > 0 ?
                            (successful * 100.0) / periodModalities.size() : 0.0;

                    double avgGrade = periodModalities.stream()
                            .filter(m -> m.getFinalGrade() != null)
                            .mapToDouble(StudentModality::getFinalGrade)
                            .average()
                            .orElse(0.0);

                    return CompletedModalitiesReportDTO.PeriodDataDTO.builder()
                            .period(period)
                            .year(year)
                            .semester(semester)
                            .completed(periodModalities.size())
                            .successful((int) successful)
                            .failed((int) failed)
                            .successRate(Math.round(successRate * 100.0) / 100.0)
                            .averageGrade(Math.round(avgGrade * 100.0) / 100.0)
                            .build();
                })
                .sorted(Comparator.comparing(CompletedModalitiesReportDTO.PeriodDataDTO::getYear)
                        .thenComparing(CompletedModalitiesReportDTO.PeriodDataDTO::getSemester))
                .collect(Collectors.toList());

        // Determinar tendencia
        String trend = "STABLE";
        Double growthRate = 0.0;
        if (periodData.size() >= 2) {
            int oldest = periodData.get(0).getCompleted();
            int newest = periodData.get(periodData.size() - 1).getCompleted();
            growthRate = oldest > 0 ? ((newest - oldest) * 100.0) / oldest : 0.0;

            if (growthRate > 10) trend = "IMPROVING";
            else if (growthRate < -10) trend = "DECLINING";
        }

        // Mejor y peor periodo
        String bestPeriod = periodData.stream()
                .max(Comparator.comparing(CompletedModalitiesReportDTO.PeriodDataDTO::getSuccessRate))
                .map(CompletedModalitiesReportDTO.PeriodDataDTO::getPeriod)
                .orElse(null);

        String worstPeriod = periodData.stream()
                .min(Comparator.comparing(CompletedModalitiesReportDTO.PeriodDataDTO::getSuccessRate))
                .map(CompletedModalitiesReportDTO.PeriodDataDTO::getPeriod)
                .orElse(null);

        return CompletedModalitiesReportDTO.TemporalAnalysisDTO.builder()
                .periodData(periodData)
                .trend(trend)
                .growthRate(Math.round(growthRate * 100.0) / 100.0)
                .bestPeriod(bestPeriod)
                .worstPeriod(worstPeriod)
                .build();
    }

    /**
     * Construye desempeño de directores
     */
    private CompletedModalitiesReportDTO.DirectorPerformanceDTO buildDirectorPerformance(
            List<StudentModality> modalities) {

        Map<String, List<StudentModality>> groupedByDirector = modalities.stream()
                .filter(m -> m.getProjectDirector() != null)
                .collect(Collectors.groupingBy(m ->
                        m.getProjectDirector().getName() + " " + m.getProjectDirector().getLastName()));

        List<CompletedModalitiesReportDTO.TopDirectorDTO> topDirectors = groupedByDirector.entrySet().stream()
                .map(entry -> {
                    String directorName = entry.getKey();
                    List<StudentModality> directorModalities = entry.getValue();

                    long successful = directorModalities.stream()
                            .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED)
                            .count();

                    long failed = directorModalities.size() - successful;
                    double successRate = directorModalities.size() > 0 ?
                            (successful * 100.0) / directorModalities.size() : 0.0;

                    double avgGrade = directorModalities.stream()
                            .filter(m -> m.getFinalGrade() != null)
                            .mapToDouble(StudentModality::getFinalGrade)
                            .average()
                            .orElse(0.0);

                    long withDistinction = directorModalities.stream()
                            .filter(m -> m.getAcademicDistinction() != null)
                            .count();

                    return CompletedModalitiesReportDTO.TopDirectorDTO.builder()
                            .directorName(directorName)
                            .totalSupervised(directorModalities.size())
                            .successful((int) successful)
                            .failed((int) failed)
                            .successRate(Math.round(successRate * 100.0) / 100.0)
                            .averageGrade(Math.round(avgGrade * 100.0) / 100.0)
                            .withDistinction((int) withDistinction)
                            .build();
                })
                .sorted(Comparator.comparing(CompletedModalitiesReportDTO.TopDirectorDTO::getSuccessRate).reversed()
                        .thenComparing(CompletedModalitiesReportDTO.TopDirectorDTO::getTotalSupervised).reversed())
                .limit(10)
                .collect(Collectors.toList());

        double avgSuccessRate = topDirectors.stream()
                .mapToDouble(CompletedModalitiesReportDTO.TopDirectorDTO::getSuccessRate)
                .average()
                .orElse(0.0);

        String bestDirector = topDirectors.isEmpty() ? null : topDirectors.get(0).getDirectorName();
        Integer bestDirectorCount = topDirectors.isEmpty() ? null : topDirectors.get(0).getSuccessful();

        return CompletedModalitiesReportDTO.DirectorPerformanceDTO.builder()
                .totalDirectors(groupedByDirector.size())
                .topDirectors(topDirectors)
                .averageSuccessRateByDirector(Math.round(avgSuccessRate * 100.0) / 100.0)
                .bestDirector(bestDirector)
                .bestDirectorSuccessCount(bestDirectorCount)
                .build();
    }

    /**
     * Construye análisis de distinciones
     */
    private CompletedModalitiesReportDTO.DistinctionAnalysisDTO buildDistinctionAnalysis(
            List<StudentModality> modalities) {

        long meritorious = modalities.stream()
                .filter(m -> m.getAcademicDistinction() != null && (
                        m.getAcademicDistinction() == com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction.AGREED_MERITORIOUS ||
                        m.getAcademicDistinction() == com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction.TIEBREAKER_MERITORIOUS))
                .count();

        long laureate = modalities.stream()
                .filter(m -> m.getAcademicDistinction() != null && (
                        m.getAcademicDistinction() == com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction.AGREED_LAUREATE ||
                        m.getAcademicDistinction() == com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction.TIEBREAKER_LAUREATE))
                .count();

        long totalWithDistinction = meritorious + laureate;
        double distinctionRate = modalities.size() > 0 ?
                (totalWithDistinction * 100.0) / modalities.size() : 0.0;

        // Modalidades con más distinciones
        Map<String, Long> distinctionsByType = modalities.stream()
                .filter(m -> m.getAcademicDistinction() != null)
                .collect(Collectors.groupingBy(
                        m -> m.getProgramDegreeModality().getDegreeModality().getName(),
                        Collectors.counting()));

        List<String> modalitiesWithMostDistinctions = distinctionsByType.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Directores con más distinciones
        Map<String, Long> distinctionsByDirector = modalities.stream()
                .filter(m -> m.getAcademicDistinction() != null)
                .filter(m -> m.getProjectDirector() != null)
                .collect(Collectors.groupingBy(
                        m -> m.getProjectDirector().getName() + " " + m.getProjectDirector().getLastName(),
                        Collectors.counting()));

        List<String> directorsWithMostDistinctions = distinctionsByDirector.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return CompletedModalitiesReportDTO.DistinctionAnalysisDTO.builder()
                .totalWithDistinction((int) totalWithDistinction)
                .meritorious((int) meritorious)
                .laureate((int) laureate)
                .distinctionRate(Math.round(distinctionRate * 100.0) / 100.0)
                .modalitiesWithMostDistinctions(modalitiesWithMostDistinctions)
                .directorsWithMostDistinctions(directorsWithMostDistinctions)
                .build();
    }

    /**
     * Aplica ordenamiento
     */
    private List<CompletedModalitiesReportDTO.CompletedModalityDetailDTO> applySortingCompleted(
            List<CompletedModalitiesReportDTO.CompletedModalityDetailDTO> details,
            CompletedModalitiesFilterDTO filters) {

        if (filters == null || filters.getSortBy() == null) {
            return details.stream()
                    .sorted(Comparator.comparing(CompletedModalitiesReportDTO.CompletedModalityDetailDTO::getCompletionDate,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        }

        Comparator<CompletedModalitiesReportDTO.CompletedModalityDetailDTO> comparator = null;

        switch (filters.getSortBy().toUpperCase()) {
            case "DATE":
                comparator = Comparator.comparing(CompletedModalitiesReportDTO.CompletedModalityDetailDTO::getCompletionDate,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "GRADE":
                comparator = Comparator.comparing(CompletedModalitiesReportDTO.CompletedModalityDetailDTO::getFinalGrade,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "TYPE":
                comparator = Comparator.comparing(CompletedModalitiesReportDTO.CompletedModalityDetailDTO::getModalityTypeName);
                break;
            case "DURATION":
                comparator = Comparator.comparing(CompletedModalitiesReportDTO.CompletedModalityDetailDTO::getCompletionDays,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            default:
                comparator = Comparator.comparing(CompletedModalitiesReportDTO.CompletedModalityDetailDTO::getCompletionDate,
                        Comparator.nullsLast(Comparator.naturalOrder()));
        }

        if ("DESC".equalsIgnoreCase(filters.getSortDirection())) {
            comparator = comparator.reversed();
        }

        return details.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }
}
