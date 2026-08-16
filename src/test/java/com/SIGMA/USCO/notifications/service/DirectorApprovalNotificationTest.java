package com.SIGMA.USCO.notifications.service;

import com.SIGMA.USCO.Modalities.entity.DegreeModality;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.event.ModalityEvent;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.ProgramDegreeModality;
import com.SIGMA.USCO.documents.entity.RequiredDocument;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.listeners.DirectorNotificationListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MODALITY_APPROVED_BY_EXAMINERS: el director recibe la notificación")
class DirectorApprovalNotificationTest {

    @Mock
    private StudentModalityRepository studentModalityRepository;
    @Mock
    private StudentDocumentRepository studentDocumentRepository;
    @Mock
    private NotificationFactory notificationFactory;

    @InjectMocks
    private DirectorNotificationListener listener;

    private StudentModality modalityWithDirector(User director, User leader) {
        DegreeModality degreeModality = DegreeModality.builder().name("Proyecto de grado").build();
        ProgramDegreeModality pdm = ProgramDegreeModality.builder().degreeModality(degreeModality).build();
        AcademicProgram program = AcademicProgram.builder().name("Ingeniería de Software").build();
        return StudentModality.builder()
                .id(1L)
                .projectDirector(director)
                .leader(leader)
                .programDegreeModality(pdm)
                .academicProgram(program)
                .build();
    }

