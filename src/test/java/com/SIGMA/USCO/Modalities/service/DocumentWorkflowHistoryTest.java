package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.repository.DefenseExaminerRepository;
import com.SIGMA.USCO.Modalities.repository.DegreeModalityRepository;
import com.SIGMA.USCO.Modalities.repository.ModalityRequirementsRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Modalities.dto.response.ApproveFinalModalityResponse;
import com.SIGMA.USCO.Modalities.dto.response.CloseModalityResponse;
import com.SIGMA.USCO.Modalities.dto.response.DocumentsAcceptedForCommitteeResponse;
import com.SIGMA.USCO.Modalities.dto.response.RejectFinalModalityResponse;
import com.SIGMA.USCO.Modalities.dto.response.RequiredDocumentsUploadedResponse;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.repository.ProgramDegreeModalityRepository;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.shared.util.ResourceAccessPolicy;
import com.SIGMA.USCO.documents.repository.ExaminerDocumentReviewRepository;
import com.SIGMA.USCO.documents.repository.FinalDocumentEvaluationRepository;
import com.SIGMA.USCO.documents.repository.ProposalEvaluationRepository;
import com.SIGMA.USCO.documents.repository.RequiredDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentStatusHistoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("T1.6 - Regresión T0.2/T0.3/T0.4: observaciones del historial en decisiones finales del comité")
class DocumentWorkflowHistoryTest {

    @Mock
    private DegreeModalityRepository degreeModalityRepository;
    @Mock
    private DefenseExaminerRepository defenseExaminerRepository;
    @Mock
    private ModalityRequirementsRepository modalityRequirementsRepository;
    @Mock
    private RequiredDocumentRepository requiredDocumentRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private StudentModalityRepository studentModalityRepository;
    @Mock
    private StudentModalityMemberRepository studentModalityMemberRepository;
    @Mock
    private StudentDocumentRepository studentDocumentRepository;
    @Mock
    private StudentDocumentStatusHistoryRepository documentHistoryRepository;
    @Mock
    private ProgramDegreeModalityRepository programDegreeModalityRepository;
    @Mock
    private ProgramAuthorityRepository programAuthorityRepository;
    @Mock
    private ProposalEvaluationRepository proposalEvaluationRepository;
    @Mock
    private FinalDocumentEvaluationRepository secondaryDocumentEvaluationRepository;
    @Mock
    private ExaminerDocumentReviewRepository examinerDocumentReviewRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private ResourceAccessPolicy resourceAccessPolicy;
    @Mock
    private ModalityStatusTransition modalityStatusTransition;
    @Mock
    private ModalityDocumentService modalityDocumentService;

    @InjectMocks
    private DocumentWorkflowService service;

    private User committeeMember;
    private AcademicProgram program;
    private StudentModality modality;

