package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.Modalities.entity.DegreeModality;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityType;
import com.SIGMA.USCO.Modalities.repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.entity.ProgramAuthority;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.ProgramDegreeModality;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.report.dto.StudentListingReportDTO;
import com.SIGMA.USCO.security.SecurityUtils;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("T1.5 - Regresión T0.5: contadores del resumen ejecutivo del listado de estudiantes")
class StudentListingReportServiceTest {

    @Mock
    private StudentModalityRepository studentModalityRepository;
    @Mock
    private StudentModalityMemberRepository studentModalityMemberRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private ProgramAuthorityRepository programAuthorityRepository;

    @InjectMocks
    private StudentListingReportService service;

    private User programHead;
    private AcademicProgram program;
    private StudentModality modalityActive1;
    private StudentModality modalityActive2;
    private StudentModality modalityCompleted;
    private StudentModalityMember member1;
    private StudentModalityMember member2;
    private StudentModalityMember member3;

    @BeforeEach
    void setUp() {
        programHead = User.builder()
                .id(10L)
                .name("Jefe")
                .lastName("Programa")
                .email("jefe.programa@usco.edu.co")
                .build();

        program = AcademicProgram.builder()
                .id(1L)
                .name("INGENIERIA DE SOFTWARE")
                .code("ING_SOFTWARE")
                .totalCredits(160L)
                .build();

        ProgramAuthority authority = ProgramAuthority.builder()
                .id(1L)
                .user(programHead)
                .academicProgram(program)
                .role(ProgramRole.PROGRAM_HEAD)
                .build();
        when(programAuthorityRepository.findByUser_Id(10L)).thenReturn(List.of(authority));

        ProgramDegreeModality programDegreeModality = ProgramDegreeModality.builder()
                .id(1L)
                .academicProgram(program)
                .degreeModality(DegreeModality.builder().id(1L).name("Proyecto de Grado").build())
                .build();

        User student = User.builder()
                .id(100L)
                .name("Ana")
                .lastName("Perez")
                .email("2020123456@usco.edu.co")
                .build();

        StudentProfile profile = StudentProfile.builder()
                .id(100L)
                .user(student)
                .academicProgram(program)
                .studentCode("2020123456")
                .gpa(4.2)
                .approvedCredits(130L)
                .semester(8L)
                .build();

        modalityActive1 = modality(1L, ModalityProcessStatus.PROPOSAL_APPROVED, program, programDegreeModality);
        modalityActive2 = modality(2L, ModalityProcessStatus.APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE, program, programDegreeModality);
        modalityCompleted = modality(3L, ModalityProcessStatus.GRADED_APPROVED, program, programDegreeModality);

        member1 = member(1L, modalityActive1, student);
        member2 = member(2L, modalityActive2, student);
        member3 = member(3L, modalityCompleted, student);

        when(studentModalityRepository.findForProgramHead(anyList()))
                .thenReturn(List.of(modalityActive1, modalityActive2, modalityCompleted));
        when(studentModalityMemberRepository.findByStudentModalityIdInAndStatus(anyList(), eq(MemberStatus.ACTIVE)))
                .thenReturn(List.of(member1, member2, member3));
        when(studentProfileRepository.findAllByUserIdIn(anyList())).thenReturn(List.of(profile));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(programHead, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("El resumen ejecutivo cuenta 2 modalidades activas y 1 completada")
    void executiveSummaryCountsActiveAndCompletedModalities() {
        assertThat(SecurityUtils.getCurrentUser()).isSameAs(programHead);

        StudentListingReportDTO report = service.generateStudentListingReport(null, programHead.getEmail());

        StudentListingReportDTO.ExecutiveSummaryDTO summary = report.getExecutiveSummary();
        assertThat(summary.getActiveModalities()).isEqualTo(2);
        assertThat(summary.getCompletedModalities()).isEqualTo(1);
        assertThat(summary.getTotalModalities()).isEqualTo(3);
        assertThat(summary.getQuickStats()).containsEntry("Modalidades Activas", 2)
                .containsEntry("Completadas", 1);
    }

    @Test
    @DisplayName("El reporte se genera para el programa del usuario autenticado")
    void reportIsGeneratedForAuthenticatedUserProgram() {
        StudentListingReportDTO report = service.generateStudentListingReport(null, programHead.getEmail());

        assertThat(report.getAcademicProgramId()).isEqualTo(1L);
        assertThat(report.getAcademicProgramName()).isEqualTo("INGENIERIA DE SOFTWARE");
        assertThat(report.getGeneratedBy()).endsWith("INGENIERIA DE SOFTWARE)");
    }

    private StudentModality modality(Long id, ModalityProcessStatus status, AcademicProgram program,
                                     ProgramDegreeModality programDegreeModality) {
        return StudentModality.builder()
                .id(id)
                .modalityType(ModalityType.INDIVIDUAL)
                .status(status)
                .academicProgram(program)
                .programDegreeModality(programDegreeModality)
                .selectionDate(LocalDateTime.now().minusDays(100))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private StudentModalityMember member(Long id, StudentModality modality, User student) {
        return StudentModalityMember.builder()
                .id(id)
                .studentModality(modality)
                .student(student)
                .isLeader(true)
                .status(MemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();
    }
}