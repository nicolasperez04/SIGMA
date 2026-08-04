package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.notifications.listeners.TranslationUtils;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
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
public class HistoricalReportService {

    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;


    // ==================== REPORTE HISTÓRICO DE MODALIDAD ESPECÍFICA ====================

    /**
     * Genera un reporte histórico completo de una modalidad específica
     * Análisis temporal de evolución, tendencias y estadísticas detalladas
     *
     * @param modalityTypeId ID del tipo de modalidad a analizar
     * @param periodsToAnalyze Número de periodos históricos a analizar
     * @return Reporte histórico completo
     */
    @Transactional(readOnly = true)
    public ModalityHistoricalReportDTO generateModalityHistoricalReport(Long modalityTypeId, Integer periodsToAnalyze) {
        long startTime = System.currentTimeMillis();

        // Obtener usuario autenticado y su programa
        String userEmail = SecurityUtils.getCurrentUser().getEmail();
        AcademicProgram userProgram = ReportUtils.getAuthenticatedUserProgram(programAuthorityRepository);

        // Obtener todas las modalidades del tipo especificado en el programa
        List<StudentModality> allModalitiesOfType = studentModalityRepository.findAll().stream()
                .filter(m -> m.getAcademicProgram().getId().equals(userProgram.getId()))
                .filter(m -> m.getProgramDegreeModality().getDegreeModality().getId().equals(modalityTypeId))
                .collect(Collectors.toList());

        if (allModalitiesOfType.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron modalidades del tipo especificado en el programa");
        }

        // Obtener información de la modalidad
        ModalityHistoricalReportDTO.ModalityInfoDTO modalityInfo = generateModalityInfo(allModalitiesOfType, modalityTypeId);

        // Generar estado actual
        ModalityHistoricalReportDTO.CurrentStateDTO currentState = generateCurrentState(allModalitiesOfType, userProgram);

        // Generar análisis histórico por periodos
        int periods = periodsToAnalyze != null ? periodsToAnalyze : 8; // Por defecto 4 años (8 semestres)
        List<ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO> historicalAnalysis =
                generateHistoricalAnalysis(allModalitiesOfType, periods);

        // Generar análisis de tendencias
        ModalityHistoricalReportDTO.TrendsEvolutionDTO trendsEvolution =
                generateTrendsEvolution(historicalAnalysis);

        // Generar análisis comparativo
        ModalityHistoricalReportDTO.ComparativeAnalysisDTO comparativeAnalysis =
                generateComparativeAnalysis(historicalAnalysis);

        // Generar estadísticas de directores
        ModalityHistoricalReportDTO.DirectorStatisticsDTO directorStatistics =
                generateDirectorStatistics(allModalitiesOfType);

        // Generar estadísticas de estudiantes
        ModalityHistoricalReportDTO.StudentStatisticsDTO studentStatistics =
                generateStudentStatistics(allModalitiesOfType);

        // Generar análisis de desempeño
        ModalityHistoricalReportDTO.PerformanceAnalysisDTO performanceAnalysis =
                generatePerformanceAnalysis(allModalitiesOfType, historicalAnalysis);

        // Generar proyecciones
        ModalityHistoricalReportDTO.ProjectionsDTO projections =
                generateProjections(historicalAnalysis, trendsEvolution);

        // Calcular tiempo de generación
        long endTime = System.currentTimeMillis();
        long generationTime = endTime - startTime;

        // Construir metadata
        ReportMetadataDTO metadata = ReportMetadataDTO.builder()
                .reportVersion("1.0")
                .reportType("MODALITY_HISTORICAL_ANALYSIS")
                .generatedBySystem("SIGMA - Sistema de Gestión de Modalidades de Grado")
                .totalRecords(historicalAnalysis.size())
                .generationTimeMs(generationTime)
                .exportFormat("JSON")
                .build();

        return ModalityHistoricalReportDTO.builder()
                .generatedAt(LocalDateTime.now())
                .generatedBy(userEmail + " (" + userProgram.getName() + ")")
                .academicProgramId(userProgram.getId())
                .academicProgramName(userProgram.getName())
                .academicProgramCode(userProgram.getCode())
                .modalityInfo(modalityInfo)
                .currentState(currentState)
                .historicalAnalysis(historicalAnalysis)
                .trendsEvolution(trendsEvolution)
                .comparativeAnalysis(comparativeAnalysis)
                .directorStatistics(directorStatistics)
                .studentStatistics(studentStatistics)
                .performanceAnalysis(performanceAnalysis)
                .projections(projections)
                .metadata(metadata)
                .build();
    }

