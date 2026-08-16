package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.entity.Seminar;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.entity.enums.SeminarStatus;
import com.SIGMA.USCO.Modalities.repository.SeminarRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Modalities.dto.response.CancelSeminarResponse;
import com.SIGMA.USCO.Users.entity.ProgramAuthority;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.ProgramDegreeModality;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.Modalities.entity.DegreeModality;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.Modalities.event.ModalityEvent;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("T1.11 - Regresión T0.11: cancelSeminar solo transiciona modalidades de seminario en curso")
class SeminarCancellationFilterTest {

    @Mock
    private SeminarRepository seminarRepository;
    @Mock
    private StudentModalityRepository studentModalityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private ProgramAuthorityRepository programAuthorityRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private ModalityStatusTransition modalityStatusTransition;

    @InjectMocks
    private SeminarModalityService service;

    private User programHead;
    private Seminar seminar;
    private StudentModality seminarInProgress;
    private StudentModality seminarFinalized;
    private StudentModality internshipInProgress;

    @BeforeEach
    void setUp() {
        programHead = User.builder()
                .id(500L)
                .name("Jefe")
                .lastName("Programa")
                .email("jefe@usco.edu.co")
                .build();

        AcademicProgram program = AcademicProgram.builder().id(1L).name("INGENIERIA DE SOFTWARE").build();

        ProgramAuthority authority = ProgramAuthority.builder()
                .id(1L)
                .academicProgram(program)
                .role(ProgramRole.PROGRAM_HEAD)
                .build();

        seminar = Seminar.builder()
                .id(1L)
                .name("Seminario 2026-1")
                .academicProgram(program)
                .status(SeminarStatus.OPEN)
                .build();

        User studentUser = User.builder()
                .id(300L)
                .name("Ana")
                .lastName("Perez")
                .email("20221204357@usco.edu.co")
                .build();

        StudentProfile enrolled = StudentProfile.builder()
                .id(300L)
                .user(studentUser)
                .build();
        seminar.getEnrolledStudents().add(enrolled);

        DegreeModality seminarModality = DegreeModality.builder().id(7L).name("SEMINARIO DE GRADO").build();
        DegreeModality internshipModality = DegreeModality.builder().id(8L).name("PRÁCTICA PROFESIONAL").build();

        seminarInProgress = StudentModality.builder()
                .id(10L)
                .status(ModalityProcessStatus.PROPOSAL_APPROVED)
                .programDegreeModality(ProgramDegreeModality.builder().degreeModality(seminarModality).build())
                .build();
        seminarFinalized = StudentModality.builder()
                .id(11L)
                .status(ModalityProcessStatus.GRADED_APPROVED)
                .programDegreeModality(ProgramDegreeModality.builder().degreeModality(seminarModality).build())
                .build();
        internshipInProgress = StudentModality.builder()
                .id(12L)
                .status(ModalityProcessStatus.PROPOSAL_APPROVED)
                .programDegreeModality(ProgramDegreeModality.builder().degreeModality(internshipModality).build())
                .build();

        when(programAuthorityRepository.findByUser_IdAndRole(500L, ProgramRole.PROGRAM_HEAD))
                .thenReturn(List.of(authority));
        when(seminarRepository.findById(1L)).thenReturn(Optional.of(seminar));
        when(seminarRepository.findEnrolledStudentsBySeminarId(1L)).thenReturn(List.of(enrolled));
        when(studentModalityRepository.findByLeaderId(300L))
                .thenReturn(List.of(seminarInProgress, seminarFinalized, internshipInProgress));
        when(userRepository.findById(300L)).thenReturn(Optional.of(studentUser));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(programHead, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("T0.11: solo se transiciona la modalidad de seminario en curso (SEMINARIO DE GRADO + PROPOSAL_APPROVED)")
    void cancelSeminarTransitionsOnlySeminarInProgressModality() {
        CancelSeminarResponse result = service.cancelSeminar(1L, "Baja matrícula", programHead);

        ArgumentCaptor<StudentModality> modalityCaptor = ArgumentCaptor.forClass(StudentModality.class);
        ArgumentCaptor<String> observationsCaptor = ArgumentCaptor.forClass(String.class);
        verify(modalityStatusTransition, times(1)).transition(
                modalityCaptor.capture(),
                eq(ModalityProcessStatus.MODALITY_CANCELLED),
                isNull(),
                observationsCaptor.capture());

        assertThat(modalityCaptor.getValue()).isSameAs(seminarInProgress);
        assertThat(observationsCaptor.getValue())
                .contains("Seminario 2026-1")
                .startsWith("Modalidad cancelada por cancelación del seminario:");

        assertThat(result.success()).isTrue();
        assertThat(result.emailsSent()).isEqualTo(1);
    }

    @Test
    @DisplayName("T0.11: la modalidad finalizada (GRADED_APPROVED) y la de otro tipo (PRÁCTICA PROFESIONAL) no se tocan")
    void cancelSeminarLeavesFinalizedAndOtherTypeModalitiesUntouched() {
        service.cancelSeminar(1L, "Baja matrícula", programHead);

        ArgumentCaptor<StudentModality> modalityCaptor = ArgumentCaptor.forClass(StudentModality.class);
        verify(modalityStatusTransition, times(1)).transition(
                modalityCaptor.capture(),
                eq(ModalityProcessStatus.MODALITY_CANCELLED),
                isNull(),
                anyString());

        assertThat(modalityCaptor.getAllValues()).containsExactly(seminarInProgress);
    }

    @Test
    @DisplayName("T0.11: se notifica al estudiante inscrito con el evento SEMINAR_CANCELLED")
    void cancelSeminarNotifiesEnrolledStudent() {
        service.cancelSeminar(1L, "Baja matrícula", programHead);

        ArgumentCaptor<ModalityEvent> eventCaptor = ArgumentCaptor.forClass(ModalityEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());

        ModalityEvent event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo(NotificationType.SEMINAR_CANCELLED);
        assertThat(event.getPayload())
                .containsEntry(ModalityEvent.KEY_RECIPIENT_EMAIL, "20221204357@usco.edu.co")
                .containsEntry(ModalityEvent.KEY_SEMINAR_NAME, "Seminario 2026-1");
    }
}