package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.academic.entity.StudentProfile;
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


    @Transactional(readOnly = true)
    public StudentsByModalityReportDTO generateStudentsByModalityReport(String modalityType) {
        // Obtener todas las modalidades activas
        List<StudentModality> allModalities = studentModalityRepository
                .findByStatusIn(ReportUtils.getActiveStatuses());

        // Filtrar por tipo de modalidad
        List<StudentModality> modalities = allModalities.stream()
                .filter(m -> m.getProgramDegreeModality().getDegreeModality().getName().equalsIgnoreCase(modalityType))
                .collect(Collectors.toList());

        // Obtener todos los estudiantes de estas modalidades
        List<StudentInfoDTO> students = modalities.stream()
                .flatMap(modality -> ReportUtils.buildStudentInfos(
                        studentModalityMemberRepository.findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE),
                        studentProfileRepository).stream())
                .distinct()
                .collect(Collectors.toList());

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

        // Obtener todas las modalidades activas del período
        // Nota: Necesitarás agregar el filtro por período en el repositorio
        List<StudentModality> modalities = studentModalityRepository
                .findByStatusIn(ReportUtils.getActiveStatuses());

        // Obtener estudiantes
        List<StudentInfoDTO> students = modalities.stream()
                .flatMap(modality -> ReportUtils.buildStudentInfos(
                        studentModalityMemberRepository.findByStudentModalityIdAndStatus(modality.getId(), MemberStatus.ACTIVE),
                        studentProfileRepository).stream())
                .distinct()
                .collect(Collectors.toList());

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
        long totalActive = modalities.size();
        // Aquí puedes agregar más lógica según los estados específicos

        return StudentStatisticsDTO.builder()
                .totalActive((int) totalActive)
                .totalInProgress((int) totalActive)
                .totalCompleted(0) // Calcular según estados
                .totalCancelled(0) // Calcular según estados
                .totalPendingApproval(0) // Calcular según estados
                .averageProgress(0.0) // Calcular promedio
                .build();
    }


    private SemesterStatisticsDTO generateSemesterStatistics(List<StudentModality> modalities) {
        int totalEnrolled = modalities.size();

        return SemesterStatisticsDTO.builder()
                .totalEnrolled(totalEnrolled)
                .totalInProgress(totalEnrolled)
                .totalCompleted(0) // Calcular según estados
                .totalCancelled(0) // Calcular según estados
                .completionRate(0.0) // Calcular tasa
                .mostPopularModality("N/A") // Calcular la más popular
                .build();
    }
}


