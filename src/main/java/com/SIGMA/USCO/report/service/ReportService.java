package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.Entity.ProgramAuthority;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.Entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.AcademicProgramRepository;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.report.dto.DirectorInfoDTO;
import com.SIGMA.USCO.report.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ReportService {

    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AcademicProgramRepository academicProgramRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;


    @Transactional(readOnly = true)
    public GlobalModalityReportDTO generateGlobalReport() {
        long startTime = System.currentTimeMillis();

        // Obtener usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth != null ? auth.getName() : "SYSTEM";

        // Obtener el programa académico del usuario autenticado
        User authenticatedUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Buscar autoridad de programa del usuario (puede ser PROGRAM_HEAD, PROGRAM_COMMITTEE, etc.)
        List<ProgramAuthority> programAuthorities = programAuthorityRepository.findByUser_Id(authenticatedUser.getId());

        if (programAuthorities.isEmpty()) {
            throw new IllegalArgumentException("El usuario no tiene asignado ningún programa académico");
        }

        // Obtener programa académico del usuario
        AcademicProgram userProgram = programAuthorities.get(0).getAcademicProgram();
        Long userProgramId = userProgram.getId();
        String programName = userProgram.getName();
        String programCode = userProgram.getCode();

        // Obtener modalidades activas filtradas por el programa del usuario
        List<StudentModality> activeModalities = studentModalityRepository.findByStatusIn(ReportUtils.getActiveStatuses())
                .stream()
                .filter(modality -> modality.getAcademicProgram().getId().equals(userProgramId))
                .collect(java.util.stream.Collectors.toList());

        // Generar resumen ejecutivo
        ExecutiveSummaryDTO executiveSummary = generateExecutiveSummary(activeModalities);

        // Generar detalles de modalidades
        List<ModalityDetailReportDTO> modalityDetails = generateModalityDetails(activeModalities);

        // Generar estadísticas del programa
        List<ProgramStatisticsDTO> programStatistics = generateProgramStatistics(activeModalities);

        // Calcular tiempo de generación
        long endTime = System.currentTimeMillis();
        long generationTime = endTime - startTime;

        // Construir metadata del reporte
        ReportMetadataDTO metadata = ReportMetadataDTO.builder()
                .reportVersion("1.0")
                .reportType("GLOBAL_ACTIVE_MODALITIES")
                .generatedBySystem("SIGMA - Sistema de Gestión de Modalidades de Grado")
                .totalRecords(modalityDetails.size())
                .generationTimeMs(generationTime)
                .exportFormat("JSON")
                .build();

        // Construir y retornar el reporte
        return GlobalModalityReportDTO.builder()
                .generatedAt(LocalDateTime.now())
                .generatedBy(userEmail + " (" + programName + ")")
                .academicProgramId(userProgramId)
                .academicProgramName(programName)
                .academicProgramCode(programCode)
                .executiveSummary(executiveSummary)
                .modalities(modalityDetails)
                .programStatistics(programStatistics)
                .metadata(metadata)
                .build();
    }


    private ExecutiveSummaryDTO generateExecutiveSummary(List<StudentModality> activeModalities) {

        int totalActiveModalities = activeModalities.size();


        Set<Long> uniqueStudents = new HashSet<>();
        for (StudentModality modality : activeModalities) {
            List<StudentModalityMember> members = studentModalityMemberRepository
                    .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
            members.forEach(member -> uniqueStudents.add(member.getStudent().getId()));
        }
        int totalActiveStudents = uniqueStudents.size();


        Set<Long> uniqueDirectors = activeModalities.stream()
                .filter(m -> m.getProjectDirector() != null)
                .map(m -> m.getProjectDirector().getId())
                .collect(Collectors.toSet());
        int totalActiveDirectors = uniqueDirectors.size();


        Map<String, Long> modalitiesByStatus = activeModalities.stream()
                .collect(Collectors.groupingBy(
                        m -> ReportUtils.describeModalityStatus(m.getStatus()),
                        Collectors.counting()
                ));


        Map<String, Long> modalitiesByType = activeModalities.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getProgramDegreeModality().getDegreeModality().getName(),
                        Collectors.counting()
                ));


        long individualCount = activeModalities.stream()
                .filter(m -> m.getModalityType() == com.SIGMA.USCO.Modalities.Entity.enums.ModalityType.INDIVIDUAL)
                .count();
        long groupCount = activeModalities.stream()
                .filter(m -> m.getModalityType() == com.SIGMA.USCO.Modalities.Entity.enums.ModalityType.GROUP)
                .count();


        double avgStudentsPerGroup = 0.0;
        if (groupCount > 0) {
            long totalStudentsInGroups = activeModalities.stream()
                    .filter(m -> m.getModalityType() == com.SIGMA.USCO.Modalities.Entity.enums.ModalityType.GROUP)
                    .mapToLong(m -> studentModalityMemberRepository
                            .countByStudentModalityIdAndStatus(m.getId(), MemberStatus.ACTIVE))
                    .sum();
            avgStudentsPerGroup = (double) totalStudentsInGroups / groupCount;
        }


        long modalitiesWithoutDirector = activeModalities.stream()
                .filter(m -> m.getProjectDirector() == null)
                .count();


        long modalitiesInReview = activeModalities.stream()
                .filter(m -> m.getStatus() == ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD ||
                           m.getStatus() == ModalityProcessStatus.UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE)
                .count();


        long advancedStates = activeModalities.stream()
                .filter(m -> ReportUtils.isAdvancedStatus(m.getStatus()))
                .count();
        double overallProgressRate = totalActiveModalities > 0
                ? (advancedStates * 100.0) / totalActiveModalities
                : 0.0;

        return ExecutiveSummaryDTO.builder()
                .totalActiveModalities(totalActiveModalities)
                .totalActiveStudents(totalActiveStudents)
                .totalActiveDirectors(totalActiveDirectors)
                .modalitiesByStatus(modalitiesByStatus)
                .modalitiesByType(modalitiesByType)
                .individualModalities((int) individualCount)
                .groupModalities((int) groupCount)
                .averageStudentsPerGroup(Math.round(avgStudentsPerGroup * 100.0) / 100.0)
                .modalitiesWithoutDirector((int) modalitiesWithoutDirector)
                .modalitiesInReview((int) modalitiesInReview)
                .overallProgressRate(Math.round(overallProgressRate * 100.0) / 100.0)
                .build();
    }


    private List<ModalityDetailReportDTO> generateModalityDetails(List<StudentModality> activeModalities) {
        return activeModalities.stream()
                .map(this::buildModalityDetail)
                .sorted(Comparator.comparing(ModalityDetailReportDTO::getLastUpdate).reversed())
                .collect(Collectors.toList());
    }


    private ModalityDetailReportDTO buildModalityDetail(StudentModality modality) {
        // Obtener información de estudiantes
        List<StudentModalityMember> members = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);

        List<StudentInfoDTO> students = members.stream()
                .map(member -> {
                    User student = member.getStudent();
                    StudentProfile profile = studentProfileRepository.findByUserId(student.getId()).orElse(null);

                    return StudentInfoDTO.builder()
                            .studentId(student.getId())
                            .fullName(student.getName() + " " + student.getLastName())
                            .studentCode(profile != null ? profile.getStudentCode() : "N/A")
                            .email(student.getEmail())
                            .semester(profile != null ? profile.getSemester() : null)
                            .gpa(profile != null ? profile.getGpa() : null)
                            .isLeader(member.getIsLeader())
                            .build();
                })
                .collect(Collectors.toList());


        DirectorInfoDTO director = null;
        if (modality.getProjectDirector() != null) {
            User directorUser = modality.getProjectDirector();
            long directorActiveProjects = studentModalityRepository
                    .countActiveModalitiesByLeader(directorUser.getId(), ReportUtils.getActiveStatuses());

            director = DirectorInfoDTO.builder()
                    .directorId(directorUser.getId())
                    .fullName(directorUser.getName() + " " + directorUser.getLastName())
                    .email(directorUser.getEmail())
                    .activeProjectsCount((int) directorActiveProjects)
                    .build();
        }


        long daysSinceStart = modality.getSelectionDate() != null
                ? ChronoUnit.DAYS.between(modality.getSelectionDate(), LocalDateTime.now())
                : 0;


        long daysInCurrentStatus = modality.getUpdatedAt() != null
                ? ChronoUnit.DAYS.between(modality.getUpdatedAt(), LocalDateTime.now())
                : 0;


        boolean hasPendingActions = ReportUtils.isPendingStatus(modality.getStatus());


        String observations = generateObservations(modality, daysInCurrentStatus);

        return ModalityDetailReportDTO.builder()
                .studentModalityId(modality.getId())
                .modalityName(modality.getProgramDegreeModality().getDegreeModality().getName())
                .modalityType(translateSessionType(modality.getModalityType()))
                .academicProgram(modality.getAcademicProgram().getName())
                .currentStatus(translateModalityProcessStatus(modality.getStatus()))
                .statusDescription(ReportUtils.describeModalityStatus(modality.getStatus()))
                .students(students)
                .director(director)
                .startDate(modality.getSelectionDate())
                .lastUpdate(modality.getUpdatedAt())
                .daysSinceStart(daysSinceStart)
                .daysInCurrentStatus(daysInCurrentStatus)
                .hasPendingActions(hasPendingActions)
                .observations(observations)
                .build();
    }


    private List<ProgramStatisticsDTO> generateProgramStatistics(List<StudentModality> activeModalities) {

        Map<Long, List<StudentModality>> modalitiesByProgram = activeModalities.stream()
                .collect(Collectors.groupingBy(m -> m.getAcademicProgram().getId()));

        return modalitiesByProgram.entrySet().stream()
                .map(entry -> {
                    Long programId = entry.getKey();
                    List<StudentModality> programModalities = entry.getValue();

                    AcademicProgram program = programModalities.get(0).getAcademicProgram();


                    Set<Long> uniqueStudents = new HashSet<>();
                    for (StudentModality modality : programModalities) {
                        List<StudentModalityMember> members = studentModalityMemberRepository
                                .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
                        members.forEach(member -> uniqueStudents.add(member.getStudent().getId()));
                    }


                    Map<String, Long> modalityDistribution = programModalities.stream()
                            .collect(Collectors.groupingBy(
                                    m -> m.getProgramDegreeModality().getDegreeModality().getName(),
                                    Collectors.counting()
                            ));


                    Map<String, Long> statusDistribution = programModalities.stream()
                            .collect(Collectors.groupingBy(
                                    m -> ReportUtils.describeModalityStatus(m.getStatus()),
                                    Collectors.counting()
                            ));


                    double avgDaysInProcess = programModalities.stream()
                            .filter(m -> m.getSelectionDate() != null)
                            .mapToLong(m -> ChronoUnit.DAYS.between(m.getSelectionDate(), LocalDateTime.now()))
                            .average()
                            .orElse(0.0);

                    return ProgramStatisticsDTO.builder()
                            .programId(programId)
                            .programName(program.getName())
                            .programCode(program.getCode())
                            .totalActiveModalities(programModalities.size())
                            .totalActiveStudents(uniqueStudents.size())
                            .modalityDistribution(modalityDistribution)
                            .statusDistribution(statusDistribution)
                            .averageDaysInProcess(Math.round(avgDaysInProcess * 100.0) / 100.0)
                            .facultyName(program.getFaculty().getName())
                            .build();
                })
                .sorted(Comparator.comparing(ProgramStatisticsDTO::getTotalActiveModalities).reversed())
                .collect(Collectors.toList());
    }


    private String generateObservations(StudentModality modality, long daysInCurrentStatus) {
        List<String> observations = new ArrayList<>();


        if (modality.getProjectDirector() == null) {
            observations.add("⚠️ Sin director asignado");
        }


        if (daysInCurrentStatus > 30) {
            observations.add("⏰ Más de 30 días sin actualización");
        } else if (daysInCurrentStatus > 15) {
            observations.add("⏱️ Más de 15 días sin actualización");
        }


        if (modality.getCorrectionAttempts() != null && modality.getCorrectionAttempts() > 0) {
            observations.add(String.format("📝 %d intento(s) de corrección", modality.getCorrectionAttempts()));
        }


        if (modality.getCorrectionDeadline() != null) {
            long daysUntilDeadline = ChronoUnit.DAYS.between(LocalDateTime.now(), modality.getCorrectionDeadline());
            if (daysUntilDeadline < 0) {
                observations.add("🚨 Plazo de corrección vencido");
            } else if (daysUntilDeadline <= 3) {
                observations.add(String.format("⚠️ Quedan %d días para corregir", daysUntilDeadline));
            }
        }


        if (modality.getStatus() == ModalityProcessStatus.READY_FOR_PROGRAM_CURRICULUM_COMMITTEE) {
            observations.add("✅ Listo para revisión del comité");
        } else if (modality.getStatus() == ModalityProcessStatus.DEFENSE_SCHEDULED) {
            observations.add("🎓 Sustentación programada");
        }

        return observations.isEmpty() ? "Sin observaciones" : String.join(" | ", observations);
    }

    // ==================== RF-46: FILTRADO POR TIPO DE MODALIDAD ====================

    /**
     * Genera un reporte global de modalidades filtrado por tipo de modalidad
     * RF-46 - Filtrado por Tipo de Modalidad
     *
     * NOTA: Este método incluye modalidades en TODOS los estados (activas, completadas, canceladas, etc.)
     *
     * @param filters Filtros a aplicar (IDs o nombres de modalidades)
     * @return Reporte filtrado por tipo de modalidad
     */
    @Transactional(readOnly = true)
    public GlobalModalityReportDTO generateFilteredReport(ModalityReportFilterDTO filters) {
        long startTime = System.currentTimeMillis();

        // Obtener usuario autenticado y su programa
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth != null ? auth.getName() : "SYSTEM";

        User authenticatedUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<ProgramAuthority> programAuthorities = programAuthorityRepository.findByUser_Id(authenticatedUser.getId());

        if (programAuthorities.isEmpty()) {
            throw new IllegalArgumentException("El usuario no tiene asignado ningún programa académico");
        }

        AcademicProgram userProgram = programAuthorities.get(0).getAcademicProgram();
        Long userProgramId = userProgram.getId();
        String programName = userProgram.getName();
        String programCode = userProgram.getCode();

        // Obtener TODAS las modalidades del programa (en cualquier estado)
        List<StudentModality> allModalities = studentModalityRepository.findAll()
                .stream()
                .filter(modality -> modality.getAcademicProgram().getId().equals(userProgramId))
                .collect(Collectors.toList());

        // Aplicar filtros
        List<StudentModality> filteredModalities = applyFilters(allModalities, filters);

        // Generar componentes del reporte
        ExecutiveSummaryDTO executiveSummary = generateExecutiveSummary(filteredModalities);
        List<ModalityDetailReportDTO> modalityDetails = generateModalityDetails(filteredModalities);
        List<ProgramStatisticsDTO> programStatistics = generateProgramStatistics(filteredModalities);

        long endTime = System.currentTimeMillis();
        long generationTime = endTime - startTime;

        // Construir metadata con información de filtros aplicados
        ReportMetadataDTO metadata = ReportMetadataDTO.builder()
                .reportVersion("1.0")
                .reportType("FILTERED_ACTIVE_MODALITIES")
                .generatedBySystem("SIGMA - Sistema de Gestión de Modalidades de Grado")
                .totalRecords(modalityDetails.size())
                .generationTimeMs(generationTime)
                .exportFormat("JSON")
                .build();

        return GlobalModalityReportDTO.builder()
                .generatedAt(LocalDateTime.now())
                .generatedBy(userEmail + " (" + programName + ")")
                .academicProgramId(userProgramId)
                .academicProgramName(programName)
                .academicProgramCode(programCode)
                .executiveSummary(executiveSummary)
                .modalities(modalityDetails)
                .programStatistics(programStatistics)
                .metadata(metadata)
                .build();
    }

    /**
     * Obtiene los tipos de modalidad disponibles para el programa del usuario autenticado
     * RF-46 - Filtrado por Tipo de Modalidad
     *
     * NOTA: Este método considera modalidades en TODOS los estados para mostrar los tipos disponibles
     *
     * @return Lista de tipos de modalidad disponibles con información de conteo
     */
    @Transactional(readOnly = true)
    public AvailableModalityTypesDTO getAvailableModalityTypes() {
        // Obtener usuario autenticado y su programa
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth != null ? auth.getName() : "SYSTEM";

        User authenticatedUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<ProgramAuthority> programAuthorities = programAuthorityRepository.findByUser_Id(authenticatedUser.getId());

        if (programAuthorities.isEmpty()) {
            throw new IllegalArgumentException("El usuario no tiene asignado ningún programa académico");
        }

        AcademicProgram userProgram = programAuthorities.get(0).getAcademicProgram();

        // Obtener TODAS las modalidades del programa (en cualquier estado)
        List<StudentModality> allModalities = studentModalityRepository.findAll()
                .stream()
                .filter(modality -> modality.getAcademicProgram().getId().equals(userProgram.getId()))
                .collect(Collectors.toList());

        // Agrupar por tipo de modalidad
        Map<Long, List<StudentModality>> modalitiesByType = allModalities.stream()
                .collect(Collectors.groupingBy(m -> m.getProgramDegreeModality().getDegreeModality().getId()));

        // Construir información de cada tipo
        List<AvailableModalityTypesDTO.ModalityTypeInfo> typeInfoList = modalitiesByType.entrySet().stream()
                .map(entry -> {
                    List<StudentModality> modalities = entry.getValue();
                    if (modalities.isEmpty()) return null;

                    var firstModality = modalities.get(0);
                    var degreeModality = firstModality.getProgramDegreeModality().getDegreeModality();

                    return AvailableModalityTypesDTO.ModalityTypeInfo.builder()
                            .id(degreeModality.getId())
                            .name(degreeModality.getName())
                            .description(degreeModality.getDescription())
                            .activeModalitiesCount(modalities.size()) // Total de modalidades en cualquier estado
                            .requiresDirector(isDirectorRequired(degreeModality.getName()))
                            .status(degreeModality.getStatus() != null ? degreeModality.getStatus().name() : "ACTIVE")
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(AvailableModalityTypesDTO.ModalityTypeInfo::getName))
                .collect(Collectors.toList());

        return AvailableModalityTypesDTO.builder()
                .availableTypes(typeInfoList)
                .academicProgramId(userProgram.getId())
                .academicProgramName(userProgram.getName())
                .totalTypes(typeInfoList.size())
                .build();
    }

    /**
     * Aplica los filtros a la lista de modalidades
     *
     * @param modalities Lista de modalidades a filtrar
     * @param filters Filtros a aplicar
     * @return Lista de modalidades filtradas
     */
    private List<StudentModality> applyFilters(List<StudentModality> modalities, ModalityReportFilterDTO filters) {
        if (filters == null) {
            return modalities;
        }

        return modalities.stream()
                .filter(modality -> {
                    boolean matches = true;

                    // Filtrar por IDs de tipo de modalidad
                    if (filters.getDegreeModalityIds() != null && !filters.getDegreeModalityIds().isEmpty()) {
                        Long modalityId = modality.getProgramDegreeModality().getDegreeModality().getId();
                        matches = filters.getDegreeModalityIds().contains(modalityId);
                    }

                    // Filtrar por nombres de tipo de modalidad
                    if (matches && filters.getDegreeModalityNames() != null && !filters.getDegreeModalityNames().isEmpty()) {
                        String modalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
                        matches = filters.getDegreeModalityNames().stream()
                                .anyMatch(name -> modalityName.toUpperCase().contains(name.toUpperCase()));
                    }

                    // Filtrar por estados de proceso
                    if (matches && filters.getProcessStatuses() != null && !filters.getProcessStatuses().isEmpty()) {
                        matches = filters.getProcessStatuses().contains(modality.getStatus().name());
                    }

                    // Filtrar por director asignado
                    if (matches && filters.getOnlyWithDirector() != null && filters.getOnlyWithDirector()) {
                        matches = modality.getProjectDirector() != null;
                    }

                    if (matches && filters.getIncludeWithoutDirector() != null && !filters.getIncludeWithoutDirector()) {
                        matches = modality.getProjectDirector() != null;
                    }

                    return matches;
                })
                .collect(Collectors.toList());
    }

    /**
     * Determina si un tipo de modalidad requiere director
     *
     * @param modalityName Nombre de la modalidad
     * @return true si requiere director, false en caso contrario
     */
    private boolean isDirectorRequired(String modalityName) {
        if (modalityName == null) {
            return true;
        }

        String normalizedName = modalityName.toUpperCase().trim();

        // Modalidades que NO requieren director
        return !(normalizedName.contains("PLAN COMPLEMENTARIO") ||
                normalizedName.contains("PRODUCCIÓN ACADEMICA") ||
                normalizedName.contains("PRODUCCION ACADEMICA") ||
                normalizedName.contains("SEMINARIO"));
    }

    // ==================== RF-48: COMPARATIVA DE MODALIDADES POR TIPO ====================

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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth != null ? auth.getName() : "SYSTEM";

        User authenticatedUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<ProgramAuthority> programAuthorities = programAuthorityRepository.findByUser_Id(authenticatedUser.getId());

        if (programAuthorities.isEmpty()) {
            throw new IllegalArgumentException("El usuario no tiene asignado ningún programa académico");
        }

        AcademicProgram userProgram = programAuthorities.get(0).getAcademicProgram();

        // Obtener modalidades según filtros
        List<StudentModality> modalities = getModalitiesForComparison(userProgram.getId(), filters);

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
            historicalComparison = generateHistoricalComparison(userProgram.getId(), periodsCount, filters);
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
    private List<StudentModality> getModalitiesForComparison(Long programId, ModalityComparisonFilterDTO filters) {
        List<StudentModality> allModalities;

        // Filtrar por activas o todas
        if (filters != null && Boolean.TRUE.equals(filters.getOnlyActiveModalities())) {
            allModalities = studentModalityRepository.findByStatusIn(ReportUtils.getActiveStatuses());
        } else {
            allModalities = studentModalityRepository.findAll();
        }

        // Filtrar por programa
        allModalities = allModalities.stream()
                .filter(m -> m.getAcademicProgram().getId().equals(programId))
                .collect(Collectors.toList());

        // Filtrar por año y semestre si se especificó
        if (filters != null && filters.getYear() != null) {
            allModalities = allModalities.stream()
                    .filter(m -> m.getSelectionDate() != null &&
                                 m.getSelectionDate().getYear() == filters.getYear())
                    .collect(Collectors.toList());

            if (filters.getSemester() != null) {
                allModalities = allModalities.stream()
                        .filter(m -> getSemesterFromDate(m.getSelectionDate()) == filters.getSemester())
                        .collect(Collectors.toList());
            }
        }

        return allModalities;
    }

    /**
     * Determina el semestre (1 o 2) basado en el mes
     */
    private int getSemesterFromDate(LocalDateTime date) {
        if (date == null) return 1;
        int month = date.getMonthValue();
        return month <= 6 ? 1 : 2; // Enero-Junio = Semestre 1, Julio-Diciembre = Semestre 2
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
        Set<Long> uniqueStudents = new HashSet<>();
        for (StudentModality modality : modalities) {
            List<StudentModalityMember> members = studentModalityMemberRepository
                    .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
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

        return byTypeId.entrySet().stream()
                .map(entry -> {
                    List<StudentModality> typeModalities = entry.getValue();
                    if (typeModalities.isEmpty()) return null;

                    var firstModality = typeModalities.get(0);
                    var degreeModality = firstModality.getProgramDegreeModality().getDegreeModality();

                    // Contar estudiantes únicos de este tipo
                    Set<Long> students = new HashSet<>();
                    for (StudentModality modality : typeModalities) {
                        List<StudentModalityMember> members = studentModalityMemberRepository
                                .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
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
                                    m -> ReportUtils.describeModalityStatus(m.getStatus()),
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
                            .requiresDirector(isDirectorRequired(degreeModality.getName()))
                            .modalitiesWithDirector((int) withDirector)
                            .modalitiesWithoutDirector((int) withoutDirector)
                            .distributionByStatus(statusDistribution)
                            .trend("STABLE") // Por defecto, se calcula en análisis de tendencias
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

        for (StudentModality modality : modalities) {
            String typeName = modality.getProgramDegreeModality().getDegreeModality().getName();
            List<StudentModalityMember> members = studentModalityMemberRepository
                    .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);

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
            Long programId, int periodsCount, ModalityComparisonFilterDTO baseFilters) {

        List<ModalityTypeComparisonReportDTO.PeriodComparisonDTO> periods = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();
        int currentSemester = getSemesterFromDate(now);

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

            List<StudentModality> periodModalities = getModalitiesForComparison(programId, periodFilter);

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
                List<StudentModalityMember> members = studentModalityMemberRepository
                        .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);

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

    // ==================== RF-49: REPORTES POR DIRECTOR ASIGNADO ====================

    /**
     * Genera un reporte de modalidades por director asignado
     * RF-49 - Generación de Reportes por Director Asignado
     *
     * @param filters Filtros para el reporte (director específico, estados, etc.)
     * @return Reporte completo de directores con sus modalidades asignadas
     */
    @Transactional(readOnly = true)
    public DirectorAssignedModalitiesReportDTO generateDirectorAssignedModalitiesReport(DirectorReportFilterDTO filters) {
        long startTime = System.currentTimeMillis();

        // Obtener usuario autenticado y su programa
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth != null ? auth.getName() : "SYSTEM";

        User authenticatedUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<ProgramAuthority> programAuthorities = programAuthorityRepository.findByUser_Id(authenticatedUser.getId());

        if (programAuthorities.isEmpty()) {
            throw new IllegalArgumentException("El usuario no tiene asignado ningún programa académico");
        }

        AcademicProgram userProgram = programAuthorities.get(0).getAcademicProgram();

        // Obtener modalidades del programa
        List<StudentModality> modalities = getModalitiesForDirectorReport(userProgram.getId(), filters);

        // Agrupar modalidades por director
        Map<Long, List<StudentModality>> modalitiesByDirector = modalities.stream()
                .filter(m -> m.getProjectDirector() != null)
                .collect(Collectors.groupingBy(m -> m.getProjectDirector().getId()));

        // Generar información de cada director con sus modalidades
        List<DirectorAssignedModalitiesReportDTO.DirectorWithModalitiesDTO> directors =
                generateDirectorWithModalitiesList(modalitiesByDirector, filters);

        // Generar resumen general
        DirectorAssignedModalitiesReportDTO.DirectorSummaryDTO summary =
                generateDirectorSummary(directors, modalities);

        // Generar distribución por estado y tipo
        Map<String, Integer> byStatus = modalities.stream()
                .collect(Collectors.groupingBy(
                        m -> ReportUtils.describeModalityStatus(m.getStatus()),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<String, Integer> byType = modalities.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getProgramDegreeModality().getDegreeModality().getName(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // Generar análisis de carga de trabajo si se solicita
        DirectorAssignedModalitiesReportDTO.WorkloadAnalysisDTO workloadAnalysis = null;
        if (filters != null && Boolean.TRUE.equals(filters.getIncludeWorkloadAnalysis())) {
            workloadAnalysis = generateWorkloadAnalysis(directors);
        }

        // Información del director específico si se filtró por uno
        DirectorAssignedModalitiesReportDTO.DirectorInfoDTO directorInfo = null;
        if (filters != null && filters.getDirectorId() != null && !directors.isEmpty()) {
            DirectorAssignedModalitiesReportDTO.DirectorWithModalitiesDTO dir = directors.get(0);
            directorInfo = DirectorAssignedModalitiesReportDTO.DirectorInfoDTO.builder()
                    .directorId(dir.getDirectorId())
                    .fullName(dir.getFullName())
                    .email(dir.getEmail())
                    .academicTitle(dir.getAcademicTitle())
                    .totalAssignedModalities(dir.getTotalAssignedModalities())
                    .activeModalities(dir.getActiveModalities())
                    .completedModalities(dir.getCompletedModalities())
                    .build();
        }

        // Calcular tiempo de generación
        long endTime = System.currentTimeMillis();
        long generationTime = endTime - startTime;

        // Construir metadata
        ReportMetadataDTO metadata = ReportMetadataDTO.builder()
                .reportVersion("1.0")
                .reportType("DIRECTOR_ASSIGNED_MODALITIES")
                .generatedBySystem("SIGMA - Sistema de Gestión de Modalidades de Grado")
                .totalRecords(directors.size())
                .generationTimeMs(generationTime)
                .exportFormat("JSON")
                .build();

        return DirectorAssignedModalitiesReportDTO.builder()
                .generatedAt(LocalDateTime.now())
                .generatedBy(userEmail + " (" + userProgram.getName() + ")")
                .academicProgramId(userProgram.getId())
                .academicProgramName(userProgram.getName())
                .academicProgramCode(userProgram.getCode())
                .directorInfo(directorInfo)
                .summary(summary)
                .directors(directors)
                .modalitiesByStatus(byStatus)
                .modalitiesByType(byType)
                .workloadAnalysis(workloadAnalysis)
                .metadata(metadata)
                .build();
    }

    /**
     * Obtiene las modalidades para el reporte de directores según los filtros
     */
    private List<StudentModality> getModalitiesForDirectorReport(Long programId, DirectorReportFilterDTO filters) {
        List<StudentModality> modalities;

        // Filtrar por activas o todas
        if (filters != null && Boolean.TRUE.equals(filters.getOnlyActiveModalities())) {
            modalities = studentModalityRepository.findByStatusIn(ReportUtils.getActiveStatuses());
        } else {
            modalities = studentModalityRepository.findAll();
        }

        // Filtrar por programa
        modalities = modalities.stream()
                .filter(m -> m.getAcademicProgram().getId().equals(programId))
                .filter(m -> m.getProjectDirector() != null) // Solo las que tienen director
                .collect(Collectors.toList());

        // Filtrar por director específico
        if (filters != null && filters.getDirectorId() != null) {
            modalities = modalities.stream()
                    .filter(m -> m.getProjectDirector().getId().equals(filters.getDirectorId()))
                    .collect(Collectors.toList());
        }

        // Filtrar por estados
        if (filters != null && filters.getProcessStatuses() != null && !filters.getProcessStatuses().isEmpty()) {
            modalities = modalities.stream()
                    .filter(m -> filters.getProcessStatuses().contains(m.getStatus().name()))
                    .collect(Collectors.toList());
        }

        // Filtrar por tipos de modalidad
        if (filters != null && filters.getModalityTypes() != null && !filters.getModalityTypes().isEmpty()) {
            modalities = modalities.stream()
                    .filter(m -> filters.getModalityTypes().contains(
                            m.getProgramDegreeModality().getDegreeModality().getName()))
                    .collect(Collectors.toList());
        }

        return modalities;
    }

    /**
     * Genera la lista de directores con sus modalidades
     */
    private List<DirectorAssignedModalitiesReportDTO.DirectorWithModalitiesDTO> generateDirectorWithModalitiesList(
            Map<Long, List<StudentModality>> modalitiesByDirector, DirectorReportFilterDTO filters) {

        return modalitiesByDirector.entrySet().stream()
                .map(entry -> {
                    Long directorId = entry.getKey();
                    List<StudentModality> directorModalities = entry.getValue();

                    if (directorModalities.isEmpty()) return null;

                    User director = directorModalities.get(0).getProjectDirector();

                    // Contar modalidades por estado
                    long active = directorModalities.stream()
                            .filter(m -> ReportUtils.getActiveStatuses().contains(m.getStatus()))
                            .count();

                    long completed = directorModalities.stream()
                            .filter(m -> m.getStatus() == ModalityProcessStatus.GRADED_APPROVED)
                            .count();

                    long pendingApproval = directorModalities.stream()
                            .filter(m -> m.getStatus() == ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD ||
                                       m.getStatus() == ModalityProcessStatus.UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE)
                            .count();

                    // Generar detalles de las modalidades
                    List<DirectorAssignedModalitiesReportDTO.ModalityDetailDTO> modalityDetails =
                            directorModalities.stream()
                                    .map(this::buildModalityDetailForDirector)
                                    .sorted(Comparator.comparing(
                                            DirectorAssignedModalitiesReportDTO.ModalityDetailDTO::getStartDate).reversed())
                                    .collect(Collectors.toList());

                    // Calcular promedio de días por modalidad
                    double avgDays = directorModalities.stream()
                            .filter(m -> m.getSelectionDate() != null)
                            .mapToLong(m -> ChronoUnit.DAYS.between(m.getSelectionDate(), LocalDateTime.now()))
                            .average()
                            .orElse(0.0);

                    // Determinar estado de carga de trabajo
                    String workloadStatus = determineWorkloadStatus(directorModalities.size());

                    return DirectorAssignedModalitiesReportDTO.DirectorWithModalitiesDTO.builder()
                            .directorId(directorId)
                            .fullName(director.getName() + " " + director.getLastName())
                            .email(director.getEmail())
                            .academicTitle(null) // Campo no disponible en User
                            .totalAssignedModalities(directorModalities.size())
                            .activeModalities((int) active)
                            .completedModalities((int) completed)
                            .pendingApprovalModalities((int) pendingApproval)
                            .modalities(modalityDetails)
                            .workloadStatus(workloadStatus)
                            .averageDaysPerModality(Math.round(avgDays * 100.0) / 100.0)
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        DirectorAssignedModalitiesReportDTO.DirectorWithModalitiesDTO::getTotalAssignedModalities).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Construye el detalle de una modalidad para el reporte de directores
     */
    private DirectorAssignedModalitiesReportDTO.ModalityDetailDTO buildModalityDetailForDirector(StudentModality modality) {
        // Obtener estudiantes
        List<StudentModalityMember> members = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);

        List<DirectorAssignedModalitiesReportDTO.StudentBasicInfoDTO> students = members.stream()
                .map(member -> {
                    User student = member.getStudent();
                    StudentProfile profile = studentProfileRepository.findByUserId(student.getId()).orElse(null);

                    return DirectorAssignedModalitiesReportDTO.StudentBasicInfoDTO.builder()
                            .studentId(student.getId())
                            .fullName(student.getName() + " " + student.getLastName())
                            .studentCode(profile != null ? profile.getStudentCode() : "N/A")
                            .email(student.getEmail())
                            .isLeader(member.getIsLeader())
                            .build();
                })
                .collect(Collectors.toList());

        // Calcular días
        long daysSinceStart = modality.getSelectionDate() != null
                ? ChronoUnit.DAYS.between(modality.getSelectionDate(), LocalDateTime.now())
                : 0;

        long daysInCurrentStatus = modality.getUpdatedAt() != null
                ? ChronoUnit.DAYS.between(modality.getUpdatedAt(), LocalDateTime.now())
                : 0;

        // Generar observaciones
        String observations = generateDirectorObservations(modality, daysInCurrentStatus);

        return DirectorAssignedModalitiesReportDTO.ModalityDetailDTO.builder()
                .modalityId(modality.getId())
                .modalityType(translateSessionType(modality.getModalityType()))
                .modalityTypeName(modality.getProgramDegreeModality().getDegreeModality().getName())
                .students(students)
                .currentStatus(translateModalityProcessStatus(modality.getStatus()))
                .statusDescription(ReportUtils.describeModalityStatus(modality.getStatus()))
                .startDate(modality.getSelectionDate())
                .lastUpdate(modality.getUpdatedAt())
                .daysSinceStart((int) daysSinceStart)
                .daysInCurrentStatus((int) daysInCurrentStatus)
                .projectTitle(null) // Campo no disponible
                .hasPendingActions(ReportUtils.isPendingStatus(modality.getStatus()))
                .observations(observations)
                .build();
    }

    /**
     * Genera observaciones específicas para el director
     */
    private String generateDirectorObservations(StudentModality modality, long daysInCurrentStatus) {
        List<String> observations = new ArrayList<>();

        // Tiempo sin actualización
        if (daysInCurrentStatus > 30) {
            observations.add("⚠ Sin actualización hace " + daysInCurrentStatus + " días");
        } else if (daysInCurrentStatus > 15) {
            observations.add("⏱ " + daysInCurrentStatus + " días en este estado");
        }

        // Correcciones pendientes
        if (modality.getCorrectionDeadline() != null) {
            long daysUntilDeadline = ChronoUnit.DAYS.between(LocalDateTime.now(), modality.getCorrectionDeadline());
            if (daysUntilDeadline < 0) {
                observations.add("🚨 Plazo de corrección vencido");
            } else if (daysUntilDeadline <= 3) {
                observations.add("⏰ " + daysUntilDeadline + " días para entregar correcciones");
            }
        }

        // Estados que requieren acción del director
        if (modality.getStatus() == ModalityProcessStatus.CORRECTIONS_SUBMITTED ||
            modality.getStatus() == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD ||
            modality.getStatus() == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_COMMITTEE ||
            modality.getStatus() == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_EXAMINERS) {
            observations.add("✅ Correcciones entregadas - Revisar");
        } else if (modality.getStatus() == ModalityProcessStatus.PROPOSAL_APPROVED) {
            observations.add("📝 Propuesta aprobada - En desarrollo");
        } else if (modality.getStatus() == ModalityProcessStatus.DEFENSE_COMPLETED) {
            observations.add("🎓 Sustentación completada");
        }

        return observations.isEmpty() ? "Sin observaciones" : String.join(" | ", observations);
    }

    /**
     * Determina el estado de carga de trabajo según cantidad de modalidades
     */
    private String determineWorkloadStatus(int modalityCount) {
        if (modalityCount >= 8) {
            return "OVERLOADED";
        } else if (modalityCount >= 5) {
            return "HIGH";
        } else if (modalityCount >= 2) {
            return "NORMAL";
        } else {
            return "LOW";
        }
    }

    /**
     * Genera el resumen general de directores
     */
    private DirectorAssignedModalitiesReportDTO.DirectorSummaryDTO generateDirectorSummary(
            List<DirectorAssignedModalitiesReportDTO.DirectorWithModalitiesDTO> directors,
            List<StudentModality> allModalities) {

        int totalDirectors = directors.size();
        int totalModalitiesAssigned = allModalities.size();

        // Contar modalidades activas
        long activeModalities = allModalities.stream()
                .filter(m -> ReportUtils.getActiveStatuses().contains(m.getStatus()))
                .count();

        // Contar estudiantes únicos
        Set<Long> uniqueStudents = new HashSet<>();
        for (StudentModality modality : allModalities) {
            List<StudentModalityMember> members = studentModalityMemberRepository
                    .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);
            members.forEach(member -> uniqueStudents.add(member.getStudent().getId()));
        }

        // Calcular promedios
        double avgModalitiesPerDirector = totalDirectors > 0 ?
                (double) totalModalitiesAssigned / totalDirectors : 0;

        // Encontrar director con más y menos modalidades
        String directorWithMost = null;
        int maxCount = 0;
        String directorWithLeast = null;
        int minCount = Integer.MAX_VALUE;

        for (DirectorAssignedModalitiesReportDTO.DirectorWithModalitiesDTO dir : directors) {
            if (dir.getTotalAssignedModalities() > maxCount) {
                maxCount = dir.getTotalAssignedModalities();
                directorWithMost = dir.getFullName();
            }
            if (dir.getTotalAssignedModalities() < minCount) {
                minCount = dir.getTotalAssignedModalities();
                directorWithLeast = dir.getFullName();
            }
        }

        // Contar directores sobrecargados y disponibles
        long overloaded = directors.stream()
                .filter(d -> "OVERLOADED".equals(d.getWorkloadStatus()) || "HIGH".equals(d.getWorkloadStatus()))
                .count();

        long available = directors.stream()
                .filter(d -> "LOW".equals(d.getWorkloadStatus()) || "NORMAL".equals(d.getWorkloadStatus()))
                .count();

        return DirectorAssignedModalitiesReportDTO.DirectorSummaryDTO.builder()
                .totalDirectors(totalDirectors)
                .totalModalitiesAssigned(totalModalitiesAssigned)
                .totalActiveModalities((int) activeModalities)
                .totalStudentsSupervised(uniqueStudents.size())
                .averageModalitiesPerDirector(Math.round(avgModalitiesPerDirector * 100.0) / 100.0)
                .directorWithMostModalities(directorWithMost)
                .maxModalitiesCount(maxCount)
                .directorWithLeastModalities(directorWithLeast)
                .minModalitiesCount(minCount == Integer.MAX_VALUE ? 0 : minCount)
                .directorsOverloaded((int) overloaded)
                .directorsAvailable((int) available)
                .build();
    }

    /**
     * Genera el análisis de carga de trabajo
     */
    private DirectorAssignedModalitiesReportDTO.WorkloadAnalysisDTO generateWorkloadAnalysis(
            List<DirectorAssignedModalitiesReportDTO.DirectorWithModalitiesDTO> directors) {

        int recommendedMax = 6; // Recomendado máximo de modalidades por director

        List<String> overloaded = directors.stream()
                .filter(d -> "OVERLOADED".equals(d.getWorkloadStatus()))
                .map(DirectorAssignedModalitiesReportDTO.DirectorWithModalitiesDTO::getFullName)
                .collect(Collectors.toList());

        List<String> available = directors.stream()
                .filter(d -> "LOW".equals(d.getWorkloadStatus()))
                .map(DirectorAssignedModalitiesReportDTO.DirectorWithModalitiesDTO::getFullName)
                .collect(Collectors.toList());

        // Calcular distribución por nivel de carga
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("LOW", (int) directors.stream().filter(d -> "LOW".equals(d.getWorkloadStatus())).count());
        distribution.put("NORMAL", (int) directors.stream().filter(d -> "NORMAL".equals(d.getWorkloadStatus())).count());
        distribution.put("HIGH", (int) directors.stream().filter(d -> "HIGH".equals(d.getWorkloadStatus())).count());
        distribution.put("OVERLOADED", (int) directors.stream().filter(d -> "OVERLOADED".equals(d.getWorkloadStatus())).count());

        // Calcular carga promedio
        double avgWorkload = directors.stream()
                .mapToInt(DirectorAssignedModalitiesReportDTO.DirectorWithModalitiesDTO::getTotalAssignedModalities)
                .average()
                .orElse(0.0);

        // Determinar estado general
        String overallStatus = overloaded.size() > directors.size() / 3 ? "UNBALANCED" : "BALANCED";

        return DirectorAssignedModalitiesReportDTO.WorkloadAnalysisDTO.builder()
                .recommendedMaxModalities(recommendedMax)
                .directorsOverloaded(overloaded)
                .directorsAvailable(available)
                .averageWorkload(Math.round(avgWorkload * 100.0) / 100.0)
                .overallWorkloadStatus(overallStatus)
                .workloadDistribution(distribution)
                .build();
    }

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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth != null ? auth.getName() : "SYSTEM";

        User authenticatedUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<ProgramAuthority> programAuthorities = programAuthorityRepository.findByUser_Id(authenticatedUser.getId());
        if (programAuthorities.isEmpty()) {
            throw new IllegalArgumentException("El usuario no tiene asignado ningún programa académico");
        }

        AcademicProgram userProgram = programAuthorities.get(0).getAcademicProgram();

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
                .requiresDirector(isDirectorRequired(degreeModality.getName()))
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
        int currentSemester = getSemesterFromDate(now);

        // Filtrar modalidades del periodo actual
        List<StudentModality> currentPeriodModalities = allModalities.stream()
                .filter(m -> m.getSelectionDate() != null)
                .filter(m -> m.getSelectionDate().getYear() == currentYear)
                .filter(m -> getSemesterFromDate(m.getSelectionDate()) == currentSemester)
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
        int currentSemester = getSemesterFromDate(now);

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
                    .filter(m -> getSemesterFromDate(m.getSelectionDate()) == finalSemester)
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
                        m -> ReportUtils.describeModalityStatus(m.getStatus()),
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
                        m -> m.getSelectionDate().getYear() + "-" + getSemesterFromDate(m.getSelectionDate()),
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
        int currentSemester = getSemesterFromDate(now);

        Map<Long, Long> countByModality = studentModalityRepository.findAll().stream()
                .filter(m -> m.getAcademicProgram().getId().equals(program.getId()))
                .filter(m -> m.getSelectionDate() != null)
                .filter(m -> m.getSelectionDate().getYear() == currentYear)
                .filter(m -> getSemesterFromDate(m.getSelectionDate()) == currentSemester)
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
                .filter(m -> getSemesterFromDate(m.getSelectionDate()) == getSemesterFromDate(now))
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
                .map(m -> m.getSelectionDate().getYear() + "-" + getSemesterFromDate(m.getSelectionDate()))
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
                .filter(m -> getSemesterFromDate(m.getSelectionDate()) == getSemesterFromDate(now))
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth != null ? auth.getName() : "SYSTEM";

        User authenticatedUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<ProgramAuthority> programAuthorities = programAuthorityRepository.findByUser_Id(authenticatedUser.getId());
        if (programAuthorities.isEmpty()) {
            throw new IllegalArgumentException("El usuario no tiene asignado ningún programa académico");
        }

        AcademicProgram userProgram = programAuthorities.get(0).getAcademicProgram();

        // Obtener todas las modalidades del programa
        List<StudentModality> allModalities = studentModalityRepository.findAll().stream()
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
                .filter(m -> filterByModalityType(m, filters.getModalityTypes()))
                .filter(m -> filterBySemester(m, filters.getSemesters(), filters.getYear()))
                .filter(m -> filterByModalityTypeFilter(m, filters.getModalityTypeFilter()))
                .filter(m -> filterByDirector(m, filters.getHasDirector()))
                .filter(m -> filterByTimelineStatus(m, filters.getTimelineStatus()))
                .collect(Collectors.toList());
    }

    private boolean filterByStatus(StudentModality modality, List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) return true;
        return statuses.contains(modality.getStatus().name());
    }

    private boolean filterByModalityType(StudentModality modality, List<String> modalityTypes) {
        if (modalityTypes == null || modalityTypes.isEmpty()) return true;
        String modalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        return modalityTypes.contains(modalityName);
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
        int modalitySemester = getSemesterFromDate(modality.getSelectionDate());
        String modalityPeriod = modalityYear + "-" + modalitySemester;

        return semesters.contains(modalityPeriod);
    }

    private boolean filterByModalityTypeFilter(StudentModality modality, String modalityTypeFilter) {
        if (modalityTypeFilter == null || modalityTypeFilter.isEmpty()) return true;
        if ("INDIVIDUAL".equals(modalityTypeFilter)) {
            return modality.getModalityType() == com.SIGMA.USCO.Modalities.Entity.enums.ModalityType.INDIVIDUAL;
        } else if ("GROUP".equals(modalityTypeFilter)) {
            return modality.getModalityType() == com.SIGMA.USCO.Modalities.Entity.enums.ModalityType.GROUP;
        }
        return true;
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

        for (StudentModality modality : modalities) {
            List<StudentModalityMember> members = studentModalityMemberRepository
                    .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);

            for (StudentModalityMember member : members) {
                User user = member.getStudent(); // getStudent() retorna User
                if (user == null) continue;

                // Buscar el perfil de estudiante
                StudentProfile profile = studentProfileRepository.findById(user.getId()).orElse(null);
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
                        .modalityStatus(translateModalityProcessStatus(modality.getStatus()))
                        .modalityStatusDescription(ReportUtils.describeModalityStatus(modality.getStatus()))
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

        if (modality.getProjectDirector() == null && isDirectorRequired(modality.getProgramDegreeModality().getDegreeModality().getName())) {
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
                    List<Double> gpas = new ArrayList<>();
                    for (StudentModality modality : typeModalities) {
                        List<StudentModalityMember> members = studentModalityMemberRepository
                                .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);

                        for (StudentModalityMember member : members) {
                            StudentProfile profile = studentProfileRepository.findById(member.getStudent().getId()).orElse(null);
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
                            .statusDescription(ReportUtils.describeModalityStatus(status))
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
                    int semester = getSemesterFromDate(m.getSelectionDate());
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
                    List<Double> semesterGPAs = new ArrayList<>();
                    for (StudentModality modality : semesterModalities) {
                        List<StudentModalityMember> members = studentModalityMemberRepository
                                .findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE);

                        for (StudentModalityMember member : members) {
                            StudentProfile profile = studentProfileRepository.findById(member.getStudent().getId()).orElse(null);
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

    /**
     * Genera reporte de modalidades finalizadas (exitosas y fallidas)
     * Incluye análisis completo de resultados, tiempos, calificaciones y distinciones
     *
     * @param filters Filtros a aplicar
     * @return Reporte completo de modalidades completadas
     */
    @Transactional(readOnly = true)
    public CompletedModalitiesReportDTO generateCompletedModalitiesReport(CompletedModalitiesFilterDTO filters) {
        long startTime = System.currentTimeMillis();

        // Obtener usuario autenticado y su programa
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth != null ? auth.getName() : "SYSTEM";

        User authenticatedUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<ProgramAuthority> programAuthorities = programAuthorityRepository.findByUser_Id(authenticatedUser.getId());
        if (programAuthorities.isEmpty()) {
            throw new IllegalArgumentException("El usuario no tiene asignado ningún programa académico");
        }

        AcademicProgram userProgram = programAuthorities.get(0).getAcademicProgram();

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
                .filter(m -> filterByModalityTypes(m, filters.getModalityTypes()))
                .filter(m -> filterByResults(m, filters.getResults()))
                .filter(m -> filterByYearSemester(m, filters.getYear(), filters.getSemester()))
                .filter(m -> filterByGradeRange(m, filters.getMinGrade(), filters.getMaxGrade()))
                .filter(m -> filterByDistinction(m, filters.getOnlyWithDistinction(), filters.getDistinctionType()))
                .filter(m -> filterByDirectorId(m, filters.getDirectorId()))
                .filter(m -> filterByModalityTypeFilter(m, filters.getModalityTypeFilter()))
                .collect(Collectors.toList());
    }

    private boolean filterByModalityTypes(StudentModality modality, List<String> types) {
        if (types == null || types.isEmpty()) return true;
        String modalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        return types.contains(modalityName);
    }

    private boolean filterByResults(StudentModality modality, List<String> results) {
        if (results == null || results.isEmpty()) return true;
        String result = modality.getStatus() == ModalityProcessStatus.GRADED_APPROVED ? "SUCCESS" : "FAILED";
        return results.contains(result);
    }

    private boolean filterByYearSemester(StudentModality modality, Integer year, Integer semester) {
        if (modality.getUpdatedAt() == null) return year == null && semester == null;

        if (year != null && modality.getUpdatedAt().getYear() != year) return false;
        if (semester != null && getSemesterFromDate(modality.getUpdatedAt()) != semester) return false;

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
                    getSemesterFromDate(modality.getUpdatedAt()) : null;

            details.add(CompletedModalitiesReportDTO.CompletedModalityDetailDTO.builder()
                    .modalityId(modality.getId())
                    .modalityType(modality.getProgramDegreeModality().getDegreeModality().getName())
                    .modalityTypeName(modality.getProgramDegreeModality().getDegreeModality().getName())
                    .result(result)
                    .completionDate(modality.getUpdatedAt())
                    .completionDays(completionDays)
                    .finalGrade(modality.getFinalGrade())
                    .gradeDescription(describeGrade(modality.getFinalGrade()))
                    .academicDistinction(translateAcademicDistinction(modality.getAcademicDistinction()))
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
            observations.add("Con distinción académica: " + translateAcademicDistinction(modality.getAcademicDistinction()));
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
        long meritorious = details.stream()
                .filter(d -> "MERITORIOUS".equals(d.getAcademicDistinction()))
                .count();
        long laureate = details.stream()
                .filter(d -> "LAUREATE".equals(d.getAcademicDistinction()))
                .count();
        long withoutDistinction = details.size() - meritorious - laureate;

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
                    int semester = getSemesterFromDate(m.getUpdatedAt());
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

    // ─── Métodos de traducción de enums ───────────────────────────────────────

    private String translateSessionType(com.SIGMA.USCO.Modalities.Entity.enums.ModalityType type) {
        if (type == null) return "Individual";
        return switch (type) {
            case INDIVIDUAL -> "Individual";
            case GROUP -> "Grupal";
        };
    }

    private String translateModalityProcessStatus(com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus status) {
        if (status == null) return "Sin estado";
        return switch (status) {
            case MODALITY_SELECTED -> "Modalidad seleccionada";
            case UNDER_REVIEW_PROGRAM_HEAD -> "En revisión por Jefatura de Programa";
            case CORRECTIONS_REQUESTED_PROGRAM_HEAD -> "Correcciones solicitadas por Jefatura";
            case CORRECTIONS_SUBMITTED -> "Correcciones enviadas";
            case CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD -> "Correcciones enviadas a Jefatura";
            case CORRECTIONS_SUBMITTED_TO_COMMITTEE -> "Correcciones enviadas al Comité";
            case CORRECTIONS_SUBMITTED_TO_EXAMINERS -> "Correcciones enviadas a los Jurados";
            case CORRECTIONS_APPROVED -> "Correcciones aprobadas";
            case CORRECTIONS_REJECTED_FINAL -> "Rechazado definitivamente";
            case READY_FOR_PROGRAM_CURRICULUM_COMMITTEE -> "Lista para Comité de Currículo";
            case UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE -> "En revisión por Comité de Currículo";
            case CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE -> "Correcciones solicitadas por Comité";
            case READY_FOR_DIRECTOR_ASSIGNMENT -> "Lista para asignar Director";
            case READY_FOR_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE -> "Lista para aprobación por Comité";
            case APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE -> "Aprobado por Comité de Currículo";
            case PROPOSAL_APPROVED -> "Propuesta aprobada";
            case PENDING_PROGRAM_HEAD_FINAL_REVIEW -> "Pendiente revisión final por Jefatura";
            case APPROVED_BY_PROGRAM_HEAD_FINAL_REVIEW -> "Documentos finales aprobados por Jefatura";
            case DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR -> "Sustentación propuesta por Director";
            case DEFENSE_SCHEDULED -> "Sustentación programada";
            case EXAMINERS_ASSIGNED -> "Jurados asignados";
            case READY_FOR_EXAMINERS -> "Lista para revisión de jurados";
            case DOCUMENTS_APPROVED_BY_EXAMINERS -> "Documentos aprobados por jurados";
            case SECONDARY_DOCUMENTS_APPROVED_BY_EXAMINERS -> "Documentos finales aprobados por jurados";
            case DOCUMENT_REVIEW_TIEBREAKER_REQUIRED -> "Requiere jurado de desempate (documentos)";
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

    private String translateAcademicDistinction(com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction distinction) {
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
