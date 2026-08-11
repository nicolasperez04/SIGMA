package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus;
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
public class GlobalReportService {

    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;


    @Transactional(readOnly = true)
    public GlobalModalityReportDTO generateGlobalReport() {
        long startTime = System.currentTimeMillis();

        // Obtener usuario autenticado y su programa
        String userEmail = SecurityUtils.getCurrentUser().getEmail();
        AcademicProgram userProgram = ReportUtils.getAuthenticatedUserProgram(programAuthorityRepository);
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
        String userEmail = SecurityUtils.getCurrentUser().getEmail();
        AcademicProgram userProgram = ReportUtils.getAuthenticatedUserProgram(programAuthorityRepository);
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
        AcademicProgram userProgram = ReportUtils.getAuthenticatedUserProgram(programAuthorityRepository);

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
                            .requiresDirector(ReportUtils.isDirectorRequired(degreeModality.getName()))
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
                        m -> TranslationUtils.translateModalityProcessStatus(m.getStatus()),
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

        List<StudentInfoDTO> students = ReportUtils.buildStudentInfos(members, studentProfileRepository);


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
                .modalityType(ReportUtils.translateSessionType(modality.getModalityType()))
                .academicProgram(modality.getAcademicProgram().getName())
                .currentStatus(TranslationUtils.translateModalityProcessStatus(modality.getStatus()))
                .statusDescription(TranslationUtils.translateModalityProcessStatus(modality.getStatus()))
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
                                    m -> TranslationUtils.translateModalityProcessStatus(m.getStatus()),
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
}