    /**
     * Genera información básica de la modalidad
     */
    private ModalityHistoricalReportDTO.ModalityInfoDTO generateModalityInfo(
            List<StudentModality> modalities, Long modalityTypeId) {

        if (modalities.isEmpty()) {
            return null;
        }

        var firstModality = modalities.get(0);
        var degreeModality = firstModality.getProgramDegreeModality().getDegreeModality();

        // Calcular años activos
        LocalDateTime oldestDate = modalities.stream()
                .map(StudentModality::getSelectionDate)
                .filter(java.util.Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        long yearsActive = ChronoUnit.YEARS.between(oldestDate, LocalDateTime.now());

        // Determinar el tipo de modalidad basado en las instancias existentes
        String modalityTypeStr = determineModalityType(modalities);

        return ModalityHistoricalReportDTO.ModalityInfoDTO.builder()
                .modalityId(degreeModality.getId())
                .modalityName(degreeModality.getName())
                .modalityCode(null)
                .description(degreeModality.getDescription())
                .requiresDirector(ReportUtils.isDirectorRequired(degreeModality.getName()))
                .modalityType(modalityTypeStr)
                .isActive(degreeModality.getStatus() != null)
                .createdAt(oldestDate)
                .yearsActive((int) yearsActive)
                .totalHistoricalInstances(modalities.size())
                .build();
    }

    /**
     * Determina el tipo de modalidad (INDIVIDUAL, GRUPAL o MIXTA)
     * basándose en las instancias existentes
     */
    private String determineModalityType(List<StudentModality> modalities) {
        if (modalities == null || modalities.isEmpty()) {
            return "MIXTA";
        }

        boolean hasIndividual = modalities.stream()
                .anyMatch(m -> m.getModalityType() == com.SIGMA.USCO.Modalities.Entity.enums.ModalityType.INDIVIDUAL);

        boolean hasGroup = modalities.stream()
                .anyMatch(m -> m.getModalityType() == com.SIGMA.USCO.Modalities.Entity.enums.ModalityType.GROUP);

        if (hasIndividual && hasGroup) {
            return "MIXTA";
        } else if (hasIndividual) {
            return "INDIVIDUAL";
        } else if (hasGroup) {
            return "GRUPAL";
        } else {
            return "MIXTA";
        }
    }

    /**
     * Genera el estado actual de la modalidad
     */
    private ModalityHistoricalReportDTO.CurrentStateDTO generateCurrentState(
            List<StudentModality> allModalities, AcademicProgram program) {

        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();
        int currentSemester = ReportUtils.getSemesterFromDate(now);

        // Filtrar modalidades del periodo actual
        List<StudentModality> currentPeriodModalities = allModalities.stream()
                .filter(m -> m.getSelectionDate() != null)
                .filter(m -> m.getSelectionDate().getYear() == currentYear)
                .filter(m -> ReportUtils.getSemesterFromDate(m.getSelectionDate()) == currentSemester)
                .collect(Collectors.toList());

        // Contar activas
        long activeCount = currentPeriodModalities.stream()
                .filter(m -> ReportUtils.getActiveStatuses().contains(m.getStatus()))
                .count();

        // Contar estudiantes
        Set<Long> students = new HashSet<>();
        for (StudentModality modality : currentPeriodModalities) {
            List<StudentModalityMember> members = studentModalityMemberRepository
                    .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
            members.forEach(member -> students.add(member.getStudent().getId()));
        }

        // Contar directores únicos
        Set<Long> directors = currentPeriodModalities.stream()
                .filter(m -> m.getProjectDirector() != null)
                .map(m -> m.getProjectDirector().getId())
                .collect(Collectors.toSet());

        // Contar por estado
        long completed = currentPeriodModalities.stream()
                .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED)
                .count();

        long inProgress = currentPeriodModalities.stream()
                .filter(m -> m.getStatus() == ModalityProcessStatus.PROPOSAL_APPROVED ||
                           m.getStatus() == ModalityProcessStatus.DEFENSE_SCHEDULED ||
                           m.getStatus() == ModalityProcessStatus.DEFENSE_COMPLETED)
                .count();

        long inReview = currentPeriodModalities.stream()
                .filter(m -> m.getStatus() == ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD ||
                           m.getStatus() == ModalityProcessStatus.UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE)
                .count();

        // Calcular días promedio de completitud
        double avgDays = currentPeriodModalities.stream()
                .filter(m -> m.getSelectionDate() != null)
                .mapToLong(m -> ChronoUnit.DAYS.between(m.getSelectionDate(), LocalDateTime.now()))
                .average()
                .orElse(0.0);

        // Determinar popularidad actual
        String popularity = determinePopularity(currentPeriodModalities.size(), allModalities);

        // Calcular posición en ranking (comparar con otras modalidades del programa)
        int position = calculateRankingPosition(program, currentPeriodModalities.size());

        return ModalityHistoricalReportDTO.CurrentStateDTO.builder()
                .currentPeriodYear(currentYear)
                .currentPeriodSemester(currentSemester)
                .activeInstances((int) activeCount)
                .totalStudentsEnrolled(students.size())
                .assignedDirectors(directors.size())
                .completedInstances((int) completed)
                .inProgressInstances((int) inProgress)
                .inReviewInstances((int) inReview)
                .averageCompletionDays(Math.round(avgDays * 100.0) / 100.0)
                .currentPopularity(popularity)
                .positionInRanking(position)
                .build();
    }

    /**
     * Genera análisis histórico por periodos académicos
     */
    private List<ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO> generateHistoricalAnalysis(
            List<StudentModality> allModalities, int periodsToAnalyze) {

        List<ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO> analysis = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();
        int currentSemester = ReportUtils.getSemesterFromDate(now);

        // Generar análisis para cada periodo hacia atrás
        for (int i = 0; i < periodsToAnalyze; i++) {
            int year = currentYear;
            int semester = currentSemester - i;

            // Ajustar año si el semestre es negativo
            while (semester <= 0) {
                year--;
                semester += 2;
            }

            // Filtrar modalidades del periodo
            final int finalYear = year;
            final int finalSemester = semester;

            List<StudentModality> periodModalities = allModalities.stream()
                    .filter(m -> m.getSelectionDate() != null)
                    .filter(m -> m.getSelectionDate().getYear() == finalYear)
                    .filter(m -> ReportUtils.getSemesterFromDate(m.getSelectionDate()) == finalSemester)
                    .collect(Collectors.toList());

            analysis.add(analyzePeriod(year, semester, periodModalities));
        }

        return analysis;
    }

