package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.Modalities.Entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.DefenseExaminerRepository;
import com.SIGMA.USCO.Modalities.Repository.DefenseEvaluationCriteriaRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Modalities.service.ModalityServiceUtils;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.common.util.TranslationUtils;
import com.SIGMA.USCO.report.dto.DefenseCalendarReportDTO;
import com.SIGMA.USCO.report.dto.DefenseCalendarReportDTO.*;
import com.SIGMA.USCO.report.dto.ReportMetadataDTO;
import com.SIGMA.USCO.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para generar reportes de calendario de sustentaciones y evaluaciones
 */
@Service
@RequiredArgsConstructor
public class DefenseCalendarReportService {

    private final StudentModalityRepository studentModalityRepository;
    private final DefenseExaminerRepository defenseExaminerRepository;
    private final DefenseEvaluationCriteriaRepository defenseEvaluationCriteriaRepository;
    private final UserRepository userRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;

    /**
     * Genera el reporte completo de calendario de sustentaciones
     */
    @Transactional(readOnly = true)
    public DefenseCalendarReportDTO generateDefenseCalendarReport(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Boolean includeCompleted
    ) {
        User user = SecurityUtils.getCurrentUser();

        AcademicProgram program = ReportUtils.getAuthenticatedUserProgram(programAuthorityRepository);

        // Si no se especifican fechas, usar un rango de 6 meses
        LocalDateTime effectiveStartDate = startDate != null ? startDate : LocalDateTime.now().minusMonths(3);
        LocalDateTime effectiveEndDate = endDate != null ? endDate : LocalDateTime.now().plusMonths(3);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ValidationException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        // Obtener todas las modalidades del programa
        List<StudentModality> allModalities = studentModalityRepository.findAll().stream()
                .filter(m -> m.getAcademicProgram().getId().equals(program.getId()))
                .collect(Collectors.toList());

        // Filtrar modalidades con sustentación programada en el rango
        // NOTA: defenseDate (defense_date en BD) es la fecha de SUSTENTACIÓN/DEFENSA
        //       NO confundir con selectionDate (selection_date en BD) que es la fecha de SELECCIÓN de modalidad
        List<StudentModality> modalitiesInRange = allModalities.stream()
                .filter(m -> m.getDefenseDate() != null)
                .filter(m -> !m.getDefenseDate().isBefore(effectiveStartDate) && !m.getDefenseDate().isAfter(effectiveEndDate))
                .collect(Collectors.toList());

        // Modalidades próximas (futuras)
        List<UpcomingDefenseDTO> upcomingDefenses = buildUpcomingDefenses(
                modalitiesInRange.stream()
                        .filter(m -> m.getDefenseDate().isAfter(LocalDateTime.now()))
                        .sorted(Comparator.comparing(StudentModality::getDefenseDate))
                        .collect(Collectors.toList())
        );

        // Modalidades en progreso (sin fecha de sustentación aún)
        List<InProgressDefenseDTO> inProgressDefenses = buildInProgressDefenses(
                allModalities.stream()
                        .filter(m -> m.getDefenseDate() == null)
                        .filter(m -> Arrays.asList(
                                ModalityProcessStatus.DEFENSE_SCHEDULED,
                                ModalityProcessStatus.EXAMINERS_ASSIGNED,
                                ModalityProcessStatus.READY_FOR_DEFENSE
                        ).contains(m.getStatus()))
                        .collect(Collectors.toList())
        );

        // Modalidades completadas recientes
        List<CompletedDefenseDTO> recentCompleted = includeCompleted != null && includeCompleted
                ? buildCompletedDefenses(
                allModalities.stream()
                        .filter(m -> m.getDefenseDate() != null)
                        .filter(m -> m.getDefenseDate().isBefore(LocalDateTime.now()))
                        .filter(m -> Arrays.asList(
                                ModalityProcessStatus.GRADED_APPROVED,
                                ModalityProcessStatus.GRADED_FAILED,
                                ModalityProcessStatus.EVALUATION_COMPLETED
                        ).contains(m.getStatus()))
                        .sorted(Comparator.comparing(StudentModality::getDefenseDate).reversed())
                        .limit(20)
                        .collect(Collectors.toList())
        )
                : Collections.emptyList();

        // Construir estadísticas
        DefenseStatisticsDTO statistics = buildStatistics(allModalities);

        // Análisis mensual
        List<MonthlyDefenseAnalysisDTO> monthlyAnalysis = buildMonthlyAnalysis(allModalities);

        // Análisis de jurados
        ExaminerAnalysisDTO examinerAnalysis = buildExaminerAnalysis(allModalities);

        // Alertas
        List<DefenseAlertDTO> alerts = buildAlerts(modalitiesInRange);

        // Resumen ejecutivo
        ExecutiveSummaryDTO executiveSummary = buildExecutiveSummary(
                upcomingDefenses,
                inProgressDefenses,
                recentCompleted,
                statistics
        );

        return DefenseCalendarReportDTO.builder()
                .generatedAt(LocalDateTime.now())
                .generatedBy(user.getName() + " " + user.getLastName())
                .academicProgramId(program.getId())
                .academicProgramName(program.getName())
                .academicProgramCode(program.getCode())
                .appliedFilters(AppliedFiltersDTO.builder()
                        .startDate(effectiveStartDate.toString())
                        .endDate(effectiveEndDate.toString())
                        .includeCompleted(includeCompleted)
                        .hasFilters(false)
                        .filterDescription(includeCompleted != null && includeCompleted ? "Incluye completadas" : "Solo próximas y en progreso")
                        .build())
                .executiveSummary(executiveSummary)
                .upcomingDefenses(upcomingDefenses)
                .inProgressDefenses(inProgressDefenses)
                .recentCompletedDefenses(recentCompleted)
                .statistics(statistics)
                .monthlyAnalysis(monthlyAnalysis)
                .examinerAnalysis(examinerAnalysis)
                .alerts(alerts)
                .metadata(ReportMetadataDTO.builder()
                        .totalRecords(allModalities.size())
                        .reportVersion("1.0")
                        .build())
                .build();
    }

