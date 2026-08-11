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
public class DirectorAssignedModalitiesReportService {

    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;


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
        String userEmail = SecurityUtils.getCurrentUser().getEmail();
        AcademicProgram userProgram = ReportUtils.getAuthenticatedUserProgram(programAuthorityRepository);

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
                        m -> TranslationUtils.translateModalityProcessStatus(m.getStatus()),
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
                .modalityType(ReportUtils.translateSessionType(modality.getModalityType()))
                .modalityTypeName(modality.getProgramDegreeModality().getDegreeModality().getName())
                .students(students)
                .currentStatus(TranslationUtils.translateModalityProcessStatus(modality.getStatus()))
                .statusDescription(TranslationUtils.translateModalityProcessStatus(modality.getStatus()))
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
}