    /**
     * Analiza un periodo académico específico
     */
    private ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO analyzePeriod(
            int year, int semester, List<StudentModality> periodModalities) {

        // Contar estudiantes únicos
        Set<Long> students = new HashSet<>();
        for (StudentModality modality : periodModalities) {
            List<StudentModalityMember> members = studentModalityMemberRepository
                    .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
            members.forEach(member -> students.add(member.getStudent().getId()));
        }

        // Contar por tipo
        long individual = periodModalities.stream()
                .filter(m -> m.getModalityType() == com.SIGMA.USCO.Modalities.Entity.enums.ModalityType.INDIVIDUAL)
                .count();

        long group = periodModalities.size() - individual;

        // Contar por resultado
        long completed = periodModalities.stream()
                .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED)
                .count();

        long abandoned = periodModalities.stream()
                .filter(m -> m.getStatus() == ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL ||
                           m.getStatus() == ModalityProcessStatus.GRADED_FAILED)
                .count();

        // Calcular tasa de completitud
        double completionRate = periodModalities.size() > 0 ?
                (double) completed / periodModalities.size() * 100 : 0;

        // Calcular días promedio
        double avgDays = periodModalities.stream()
                .filter(m -> m.getSelectionDate() != null && m.getUpdatedAt() != null)
                .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED)
                .mapToLong(m -> ChronoUnit.DAYS.between(m.getSelectionDate(), m.getUpdatedAt()))
                .average()
                .orElse(0.0);

        // Directores involucrados
        Set<User> directors = periodModalities.stream()
                .map(StudentModality::getProjectDirector)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        // Top 3 directores
        List<String> topDirectors = directors.stream()
                .limit(3)
                .map(d -> d.getName() + " " + d.getLastName())
                .collect(Collectors.toList());

        // Distribución por estado
        Map<String, Integer> statusDistribution = periodModalities.stream()
                .collect(Collectors.groupingBy(
                        m -> TranslationUtils.translateModalityProcessStatus(m.getStatus()),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // Generar observaciones
        String observations = generatePeriodObservations(periodModalities, completionRate, avgDays);

        return ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO.builder()
                .year(year)
                .semester(semester)
                .periodLabel(year + "-" + semester)
                .totalInstances(periodModalities.size())
                .studentsEnrolled(students.size())
                .individualInstances((int) individual)
                .groupInstances((int) group)
                .completedSuccessfully((int) completed)
                .abandoned((int) abandoned)
                .cancelled(0) // Se puede calcular si hay estados cancelados
                .completionRate(Math.round(completionRate * 100.0) / 100.0)
                .averageCompletionDays(Math.round(avgDays * 100.0) / 100.0)
                .directorsInvolved(directors.size())
                .topDirectors(topDirectors)
                .averageGrade(null) // No disponible actualmente
                .distributionByStatus(statusDistribution)
                .observations(observations)
                .build();
    }

    /**
     * Genera observaciones para un periodo
     */
    private String generatePeriodObservations(List<StudentModality> modalities,
                                              double completionRate, double avgDays) {
        List<String> observations = new ArrayList<>();

        if (modalities.isEmpty()) {
            return "No se registraron modalidades en este periodo";
        }

        if (completionRate >= 80) {
            observations.add("Excelente tasa de completitud");
        } else if (completionRate >= 60) {
            observations.add("Tasa de completitud aceptable");
        } else if (completionRate > 0) {
            observations.add("Tasa de completitud por debajo del promedio");
        }

        if (avgDays > 0 && avgDays < 180) {
            observations.add("Tiempo de completitud óptimo");
        } else if (avgDays >= 365) {
            observations.add("Tiempo de completitud extendido");
        }

        if (modalities.size() > 10) {
            observations.add("Alta demanda en este periodo");
        } else if (modalities.size() < 3) {
            observations.add("Baja demanda en este periodo");
        }

        return observations.isEmpty() ? "Periodo regular" : String.join(" | ", observations);
    }

    /**
     * Determina la popularidad actual de la modalidad
     */
    private String determinePopularity(int currentInstances, List<StudentModality> allModalities) {
        // Calcular promedio histórico
        Map<String, Long> byPeriod = allModalities.stream()
                .filter(m -> m.getSelectionDate() != null)
                .collect(Collectors.groupingBy(
                        m -> m.getSelectionDate().getYear() + "-" + ReportUtils.getSemesterFromDate(m.getSelectionDate()),
                        Collectors.counting()
                ));

        double avgPerPeriod = byPeriod.values().stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        if (currentInstances >= avgPerPeriod * 1.3) {
            return "HIGH";
        } else if (currentInstances >= avgPerPeriod * 0.7) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * Calcula la posición en el ranking del programa
     */
    private int calculateRankingPosition(AcademicProgram program, int currentInstances) {
        // Obtener todas las modalidades del periodo actual del programa
        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();
        int currentSemester = ReportUtils.getSemesterFromDate(now);

        Map<Long, Long> countByModality = studentModalityRepository.findAll().stream()
                .filter(m -> m.getAcademicProgram().getId().equals(program.getId()))
                .filter(m -> m.getSelectionDate() != null)
                .filter(m -> m.getSelectionDate().getYear() == currentYear)
                .filter(m -> ReportUtils.getSemesterFromDate(m.getSelectionDate()) == currentSemester)
                .collect(Collectors.groupingBy(
                        m -> m.getProgramDegreeModality().getDegreeModality().getId(),
                        Collectors.counting()
                ));

        // Ordenar y encontrar posición
        List<Long> sorted = countByModality.values().stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i) <= currentInstances) {
                return i + 1;
            }
        }

        return sorted.size() + 1;
    }

    /**
     * Genera análisis de tendencias y evolución
     */
    private ModalityHistoricalReportDTO.TrendsEvolutionDTO generateTrendsEvolution(
            List<ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO> historical) {

        if (historical.size() < 2) {
            return ModalityHistoricalReportDTO.TrendsEvolutionDTO.builder()
                    .overallTrend("INSUFFICIENT_DATA")
                    .build();
        }

        // Encontrar pico y valle
        ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO peak = historical.stream()
                .max(Comparator.comparing(ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO::getTotalInstances))
                .orElse(null);

        ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO lowest = historical.stream()
                .min(Comparator.comparing(ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO::getTotalInstances))
                .orElse(null);

        // Calcular tasa de crecimiento
        int oldestValue = historical.get(historical.size() - 1).getTotalInstances();
        int newestValue = historical.get(0).getTotalInstances();
        double growthRate = oldestValue > 0 ?
                ((double) (newestValue - oldestValue) / oldestValue * 100) : 0;

        // Determinar tendencia general
        String overallTrend;
        if (growthRate > 15) {
            overallTrend = "GROWING";
        } else if (growthRate < -15) {
            overallTrend = "DECLINING";
        } else {
            overallTrend = "STABLE";
        }

        // Generar puntos de evolución
        List<ModalityHistoricalReportDTO.TrendPointDTO> evolutionPoints = new ArrayList<>();
        for (int i = 0; i < historical.size() - 1; i++) {
            var current = historical.get(i);
            var previous = historical.get(i + 1);

            double changePercent = previous.getTotalInstances() > 0 ?
                    ((double) (current.getTotalInstances() - previous.getTotalInstances()) /
                     previous.getTotalInstances() * 100) : 0;

            String indicator;
            if (changePercent > 5) indicator = "UP";
            else if (changePercent < -5) indicator = "DOWN";
            else indicator = "STABLE";

            evolutionPoints.add(ModalityHistoricalReportDTO.TrendPointDTO.builder()
                    .period(current.getPeriodLabel())
                    .value(current.getTotalInstances())
                    .indicator(indicator)
                    .changePercentage(Math.round(changePercent * 100.0) / 100.0)
                    .build());
        }

        // Identificar patrones
        List<String> patterns = identifyPatterns(historical);

        return ModalityHistoricalReportDTO.TrendsEvolutionDTO.builder()
                .overallTrend(overallTrend)
                .growthRate(Math.round(growthRate * 100.0) / 100.0)
                .peakYear(peak != null ? peak.getYear() : null)
                .peakSemester(peak != null ? peak.getSemester() : null)
                .peakInstances(peak != null ? peak.getTotalInstances() : 0)
                .lowestYear(lowest != null ? lowest.getYear() : null)
                .lowestSemester(lowest != null ? lowest.getSemester() : null)
                .lowestInstances(lowest != null ? lowest.getTotalInstances() : 0)
                .evolutionPoints(evolutionPoints)
                .popularityTrend(overallTrend)
                .completionTrend(analyzeCompletionTrend(historical))
                .identifiedPatterns(patterns)
                .build();
    }

    /**
     * Identifica patrones en los datos históricos
     */
    private List<String> identifyPatterns(List<ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO> historical) {
        List<String> patterns = new ArrayList<>();

        // Patrón estacional (semestre 1 vs semestre 2)
        double avg1 = historical.stream()
                .filter(p -> p.getSemester() == 1)
                .mapToInt(ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO::getTotalInstances)
                .average()
                .orElse(0);

        double avg2 = historical.stream()
                .filter(p -> p.getSemester() == 2)
                .mapToInt(ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO::getTotalInstances)
                .average()
                .orElse(0);

        if (avg1 > avg2 * 1.3) {
            patterns.add("Mayor demanda en semestre 1");
        } else if (avg2 > avg1 * 1.3) {
            patterns.add("Mayor demanda en semestre 2");
        }

        // Patrón de crecimiento sostenido
        int consecutiveGrowth = 0;
        for (int i = 0; i < historical.size() - 1; i++) {
            if (historical.get(i).getTotalInstances() > historical.get(i + 1).getTotalInstances()) {
                consecutiveGrowth++;
            } else {
                break;
            }
        }

        if (consecutiveGrowth >= 3) {
            patterns.add("Crecimiento sostenido en los últimos " + consecutiveGrowth + " periodos");
        }

        // Patrón de tasa de completitud
        double avgCompletion = historical.stream()
                .filter(p -> p.getCompletionRate() != null && p.getCompletionRate() > 0)
                .mapToDouble(ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO::getCompletionRate)
                .average()
                .orElse(0);

        if (avgCompletion >= 75) {
            patterns.add("Alta tasa de completitud consistente");
        } else if (avgCompletion < 50) {
            patterns.add("Tasa de completitud requiere atención");
        }

        return patterns;
    }

    /**
     * Analiza tendencia de completitud
     */
    private String analyzeCompletionTrend(List<ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO> historical) {
        List<Double> rates = historical.stream()
                .filter(p -> p.getCompletionRate() != null && p.getCompletionRate() > 0)
                .map(ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO::getCompletionRate)
                .collect(Collectors.toList());

        if (rates.size() < 2) return "INSUFFICIENT_DATA";

        double firstHalf = rates.subList(0, rates.size() / 2).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);
        double secondHalf = rates.subList(rates.size() / 2, rates.size()).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);

        if (firstHalf > secondHalf * 1.1) {
            return "IMPROVING";
        } else if (secondHalf > firstHalf * 1.1) {
            return "DECLINING";
        } else {
            return "STABLE";
        }
    }

    /**
     * Genera análisis comparativo entre periodos
     */
    private ModalityHistoricalReportDTO.ComparativeAnalysisDTO generateComparativeAnalysis(
            List<ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO> historical) {

        if (historical.isEmpty()) return null;

        // Comparar actual vs anterior
        ModalityHistoricalReportDTO.PeriodComparisonDTO currentVsPrevious = null;
        if (historical.size() >= 2) {
            currentVsPrevious = comparePeriods(historical.get(0), historical.get(1));
        }

        // Comparar actual vs año pasado (mismo semestre)
        ModalityHistoricalReportDTO.PeriodComparisonDTO currentVsLastYear = null;
        if (historical.size() >= 3) {
            // Buscar mismo semestre del año anterior
            var current = historical.get(0);
            var lastYear = historical.stream()
                    .filter(p -> p.getSemester().equals(current.getSemester()) &&
                               p.getYear().equals(current.getYear() - 1))
                    .findFirst()
                    .orElse(null);

            if (lastYear != null) {
                currentVsLastYear = comparePeriods(current, lastYear);
            }
        }

        // Comparar mejor vs peor
        var best = historical.stream()
                .max(Comparator.comparing(ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO::getTotalInstances))
                .orElse(null);
        var worst = historical.stream()
                .min(Comparator.comparing(ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO::getTotalInstances))
                .orElse(null);

        ModalityHistoricalReportDTO.PeriodComparisonDTO bestVsWorst = null;
        if (best != null && worst != null) {
            bestVsWorst = comparePeriods(best, worst);
        }

        // Promedios por año
        Map<String, Double> averagesByYear = historical.stream()
                .collect(Collectors.groupingBy(
                        p -> String.valueOf(p.getYear()),
                        Collectors.averagingInt(ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO::getTotalInstances)
                ));

        // Key findings
        List<String> keyFindings = generateKeyFindings(historical, currentVsPrevious, currentVsLastYear);

        return ModalityHistoricalReportDTO.ComparativeAnalysisDTO.builder()
                .currentVsPrevious(currentVsPrevious)
                .currentVsLastYear(currentVsLastYear)
                .bestVsWorst(bestVsWorst)
                .averagesByYear(averagesByYear)
                .keyFindings(keyFindings)
                .build();
    }

    /**
     * Compara dos periodos
     */
    private ModalityHistoricalReportDTO.PeriodComparisonDTO comparePeriods(
            ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO period1,
            ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO period2) {

        double instancesChange = period2.getTotalInstances() > 0 ?
                ((double) (period1.getTotalInstances() - period2.getTotalInstances()) /
                 period2.getTotalInstances() * 100) : 0;

        double studentsChange = period2.getStudentsEnrolled() > 0 ?
                ((double) (period1.getStudentsEnrolled() - period2.getStudentsEnrolled()) /
                 period2.getStudentsEnrolled() * 100) : 0;

        String verdict;
        if (instancesChange > 10) verdict = "IMPROVED";
        else if (instancesChange < -10) verdict = "DECLINED";
        else verdict = "STABLE";

        return ModalityHistoricalReportDTO.PeriodComparisonDTO.builder()
                .period1Label(period1.getPeriodLabel())
                .period1Instances(period1.getTotalInstances())
                .period1Students(period1.getStudentsEnrolled())
                .period2Label(period2.getPeriodLabel())
                .period2Instances(period2.getTotalInstances())
                .period2Students(period2.getStudentsEnrolled())
                .instancesChange(Math.round(instancesChange * 100.0) / 100.0)
                .studentsChange(Math.round(studentsChange * 100.0) / 100.0)
                .verdict(verdict)
                .build();
    }

    /**
     * Genera hallazgos clave
     */
    private List<String> generateKeyFindings(
            List<ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO> historical,
            ModalityHistoricalReportDTO.PeriodComparisonDTO currentVsPrevious,
            ModalityHistoricalReportDTO.PeriodComparisonDTO currentVsLastYear) {

        List<String> findings = new ArrayList<>();

        // Análisis de cambio vs periodo anterior
        if (currentVsPrevious != null) {
            if ("IMPROVED".equals(currentVsPrevious.getVerdict())) {
                findings.add("Incremento de " + Math.abs(currentVsPrevious.getInstancesChange()) +
                           "% respecto al periodo anterior");
            } else if ("DECLINED".equals(currentVsPrevious.getVerdict())) {
                findings.add("Disminución de " + Math.abs(currentVsPrevious.getInstancesChange()) +
                           "% respecto al periodo anterior");
            }
        }

        // Análisis anual
        if (currentVsLastYear != null) {
            if (Math.abs(currentVsLastYear.getInstancesChange()) > 20) {
                findings.add("Cambio significativo (" + currentVsLastYear.getInstancesChange() +
                           "%) comparado con el mismo periodo del año anterior");
            }
        }

        // Análisis de consistencia
        double stdDev = calculateStandardDeviation(historical.stream()
                .mapToInt(ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO::getTotalInstances)
                .asDoubleStream()
                .boxed()
                .collect(Collectors.toList()));

        if (stdDev < 2) {
            findings.add("Demanda muy consistente a lo largo del tiempo");
        } else if (stdDev > 5) {
            findings.add("Alta variabilidad en la demanda entre periodos");
        }

        return findings;
    }

    /**
     * Calcula desviación estándar
     */
    private double calculateStandardDeviation(List<Double> values) {
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);
        return Math.sqrt(variance);
    }

    /**
     * Genera estadísticas de directores
     */
    private ModalityHistoricalReportDTO.DirectorStatisticsDTO generateDirectorStatistics(
            List<StudentModality> allModalities) {

        // Directores únicos históricos
        Set<User> allDirectors = allModalities.stream()
                .map(StudentModality::getProjectDirector)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        // Directores actuales
        LocalDateTime now = LocalDateTime.now();
        Set<User> currentDirectors = allModalities.stream()
                .filter(m -> m.getSelectionDate() != null)
                .filter(m -> m.getSelectionDate().getYear() == now.getYear())
                .filter(m -> ReportUtils.getSemesterFromDate(m.getSelectionDate()) == ReportUtils.getSemesterFromDate(now))
                .map(StudentModality::getProjectDirector)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        // Top directores históricos
        Map<User, Long> directorCounts = allModalities.stream()
                .filter(m -> m.getProjectDirector() != null)
                .collect(Collectors.groupingBy(
                        StudentModality::getProjectDirector,
                        Collectors.counting()
                ));

        List<ModalityHistoricalReportDTO.TopDirectorDTO> topDirectors = directorCounts.entrySet().stream()
                .sorted(Map.Entry.<User, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> buildTopDirector(entry.getKey(), entry.getValue().intValue(), allModalities))
                .collect(Collectors.toList());

        // Director más experimentado
        Map.Entry<User, Long> mostExperienced = directorCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        double avgPerDirector = allDirectors.size() > 0 ?
                (double) allModalities.size() / allDirectors.size() : 0;

        return ModalityHistoricalReportDTO.DirectorStatisticsDTO.builder()
                .totalUniqueDirectors(allDirectors.size())
                .currentActiveDirectors(currentDirectors.size())
                .topDirectorsAllTime(topDirectors)
                .topDirectorsCurrentPeriod(new ArrayList<>()) // Se puede calcular si se necesita
                .averageInstancesPerDirector(Math.round(avgPerDirector * 100.0) / 100.0)
                .mostExperiencedDirector(mostExperienced != null ?
                        mostExperienced.getKey().getName() + " " + mostExperienced.getKey().getLastName() : null)
                .mostExperiencedCount(mostExperienced != null ? mostExperienced.getValue().intValue() : 0)
                .build();
    }

    /**
     * Construye información de un top director
     */
    private ModalityHistoricalReportDTO.TopDirectorDTO buildTopDirector(
            User director, int instances, List<StudentModality> allModalities) {

        // Contar estudiantes supervisados
        int students = 0;
        for (StudentModality m : allModalities) {
            if (m.getProjectDirector() != null && m.getProjectDirector().getId().equals(director.getId())) {
                students += studentModalityMemberRepository
                        .findByStudentModalityIdAndStatus(m.getId(), MemberStatus.ACTIVE).size();
            }
        }

        // Calcular tasa de éxito
        long completed = allModalities.stream()
                .filter(m -> m.getProjectDirector() != null &&
                           m.getProjectDirector().getId().equals(director.getId()))
                .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED)
                .count();

        double successRate = instances > 0 ? (double) completed / instances * 100 : 0;

        // Periodos en los que ha participado
        List<String> periods = allModalities.stream()
                .filter(m -> m.getProjectDirector() != null &&
                           m.getProjectDirector().getId().equals(director.getId()))
                .filter(m -> m.getSelectionDate() != null)
                .map(m -> m.getSelectionDate().getYear() + "-" + ReportUtils.getSemesterFromDate(m.getSelectionDate()))
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        return ModalityHistoricalReportDTO.TopDirectorDTO.builder()
                .directorName(director.getName() + " " + director.getLastName())
                .instancesSupervised(instances)
                .studentsSupervised(students)
                .successRate(Math.round(successRate * 100.0) / 100.0)
                .periods(periods)
                .build();
    }

    /**
     * Genera estadísticas de estudiantes
     */
    private ModalityHistoricalReportDTO.StudentStatisticsDTO generateStudentStatistics(
            List<StudentModality> allModalities) {

        // Estudiantes históricos únicos
        Set<Long> allStudents = new HashSet<>();
        for (StudentModality m : allModalities) {
            List<StudentModalityMember> members = studentModalityMemberRepository
                    .findByStudentModalityIdAndStatus(m.getId(), MemberStatus.ACTIVE);
            members.forEach(member -> allStudents.add(member.getStudent().getId()));
        }

        // Estudiantes actuales
        LocalDateTime now = LocalDateTime.now();
        Set<Long> currentStudents = new HashSet<>();
        List<StudentModality> currentModalities = allModalities.stream()
                .filter(m -> m.getSelectionDate() != null)
                .filter(m -> m.getSelectionDate().getYear() == now.getYear())
                .filter(m -> ReportUtils.getSemesterFromDate(m.getSelectionDate()) == ReportUtils.getSemesterFromDate(now))
                .collect(Collectors.toList());

        for (StudentModality m : currentModalities) {
            List<StudentModalityMember> members = studentModalityMemberRepository
                    .findByStudentModalityIdAndStatus(m.getId(), MemberStatus.ACTIVE);
            members.forEach(member -> currentStudents.add(member.getStudent().getId()));
        }

        double avgStudentsPerInstance = allModalities.size() > 0 ?
                (double) allStudents.size() / allModalities.size() : 0;

        // Contar individuales vs grupales
        long individual = allModalities.stream()
                .filter(m -> m.getModalityType() == com.SIGMA.USCO.Modalities.Entity.enums.ModalityType.INDIVIDUAL)
                .count();

        double individualRatio = allModalities.size() > 0 ?
                (double) individual / allModalities.size() * 100 : 0;

        // Estudiantes por semestre
        Map<String, Integer> studentsBySemester = new HashMap<>();
        // Se puede implementar si se necesita

        return ModalityHistoricalReportDTO.StudentStatisticsDTO.builder()
                .totalHistoricalStudents(allStudents.size())
                .currentStudents(currentStudents.size())
                .averageStudentsPerInstance(Math.round(avgStudentsPerInstance * 100.0) / 100.0)
                .maxStudentsInGroup(3) // Valor por defecto, se puede calcular
                .minStudentsInGroup(1)
                .individualVsGroupRatio(Math.round(individualRatio * 100.0) / 100.0)
                .studentsBySemester(studentsBySemester)
                .preferredType(individualRatio > 50 ? "INDIVIDUAL" : "GROUP")
                .build();
    }

    /**
     * Genera análisis de desempeño
     */
    private ModalityHistoricalReportDTO.PerformanceAnalysisDTO generatePerformanceAnalysis(
            List<StudentModality> allModalities,
            List<ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO> historical) {

        // Tasa de completitud general
        long completed = allModalities.stream()
                .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED)
                .count();

        double completionRate = allModalities.size() > 0 ?
                (double) completed / allModalities.size() * 100 : 0;

        // Tiempo promedio de completitud
        double avgDays = allModalities.stream()
                .filter(m -> m.getSelectionDate() != null && m.getUpdatedAt() != null)
                .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED)
                .mapToLong(m -> ChronoUnit.DAYS.between(m.getSelectionDate(), m.getUpdatedAt()))
                .average()
                .orElse(0.0);

        // Tasa de éxito (completed / (completed + cancelled))
        long cancelled = allModalities.stream()
                .filter(m -> m.getStatus() == ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL ||
                           m.getStatus() == ModalityProcessStatus.GRADED_FAILED)
                .count();

        double successRate = (completed + cancelled) > 0 ?
                (double) completed / (completed + cancelled) * 100 : 0;

        double abandonmentRate = (completed + cancelled) > 0 ?
                (double) cancelled / (completed + cancelled) * 100 : 0;

        // Tiempo más rápido y más lento
        var completionTimes = allModalities.stream()
                .filter(m -> m.getSelectionDate() != null && m.getUpdatedAt() != null)
                .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED)
                .mapToLong(m -> ChronoUnit.DAYS.between(m.getSelectionDate(), m.getUpdatedAt()))
                .boxed()
                .collect(Collectors.toList());

        int fastest = completionTimes.stream().min(Long::compare).orElse(0L).intValue();
        int slowest = completionTimes.stream().max(Long::compare).orElse(0L).intValue();

        // Tasas por año
        Map<String, Double> completionRateByYear = historical.stream()
                .collect(Collectors.groupingBy(
                        p -> String.valueOf(p.getYear()),
                        Collectors.averagingDouble(p -> p.getCompletionRate() != null ? p.getCompletionRate() : 0)
                ));

        Map<String, Double> successRateByYear = new HashMap<>(); // Se puede calcular si se necesita

        // Determinar veredicto
        String verdict;
        if (completionRate >= 80 && successRate >= 85) verdict = "EXCELLENT";
        else if (completionRate >= 60 && successRate >= 70) verdict = "GOOD";
        else if (completionRate >= 40 && successRate >= 55) verdict = "REGULAR";
        else verdict = "NEEDS_IMPROVEMENT";

        // Puntos fuertes y áreas de mejora
        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();

        if (completionRate >= 70) {
            strengths.add("Alta tasa de completitud");
        } else {
            improvements.add("Mejorar tasa de completitud");
        }

        if (avgDays < 270) {
            strengths.add("Tiempo de completitud óptimo");
        } else if (avgDays > 450) {
            improvements.add("Reducir tiempo promedio de completitud");
        }

        if (abandonmentRate < 15) {
            strengths.add("Baja tasa de abandono");
        } else {
            improvements.add("Reducir tasa de abandono");
        }

        return ModalityHistoricalReportDTO.PerformanceAnalysisDTO.builder()
                .overallCompletionRate(Math.round(completionRate * 100.0) / 100.0)
                .averageCompletionTimeDays(Math.round(avgDays * 100.0) / 100.0)
                .successRate(Math.round(successRate * 100.0) / 100.0)
                .abandonmentRate(Math.round(abandonmentRate * 100.0) / 100.0)
                .fastestCompletionDays(fastest)
                .slowestCompletionDays(slowest)
                .completionRateByYear(completionRateByYear)
                .successRateByYear(successRateByYear)
                .performanceVerdict(verdict)
                .strengthPoints(strengths)
                .improvementAreas(improvements)
                .build();
    }

    /**
     * Genera proyecciones futuras
     */
    private ModalityHistoricalReportDTO.ProjectionsDTO generateProjections(
            List<ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO> historical,
            ModalityHistoricalReportDTO.TrendsEvolutionDTO trends) {

        if (historical.size() < 3) {
            return ModalityHistoricalReportDTO.ProjectionsDTO.builder()
                    .projectedNextSemester(0)
                    .projectedNextYear(0)
                    .demandProjection("INSUFFICIENT_DATA")
                    .confidenceLevel(0.0)
                    .build();
        }

        // Calcular promedio de los últimos 3 periodos
        double avgLast3 = historical.stream()
                .limit(3)
                .mapToInt(ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO::getTotalInstances)
                .average()
                .orElse(0);

        // Proyectar siguiente semestre basado en tendencia
        int projected = (int) Math.round(avgLast3);

        if ("GROWING".equals(trends.getOverallTrend())) {
            projected = (int) Math.round(avgLast3 * 1.15);
        } else if ("DECLINING".equals(trends.getOverallTrend())) {
            projected = (int) Math.round(avgLast3 * 0.85);
        }

        // Proyectar siguiente año
        int projectedYear = projected * 2;

        // Determinar proyección de demanda
        String demandProjection;
        if (projected >= avgLast3 * 1.2) demandProjection = "HIGH";
        else if (projected >= avgLast3 * 0.8) demandProjection = "MEDIUM";
        else demandProjection = "LOW";

        // Generar recomendaciones
        String recommendations = generateRecommendations(trends, projected, historical);

        // Oportunidades y riesgos
        List<String> opportunities = identifyOpportunities(trends, historical);
        List<String> risks = identifyRisks(trends, historical);

        // Calcular nivel de confianza
        double confidence = calculateConfidenceLevel(historical);

        return ModalityHistoricalReportDTO.ProjectionsDTO.builder()
                .projectedNextSemester(projected)
                .projectedNextYear(projectedYear)
                .demandProjection(demandProjection)
                .recommendedActions(recommendations)
                .opportunities(opportunities)
                .risks(risks)
                .confidenceLevel(Math.round(confidence * 100.0) / 100.0)
                .build();
    }

    /**
     * Genera recomendaciones basadas en análisis
     */
    private String generateRecommendations(ModalityHistoricalReportDTO.TrendsEvolutionDTO trends,
                                          int projected,
                                          List<ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO> historical) {
        List<String> recommendations = new ArrayList<>();

        if ("GROWING".equals(trends.getOverallTrend())) {
            recommendations.add("Considerar aumento de cupos y recursos");
            recommendations.add("Planificar asignación de directores adicionales");
        } else if ("DECLINING".equals(trends.getOverallTrend())) {
            recommendations.add("Revisar causas de la disminución en demanda");
            recommendations.add("Implementar estrategias de promoción");
        }

        double avgCompletion = historical.stream()
                .filter(p -> p.getCompletionRate() != null)
                .mapToDouble(ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO::getCompletionRate)
                .average()
                .orElse(0);

        if (avgCompletion < 60) {
            recommendations.add("Implementar programas de seguimiento y apoyo");
        }

        return recommendations.isEmpty() ? "Mantener estrategia actual" : String.join(" | ", recommendations);
    }

    /**
     * Identifica oportunidades
     */
    private List<String> identifyOpportunities(ModalityHistoricalReportDTO.TrendsEvolutionDTO trends,
                                               List<ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO> historical) {
        List<String> opportunities = new ArrayList<>();

        if ("GROWING".equals(trends.getOverallTrend())) {
            opportunities.add("Expandir capacidad de supervisión");
            opportunities.add("Posicionar como modalidad líder del programa");
        }

        double avgCompletion = historical.stream()
                .filter(p -> p.getCompletionRate() != null)
                .mapToDouble(ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO::getCompletionRate)
                .average()
                .orElse(0);

        if (avgCompletion >= 70) {
            opportunities.add("Destacar alta tasa de éxito en promoción");
        }

        return opportunities;
    }

    /**
     * Identifica riesgos
     */
    private List<String> identifyRisks(ModalityHistoricalReportDTO.TrendsEvolutionDTO trends,
                                      List<ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO> historical) {
        List<String> risks = new ArrayList<>();

        if ("DECLINING".equals(trends.getOverallTrend())) {
            risks.add("Pérdida de interés estudiantil");
            risks.add("Posible desactivación por baja demanda");
        }

        double stdDev = calculateStandardDeviation(historical.stream()
                .mapToInt(ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO::getTotalInstances)
                .asDoubleStream()
                .boxed()
                .collect(Collectors.toList()));

        if (stdDev > 5) {
            risks.add("Alta variabilidad dificulta planificación");
        }

        return risks;
    }

    /**
     * Calcula nivel de confianza de las proyecciones
     */
    private double calculateConfidenceLevel(List<ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO> historical) {
        double baseConfidence = 50.0;

        // Más datos = más confianza
        baseConfidence += Math.min(historical.size() * 5, 30);

        // Menor desviación estándar = más confianza
        double stdDev = calculateStandardDeviation(historical.stream()
                .mapToInt(ModalityHistoricalReportDTO.AcademicPeriodAnalysisDTO::getTotalInstances)
                .asDoubleStream()
                .boxed()
                .collect(Collectors.toList()));

        if (stdDev < 2) baseConfidence += 15;
        else if (stdDev > 5) baseConfidence -= 10;

        return Math.min(Math.max(baseConfidence, 0), 95);
    }
}
