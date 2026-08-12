package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.common.util.TranslationUtils;
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
public class StudentListingReportService {

    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;


    /**
     * Genera reporte de listado de estudiantes con filtros múltiples
     * Permite filtrar por estados, modalidades y semestres
     *
     * @param filters Filtros a aplicar
     * @return Reporte completo de estudiantes
     */
    @Transactional(readOnly = true)
    public StudentListingReportDTO generateStudentListingReport(StudentListingFilterDTO filters) {
        long startTime = System.currentTimeMillis();

        // Obtener usuario autenticado y su programa
        String userEmail = SecurityUtils.getCurrentUser().getEmail();
        AcademicProgram userProgram = ReportUtils.getAuthenticatedUserProgram(programAuthorityRepository);

        // Obtener todas las modalidades del programa
        List<StudentModality> allModalities = studentModalityRepository.findForProgramHead(List.of(userProgram.getId())).stream()
                .filter(m -> m.getAcademicProgram().getId().equals(userProgram.getId()))
                .collect(Collectors.toList());

        // Aplicar filtros
        List<StudentModality> filteredModalities = applyFilters(allModalities, filters);

        // Generar filtros aplicados
        StudentListingReportDTO.AppliedFiltersDTO appliedFilters = buildAppliedFilters(filters);

        // Construir detalles de estudiantes
        List<StudentListingReportDTO.StudentDetailDTO> studentDetails = buildStudentDetails(filteredModalities);

        // Generar resumen ejecutivo
        StudentListingReportDTO.ExecutiveSummaryDTO executiveSummary = buildStudentExecutiveSummary(studentDetails, filteredModalities);


        // Generar análisis de distribución
        StudentListingReportDTO.DistributionAnalysisDTO distributionAnalysis = buildDistributionAnalysis(studentDetails, filteredModalities);

        // Generar estadísticas por modalidad
        List<StudentListingReportDTO.ModalityStatisticsDTO> modalityStatistics = buildModalityStatistics(filteredModalities);

        // Generar estadísticas por estado
        List<StudentListingReportDTO.StatusStatisticsDTO> statusStatistics = buildStatusStatistics(filteredModalities);

        // Generar estadísticas por semestre
        List<StudentListingReportDTO.SemesterStatisticsDTO> semesterStatistics = buildSemesterStatistics(filteredModalities);

        // Generar estadísticas generales
        StudentListingReportDTO.GeneralStatisticsDTO generalStatistics = buildGeneralStatistics(studentDetails, filteredModalities);

        // Aplicar ordenamiento
        studentDetails = applySorting(studentDetails, filters);

        // Calcular tiempo de generación
        long endTime = System.currentTimeMillis();
        long generationTime = endTime - startTime;

        // Construir metadata
        ReportMetadataDTO metadata = ReportMetadataDTO.builder()
                .reportVersion("1.0")
                .reportType("STUDENT_LISTING_FILTERED")
                .generatedBySystem("SIGMA - Sistema de Gestión de Modalidades de Grado")
                .totalRecords(studentDetails.size())
                .generationTimeMs(generationTime)
                .exportFormat("JSON")
                .build();

        return StudentListingReportDTO.builder()
                .generatedAt(LocalDateTime.now())
                .generatedBy(userEmail + " (" + userProgram.getName() + ")")
                .academicProgramId(userProgram.getId())
                .academicProgramName(userProgram.getName())
                .academicProgramCode(userProgram.getCode())
                .appliedFilters(appliedFilters)
                .executiveSummary(executiveSummary)
                .students(studentDetails)
                .generalStatistics(generalStatistics)
                .distributionAnalysis(distributionAnalysis)
                .modalityStatistics(modalityStatistics)
                .statusStatistics(statusStatistics)
                .semesterStatistics(semesterStatistics)
                .metadata(metadata)
                .build();
    }

    /**
     * Aplica filtros a la lista de modalidades
     */
    private List<StudentModality> applyFilters(List<StudentModality> modalities, StudentListingFilterDTO filters) {
        if (filters == null) {
            return modalities;
        }

        return modalities.stream()
                .filter(m -> filterByStatus(m, filters.getStatuses()))
                .filter(m -> ReportUtils.filterByModalityTypes(m, filters.getModalityTypes()))
                .filter(m -> filterBySemester(m, filters.getSemesters(), filters.getYear()))
                .filter(m -> ReportUtils.filterByModalityTypeFilter(m, filters.getModalityTypeFilter()))
                .filter(m -> filterByDirector(m, filters.getHasDirector()))
                .filter(m -> filterByTimelineStatus(m, filters.getTimelineStatus()))
                .collect(Collectors.toList());
    }