    private List<UpcomingDefenseDTO> buildUpcomingDefenses(List<StudentModality> modalities) {
        return modalities.stream()
                .map(this::mapToUpcomingDefense)
                .collect(Collectors.toList());
    }

    private UpcomingDefenseDTO mapToUpcomingDefense(StudentModality modality) {
        List<DefenseExaminer> examiners = defenseExaminerRepository.findByStudentModalityId(modality.getId());
        List<StudentBasicInfoDTO> students = buildStudentList(modality);

        long daysUntil = ChronoUnit.DAYS.between(LocalDateTime.now(), modality.getDefenseDate());
        String urgency = daysUntil <= 3 ? "URGENT" : daysUntil <= 7 ? "SOON" : "NORMAL";

        List<String> pendingTasks = new ArrayList<>();
        int readyItems = 0;
        int totalItems = 5;

        if (examiners.isEmpty()) {
            pendingTasks.add("Asignar jurados");
        } else {
            readyItems++;
        }

        if (modality.getDefenseLocation() == null || modality.getDefenseLocation().isEmpty()) {
            pendingTasks.add("Definir ubicación");
        } else {
            readyItems++;
        }

        if (modality.getProjectDirector() == null) {
            pendingTasks.add("Asignar director");
        } else {
            readyItems++;
        }

        readyItems++; // Fecha ya programada
        readyItems++; // Modalidad activa

        String preparationStatus = pendingTasks.isEmpty() ? "READY" : pendingTasks.size() <= 2 ? "PENDING_CONFIRMATION" : "INCOMPLETE";

        return UpcomingDefenseDTO.builder()
                .modalityId(modality.getId())
                .modalityType(ReportUtils.translateSessionType(modality.getModalityType()))
                .modalityTypeName(modality.getProgramDegreeModality().getDegreeModality().getName())
                .defenseDate(modality.getDefenseDate())
                .defenseTime(modality.getDefenseDate().toLocalTime().toString())
                .defenseLocation(modality.getDefenseLocation())
                .daysUntilDefense((int) daysUntil)
                .urgency(urgency)
                .students(students)
                .leaderName(modality.getLeader().getName() + " " + modality.getLeader().getLastName())
                .studentCount(students.size())
                .directorName(modality.getProjectDirector() != null
                        ? modality.getProjectDirector().getName() + " " + modality.getProjectDirector().getLastName()
                        : "Sin asignar")
                .directorEmail(modality.getProjectDirector() != null ? modality.getProjectDirector().getEmail() : null)
                .examiners(examiners.stream().map(this::mapToExaminerInfo).collect(Collectors.toList()))
                .allExaminersConfirmed(true)
                .confirmedExaminers(examiners.size())
                .totalExaminers(examiners.size())
                .preparationStatus(preparationStatus)
                .pendingTasks(pendingTasks)
                .readinessPercentage((readyItems * 100.0) / totalItems)
                .projectTitle("Proyecto " + modality.getModalityType().name())
                .estimatedDuration(120)
                .modalityStatus(TranslationUtils.translateModalityProcessStatus(modality.getStatus()))
                .scheduledDate(modality.getDefenseDate())
                .reminderSent(false)
                .build();
    }