    @BeforeEach
    void setUp() {
        committeeMember = User.builder()
                .id(5L)
                .name("Miembro")
                .lastName("Comité")
                .email("comite@usco.edu.co")
                .build();

        program = AcademicProgram.builder().id(1L).name("INGENIERIA DE SOFTWARE").build();

        modality = StudentModality.builder()
                .id(1L)
                .academicProgram(program)
                .leader(User.builder().id(100L).name("Ana").lastName("Perez").build())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(committeeMember, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("T0.2: closeModalityByCommittee registra el motivo y no el nombre del estado previo")
    void closeModalityRecordsReasonInsteadOfPreviousStatusName() {
        modality.setStatus(ModalityProcessStatus.PROPOSAL_APPROVED);
        when(studentModalityRepository.findById(1L)).thenReturn(Optional.of(modality));
        when(programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                eq(5L), eq(1L), eq(ProgramRole.PROGRAM_CURRICULUM_COMMITTEE))).thenReturn(true);

        CloseModalityResponse result = service.closeModalityByCommittee(1L, "Incumplimiento de cronograma", committeeMember);

        ArgumentCaptor<String> observationsCaptor = ArgumentCaptor.forClass(String.class);
        verify(modalityStatusTransition).transition(
                eq(modality), eq(ModalityProcessStatus.MODALITY_CLOSED), eq(committeeMember), observationsCaptor.capture());

        assertThat(observationsCaptor.getValue())
                .contains("Incumplimiento de cronograma")
                .doesNotContain("PROPOSAL_APPROVED")
                .doesNotContain("Propuesta aprobada");
        assertThat(result.previousStatus()).isEqualTo(ModalityProcessStatus.PROPOSAL_APPROVED);
        assertThat(result.newStatus()).isEqualTo(ModalityProcessStatus.MODALITY_CLOSED);
    }

    @Test
    @DisplayName("T0.3: rejectFinalModalityByCommittee registra la razón y no el nombre del estado previo")
    void rejectFinalModalityRecordsReasonInsteadOfPreviousStatusName() {
        modality.setStatus(ModalityProcessStatus.APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE);
        when(studentModalityRepository.findById(1L)).thenReturn(Optional.of(modality));
        when(programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                eq(5L), eq(1L), eq(ProgramRole.PROGRAM_CURRICULUM_COMMITTEE))).thenReturn(true);
        when(modalityDocumentService.validateAllRequiredDocumentsUploaded(1L))
                .thenReturn(new RequiredDocumentsUploadedResponse(true, 0, 0, List.of(), 0));
        when(modalityDocumentService.validateAllDocumentsAcceptedForCommittee(1L))
                .thenReturn(new DocumentsAcceptedForCommitteeResponse(true, List.of(), 0, 0));
        when(studentModalityMemberRepository.findByStudentModalityIdAndStatus(1L, MemberStatus.ACTIVE))
                .thenReturn(List.of());

        RejectFinalModalityResponse result = service.rejectFinalModalityByCommittee(1L, "Incumplimiento de cronograma", committeeMember);

        ArgumentCaptor<String> observationsCaptor = ArgumentCaptor.forClass(String.class);
        verify(modalityStatusTransition).transition(
                eq(modality), eq(ModalityProcessStatus.GRADED_FAILED), eq(committeeMember), observationsCaptor.capture());

        assertThat(observationsCaptor.getValue())
                .contains("Incumplimiento de cronograma")
                .doesNotContain("APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE")
                .doesNotContain("Aprobado por Comité de Currículo");
        assertThat(result.previousStatus()).isEqualTo(ModalityProcessStatus.APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE);
    }

    @Test
    @DisplayName("T0.4: approveFinalModalityByCommittee con observaciones nulas no escribe 'null'")
    void approveFinalModalityWithNullObservationsDoesNotWriteNullText() {
        modality.setStatus(ModalityProcessStatus.READY_FOR_DIRECTOR_ASSIGNMENT);
        when(studentModalityRepository.findById(1L)).thenReturn(Optional.of(modality));
        when(programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                eq(5L), eq(1L), eq(ProgramRole.PROGRAM_CURRICULUM_COMMITTEE))).thenReturn(true);
        when(modalityDocumentService.validateAllRequiredDocumentsUploaded(1L))
                .thenReturn(new RequiredDocumentsUploadedResponse(true, 0, 0, List.of(), 0));
        when(modalityDocumentService.validateAllDocumentsAcceptedForCommittee(1L))
                .thenReturn(new DocumentsAcceptedForCommitteeResponse(true, List.of(), 0, 0));
        when(studentModalityMemberRepository.findByStudentModalityIdAndStatus(1L, MemberStatus.ACTIVE))
                .thenReturn(List.of());

        ApproveFinalModalityResponse result = service.approveFinalModalityByCommittee(1L, null, committeeMember);

        ArgumentCaptor<String> observationsCaptor = ArgumentCaptor.forClass(String.class);
        verify(modalityStatusTransition).transition(
                eq(modality), eq(ModalityProcessStatus.GRADED_APPROVED), eq(committeeMember), observationsCaptor.capture());

        assertThat(observationsCaptor.getValue())
                .contains("aprobada definitivamente")
                .doesNotContain("null")
                .doesNotContain("Observaciones: null");
        assertThat(result.previousStatus()).isEqualTo(ModalityProcessStatus.READY_FOR_DIRECTOR_ASSIGNMENT);
        assertThat(result.observations()).isEqualTo("Sin observaciones");
    }

    @Test
    @DisplayName("closeModalityByCommittee rechaza motivo en blanco sin tocar transición")
    void closeModalityWithBlankReasonThrowsValidation() {
        assertThrows(ValidationException.class,
                () -> service.closeModalityByCommittee(1L, "  ", committeeMember));

        verify(modalityStatusTransition, never()).transition(any(), any(), any(), any());
    }
}