    @Test
    @DisplayName("Director con director asignado: buildAndDispatch con recipientType PROJECT_DIRECTOR")
    void notifiesDirectorWhenAssigned() {
        User director = User.builder().id(7L).name("Director").lastName("Software").build();
        User examiner = User.builder().id(9L).name("Jurado").lastName("Dos").build();
        DegreeModality degreeModality = DegreeModality.builder().name("Proyecto de grado").build();
        ProgramDegreeModality pdm = ProgramDegreeModality.builder().degreeModality(degreeModality).build();
        AcademicProgram program = AcademicProgram.builder().name("Ingeniería de Software").build();
        StudentModality sm = StudentModality.builder()
                .id(1L)
                .projectDirector(director)
                .programDegreeModality(pdm)
                .academicProgram(program)
                .build();
        when(studentModalityRepository.findById(1L)).thenReturn(Optional.of(sm));

        listener.handleEvent(new ModalityEvent(NotificationType.MODALITY_APPROVED_BY_EXAMINERS, 1L, 9L,
                Map.of(ModalityEvent.KEY_EXAMINER_ID, examiner.getId())));

        ArgumentCaptor<User> recipient = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notificationFactory).buildAndDispatch(
                any(NotificationType.class),
                any(NotificationRecipientType.class),
                recipient.capture(),
                any(StudentModality.class),
                subject.capture(),
                message.capture());
        assertThat(recipient.getValue().getId()).isEqualTo(7L);
        assertThat(subject.getValue()).contains("aprobada por jurado");
        assertThat(message.getValue()).contains("APROBADA por el jurado evaluador");
    }

    @Test
    @DisplayName("Modalidad sin director: no se despacha notificación")
    void doesNotNotifyWithoutDirector() {
        StudentModality sm = StudentModality.builder().id(2L).build();
        when(studentModalityRepository.findById(2L)).thenReturn(Optional.of(sm));

        listener.handleEvent(new ModalityEvent(NotificationType.MODALITY_APPROVED_BY_EXAMINERS, 2L, 9L,
                Map.of(ModalityEvent.KEY_EXAMINER_ID, 9L)));

        verify(notificationFactory, never()).buildAndDispatch(
                any(NotificationType.class),
                any(NotificationRecipientType.class),
                any(User.class),
                any(StudentModality.class),
                any(String.class),
                any(String.class));
    }

    @Test
    @DisplayName("CORRECTIONS_REQUESTED de jurado: el director recibe la notificación")
    void notifiesDirectorOnExaminerCorrectionsRequested() {
        User director = User.builder().id(7L).name("Director").lastName("Software").build();
        User leader = User.builder().id(10L).name("Estudiante").lastName("Prueba").build();
        StudentModality sm = modalityWithDirector(director, leader);
        when(studentModalityRepository.findById(1L)).thenReturn(Optional.of(sm));
        StudentDocument document = StudentDocument.builder()
                .id(55L)
                .documentConfig(RequiredDocument.builder().documentName("Propuesta de grado").build())
                .build();
        when(studentDocumentRepository.findById(55L)).thenReturn(Optional.of(document));

        listener.handleEvent(new ModalityEvent(NotificationType.DOCUMENT_CORRECTIONS_REQUESTED, 1L, 9L,
                Map.of(
                        ModalityEvent.KEY_STUDENT_DOCUMENT_ID, 55L,
                        ModalityEvent.KEY_OBSERVATIONS, "Ajustar sección de objetivos.",
                        ModalityEvent.KEY_REQUESTED_BY, NotificationRecipientType.EXAMINER.name()
                )));

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notificationFactory).buildAndDispatch(
                any(NotificationType.class),
                any(NotificationRecipientType.class),
                any(User.class),
                any(StudentModality.class),
                subject.capture(),
                message.capture());
        assertThat(subject.getValue()).contains("Correcciones solicitadas");
        assertThat(message.getValue()).contains("jurado evaluador");
    }

    @Test
    @DisplayName("CORRECTIONS_REQUESTED que NO es de jurado: el director NO se notifica")
    void doesNotNotifyDirectorOnProgramHeadCorrections() {
        User director = User.builder().id(7L).name("Director").lastName("Software").build();
        StudentModality sm = modalityWithDirector(director, User.builder().id(10L).name("E").lastName("S").build());
        when(studentModalityRepository.findById(1L)).thenReturn(Optional.of(sm));

        listener.handleEvent(new ModalityEvent(NotificationType.DOCUMENT_CORRECTIONS_REQUESTED, 1L, 9L,
                Map.of(
                        ModalityEvent.KEY_STUDENT_DOCUMENT_ID, 55L,
                        ModalityEvent.KEY_OBSERVATIONS, "Ajustar.",
                        ModalityEvent.KEY_REQUESTED_BY, NotificationRecipientType.PROGRAM_HEAD.name()
                )));

        verify(notificationFactory, never()).buildAndDispatch(
                any(NotificationType.class),
                any(NotificationRecipientType.class),
                any(User.class),
                any(StudentModality.class),
                any(String.class),
                any(String.class));
    }

    @Test
    @DisplayName("CORRECTION_REJECTED_FINAL: el director recibe la notificación")
    void notifiesDirectorOnCorrectionRejectedFinal() {
        User director = User.builder().id(7L).name("Director").lastName("Software").build();
        StudentModality sm = modalityWithDirector(director, User.builder().id(10L).name("Estudiante").lastName("Prueba").build());
        when(studentModalityRepository.findById(1L)).thenReturn(Optional.of(sm));

        listener.handleEvent(new ModalityEvent(NotificationType.CORRECTION_REJECTED_FINAL, 1L, 9L,
                Map.of(
                        ModalityEvent.KEY_DOCUMENT_NAME, "Documento final",
                        ModalityEvent.KEY_REASON, "No cumple con los lineamientos."
                )));

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notificationFactory).buildAndDispatch(
                any(NotificationType.class),
                any(NotificationRecipientType.class),
                any(User.class),
                any(StudentModality.class),
                subject.capture(),
                message.capture());
        assertThat(subject.getValue()).contains("Cancelación");
        assertThat(message.getValue()).contains("cancelación definitiva");
    }

    @Test
    @DisplayName("TIEBREAKER_REQUIRED: el director recibe la notificación")
    void notifiesDirectorOnTiebreakerRequired() {
        User director = User.builder().id(7L).name("Director").lastName("Software").build();
        StudentModality sm = modalityWithDirector(director, User.builder().id(10L).name("Estudiante").lastName("Prueba").build());
        when(studentModalityRepository.findById(1L)).thenReturn(Optional.of(sm));

        listener.handleEvent(new ModalityEvent(NotificationType.DOCUMENT_REVIEW_TIEBREAKER_REQUIRED, 1L, 9L,
                Map.of(ModalityEvent.KEY_DOCUMENT_NAME, "Propuesta")));

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notificationFactory).buildAndDispatch(
                any(NotificationType.class),
                any(NotificationRecipientType.class),
                any(User.class),
                any(StudentModality.class),
                subject.capture(),
                message.capture());
        assertThat(subject.getValue()).contains("desempate");
        assertThat(message.getValue()).contains("jurado de desempate");
    }

    @Test
    @DisplayName("MODALITY_READY_FOR_DEFENSE: el director recibe la notificación")
    void notifiesDirectorOnModalityReadyForDefense() {
        User director = User.builder().id(7L).name("Director").lastName("Software").build();
        StudentModality sm = modalityWithDirector(director, User.builder().id(10L).name("Estudiante").lastName("Prueba").build());
        when(studentModalityRepository.findById(1L)).thenReturn(Optional.of(sm));

        listener.handleEvent(new ModalityEvent(NotificationType.MODALITY_READY_FOR_DEFENSE, 1L, 9L, Map.of()));

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notificationFactory).buildAndDispatch(
                any(NotificationType.class),
                any(NotificationRecipientType.class),
                any(User.class),
                any(StudentModality.class),
                subject.capture(),
                message.capture());
        assertThat(subject.getValue()).contains("lista para revisión final por parte de los jurados");
        assertThat(message.getValue()).contains("lista para revisión final por parte de los jurados");
    }
}