    private List<InProgressDefenseDTO> buildInProgressDefenses(List<StudentModality> modalities) {
        return modalities.stream()
                .map(this::mapToInProgressDefense)
                .collect(Collectors.toList());
    }

    private InProgressDefenseDTO mapToInProgressDefense(StudentModality modality) {
        List<StudentBasicInfoDTO> students = buildStudentList(modality);

        long daysInStatus = modality.getUpdatedAt() != null
                ? ChronoUnit.DAYS.between(modality.getUpdatedAt(), LocalDateTime.now())
                : 0;

        List<String> completedSteps = new ArrayList<>();
        List<String> pendingSteps = new ArrayList<>();

        completedSteps.add("Modalidad seleccionada");

        if (modality.getProjectDirector() != null) {
            completedSteps.add("Director asignado");
        } else {
            pendingSteps.add("Asignar director");
        }

        if (modality.getStatus() == ModalityProcessStatus.DEFENSE_SCHEDULED) {
            completedSteps.add("Sustentación en progreso");
            pendingSteps.add("Asignar jurados y ubicación");
        } else {
            pendingSteps.add("Programar sustentación");
        }

        double progress = (completedSteps.size() * 100.0) / (completedSteps.size() + pendingSteps.size());

        return InProgressDefenseDTO.builder()
                .modalityId(modality.getId())
                .modalityType(translateModalityType(modality.getModalityType().name()))
                .students(students)
                .directorName(modality.getProjectDirector() != null
                        ? modality.getProjectDirector().getName() + " " + modality.getProjectDirector().getLastName()
                        : "Sin asignar")
                .currentStatus(TranslationUtils.translateModalityProcessStatus(modality.getStatus()))
                .statusDate(modality.getUpdatedAt())
                .daysInCurrentStatus((int) daysInStatus)
                .nextAction(getNextAction(modality.getStatus()))
                .expectedDefenseDate(null)
                .progressPercentage(progress)
                .completedSteps(completedSteps)
                .pendingSteps(pendingSteps)
                .build();
    }

    private List<CompletedDefenseDTO> buildCompletedDefenses(List<StudentModality> modalities) {
        return modalities.stream()
                .map(this::mapToCompletedDefense)
                .collect(Collectors.toList());
    }

