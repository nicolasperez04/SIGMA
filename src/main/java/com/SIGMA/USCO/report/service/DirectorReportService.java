package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.report.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio especializado en reportes de directores
 */
@Service
@RequiredArgsConstructor
public class DirectorReportService {

    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final StudentProfileRepository studentProfileRepository;


    @Transactional(readOnly = true)
    public DirectorsByModalityReportDTO generateDirectorsByModalityReport(String modalityType) {
        // Obtener todas las modalidades activas
        List<StudentModality> allModalities = studentModalityRepository
                .findByStatusIn(ReportUtils.getActiveStatuses());

        // Filtrar por tipo de modalidad
        List<StudentModality> modalities = allModalities.stream()
                .filter(m -> m.getProgramDegreeModality().getDegreeModality().getName().equalsIgnoreCase(modalityType))
                .collect(Collectors.toList());

        // Agrupar por director
        Map<Long, List<StudentModality>> modalitiesByDirector = modalities.stream()
                .filter(m -> m.getProjectDirector() != null)
                .collect(Collectors.groupingBy(m -> m.getProjectDirector().getId()));

        // Generar información de carga de trabajo por director
        Map<Long, List<StudentModalityMember>> membersByModality = ReportUtils.loadActiveMembersByModalityIds(
                modalities.stream().map(StudentModality::getId).toList(), studentModalityMemberRepository);
        List<DirectorWorkloadDTO> directors = modalitiesByDirector.entrySet().stream()
                .map(entry -> {
                    Long directorId = entry.getKey();
                    List<StudentModality> directorModalities = entry.getValue();
                    User director = directorModalities.get(0).getProjectDirector();

                    // Obtener todos los proyectos del director
                    long totalProjects = directorModalities.stream()
                            .filter(sm -> sm.getProjectDirector() != null
                                    && sm.getProjectDirector().getId().equals(directorId)
                                    && ReportUtils.getActiveStatuses().contains(sm.getStatus()))
                            .count();

                    // Obtener estudiantes asignados
                    List<StudentInfoDTO> assignedStudents = directorModalities.stream()
                            .flatMap(modality -> ReportUtils.buildStudentInfos(
                                    membersByModality.getOrDefault(modality.getId(), List.of()),
                                    studentProfileRepository).stream())
                            .collect(Collectors.toList());

                    // Tipos de modalidades que dirige
                    List<String> modalityTypes = directorModalities.stream()
                            .map(m -> m.getProgramDegreeModality().getDegreeModality().getName())
                            .distinct()
                            .collect(Collectors.toList());

                    return DirectorWorkloadDTO.builder()
                            .directorId(directorId)
                            .directorName(director.getName() + " " + director.getLastName())
                            .directorEmail(director.getEmail())
                            .activeProjects(directorModalities.size())
                            .completedProjects(0) // Calcular proyectos completados
                            .totalProjects((int) totalProjects)
                            .modalityTypes(modalityTypes)
                            .assignedStudents(assignedStudents)
                            .build();
                })
                .sorted(Comparator.comparing(DirectorWorkloadDTO::getActiveProjects).reversed())
                .collect(Collectors.toList());

        // Generar estadísticas
        DirectorStatisticsDTO statistics = generateDirectorStatistics(directors);

        return DirectorsByModalityReportDTO.builder()
                .modalityType(modalityType)
                .totalDirectors(directors.size())
                .directors(directors)
                .statistics(statistics)
                .build();
    }


    private DirectorStatisticsDTO generateDirectorStatistics(List<DirectorWorkloadDTO> directors) {
        if (directors.isEmpty()) {
            return DirectorStatisticsDTO.builder()
                    .totalActiveDirectors(0)
                    .totalDirectorsWithMaxCapacity(0)
                    .averageProjectsPerDirector(0.0)
                    .maxProjectsPerDirector(0)
                    .minProjectsPerDirector(0)
                    .build();
        }

        int totalActive = directors.size();
        double avgProjects = directors.stream()
                .mapToInt(DirectorWorkloadDTO::getActiveProjects)
                .average()
                .orElse(0.0);

        int maxProjects = directors.stream()
                .mapToInt(DirectorWorkloadDTO::getActiveProjects)
                .max()
                .orElse(0);

        int minProjects = directors.stream()
                .mapToInt(DirectorWorkloadDTO::getActiveProjects)
                .min()
                .orElse(0);

        // Supongamos que la capacidad máxima es 5 proyectos
        int maxCapacity = 5;
        int directorsAtMaxCapacity = (int) directors.stream()
                .filter(d -> d.getActiveProjects() >= maxCapacity)
                .count();

        return DirectorStatisticsDTO.builder()
                .totalActiveDirectors(totalActive)
                .totalDirectorsWithMaxCapacity(directorsAtMaxCapacity)
                .averageProjectsPerDirector(Math.round(avgProjects * 100.0) / 100.0)
                .maxProjectsPerDirector(maxProjects)
                .minProjectsPerDirector(minProjects)
                .build();
    }
}


