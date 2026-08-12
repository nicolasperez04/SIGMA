package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.common.util.TranslationUtils;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.report.dto.*;
import com.SIGMA.USCO.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ComparisonReportService {

    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;


    /**
     * Genera un reporte comparativo de modalidades por tipo de grado
     * RF-48 - Comparativa de Modalidades por Tipo de Grado
     *
     * @param filters Filtros para la comparativa (año, semestre, histórico)
     * @return Reporte comparativo con estadísticas por tipo
     */
    @Transactional(readOnly = true)
    public ModalityTypeComparisonReportDTO generateModalityTypeComparison(ModalityComparisonFilterDTO filters) {
        long startTime = System.currentTimeMillis();

        // Obtener usuario autenticado y su programa
        String userEmail = SecurityUtils.getCurrentUser().getEmail();
        AcademicProgram userProgram = ReportUtils.getAuthenticatedUserProgram(programAuthorityRepository);

        // Obtener modalidades según filtros (carga única del programa, filtros en memoria)
        List<StudentModality> programModalities = studentModalityRepository.findForProgramHead(List.of(userProgram.getId()));
        List<StudentModality> modalities = getModalitiesForComparison(programModalities, userProgram.getId(), filters);

        // Generar resumen general
        ModalityTypeComparisonReportDTO.ComparisonSummaryDTO summary = generateComparisonSummary(modalities);

        // Generar estadísticas por tipo de modalidad
        List<ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO> typeStatistics =
                generateModalityTypeStatistics(modalities);

        // Generar distribución de estudiantes
        Map<String, Integer> studentDistribution = generateStudentDistribution(modalities);

        // Generar comparación histórica si se solicita
        List<ModalityTypeComparisonReportDTO.PeriodComparisonDTO> historicalComparison = null;
        if (filters != null && Boolean.TRUE.equals(filters.getIncludeHistoricalComparison())) {
            int periodsCount = filters.getHistoricalPeriodsCount() != null ? filters.getHistoricalPeriodsCount() : 4;
            historicalComparison = generateHistoricalComparison(programModalities, userProgram.getId(), periodsCount, filters);
        }

        // Generar análisis de tendencias si se solicita
        ModalityTypeComparisonReportDTO.TrendsAnalysisDTO trendsAnalysis = null;
        if (filters != null && Boolean.TRUE.equals(filters.getIncludeTrendsAnalysis()) && historicalComparison != null) {
            trendsAnalysis = generateTrendsAnalysis(historicalComparison, typeStatistics);
        }

        // Calcular tiempo de generación
        long endTime = System.currentTimeMillis();
        long generationTime = endTime - startTime;

        // Construir metadata
        ReportMetadataDTO metadata = ReportMetadataDTO.builder()
                .reportVersion("1.0")
                .reportType("MODALITY_TYPE_COMPARISON")
                .generatedBySystem("SIGMA - Sistema de Gestión de Modalidades de Grado")
                .totalRecords(typeStatistics.size())
                .generationTimeMs(generationTime)
                .exportFormat("JSON")
                .build();

        return ModalityTypeComparisonReportDTO.builder()
                .generatedAt(LocalDateTime.now())
                .generatedBy(userEmail + " (" + userProgram.getName() + ")")
                .academicProgramId(userProgram.getId())
                .academicProgramName(userProgram.getName())
                .academicProgramCode(userProgram.getCode())
                .year(filters != null ? filters.getYear() : null)
                .semester(filters != null ? filters.getSemester() : null)
                .summary(summary)
                .modalityTypeStatistics(typeStatistics)
                .historicalComparison(historicalComparison)
                .studentDistributionByType(studentDistribution)
                .trendsAnalysis(trendsAnalysis)
                .metadata(metadata)
                .build();
    }

    /**
     * Obtiene las modalidades para la comparativa según los filtros
     */
    private List<StudentModality> getModalitiesForComparison(List<StudentModality> programModalities,
                                                             Long programId, ModalityComparisonFilterDTO filters) {
        List<StudentModality> filteredModalities = programModalities;

        // Filtrar por programa
        filteredModalities = filteredModalities.stream()
                .filter(m -> m.getAcademicProgram().getId().equals(programId))
                .collect(Collectors.toList());

        // Filtrar por activas o todas
        if (filters != null && Boolean.TRUE.equals(filters.getOnlyActiveModalities())) {
            List<ModalityProcessStatus> activeStatuses = ReportUtils.getActiveStatuses();
            filteredModalities = filteredModalities.stream()
                    .filter(m -> activeStatuses.contains(m.getStatus()))
                    .collect(Collectors.toList());
        }

        // Filtrar por año y semestre si se especificó
        if (filters != null && filters.getYear() != null) {
            filteredModalities = filteredModalities.stream()
                    .filter(m -> m.getSelectionDate() != null &&
                                 m.getSelectionDate().getYear() == filters.getYear())
                    .collect(Collectors.toList());

            if (filters.getSemester() != null) {
                filteredModalities = filteredModalities.stream()
                        .filter(m -> ReportUtils.getSemesterFromDate(m.getSelectionDate()) == filters.getSemester())
                        .collect(Collectors.toList());
            }
        }

        return filteredModalities;
    }

    /**
     * Genera el resumen general de la comparativa
     */
    private ModalityTypeComparisonReportDTO.ComparisonSummaryDTO generateComparisonSummary(
            List<StudentModality> modalities) {

        // Agrupar por tipo de modalidad
        Map<String, List<StudentModality>> byType = modalities.stream()
                .collect(Collectors.groupingBy(m -> m.getProgramDegreeModality().getDegreeModality().getName()));

        int totalTypes = byType.size();
        int totalModalities = modalities.size();

        // Contar estudiantes únicos
        Map<Long, List<StudentModalityMember>> membersByModality =
                ReportUtils.loadActiveMembersByModalityIds(
                        modalities.stream().map(StudentModality::getId).toList(),
                        studentModalityMemberRepository);
        Set<Long> uniqueStudents = new HashSet<>();
        for (StudentModality modality : modalities) {
            List<StudentModalityMember> members = membersByModality.getOrDefault(modality.getId(), List.of());
            members.forEach(member -> uniqueStudents.add(member.getStudent().getId()));
        }

        // Encontrar tipo más y menos popular
        String mostPopular = null;
        int mostPopularCount = 0;
        String leastPopular = null;
        int leastPopularCount = Integer.MAX_VALUE;

        for (Map.Entry<String, List<StudentModality>> entry : byType.entrySet()) {
            int count = entry.getValue().size();
            if (count > mostPopularCount) {
                mostPopularCount = count;
                mostPopular = entry.getKey();
            }
            if (count < leastPopularCount) {
                leastPopularCount = count;
                leastPopular = entry.getKey();
            }
        }

        double avgModalitiesPerType = totalTypes > 0 ? (double) totalModalities / totalTypes : 0;
        double avgStudentsPerType = totalTypes > 0 ? (double) uniqueStudents.size() / totalTypes : 0;

        return ModalityTypeComparisonReportDTO.ComparisonSummaryDTO.builder()
                .totalModalityTypes(totalTypes)
                .totalModalities(totalModalities)
                .totalStudents(uniqueStudents.size())
                .mostPopularType(mostPopular)
                .mostPopularTypeCount(mostPopularCount)
                .leastPopularType(leastPopular)
                .leastPopularTypeCount(leastPopularCount == Integer.MAX_VALUE ? 0 : leastPopularCount)
                .averageModalitiesPerType(Math.round(avgModalitiesPerType * 100.0) / 100.0)
                .averageStudentsPerType(Math.round(avgStudentsPerType * 100.0) / 100.0)
                .build();
    }

    /**
     * Genera estadísticas detalladas por cada tipo de modalidad
     */
    private List<ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO> generateModalityTypeStatistics(
            List<StudentModality> modalities) {

        Map<Long, List<StudentModality>> byTypeId = modalities.stream()
                .collect(Collectors.groupingBy(m -> m.getProgramDegreeModality().getDegreeModality().getId()));

        int totalModalities = modalities.size();

        Map<Long, List<StudentModalityMember>> membersByModality =
                ReportUtils.loadActiveMembersByModalityIds(
                        modalities.stream().map(StudentModality::getId).toList(),
                        studentModalityMemberRepository);

        return byTypeId.entrySet().stream()
                .map(entry -> {
                    List<StudentModality> typeModalities = entry.getValue();
                    if (typeModalities.isEmpty()) return null;

                    var firstModality = typeModalities.get(0);
                    var degreeModality = firstModality.getProgramDegreeModality().getDegreeModality();

                    // Contar estudiantes únicos de este tipo
                    Set<Long> students = new HashSet<>();
                    for (StudentModality modality : typeModalities) {
                        List<StudentModalityMember> members = membersByModality.getOrDefault(modality.getId(), List.of());
                        members.forEach(member -> students.add(member.getStudent().getId()));
                    }

                    // Contar individuales y grupales
                    long individual = typeModalities.stream()
                            .filter(m -> m.getModalityType() == com.SIGMA.USCO.Modalities.Entity.enums.ModalityType.INDIVIDUAL)
                            .count();
                    long group = typeModalities.stream()
                            .filter(m -> m.getModalityType() == com.SIGMA.USCO.Modalities.Entity.enums.ModalityType.GROUP)
                            .count();

                    // Contar con/sin director
                    long withDirector = typeModalities.stream()
                            .filter(m -> m.getProjectDirector() != null)
                            .count();
                    long withoutDirector = typeModalities.size() - withDirector;

                    // Distribución por estado
                    Map<String, Integer> statusDistribution = typeModalities.stream()
                            .collect(Collectors.groupingBy(
                                    m -> TranslationUtils.translateModalityProcessStatus(m.getStatus()),
                                    Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                            ));

                    double avgStudents = typeModalities.size() > 0 ?
                            (double) students.size() / typeModalities.size() : 0;
                    double percentage = totalModalities > 0 ?
                            (double) typeModalities.size() * 100 / totalModalities : 0;

                    return ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO.builder()
                            .modalityTypeId(degreeModality.getId())
                            .modalityTypeName(degreeModality.getName())
                            .description(degreeModality.getDescription())
                            .totalModalities(typeModalities.size())
                            .totalStudents(students.size())
                            .individualModalities((int) individual)
                            .groupModalities((int) group)
                            .averageStudentsPerModality(Math.round(avgStudents * 100.0) / 100.0)
                            .percentageOfTotal(Math.round(percentage * 100.0) / 100.0)
                            .requiresDirector(ReportUtils.isDirectorRequired(degreeModality.getName()))
                            .modalitiesWithDirector((int) withDirector)
                            .modalitiesWithoutDirector((int) withoutDirector)
                            .distributionByStatus(statusDistribution)
                            .trend(null) // No calculable por tipo; el análisis de tendencias es a nivel global
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO::getTotalModalities).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Genera la distribución de estudiantes por tipo
     */
    private Map<String, Integer> generateStudentDistribution(List<StudentModality> modalities) {
        Map<String, Set<Long>> studentsByType = new HashMap<>();
        Map<Long, List<StudentModalityMember>> membersByModality =
                ReportUtils.loadActiveMembersByModalityIds(
                        modalities.stream().map(StudentModality::getId).toList(),
                        studentModalityMemberRepository);

        for (StudentModality modality : modalities) {
            String typeName = modality.getProgramDegreeModality().getDegreeModality().getName();
            List<StudentModalityMember> members = membersByModality.getOrDefault(modality.getId(), List.of());

            studentsByType.computeIfAbsent(typeName, k -> new HashSet<>());
            members.forEach(member -> studentsByType.get(typeName).add(member.getStudent().getId()));
        }

        return studentsByType.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()
                ));
    }

    /**
     * Genera la comparación histórica por periodos
     */
    private List<ModalityTypeComparisonReportDTO.PeriodComparisonDTO> generateHistoricalComparison(
            List<StudentModality> programModalities, Long programId, int periodsCount,
            ModalityComparisonFilterDTO baseFilters) {

        List<ModalityTypeComparisonReportDTO.PeriodComparisonDTO> periods = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();
        int currentSemester = ReportUtils.getSemesterFromDate(now);

        // Miembros activos de todas las modalidades del programa (una sola consulta)
        Map<Long, List<StudentModalityMember>> membersByModality =
                ReportUtils.loadActiveMembersByModalityIds(
                        programModalities.stream().map(StudentModality::getId).toList(),
                        studentModalityMemberRepository);

        // Generar periodos hacia atrás
        for (int i = 0; i < periodsCount; i++) {
            int year = currentYear;
            int semester = currentSemester - i;

            // Ajustar año si el semestre es negativo
            while (semester <= 0) {
                year--;
                semester += 2;
            }

            ModalityComparisonFilterDTO periodFilter = ModalityComparisonFilterDTO.builder()
                    .year(year)
                    .semester(semester)
                    .onlyActiveModalities(baseFilters != null ? baseFilters.getOnlyActiveModalities() : false)
                    .build();

            List<StudentModality> periodModalities = getModalitiesForComparison(programModalities, programId, periodFilter);

            // Contar modalidades por tipo
            Map<String, Integer> modalitiesByType = periodModalities.stream()
                    .collect(Collectors.groupingBy(
                            m -> m.getProgramDegreeModality().getDegreeModality().getName(),
                            Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                    ));

            // Contar estudiantes por tipo
            Map<String, Set<Long>> studentsSetByType = new HashMap<>();
            for (StudentModality modality : periodModalities) {
                String typeName = modality.getProgramDegreeModality().getDegreeModality().getName();
                List<StudentModalityMember> members = membersByModality.getOrDefault(modality.getId(), List.of());

                studentsSetByType.computeIfAbsent(typeName, k -> new HashSet<>());
                members.forEach(member -> studentsSetByType.get(typeName).add(member.getStudent().getId()));
            }

            Map<String, Integer> studentsByType = studentsSetByType.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));

            int totalStudents = studentsSetByType.values().stream()
                    .mapToInt(Set::size)
                    .sum();

            periods.add(ModalityTypeComparisonReportDTO.PeriodComparisonDTO.builder()
                    .year(year)
                    .semester(semester)
                    .periodLabel(year + "-" + semester)
                    .modalitiesByType(modalitiesByType)
                    .studentsByType(studentsByType)
                    .totalModalitiesInPeriod(periodModalities.size())
                    .totalStudentsInPeriod(totalStudents)
                    .build());
        }

        return periods;
    }

    /**
     * Genera el análisis de tendencias
     */
    private ModalityTypeComparisonReportDTO.TrendsAnalysisDTO generateTrendsAnalysis(
            List<ModalityTypeComparisonReportDTO.PeriodComparisonDTO> historicalData,
            List<ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO> currentStats) {

        if (historicalData == null || historicalData.size() < 2) {
            return null;
        }

        List<String> growingTypes = new ArrayList<>();
        List<String> decliningTypes = new ArrayList<>();
        List<String> stableTypes = new ArrayList<>();
        Map<String, Double> growthRates = new HashMap<>();

        String mostImproved = null;
        double maxGrowth = Double.MIN_VALUE;
        String mostDeclined = null;
        double maxDecline = Double.MAX_VALUE;

        // Analizar cada tipo de modalidad
        for (ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO stat : currentStats) {
            String typeName = stat.getModalityTypeName();

            // Obtener valores del periodo más reciente y más antiguo
            int recentCount = historicalData.get(0).getModalitiesByType().getOrDefault(typeName, 0);
            int oldestCount = historicalData.get(historicalData.size() - 1).getModalitiesByType().getOrDefault(typeName, 0);

            double growthRate = 0.0;
            if (oldestCount > 0) {
                growthRate = ((double) (recentCount - oldestCount) / oldestCount) * 100;
            } else if (recentCount > 0) {
                growthRate = 100.0; // Nuevo tipo que no existía antes
            }

            growthRates.put(typeName, Math.round(growthRate * 100.0) / 100.0);

            // Clasificar tendencia
            if (growthRate > 10) {
                growingTypes.add(typeName);
                if (growthRate > maxGrowth) {
                    maxGrowth = growthRate;
                    mostImproved = typeName;
                }
            } else if (growthRate < -10) {
                decliningTypes.add(typeName);
                if (growthRate < maxDecline) {
                    maxDecline = growthRate;
                    mostDeclined = typeName;
                }
            } else {
                stableTypes.add(typeName);
            }
        }

        // Determinar tendencia general
        int totalGrowing = growingTypes.size();
        int totalDeclining = decliningTypes.size();
        String overallTrend;
        if (totalGrowing > totalDeclining) {
            overallTrend = "GROWING";
        } else if (totalDeclining > totalGrowing) {
            overallTrend = "DECLINING";
        } else {
            overallTrend = "STABLE";
        }

        return ModalityTypeComparisonReportDTO.TrendsAnalysisDTO.builder()
                .overallTrend(overallTrend)
                .growingTypes(growingTypes)
                .decliningTypes(decliningTypes)
                .stableTypes(stableTypes)
                .mostImprovedType(mostImproved)
                .mostDeclinedType(mostDeclined)
                .growthRateByType(growthRates)
                .build();
    }
}