    private CompletedDefenseDTO mapToCompletedDefense(StudentModality modality) {
        List<DefenseExaminer> examiners = defenseExaminerRepository.findByStudentModalityId(modality.getId());
        List<StudentBasicInfoDTO> students = buildStudentList(modality);

        long daysAgo = modality.getDefenseDate() != null
                ? ChronoUnit.DAYS.between(modality.getDefenseDate(), LocalDateTime.now())
                : 0;

        String result = modality.getStatus() == ModalityProcessStatus.GRADED_APPROVED ? "APROBADO"
                : modality.getStatus() == ModalityProcessStatus.GRADED_FAILED ? "REPROBADO"
                : "EN EVALUACIÓN";

        // Recuperar evaluaciones
        List<DefenseEvaluationSummaryDTO> evaluationsSummary = new ArrayList<>();
        String proposedMention = "NONE";
        Boolean allEvaluationsCompleted = true;

        for (DefenseExaminer examiner : examiners) {
            Optional<com.SIGMA.USCO.Modalities.Entity.DefenseEvaluationCriteria> eval =
                defenseEvaluationCriteriaRepository.findByDefenseExaminerId(examiner.getId());
            
            if (eval.isPresent()) {
                com.SIGMA.USCO.Modalities.Entity.DefenseEvaluationCriteria evaluation = eval.get();
                evaluationsSummary.add(DefenseEvaluationSummaryDTO.builder()
                        .examinerId(examiner.getExaminer().getId())
                        .examinerName(examiner.getExaminer().getName() + " " + examiner.getExaminer().getLastName())
                        .examinerType(examiner.getExaminerType() != null ? examiner.getExaminerType().name() : "")
                        .grade(evaluation.getGrade())
                        .proposedMention(evaluation.getProposedMention() != null ? evaluation.getProposedMention().name() : "NONE")
                        .isFinalDecision(evaluation.getIsFinalDecision())
                        .rubricType(evaluation.getRubricType() != null ? evaluation.getRubricType().name() : "STANDARD")
                        .evaluatedAt(evaluation.getEvaluatedAt())
                        .build());
                
                // Obtener la mención propuesta (preferiblemente de decisión final)
                if (evaluation.getIsFinalDecision() && evaluation.getProposedMention() != null) {
                    proposedMention = evaluation.getProposedMention().name();
                }
            } else {
                allEvaluationsCompleted = false;
            }
        }
        
        String evaluationStatus = evaluationsSummary.isEmpty() ? "PENDING" 
            : allEvaluationsCompleted ? "COMPLETED" 
            : "IN_PROGRESS";

        return CompletedDefenseDTO.builder()
                .modalityId(modality.getId())
                .modalityType(translateModalityType(modality.getModalityType().name()))
                .defenseDate(modality.getDefenseDate())
                .students(students)
                .directorName(modality.getProjectDirector() != null
                        ? modality.getProjectDirector().getName() + " " + modality.getProjectDirector().getLastName()
                        : "Sin asignar")
                .result(result)
                .finalGrade(modality.getFinalGrade())
                .academicDistinction(modality.getAcademicDistinction() != null ? ModalityServiceUtils.translateAcademicDistinction(modality.getAcademicDistinction()) : null)
                .examiners(examiners.stream().map(this::mapToExaminerInfo).collect(Collectors.toList()))
                .defenseLocation(modality.getDefenseLocation())
                .daysAgo((int) daysAgo)
                .evaluationsSummary(evaluationsSummary)
                .proposedMention(proposedMention)
                .totalEvaluations(evaluationsSummary.size())
                .allEvaluationsCompleted(allEvaluationsCompleted)
                .evaluationStatus(evaluationStatus)
                .build();
    }

    private DefenseStatisticsDTO buildStatistics(List<StudentModality> modalities) {
        List<StudentModality> withDefenseDate = modalities.stream()
                .filter(m -> m.getDefenseDate() != null)
                .collect(Collectors.toList());

        List<StudentModality> completed = withDefenseDate.stream()
                .filter(m -> m.getDefenseDate().isBefore(LocalDateTime.now()))
                .filter(m -> Arrays.asList(
                        ModalityProcessStatus.GRADED_APPROVED,
                        ModalityProcessStatus.GRADED_FAILED,
                        ModalityProcessStatus.EVALUATION_COMPLETED
                ).contains(m.getStatus()))
                .collect(Collectors.toList());

        int approved = (int) completed.stream()
                .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED)
                .count();

