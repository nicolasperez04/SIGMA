package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.dto.response.StartGroupModalityResponse;
import com.SIGMA.USCO.Modalities.entity.DegreeModality;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.repository.DefenseExaminerRepository;
import com.SIGMA.USCO.Modalities.repository.DegreeModalityRepository;
import com.SIGMA.USCO.Modalities.repository.ModalityInvitationRepository;
import com.SIGMA.USCO.Modalities.repository.ModalityRequirementsRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.documents.repository.RequiredDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentStatusHistoryRepository;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.ProgramDegreeModality;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.FacultyRepository;
import com.SIGMA.USCO.academic.repository.ProgramDegreeModalityRepository;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.common.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("F11.2 - Whitelist grupal: CORRECTIONS_REJECTED_FINAL permite iniciar nueva modalidad")
class ModalityGroupStartWhitelistTest {

    @Mock
    private DegreeModalityRepository degreeModalityRepository;
    @Mock
    private ModalityRequirementsRepository modalityRequirementsRepository;
    @Mock
    private RequiredDocumentRepository requiredDocumentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private StudentModalityRepository studentModalityRepository;
    @Mock
    private StudentModalityMemberRepository studentModalityMemberRepository;
    @Mock
    private ModalityInvitationRepository modalityInvitationRepository;
    @Mock
    private StudentDocumentRepository studentDocumentRepository;
    @Mock
    private ModalityStatusTransition modalityStatusTransition;
    @Mock
    private StudentDocumentStatusHistoryRepository documentHistoryRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private FacultyRepository facultyRepository;
    @Mock
    private ProgramDegreeModalityRepository programDegreeModalityRepository;
    @Mock
    private ProgramAuthorityRepository programAuthorityRepository;
    @Mock
    private DefenseExaminerRepository defenseExaminerRepository;

    @InjectMocks
    private ModalityGroupService service;

    private User student;
    private DegreeModality degreeModality;

    @BeforeEach
    void setUp() {
        student = User.builder().id(300L).name("Ana").lastName("Perez").build();
        degreeModality = DegreeModality.builder().id(1L).name("PROYECTO DE GRADO").build();

        AcademicProgram program = AcademicProgram.builder().id(5L).name("INGENIERIA DE SOFTWARE").build();
        StudentProfile profile = StudentProfile.builder().id(300L).user(student).academicProgram(program).build();

        when(studentProfileRepository.findByUserId(300L)).thenReturn(Optional.of(profile));
        when(degreeModalityRepository.findById(1L)).thenReturn(Optional.of(degreeModality));
        when(programDegreeModalityRepository.findByAcademicProgramIdAndDegreeModalityIdAndActiveTrue(5L, 1L))
                .thenReturn(Optional.of(ProgramDegreeModality.builder().degreeModality(degreeModality).build()));
    }

    @Test
    @DisplayName("Una modalidad grupal previa en CORRECTIONS_REJECTED_FINAL no bloquea iniciar otra")
    void startsNewModalityDespiteRejectedFinalCorrections() {
        StudentModality previous = StudentModality.builder()
                .id(50L)
                .status(ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL)
                .build();
        StudentModalityMember member = StudentModalityMember.builder()
                .studentModality(previous)
                .student(student)
                .status(MemberStatus.ACTIVE)
                .build();
        when(studentModalityMemberRepository.findByStudentIdAndStatus(300L, MemberStatus.ACTIVE))
                .thenReturn(List.of(member));
        when(studentModalityRepository.findByLeaderIdAndStatus(300L, ModalityProcessStatus.MODALITY_CLOSED))
                .thenReturn(List.of());
        when(modalityRequirementsRepository.findByModalityIdAndActiveTrue(1L)).thenReturn(List.of());
        when(studentModalityRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentModalityMemberRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StartGroupModalityResponse result = service.startStudentModalityGroup(1L, student);

        assertThat(result.eligible()).isTrue();
        assertThat(result.studentModalityName()).isEqualTo("PROYECTO DE GRADO");
    }

    @Test
    @DisplayName("Una modalidad en curso (PROPOSAL_APPROVED) sigue bloqueando el inicio")
    void inProgressModalityStillBlocks() {
        StudentModality previous = StudentModality.builder()
                .id(50L)
                .status(ModalityProcessStatus.PROPOSAL_APPROVED)
                .build();
        StudentModalityMember member = StudentModalityMember.builder()
                .studentModality(previous)
                .student(student)
                .status(MemberStatus.ACTIVE)
                .build();
        when(studentModalityMemberRepository.findByStudentIdAndStatus(300L, MemberStatus.ACTIVE))
                .thenReturn(List.of(member));

        assertThatThrownBy(() -> service.startStudentModalityGroup(1L, student))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Ya tienes una modalidad de grado en curso");
    }
}