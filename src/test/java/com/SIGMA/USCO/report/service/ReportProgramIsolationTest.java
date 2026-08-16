package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.Modalities.entity.DegreeModality;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.entity.ProgramAuthority;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.ProgramDegreeModality;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.report.dto.DirectorWorkloadDTO;
import com.SIGMA.USCO.report.dto.DirectorsByModalityReportDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Regresión T0.8: aislamiento por programa del reporte de directores")
class ReportProgramIsolationTest {

    @Mock
    private StudentModalityRepository studentModalityRepository;
    @Mock
    private StudentModalityMemberRepository studentModalityMemberRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private ProgramAuthorityRepository programAuthorityRepository;

    @InjectMocks
    private DirectorReportService service;

    private static final String MODALITY_TYPE = "PROYECTO DE GRADO";

    private AcademicProgram programA;
    private AcademicProgram programB;
    private User directorA;
    private User directorB;
    private User authenticatedUser;

    @BeforeEach
    void setUp() {
        programA = AcademicProgram.builder().id(1L).name("Ingeniería de Software").code("ING_SOFTWARE").build();
        programB = AcademicProgram.builder().id(2L).name("Ingeniería Civil").code("ING_CIVIL").build();
        directorA = User.builder().id(10L).name("Ana").lastName("Pérez").email("ana@usco.edu.co").build();
        directorB = User.builder().id(11L).name("Luis").lastName("Gómez").email("luis@usco.edu.co").build();
        authenticatedUser = User.builder().id(100L).name("Jefe").lastName("Programa").email("jefe@usco.edu.co").build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authenticatedUser, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("El reporte solo incluye modalidades del programa del usuario autenticado")
    void onlyIncludesModalitiesFromAuthenticatedUsersProgram() {
        ProgramAuthority authority = ProgramAuthority.builder()
                .id(1L)
                .user(authenticatedUser)
                .academicProgram(programA)
                .role(ProgramRole.PROGRAM_HEAD)
                .build();
        when(programAuthorityRepository.findByUser_Id(authenticatedUser.getId())).thenReturn(List.of(authority));

        DegreeModality degreeModality = DegreeModality.builder().id(1L).name(MODALITY_TYPE).build();
        StudentModality modalityA1 = modality(1L, programA, directorA, degreeModality);
        StudentModality modalityA2 = modality(2L, programA, directorA, degreeModality);
        StudentModality modalityB1 = modality(3L, programB, directorB, degreeModality);
        when(studentModalityRepository.findByStatusIn(anyList())).thenReturn(List.of(modalityA1, modalityA2, modalityB1));
        when(studentModalityMemberRepository.findByStudentModalityIdInAndStatus(anyList(), any())).thenReturn(List.of());

        DirectorsByModalityReportDTO report = service.generateDirectorsByModalityReport(MODALITY_TYPE);

        // El DTO no expone el programa por ítem; el aislamiento se prueba vía identidad:
        // el director B (único con modalidades del programa B) no debe aparecer.
        assertThat(report.getTotalDirectors()).isEqualTo(1);
        assertThat(report.getDirectors()).hasSize(1);

        DirectorWorkloadDTO director = report.getDirectors().get(0);
        assertThat(director.getDirectorId()).isEqualTo(directorA.getId());
        assertThat(director.getActiveProjects()).isEqualTo(2);
        assertThat(director.getTotalProjects()).isEqualTo(2);
        assertThat(director.getModalityTypes()).containsExactly(MODALITY_TYPE);
        assertThat(director.getAssignedStudents()).isEmpty();

        assertThat(report.getDirectors())
                .extracting(DirectorWorkloadDTO::getDirectorId)
                .doesNotContain(directorB.getId());
        assertThat(report.getStatistics().getTotalActiveDirectors()).isEqualTo(1);
    }

    private StudentModality modality(Long id, AcademicProgram program, User director, DegreeModality degreeModality) {
        return StudentModality.builder()
                .id(id)
                .status(ModalityProcessStatus.PROPOSAL_APPROVED)
                .academicProgram(program)
                .programDegreeModality(ProgramDegreeModality.builder()
                        .id(id)
                        .academicProgram(program)
                        .degreeModality(degreeModality)
                        .build())
                .projectDirector(director)
                .build();
    }
}