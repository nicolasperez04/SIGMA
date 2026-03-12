package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.Modalities.Entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.DefenseExaminerRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.Entity.ProgramAuthority;
import com.SIGMA.USCO.Users.Entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.repository.AcademicProgramRepository;
import com.SIGMA.USCO.report.dto.DefenseCalendarReportDTO;
import com.SIGMA.USCO.report.dto.DefenseCalendarReportDTO.*;
import com.SIGMA.USCO.report.dto.ReportMetadataDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserRepository userRepository;
    private final AcademicProgramRepository academicProgramRepository;
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Obtener el programa académico del usuario autenticado
        Long programId = programAuthorityRepository
                .findByUser_IdAndRole(user.getId(), ProgramRole.PROGRAM_HEAD)
                .stream()
                .findFirst()
                .map(pa -> pa.getAcademicProgram().getId())
                .orElseGet(() -> programAuthorityRepository
                        .findByUser_Id(user.getId())
                        .stream()
                        .findFirst()
                        .map(pa -> pa.getAcademicProgram().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Usuario sin programa académico asignado")));

        AcademicProgram program = academicProgramRepository.findById(programId)
                .orElseThrow(() -> new IllegalArgumentException("Programa académico no encontrado"));

        // Si no se especifican fechas, usar un rango de 6 meses
        LocalDateTime effectiveStartDate = startDate != null ? startDate : LocalDateTime.now().minusMonths(3);
        LocalDateTime effectiveEndDate = endDate != null ? endDate : LocalDateTime.now().plusMonths(3);

        // Obtener todas las modalidades del programa
        List<StudentModality> allModalities = studentModalityRepository.findAll().stream()
                .filter(m -> m.getAcademicProgram().getId().equals(programId))
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
                .modalityType(translateSessionType(modality.getModalityType()))
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
                .modalityStatus(translateStatus(modality.getStatus()))
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
                .currentStatus(translateStatus(modality.getStatus()))
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
                .academicDistinction(modality.getAcademicDistinction() != null ? translateDistinction(modality.getAcademicDistinction()) : null)
                .examiners(examiners.stream().map(this::mapToExaminerInfo).collect(Collectors.toList()))
                .defenseLocation(modality.getDefenseLocation())
                .daysAgo((int) daysAgo)
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

        return DefenseStatisticsDTO.builder()
                .totalScheduled(withDefenseDate.size())
                .totalCompleted(completed.size())
                .totalPending(modalities.size() - completed.size())
                .totalCancelled(0)
                .approved(approved)
                .rejected(rejected)
                .withCorrections(0)
                .approvalRate(approvalRate)
                .averageDaysToDefense(0.0)
                .averageDefenseDuration(120.0)
                .byModalityType(byModalityType)
                .successRateByType(new HashMap<>())
                .defensesByMonth(new HashMap<>())
                .withMeritorious(withMeritorious)
                .withLaudeate(withLaureate)
                .distinctionRate(distinctionRate)
                .averageGrade(averageGrade)
                .highestGrade(highestGrade)
                .lowestGrade(lowestGrade)
                .trend("STABLE")
                .growthRate(0.0)
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
                .examinerType(translateExaminerType(examiner.getExaminerType()))
                .assignmentDate(examiner.getAssignmentDate())
                .confirmed(true)
                .affiliation("Universidad Surcolombiana")
                .build();
    }

    private String translateExaminerType(com.SIGMA.USCO.Modalities.Entity.enums.ExaminerType type) {
        if (type == null) return "Jurado";
        return switch (type) {
            case PRIMARY_EXAMINER_1 -> "Jurado Principal 1";
            case PRIMARY_EXAMINER_2 -> "Jurado Principal 2";
            case TIEBREAKER_EXAMINER -> "Jurado de Desempate";
        };
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

    private String translateStatus(ModalityProcessStatus status) {
        if (status == null) return "Sin estado";
        return switch (status) {
            case MODALITY_SELECTED -> "Modalidad seleccionada";
            case UNDER_REVIEW_PROGRAM_HEAD -> "En revisión por Jefatura";
            case CORRECTIONS_REQUESTED_PROGRAM_HEAD -> "Correcciones solicitadas por Jefatura";
            case CORRECTIONS_SUBMITTED -> "Correcciones enviadas";
            case CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD -> "Correcciones enviadas a Jefatura";
            case CORRECTIONS_SUBMITTED_TO_COMMITTEE -> "Correcciones enviadas al Comité";
            case CORRECTIONS_SUBMITTED_TO_EXAMINERS -> "Correcciones enviadas a los Jurados";
            case CORRECTIONS_APPROVED -> "Correcciones aprobadas";
            case CORRECTIONS_REJECTED_FINAL -> "Rechazado definitivamente";
            case READY_FOR_PROGRAM_CURRICULUM_COMMITTEE -> "Lista para Comité de Currículo";
            case UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE -> "En revisión por Comité";
            case CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE -> "Correcciones solicitadas por Comité";
            case READY_FOR_DIRECTOR_ASSIGNMENT -> "Lista para asignar Director";
            case READY_FOR_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE -> "Lista para aprobación por Comité";
            case APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE -> "Aprobado por Comité de Currículo";
            case PROPOSAL_APPROVED -> "Propuesta aprobada";
            case PENDING_PROGRAM_HEAD_FINAL_REVIEW -> "Pendiente revisión final por Jefatura";
            case APPROVED_BY_PROGRAM_HEAD_FINAL_REVIEW -> "Aprobado por Jefatura - Notificando Jurados";
            case DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR -> "Sustentación propuesta por Director";
            case DEFENSE_SCHEDULED -> "Sustentación programada";
            case EXAMINERS_ASSIGNED -> "Jurados asignados";
            case READY_FOR_EXAMINERS -> "Lista para revisión de jurados";
            case DOCUMENTS_APPROVED_BY_EXAMINERS -> "Documentos aprobados por jurados";
            case SECONDARY_DOCUMENTS_APPROVED_BY_EXAMINERS -> "Documentos finales aprobados";
            case DOCUMENT_REVIEW_TIEBREAKER_REQUIRED -> "Requiere jurado de desempate";
            case EDIT_REQUESTED_BY_STUDENT -> "Edición solicitada por el estudiante";
            case CORRECTIONS_REQUESTED_EXAMINERS -> "Correcciones solicitadas por jurados";
            case READY_FOR_DEFENSE -> "Lista para sustentación";
            case FINAL_REVIEW_COMPLETED -> "Revisión final completada";
            case DEFENSE_COMPLETED -> "Sustentación completada";
            case UNDER_EVALUATION_PRIMARY_EXAMINERS -> "En evaluación por jurados principales";
            case DISAGREEMENT_REQUIRES_TIEBREAKER -> "Desacuerdo – Requiere jurado de desempate";
            case UNDER_EVALUATION_TIEBREAKER -> "En evaluación por jurado de desempate";
            case EVALUATION_COMPLETED -> "Evaluación completada";
            case PENDING_DISTINCTION_COMMITTEE_REVIEW -> "Aprobado – Distinción honorífica pendiente del comité";
            case GRADED_APPROVED -> "Aprobado";
            case GRADED_FAILED -> "Reprobado";
            case MODALITY_CLOSED -> "Modalidad cerrada";
            case SEMINAR_CANCELED -> "Seminario cancelado";
            case MODALITY_CANCELLED -> "Modalidad cancelada";
            case CANCELLATION_REQUESTED -> "Cancelación solicitada";
            case CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR -> "Cancelación aprobada por Director";
            case CANCELLATION_REJECTED_BY_PROJECT_DIRECTOR -> "Cancelación rechazada por Director";
            case CANCELLED_WITHOUT_REPROVAL -> "Cancelada sin reprobación";
            case CANCELLATION_REJECTED -> "Cancelación rechazada";
            case CANCELLED_BY_CORRECTION_TIMEOUT -> "Cancelada por vencimiento de plazo";
        };
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

    private String translateSessionType(com.SIGMA.USCO.Modalities.Entity.enums.ModalityType type) {
        if (type == null) return "Individual";
        return switch (type) {
            case INDIVIDUAL -> "Individual";
            case GROUP -> "Grupal";
        };
    }

    private String translateDistinction(com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction distinction) {
        if (distinction == null) return null;
        return switch (distinction) {
            case NO_DISTINCTION -> "Sin distinción";
            case AGREED_APPROVED -> "Aprobado por consenso";
            case AGREED_MERITORIOUS -> "Mención Meritoria";
            case AGREED_LAUREATE -> "Mención Laureada";
            case AGREED_REJECTED -> "Reprobado por consenso";
            case DISAGREEMENT_PENDING_TIEBREAKER -> "Desacuerdo – Pendiente de desempate";
            case TIEBREAKER_APPROVED -> "Aprobado por desempate";
            case TIEBREAKER_MERITORIOUS -> "Mención Meritoria (desempate)";
            case TIEBREAKER_LAUREATE -> "Mención Laureada (desempate)";
            case TIEBREAKER_REJECTED -> "Reprobado por desempate";
            case REJECTED_BY_COMMITTEE -> "Rechazado por el comité";
            case PENDING_COMMITTEE_MERITORIOUS -> "Mención Meritoria propuesta (pendiente del comité)";
            case PENDING_COMMITTEE_LAUREATE -> "Mención Laureada propuesta (pendiente del comité)";
            case TIEBREAKER_PENDING_COMMITTEE_MERITORIOUS -> "Mención Meritoria por desempate (pendiente del comité)";
            case TIEBREAKER_PENDING_COMMITTEE_LAUREATE -> "Mención Laureada por desempate (pendiente del comité)";
        };
    }
}





