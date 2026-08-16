package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.report.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio especializado en reportes de estudiantes
 */
@Service
@RequiredArgsConstructor
public class StudentReportService {

    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;


    @Transactional(readOnly = true)
    public StudentsByModalityReportDTO generateStudentsByModalityReport(String modalityType) {
        // Obtener usuario autenticado y su programa
        AcademicProgram userProgram = ReportUtils.getAuthenticatedUserProgram(programAuthorityRepository);

        // Obtener todas las modalidades activas
        List<StudentModality> allModalities = studentModalityRepository
                .findByStatusIn(ReportUtils.getActiveStatuses()).stream()
                .filter(m -> m.getAcademicProgram().getId().equals(userProgram.getId()))
                .toList();

        // Filtrar por tipo de modalidad
        List<StudentModality> modalities = allModalities.stream()
                .filter(m -> m.getProgramDegreeModality().getDegreeModality().getName().equalsIgnoreCase(modalityType))
                .toList();

        // Obtener todos los estudiantes de estas modalidades
        Map<Long, List<StudentModalityMember>> membersByModality = ReportUtils.loadActiveMembersByModalityIds(
                modalities.stream().map(StudentModality::getId).toList(), studentModalityMemberRepository);
        List<StudentInfoDTO> students = modalities.stream()
                .flatMap(modality -> ReportUtils.buildStudentInfos(
                        membersByModality.getOrDefault(modality.getId(), List.of()),
                        studentProfileRepository).stream())
                .distinct()
                .toList();

        // Generar estadísticas
        StudentStatisticsDTO statistics = generateStudentStatistics(modalities);

        return StudentsByModalityReportDTO.builder()
                .modalityType(modalityType)
                .modalityName(modalityType)
                .totalStudents(students.size())
                .students(students)
                .statistics(statistics)
                .build();
    }


    @Transactional(readOnly = true)
    public StudentsBySemesterReportDTO generateStudentsBySemesterReport(Integer year, Integer semester) {
        // Construir período académico
        String academicPeriod = year + "-" + semester;

        // Obtener usuario autenticado y su programa
        AcademicProgram userProgram = ReportUtils.getAuthenticatedUserProgram(programAuthorityRepository);

        // Obtener todas las modalidades activas del período
        List<StudentModality> modalities = studentModalityRepository
                .findByStatusIn(ReportUtils.getActiveStatuses())
                .stream()
                .filter(m -> m.getAcademicProgram().getId().equals(userProgram.getId()))
                .filter(m -> m.getSelectionDate() != null)
                .filter(m -> year == null ||
                        m.getSelectionDate().getYear() == year)
                .filter(m -> semester == null ||
                        ReportUtils.getSemesterFromDate(m.getSelectionDate()) == semester)
                .toList();

        // Obtener estudiantes
        Map<Long, List<StudentModalityMember>> membersByModality = ReportUtils.loadActiveMembersByModalityIds(
                modalities.stream().map(StudentModality::getId).toList(), studentModalityMemberRepository);
        List<StudentInfoDTO> students = modalities.stream()
                .flatMap(modality -> ReportUtils.buildStudentInfos(
                        membersByModality.getOrDefault(modality.getId(), List.of()),
                        studentProfileRepository).stream())
                .distinct()
                .toList();

        // Estudiantes por modalidad
        Map<String, Integer> studentsByModality = modalities.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getProgramDegreeModality().getDegreeModality().getName(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // Generar estadísticas del semestre
        SemesterStatisticsDTO statistics = generateSemesterStatistics(modalities);

        return StudentsBySemesterReportDTO.builder()
                .academicPeriod(academicPeriod)
                .year(year)
                .semester(semester)
                .totalStudents(students.size())
                .studentsByModality(studentsByModality)
                .students(students)
                .statistics(statistics)
                .build();
    }


    private StudentStatisticsDTO generateStudentStatistics(List<StudentModality> modalities) {
        return StudentStatisticsDTO.builder()
                .totalActive(modalities.size())
                .totalInProgress(modalities.size())
                .totalCompleted(null) // No calculable: la consulta solo trae estados activos
                .totalCancelled(null) // No calculable: la consulta solo trae estados activos
                .totalPendingApproval(null) // No calculable: la consulta solo trae estados activos
                .averageProgress(null) // No se cuenta con medición de progreso
                .build();
    }


    private SemesterStatisticsDTO generateSemesterStatistics(List<StudentModality> modalities) {
        String mostPopularModality = modalities.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getProgramDegreeModality().getDegreeModality().getName(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        return SemesterStatisticsDTO.builder()
                .totalEnrolled(modalities.size())
                .totalInProgress(modalities.size())
                .totalCompleted(null) // No calculable: la consulta solo trae estados activos
                .totalCancelled(null) // No calculable: la consulta solo trae estados activos
                .completionRate(null) // No calculable: requiere conteo de completadas
                .mostPopularModality(mostPopularModality)
                .build();
    }
}