    private boolean filterByStatus(StudentModality modality, List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) return true;
        return statuses.contains(modality.getStatus().name());
    }

    private boolean filterBySemester(StudentModality modality, List<String> semesters, Integer year) {
        if (semesters == null || semesters.isEmpty()) {
            if (year != null && modality.getSelectionDate() != null) {
                return modality.getSelectionDate().getYear() == year;
            }
            return true;
        }

        if (modality.getSelectionDate() == null) return false;

        int modalityYear = modality.getSelectionDate().getYear();
        int modalitySemester = ReportUtils.getSemesterFromDate(modality.getSelectionDate());
        String modalityPeriod = modalityYear + "-" + modalitySemester;

        return semesters.contains(modalityPeriod);
    }

    private boolean filterByDirector(StudentModality modality, Boolean hasDirector) {
        if (hasDirector == null) return true;
        return hasDirector ? modality.getProjectDirector() != null : modality.getProjectDirector() == null;
    }

    private boolean filterByTimelineStatus(StudentModality modality, String timelineStatus) {
        if (timelineStatus == null || timelineStatus.isEmpty()) return true;
        String calculatedStatus = calculateTimelineStatus(modality);
        return timelineStatus.equals(calculatedStatus);
    }

    /**
     * Construye los filtros aplicados
     */
    private StudentListingReportDTO.AppliedFiltersDTO buildAppliedFilters(StudentListingFilterDTO filters) {
        if (filters == null) {
            return StudentListingReportDTO.AppliedFiltersDTO.builder()
                    .hasFilters(false)
                    .filterDescription("Sin filtros aplicados - Mostrando todos los estudiantes")
                    .build();
        }

        List<String> filterParts = new ArrayList<>();

        if (filters.getStatuses() != null && !filters.getStatuses().isEmpty()) {
            filterParts.add("Estados: " + String.join(", ", filters.getStatuses()));
        }

        if (filters.getModalityTypes() != null && !filters.getModalityTypes().isEmpty()) {
            filterParts.add("Modalidades: " + String.join(", ", filters.getModalityTypes()));
        }

        if (filters.getSemesters() != null && !filters.getSemesters().isEmpty()) {
            filterParts.add("Semestres: " + String.join(", ", filters.getSemesters()));
        }

        if (filters.getYear() != null) {
            filterParts.add("Año: " + filters.getYear());
        }

        if (filters.getTimelineStatus() != null) {
            filterParts.add("Estado temporal: " + filters.getTimelineStatus());
        }

        if (filters.getModalityTypeFilter() != null) {
            filterParts.add("Tipo: " + filters.getModalityTypeFilter());
        }

        if (filters.getHasDirector() != null) {
            filterParts.add("Con director: " + (filters.getHasDirector() ? "Sí" : "No"));
        }

        String description = filterParts.isEmpty() ?
                "Sin filtros aplicados" :
                String.join(" | ", filterParts);

        return StudentListingReportDTO.AppliedFiltersDTO.builder()
                .statuses(filters.getStatuses())
                .modalityTypes(filters.getModalityTypes())
                .semesters(filters.getSemesters())
                .year(filters.getYear())
                .filterDescription(description)
                .hasFilters(!filterParts.isEmpty())
                .build();
    }

    /**
     * Construye detalles de estudiantes
     */
    private List<StudentListingReportDTO.StudentDetailDTO> buildStudentDetails(List<StudentModality> modalities) {
        List<StudentListingReportDTO.StudentDetailDTO> details = new ArrayList<>();

        List<Long> modalityIds = modalities.stream().map(StudentModality::getId).toList();
        Map<Long, List<StudentModalityMember>> membersByModality =
                ReportUtils.loadActiveMembersByModalityIds(modalityIds, studentModalityMemberRepository);
        List<Long> allUserIds = membersByModality.values().stream()
                .flatMap(List::stream)
                .map(StudentModalityMember::getStudent)
                .filter(Objects::nonNull)
                .map(User::getId)
                .distinct()
                .toList();
        Map<Long, StudentProfile> profs = ReportUtils.loadProfilesByUserIds(allUserIds, studentProfileRepository);

        for (StudentModality modality : modalities) {
            List<StudentModalityMember> members = membersByModality.getOrDefault(modality.getId(), List.of());

            for (StudentModalityMember member : members) {
                User user = member.getStudent(); // getStudent() retorna User
                if (user == null) continue;

                // Buscar el perfil de estudiante
                StudentProfile profile = profs.get(user.getId());
                if (profile == null) continue;

                // Obtener miembros del grupo
                List<String> groupMembers = new ArrayList<>();
                int groupSize = members.size();

                if (groupSize > 1) {
                    groupMembers = members.stream()
                            .filter(m -> !m.getStudent().getId().equals(user.getId()))
                            .map(m -> {
                                User memberUser = m.getStudent();
                                return memberUser != null ? memberUser.getName() + " " + memberUser.getLastName() : "N/D";
                            })
                            .collect(Collectors.toList());
                }

                // Calcular días en modalidad
                long daysInModality = modality.getSelectionDate() != null ?
                        ChronoUnit.DAYS.between(modality.getSelectionDate(), LocalDateTime.now()) : 0;

                // Calcular progreso
                double progress = calculateProgress(modality);

                // Estado de línea de tiempo
                String timelineStatus = calculateTimelineStatus(modality);

                details.add(StudentListingReportDTO.StudentDetailDTO.builder()
                        .studentId(user.getId())
                        .studentCode(profile.getStudentCode())
                        .fullName(user.getName() + " " + user.getLastName())
                        .firstName(user.getName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .phone(null) // Campo no disponible en User
                        .academicStatus("ACTIVE") // Campo no disponible, valor por defecto
                        .cumulativeAverage(profile.getGpa())
                        .completedCredits(profile.getApprovedCredits() != null ? profile.getApprovedCredits().intValue() : null)
                        .totalCredits(null) // Campo no disponible
                        .currentSemester(profile.getSemester() != null ? profile.getSemester().intValue() : null)
                        .modalityId(modality.getId())
                        .modalityType(modality.getProgramDegreeModality().getDegreeModality().getName())
                        .modalityName(modality.getProgramDegreeModality().getDegreeModality().getName())
                        .modalityStatus(TranslationUtils.translateModalityProcessStatus(modality.getStatus()))
                        .modalityStatusDescription(TranslationUtils.translateModalityProcessStatus(modality.getStatus()))
                        .selectionDate(modality.getSelectionDate())
                        .lastUpdateDate(modality.getUpdatedAt())
                        .daysInModality((int) daysInModality)
                        .directorName(modality.getProjectDirector() != null ?
                                modality.getProjectDirector().getName() + " " + modality.getProjectDirector().getLastName() : null)
                        .directorEmail(modality.getProjectDirector() != null ?
                                modality.getProjectDirector().getEmail() : null)
                        .projectTitle(null) // Campo no disponible
                        .projectDescription(null) // Campo no disponible
                        .groupSize(groupSize)
                        .groupMembers(groupMembers)
                        .progressPercentage(progress)
                        .timelineStatus(timelineStatus)
                        .expectedCompletionDays(calculateExpectedCompletionDays(modality))
                        .observations(generateObservations(modality, timelineStatus))
                        .build());
            }
        }

        return details;
    }

    /**
     * Calcula el progreso de una modalidad
     */
    private double calculateProgress(StudentModality modality) {
        if (modality.getStatus() == ModalityProcessStatus.GRADED_APPROVED) {
            return 100.0;
        }

        Map<ModalityProcessStatus, Integer> progressMap = new HashMap<>();
        progressMap.put(ModalityProcessStatus.MODALITY_SELECTED, 10);
        progressMap.put(ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD, 20);
        progressMap.put(ModalityProcessStatus.READY_FOR_PROGRAM_CURRICULUM_COMMITTEE, 30);
        progressMap.put(ModalityProcessStatus.UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE, 40);
        progressMap.put(ModalityProcessStatus.READY_FOR_DIRECTOR_ASSIGNMENT, 45);
        progressMap.put(ModalityProcessStatus.PROPOSAL_APPROVED, 50);
        progressMap.put(ModalityProcessStatus.CORRECTIONS_SUBMITTED, 55);
        progressMap.put(ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD, 55);
        progressMap.put(ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_COMMITTEE, 55);
        progressMap.put(ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_EXAMINERS, 55);
        progressMap.put(ModalityProcessStatus.CORRECTIONS_APPROVED, 60);
        progressMap.put(ModalityProcessStatus.DEFENSE_SCHEDULED, 70);
        progressMap.put(ModalityProcessStatus.EXAMINERS_ASSIGNED, 75);
        progressMap.put(ModalityProcessStatus.DEFENSE_COMPLETED, 80);
        progressMap.put(ModalityProcessStatus.UNDER_EVALUATION_PRIMARY_EXAMINERS, 85);
        progressMap.put(ModalityProcessStatus.EVALUATION_COMPLETED, 90);

        return progressMap.getOrDefault(modality.getStatus(), 5);
    }

    /**
     * Calcula el estado de línea de tiempo
     */
    private String calculateTimelineStatus(StudentModality modality) {
        if (modality.getSelectionDate() == null) return "N/D";

        long daysInModality = ChronoUnit.DAYS.between(modality.getSelectionDate(), LocalDateTime.now());

        if (modality.getStatus() == ModalityProcessStatus.GRADED_APPROVED) {
            return "COMPLETED";
        }

        // Criterios de tiempo esperado (ajustables)
        if (daysInModality <= 180) return "ON_TIME";
        if (daysInModality <= 365) return "AT_RISK";
        return "DELAYED";
    }

    /**
     * Calcula días esperados para completar
     */
    private Integer calculateExpectedCompletionDays(StudentModality modality) {
        if (modality.getStatus() == ModalityProcessStatus.GRADED_APPROVED) {
            return 0;
        }

        // Tiempo promedio estimado: 365 días (1 año)
        long daysElapsed = modality.getSelectionDate() != null ?
                ChronoUnit.DAYS.between(modality.getSelectionDate(), LocalDateTime.now()) : 0;

        return Math.max(0, (int) (365 - daysElapsed));
    }

    /**
     * Genera observaciones
     */
    private String generateObservations(StudentModality modality, String timelineStatus) {
        List<String> observations = new ArrayList<>();

        if ("DELAYED".equals(timelineStatus)) {
            observations.add("Requiere seguimiento prioritario");
        } else if ("AT_RISK".equals(timelineStatus)) {
            observations.add("Seguimiento preventivo recomendado");
        }

        if (modality.getProjectDirector() == null && ReportUtils.isDirectorRequired(modality.getProgramDegreeModality().getDegreeModality().getName())) {
            observations.add("Pendiente asignación de director");
        }

        return observations.isEmpty() ? null : String.join(" | ", observations);
    }

    /**
     * Construye resumen ejecutivo
     */
    private StudentListingReportDTO.ExecutiveSummaryDTO buildStudentExecutiveSummary(
            List<StudentListingReportDTO.StudentDetailDTO> students,
            List<StudentModality> modalities) {

        Map<String, Integer> quickStats = new LinkedHashMap<>();
        quickStats.put("Total Estudiantes", students.size());
        quickStats.put("Modalidades Activas", (int) modalities.stream()
                .filter(m -> ReportUtils.getActiveStatuses().contains(m.getStatus())).count());
        quickStats.put("Completadas", (int) modalities.stream()
                .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED).count());

        Map<String, Long> byType = students.stream()
                .collect(Collectors.groupingBy(
                        StudentListingReportDTO.StudentDetailDTO::getModalityType,
                        Collectors.counting()));

        Map<String, Long> byStatus = students.stream()
                .collect(Collectors.groupingBy(
                        StudentListingReportDTO.StudentDetailDTO::getModalityStatusDescription,
                        Collectors.counting()));

        String mostCommonType = byType.isEmpty() ? "N/D" :
                Collections.max(byType.entrySet(), Map.Entry.comparingByValue()).getKey();

        String mostCommonStatus = byStatus.isEmpty() ? "N/D" :
                Collections.max(byStatus.entrySet(), Map.Entry.comparingByValue()).getKey();

        double avgProgress = students.stream()
                .filter(s -> s.getProgressPercentage() != null)
                .mapToDouble(StudentListingReportDTO.StudentDetailDTO::getProgressPercentage)
                .average()
                .orElse(0.0);

        Set<Long> uniqueModalities = students.stream()
                .map(StudentListingReportDTO.StudentDetailDTO::getModalityId)
                .collect(Collectors.toSet());

        long activeCount = students.stream()
                .filter(s -> ReportUtils.getActiveStatuses().stream()
                        .anyMatch(status -> status.name().equals(s.getModalityStatus())))
                .count();

        long completedCount = students.stream()
                .filter(s -> "GRADED_APPROVED".equals(s.getModalityStatus()))
                .count();

        return StudentListingReportDTO.ExecutiveSummaryDTO.builder()
                .totalStudents(students.size())
                .totalModalities(uniqueModalities.size())
                .activeModalities((int) activeCount)
                .completedModalities((int) completedCount)
                .differentModalityTypes((int) byType.size())
                .differentStatuses((int) byStatus.size())
                .averageProgress(Math.round(avgProgress * 100.0) / 100.0)
                .mostCommonModalityType(mostCommonType)
                .mostCommonStatus(mostCommonStatus)
                .quickStats(quickStats)
                .build();
    }

    /**
     * Construye estadísticas generales
     */
    private StudentListingReportDTO.GeneralStatisticsDTO buildGeneralStatistics(
            List<StudentListingReportDTO.StudentDetailDTO> students,
            List<StudentModality> modalities) {

        long individualCount = modalities.stream()
                .filter(m -> m.getModalityType() == com.SIGMA.USCO.Modalities.Entity.enums.ModalityType.INDIVIDUAL)
                .count();

        long groupCount = modalities.size() - individualCount;

        double avgCredits = students.stream()
                .filter(s -> s.getCompletedCredits() != null)
                .mapToInt(StudentListingReportDTO.StudentDetailDTO::getCompletedCredits)
                .average()
                .orElse(0.0);

        double avgGPA = students.stream()
                .filter(s -> s.getCumulativeAverage() != null)
                .mapToDouble(StudentListingReportDTO.StudentDetailDTO::getCumulativeAverage)
                .average()
                .orElse(0.0);

        double avgDays = students.stream()
                .filter(s -> s.getDaysInModality() != null)
                .mapToInt(StudentListingReportDTO.StudentDetailDTO::getDaysInModality)
                .average()
                .orElse(0.0);

        long withDirector = students.stream()
                .filter(s -> s.getDirectorName() != null)
                .count();

        long onTime = students.stream()
                .filter(s -> "ON_TIME".equals(s.getTimelineStatus()))
                .count();

        long delayed = students.stream()
                .filter(s -> "DELAYED".equals(s.getTimelineStatus()))
                .count();

        long atRisk = students.stream()
                .filter(s -> "AT_RISK".equals(s.getTimelineStatus()))
                .count();

        Map<String, Integer> byAcademicStatus = students.stream()
                .collect(Collectors.groupingBy(
                        StudentListingReportDTO.StudentDetailDTO::getAcademicStatus,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        Map<String, Integer> bySemester = students.stream()
                .filter(s -> s.getCurrentSemester() != null)
                .collect(Collectors.groupingBy(
                        s -> "Semestre " + s.getCurrentSemester(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        return StudentListingReportDTO.GeneralStatisticsDTO.builder()
                .totalStudents(students.size())
                .individualModalities((int) individualCount)
                .groupModalities((int) groupCount)
                .individualVsGroupRatio(groupCount > 0 ? (double) individualCount / groupCount : 0)
                .averageCompletedCredits(Math.round(avgCredits * 100.0) / 100.0)
                .averageCumulativeGPA(Math.round(avgGPA * 100.0) / 100.0)
                .averageDaysInModality(Math.round(avgDays * 100.0) / 100.0)
                .studentsWithDirector((int) withDirector)
                .studentsWithoutDirector(students.size() - (int) withDirector)
                .studentsOnTime((int) onTime)
                .studentsDelayed((int) delayed)
                .studentsAtRisk((int) atRisk)
                .studentsByAcademicStatus(byAcademicStatus)
                .studentsBySemester(bySemester)
                .build();
    }

    /**
     * Construye análisis de distribución
     */
    private StudentListingReportDTO.DistributionAnalysisDTO buildDistributionAnalysis(
            List<StudentListingReportDTO.StudentDetailDTO> students,
            List<StudentModality> modalities) {

        int total = students.size();

        // Por tipo de modalidad
        Map<String, Integer> byModalityType = students.stream()
                .collect(Collectors.groupingBy(
                        StudentListingReportDTO.StudentDetailDTO::getModalityType,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        Map<String, Double> byModalityTypePercentage = byModalityType.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> total > 0 ? (e.getValue() * 100.0) / total : 0.0));

        // Por estado
        Map<String, Integer> byStatus = students.stream()
                .collect(Collectors.groupingBy(
                        StudentListingReportDTO.StudentDetailDTO::getModalityStatusDescription,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        Map<String, Double> byStatusPercentage = byStatus.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> total > 0 ? (e.getValue() * 100.0) / total : 0.0));

        // Por estado de línea de tiempo
        Map<String, Integer> byTimelineStatus = students.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getTimelineStatus() != null ? s.getTimelineStatus() : "N/D",
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        Map<String, Double> byTimelineStatusPercentage = byTimelineStatus.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> total > 0 ? (e.getValue() * 100.0) / total : 0.0));

        return StudentListingReportDTO.DistributionAnalysisDTO.builder()
                .byModalityType(byModalityType)
                .byModalityTypePercentage(byModalityTypePercentage)
                .byStatus(byStatus)
                .byStatusPercentage(byStatusPercentage)
                .byTimelineStatus(byTimelineStatus)
                .byTimelineStatusPercentage(byTimelineStatusPercentage)
                .build();
    }

    /**
     * Construye estadísticas por modalidad
     */
    private List<StudentListingReportDTO.ModalityStatisticsDTO> buildModalityStatistics(
            List<StudentModality> modalities) {

        Map<String, List<StudentModality>> groupedByType = modalities.stream()
                .collect(Collectors.groupingBy(m -> m.getProgramDegreeModality().getDegreeModality().getName()));

        return groupedByType.entrySet().stream()
                .map(entry -> {
                    String typeName = entry.getKey();
                    List<StudentModality> typeModalities = entry.getValue();

                    long activeCount = typeModalities.stream()
                            .filter(m -> ReportUtils.getActiveStatuses().contains(m.getStatus()))
                            .count();

                    long completedCount = typeModalities.stream()
                            .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED)
                            .count();

                    double completionRate = typeModalities.size() > 0 ?
                            (completedCount * 100.0) / typeModalities.size() : 0.0;

                    // Calcular días promedio para completar (solo modalidades completadas)
                    Double avgDaysToComplete = typeModalities.stream()
                            .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED)
                            .filter(m -> m.getSelectionDate() != null && m.getUpdatedAt() != null)
                            .mapToLong(m -> ChronoUnit.DAYS.between(m.getSelectionDate(), m.getUpdatedAt()))
                            .average()
                            .orElse(0.0);

                    // Encontrar días mínimo y máximo
                    Integer minDays = typeModalities.stream()
                            .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED)
                            .filter(m -> m.getSelectionDate() != null && m.getUpdatedAt() != null)
                            .mapToInt(m -> (int) ChronoUnit.DAYS.between(m.getSelectionDate(), m.getUpdatedAt()))
                            .min()
                            .orElse(0);

                    Integer maxDays = typeModalities.stream()
                            .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED)
                            .filter(m -> m.getSelectionDate() != null && m.getUpdatedAt() != null)
                            .mapToInt(m -> (int) ChronoUnit.DAYS.between(m.getSelectionDate(), m.getUpdatedAt()))
                            .max()
                            .orElse(0);

                    // Calcular GPA promedio de estudiantes en esta modalidad
                    List<Long> typeModalityIds = typeModalities.stream().map(StudentModality::getId).toList();
                    Map<Long, List<StudentModalityMember>> membersByType =
                            ReportUtils.loadActiveMembersByModalityIds(typeModalityIds, studentModalityMemberRepository);
                    List<Long> typeUserIds = membersByType.values().stream()
                            .flatMap(List::stream)
                            .map(StudentModalityMember::getStudent)
                            .filter(Objects::nonNull)
                            .map(User::getId)
                            .distinct()
                            .toList();
                    Map<Long, StudentProfile> typeProfiles =
                            ReportUtils.loadProfilesByUserIds(typeUserIds, studentProfileRepository);

                    List<Double> gpas = new ArrayList<>();
                    for (StudentModality modality : typeModalities) {
                        for (StudentModalityMember member : membersByType.getOrDefault(modality.getId(), List.of())) {
                            StudentProfile profile = typeProfiles.get(member.getStudent().getId());
                            if (profile != null && profile.getGpa() != null) {
                                gpas.add(profile.getGpa());
                            }
                        }
                    }

                    Double averageGPA = gpas.isEmpty() ? 0.0 : gpas.stream()
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(0.0);

                    // Top directores (los 3 más activos)
                    List<String> topDirectors = typeModalities.stream()
                            .filter(m -> m.getProjectDirector() != null)
                            .collect(Collectors.groupingBy(
                                    m -> m.getProjectDirector().getName() + " " + m.getProjectDirector().getLastName(),
                                    Collectors.counting()))
                            .entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            .limit(3)
                            .map(e -> e.getKey() + " (" + e.getValue() + ")")
                            .collect(Collectors.toList());

                    return StudentListingReportDTO.ModalityStatisticsDTO.builder()
                            .modalityType(typeName)
                            .modalityName(typeName)
                            .totalStudents(typeModalities.size())
                            .activeStudents((int) activeCount)
                            .completedStudents((int) completedCount)
                            .completionRate(Math.round(completionRate * 100.0) / 100.0)
                            .averageDaysToComplete(avgDaysToComplete > 0 ? Math.round(avgDaysToComplete * 100.0) / 100.0 : null)
                            .minDaysToComplete(minDays > 0 ? minDays : null)
                            .maxDaysToComplete(maxDays > 0 ? maxDays : null)
                            .topDirectors(topDirectors.isEmpty() ? null : topDirectors)
                            .averageGPA(averageGPA > 0 ? Math.round(averageGPA * 100.0) / 100.0 : null)
                            .build();
                })
                .sorted(Comparator.comparing(StudentListingReportDTO.ModalityStatisticsDTO::getTotalStudents).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Construye estadísticas por estado
     */
    private List<StudentListingReportDTO.StatusStatisticsDTO> buildStatusStatistics(
            List<StudentModality> modalities) {

        int total = modalities.size();

        Map<ModalityProcessStatus, List<StudentModality>> groupedByStatus = modalities.stream()
                .collect(Collectors.groupingBy(StudentModality::getStatus));

        return groupedByStatus.entrySet().stream()
                .map(entry -> {
                    ModalityProcessStatus status = entry.getKey();
                    List<StudentModality> statusModalities = entry.getValue();

                    double percentage = total > 0 ? (statusModalities.size() * 100.0) / total : 0.0;

                    // Calcular días promedio en este estado
                    Double avgDaysInStatus = statusModalities.stream()
                            .filter(m -> m.getSelectionDate() != null)
                            .mapToLong(m -> ChronoUnit.DAYS.between(m.getSelectionDate(), LocalDateTime.now()))
                            .average()
                            .orElse(0.0);

                    // Top modalidades en este estado (las 3 más comunes)
                    List<String> topModalities = statusModalities.stream()
                            .collect(Collectors.groupingBy(
                                    m -> m.getProgramDegreeModality().getDegreeModality().getName(),
                                    Collectors.counting()))
                            .entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            .limit(3)
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toList());

                    // Determinar tendencia (simplificada - basada en cantidad)
                    String trend = "STABLE";
                    if (ReportUtils.getActiveStatuses().contains(status)) {
                        trend = statusModalities.size() > (total * 0.2) ? "INCREASING" : "STABLE";
                    } else if (status == ModalityProcessStatus.GRADED_APPROVED) {
                        trend = statusModalities.size() > (total * 0.3) ? "INCREASING" : "STABLE";
                    } else if (status == ModalityProcessStatus.GRADED_FAILED ||
                               status == ModalityProcessStatus.MODALITY_CANCELLED) {
                        trend = statusModalities.size() > (total * 0.1) ? "INCREASING" : "DECLINING";
                    }

                    return StudentListingReportDTO.StatusStatisticsDTO.builder()
                            .status(status.name())
                            .statusDescription(TranslationUtils.translateModalityProcessStatus(status))
                            .studentCount(statusModalities.size())
                            .percentage(Math.round(percentage * 100.0) / 100.0)
                            .averageDaysInStatus(avgDaysInStatus > 0 ? Math.round(avgDaysInStatus * 100.0) / 100.0 : null)
                            .topModalities(topModalities.isEmpty() ? null : topModalities)
                            .trend(trend)
                            .build();
                })
                .sorted(Comparator.comparing(StudentListingReportDTO.StatusStatisticsDTO::getStudentCount).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Construye estadísticas por semestre
     */
    private List<StudentListingReportDTO.SemesterStatisticsDTO> buildSemesterStatistics(
            List<StudentModality> modalities) {

        Map<String, List<StudentModality>> groupedBySemester = modalities.stream()
                .filter(m -> m.getSelectionDate() != null)
                .collect(Collectors.groupingBy(m -> {
                    int year = m.getSelectionDate().getYear();
                    int semester = ReportUtils.getSemesterFromDate(m.getSelectionDate());
                    return year + "-" + semester;
                }));

        int total = modalities.size();

        return groupedBySemester.entrySet().stream()
                .map(entry -> {
                    String period = entry.getKey();
                    String[] parts = period.split("-");
                    int year = Integer.parseInt(parts[0]);
                    int semester = Integer.parseInt(parts[1]);

                    List<StudentModality> semesterModalities = entry.getValue();
                    double percentage = total > 0 ? (semesterModalities.size() * 100.0) / total : 0.0;

                    long completed = semesterModalities.stream()
                            .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED)
                            .count();

                    double completionRate = semesterModalities.size() > 0 ?
                            (completed * 100.0) / semesterModalities.size() : 0.0;

                    // Calcular GPA promedio de estudiantes en este semestre
                    List<Long> semesterModalityIds = semesterModalities.stream().map(StudentModality::getId).toList();
                    Map<Long, List<StudentModalityMember>> membersBySemester =
                            ReportUtils.loadActiveMembersByModalityIds(semesterModalityIds, studentModalityMemberRepository);
                    List<Long> semesterUserIds = membersBySemester.values().stream()
                            .flatMap(List::stream)
                            .map(StudentModalityMember::getStudent)
                            .filter(Objects::nonNull)
                            .map(User::getId)
                            .distinct()
                            .toList();
                    Map<Long, StudentProfile> semesterProfiles =
                            ReportUtils.loadProfilesByUserIds(semesterUserIds, studentProfileRepository);

                    List<Double> semesterGPAs = new ArrayList<>();
                    for (StudentModality modality : semesterModalities) {
                        for (StudentModalityMember member : membersBySemester.getOrDefault(modality.getId(), List.of())) {
                            StudentProfile profile = semesterProfiles.get(member.getStudent().getId());
                            if (profile != null && profile.getGpa() != null) {
                                semesterGPAs.add(profile.getGpa());
                            }
                        }
                    }

                    Double averageGPA = semesterGPAs.isEmpty() ? null : semesterGPAs.stream()
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(0.0);

                    // Top modalidades de este semestre (las 3 más comunes)
                    List<String> topModalityTypes = semesterModalities.stream()
                            .collect(Collectors.groupingBy(
                                    m -> m.getProgramDegreeModality().getDegreeModality().getName(),
                                    Collectors.counting()))
                            .entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            .limit(3)
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toList());

                    return StudentListingReportDTO.SemesterStatisticsDTO.builder()
                            .semester(period)
                            .year(year)
                            .studentCount(semesterModalities.size())
                            .percentage(Math.round(percentage * 100.0) / 100.0)
                            .modalitiesStarted(semesterModalities.size())
                            .modalitiesCompleted((int) completed)
                            .completionRate(Math.round(completionRate * 100.0) / 100.0)
                            .averageGPA(averageGPA != null ? Math.round(averageGPA * 100.0) / 100.0 : null)
                            .topModalityTypes(topModalityTypes.isEmpty() ? null : topModalityTypes)
                            .build();
                })
                .sorted(Comparator.comparing(StudentListingReportDTO.SemesterStatisticsDTO::getYear)
                        .thenComparing(StudentListingReportDTO.SemesterStatisticsDTO::getStudentCount).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Aplica ordenamiento a la lista de estudiantes
     */
    private List<StudentListingReportDTO.StudentDetailDTO> applySorting(
            List<StudentListingReportDTO.StudentDetailDTO> students,
            StudentListingFilterDTO filters) {

        if (filters == null || filters.getSortBy() == null) {
            return students.stream()
                    .sorted(Comparator.comparing(StudentListingReportDTO.StudentDetailDTO::getFullName))
                    .collect(Collectors.toList());
        }

        Comparator<StudentListingReportDTO.StudentDetailDTO> comparator = null;

        switch (filters.getSortBy().toUpperCase()) {
            case "NAME":
                comparator = Comparator.comparing(StudentListingReportDTO.StudentDetailDTO::getFullName);
                break;
            case "DATE":
                comparator = Comparator.comparing(StudentListingReportDTO.StudentDetailDTO::getSelectionDate,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "STATUS":
                comparator = Comparator.comparing(StudentListingReportDTO.StudentDetailDTO::getModalityStatusDescription);
                break;
            case "MODALITY":
                comparator = Comparator.comparing(StudentListingReportDTO.StudentDetailDTO::getModalityType);
                break;
            case "PROGRESS":
                comparator = Comparator.comparing(StudentListingReportDTO.StudentDetailDTO::getProgressPercentage,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            default:
                comparator = Comparator.comparing(StudentListingReportDTO.StudentDetailDTO::getFullName);
        }

        if ("DESC".equalsIgnoreCase(filters.getSortDirection())) {
            comparator = comparator.reversed();
        }

        return students.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }
}