        int rejected = (int) completed.stream()
                .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_FAILED)
                .count();

        double approvalRate = completed.isEmpty() ? 0.0 : (approved * 100.0) / completed.size();

        Map<String, Integer> byModalityType = modalities.stream()
                .collect(Collectors.groupingBy(
                        m -> translateModalityType(m.getModalityType().name()),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        int withMeritorious = (int) completed.stream()
                .filter(m -> m.getAcademicDistinction() != null)
                .filter(m -> m.getAcademicDistinction().name().contains("MERITORIOUS"))
                .count();

        int withLaureate = (int) completed.stream()
                .filter(m -> m.getAcademicDistinction() != null)
                .filter(m -> m.getAcademicDistinction().name().contains("LAUREATE"))
                .count();

        double distinctionRate = completed.isEmpty() ? 0.0
                : ((withMeritorious + withLaureate) * 100.0) / completed.size();

        double averageGrade = completed.stream()
                .filter(m -> m.getFinalGrade() != null)
                .mapToDouble(StudentModality::getFinalGrade)
                .average()
                .orElse(0.0);

        double highestGrade = completed.stream()
                .filter(m -> m.getFinalGrade() != null)
                .mapToDouble(StudentModality::getFinalGrade)
                .max()
                .orElse(0.0);

        double lowestGrade = completed.stream()
                .filter(m -> m.getFinalGrade() != null)
                .mapToDouble(StudentModality::getFinalGrade)
                .min()
                .orElse(0.0);

        int totalCancelled = (int) modalities.stream()
                .filter(m -> m.getStatus() == ModalityProcessStatus.MODALITY_CANCELLED ||
                           m.getStatus() == ModalityProcessStatus.CANCELLED_WITHOUT_REPROVAL ||
                           m.getStatus() == ModalityProcessStatus.CANCELLED_BY_CORRECTION_TIMEOUT)
                .count();

        double averageDaysToDefense = withDefenseDate.stream()
                .filter(m -> m.getSelectionDate() != null)
                .mapToLong(m -> ChronoUnit.DAYS.between(m.getSelectionDate(), m.getDefenseDate()))
                .average()
                .orElse(0.0);

        Map<String, Double> successRateByType = byModalityType.keySet().stream().collect(Collectors.toMap(
                type -> type,
                type -> {
                    List<StudentModality> typeModalities = completed.stream()
                            .filter(m -> translateModalityType(m.getModalityType().name()).equals(type))
                            .collect(Collectors.toList());
                    if (typeModalities.isEmpty()) return 0.0;
                    long ok = typeModalities.stream()
                            .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED)
                            .count();
                    return (ok * 100.0) / typeModalities.size();
                }
        ));

        Map<String, Integer> defensesByMonth = completed.stream()
                .collect(Collectors.groupingBy(
                        m -> {
                            LocalDate date = m.getDefenseDate().toLocalDate();
                            return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
                        },
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        return DefenseStatisticsDTO.builder()
                .totalScheduled(withDefenseDate.size())
                .totalCompleted(completed.size())
                .totalPending(modalities.size() - completed.size())
                .totalCancelled(totalCancelled)
                .approved(approved)
                .rejected(rejected)
                .withCorrections(null)
                .approvalRate(approvalRate)
                .averageDaysToDefense(Math.round(averageDaysToDefense * 100.0) / 100.0)
                .averageDefenseDuration(null)
                .byModalityType(byModalityType)
                .successRateByType(successRateByType)
                .defensesByMonth(defensesByMonth)
                .withMeritorious(withMeritorious)
                .withLaudeate(withLaureate)
                .distinctionRate(distinctionRate)
                .averageGrade(averageGrade)
                .highestGrade(highestGrade)
                .lowestGrade(lowestGrade)
                .trend(null)
                .growthRate(null)
                .build();
    }

    private List<MonthlyDefenseAnalysisDTO> buildMonthlyAnalysis(List<StudentModality> modalities) {
        Map<String, List<StudentModality>> byMonth = modalities.stream()
                .filter(m -> m.getDefenseDate() != null)
                .collect(Collectors.groupingBy(m -> {
                    LocalDate date = m.getDefenseDate().toLocalDate();
                    return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
                }));

        return byMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<StudentModality> monthModalities = entry.getValue();
                    int completed = (int) monthModalities.stream()
                            .filter(m -> m.getDefenseDate().isBefore(LocalDateTime.now()))
                            .count();

                    int approved = (int) monthModalities.stream()
                            .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED)
                            .count();

                    double successRate = completed == 0 ? 0.0 : (approved * 100.0) / completed;

                    double avgGrade = monthModalities.stream()
                            .filter(m -> m.getFinalGrade() != null)
                            .mapToDouble(StudentModality::getFinalGrade)
                            .average()
                            .orElse(0.0);

                    int totalStudents = monthModalities.stream()
                            .mapToInt(m -> m.getMembers() != null ? m.getMembers().size() + 1 : 1)
                            .sum();

                    String[] parts = entry.getKey().split("-");
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);

                    return MonthlyDefenseAnalysisDTO.builder()
                            .month(String.valueOf(month))
                            .year(year)
                            .periodLabel(getMonthName(month) + " " + year)
                            .totalScheduled(monthModalities.size())
                            .completed(completed)
                            .pending(monthModalities.size() - completed)
                            .approved(approved)
                            .successRate(successRate)
                            .averageGrade(avgGrade)
                            .totalStudents(totalStudents)
                            .peakDays(Collections.emptyList())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private ExaminerAnalysisDTO buildExaminerAnalysis(List<StudentModality> modalities) {
        List<DefenseExaminer> allExaminers = modalities.stream()
                .flatMap(m -> defenseExaminerRepository.findByStudentModalityId(m.getId()).stream())
                .collect(Collectors.toList());

        Set<Long> uniqueExaminers = allExaminers.stream()
                .map(e -> e.getExaminer().getId())
                .collect(Collectors.toSet());

        Map<String, Integer> examinersByType = allExaminers.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getExaminerType().name(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        double avgPerExaminer = uniqueExaminers.isEmpty() ? 0.0
                : (double) allExaminers.size() / uniqueExaminers.size();

        return ExaminerAnalysisDTO.builder()
                .totalExaminers(uniqueExaminers.size())
                .activeExaminers(uniqueExaminers.size())
                .topExaminers(Collections.emptyList())
                .examinersByType(examinersByType)
                .averageDefensesPerExaminer(avgPerExaminer)
                .mostActiveExaminer("N/A")
                .mostActiveExaminerCount(0)
                .build();
    }

    private List<DefenseAlertDTO> buildAlerts(List<StudentModality> modalities) {
        List<DefenseAlertDTO> alerts = new ArrayList<>();

        modalities.stream()
                .filter(m -> m.getDefenseDate() != null)
                .filter(m -> m.getDefenseDate().isAfter(LocalDateTime.now()))
                .filter(m -> ChronoUnit.DAYS.between(LocalDateTime.now(), m.getDefenseDate()) <= 7)
                .forEach(m -> {
                    List<DefenseExaminer> examiners = defenseExaminerRepository.findByStudentModalityId(m.getId());
                    if (examiners.isEmpty()) {
                        alerts.add(DefenseAlertDTO.builder()
                                .alertType("URGENT")
                                .title("Sustentación sin jurados")
                                .description("La sustentación está programada pero no tiene jurados asignados")
                                .modalityId(m.getId())
                                .studentName(m.getLeader().getName() + " " + m.getLeader().getLastName())
                                .defenseDate(m.getDefenseDate())
                                .priority(1)
                                .actionRequired("Asignar jurados inmediatamente")
                                .alertDate(LocalDateTime.now())
                                .build());
                    }

                    if (m.getDefenseLocation() == null || m.getDefenseLocation().isEmpty()) {
                        alerts.add(DefenseAlertDTO.builder()
                                .alertType("WARNING")
                                .title("Sustentación sin ubicación")
                                .description("La sustentación no tiene ubicación definida")
                                .modalityId(m.getId())
                                .studentName(m.getLeader().getName() + " " + m.getLeader().getLastName())
                                .defenseDate(m.getDefenseDate())
                                .priority(2)
                                .actionRequired("Definir ubicación de sustentación")
                                .alertDate(LocalDateTime.now())
                                .build());
                    }
                });

        return alerts;
    }

    private ExecutiveSummaryDTO buildExecutiveSummary(
            List<UpcomingDefenseDTO> upcoming,
            List<InProgressDefenseDTO> inProgress,
            List<CompletedDefenseDTO> completed,
            DefenseStatisticsDTO statistics
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfWeek = now.plusDays(7);
        LocalDateTime endOfMonth = now.plusMonths(1);

        int upcomingThisWeek = (int) upcoming.stream()
                .filter(d -> d.getDefenseDate().isBefore(endOfWeek))
                .count();

        int upcomingThisMonth = (int) upcoming.stream()
                .filter(d -> d.getDefenseDate().isBefore(endOfMonth))
                .count();

        int defensesToday = (int) upcoming.stream()
                .filter(d -> d.getDefenseDate().toLocalDate().equals(now.toLocalDate()))
                .count();

        String nextDefenseDate = upcoming.isEmpty() ? "Sin sustentaciones programadas"
                : upcoming.get(0).getDefenseDate().toLocalDate().toString();

        Map<String, Integer> quickStats = new HashMap<>();
        quickStats.put("Total Programadas", upcoming.size());
        quickStats.put("En Progreso", inProgress.size());
        quickStats.put("Completadas (mes)", completed.size());
        quickStats.put("Esta Semana", upcomingThisWeek);

        return ExecutiveSummaryDTO.builder()
                .totalScheduled(upcoming.size())
                .upcomingThisWeek(upcomingThisWeek)
                .upcomingThisMonth(upcomingThisMonth)
                .pendingScheduling(inProgress.size())
                .completedThisMonth(completed.size())
                .averageSuccessRate(statistics.getApprovalRate())
                .totalExaminersInvolved(statistics.getTotalScheduled())
                .nextDefenseDate(nextDefenseDate)
                .defensesToday(defensesToday)
                .overduePending(0)
                .quickStats(quickStats)
                .build();
    }

    // Métodos auxiliares

    private List<StudentBasicInfoDTO> buildStudentList(StudentModality modality) {
        List<StudentBasicInfoDTO> students = new ArrayList<>();

        User leader = modality.getLeader();
        students.add(StudentBasicInfoDTO.builder()
                .studentId(leader.getId())
                .studentCode("N/A")
                .fullName(leader.getName() + " " + leader.getLastName())
                .email(leader.getEmail())
                .phone(leader.getEmail())
                .isLeader(true)
                .build());

        if (modality.getMembers() != null) {
            modality.getMembers().forEach(member -> {
                User student = member.getStudent();
                students.add(StudentBasicInfoDTO.builder()
                        .studentId(student.getId())
                        .studentCode("N/A")
                        .fullName(student.getName() + " " + student.getLastName())
                        .email(student.getEmail())
                        .phone(student.getEmail())
                        .isLeader(false)
                        .build());
            });
        }

        return students;
    }

    private ExaminerInfoDTO mapToExaminerInfo(DefenseExaminer examiner) {
        User examinerUser = examiner.getExaminer();
        return ExaminerInfoDTO.builder()
                .examinerId(examiner.getId())
                .fullName(examinerUser.getName() + " " + examinerUser.getLastName())
                .email(examinerUser.getEmail())
                .examinerType(TranslationUtils.translateExaminerType(examiner.getExaminerType()))
                .assignmentDate(examiner.getAssignmentDate())
                .confirmed(true)
                .affiliation("Universidad Surcolombiana")
                .build();
    }

    private String translateModalityType(String type) {
        Map<String, String> translations = new HashMap<>();
        translations.put("PROYECTO_DE_GRADO", "Proyecto de Grado");
        translations.put("PRACTICA_PROFESIONAL", "Práctica Profesional");
        translations.put("PASANTIA", "Pasantía");
        translations.put("PRODUCCION_ACADEMICA_DE_ALTO_NIVEL", "Producción Académica");
        translations.put("SEMILLERO_DE_INVESTIGACION", "Semillero de Investigación");
        translations.put("PLAN_COMPLEMENTARIO_POSGRADO", "Plan Complementario");
        translations.put("PORTAFOLIO_PROFESIONAL", "Portafolio Profesional");
        translations.put("EMPRENDIMIENTO_Y_FORTALECIMIENTO_DE_EMPRESA", "Emprendimiento");
        translations.put("SEMINARIO_DE_GRADO", "Seminario de Grado");
        return translations.getOrDefault(type, type);
    }

    private String getNextAction(ModalityProcessStatus status) {
        Map<ModalityProcessStatus, String> actions = new HashMap<>();
        actions.put(ModalityProcessStatus.DEFENSE_SCHEDULED, "Asignar jurados y ubicación");
        actions.put(ModalityProcessStatus.EXAMINERS_ASSIGNED, "Confirmar ubicación y hora");
        actions.put(ModalityProcessStatus.READY_FOR_DEFENSE, "Realizar sustentación");
        actions.put(ModalityProcessStatus.DEFENSE_COMPLETED, "Esperar evaluación");
        return actions.getOrDefault(status, "Verificar estado");
    }

    private String getMonthName(int month) {
        String[] months = {"", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        return months[month];
    }
}





