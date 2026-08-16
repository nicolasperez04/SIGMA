package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.entity.DefenseEvaluationCriteria;
import com.SIGMA.USCO.Modalities.entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.repository.DefenseEvaluationCriteriaRepository;
import com.SIGMA.USCO.Modalities.repository.DefenseExaminerRepository;
import com.SIGMA.USCO.Modalities.repository.ModalityProcessStatusHistoryRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.common.exception.ForbiddenException;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.common.web.OperationResultResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("F11.1 - Reset de evaluación de defensa por el comité de currículo (bug 8)")
class DefenseEvaluationResetTest {

    @Mock
    private DefenseExaminerRepository defenseExaminerRepository;
    @Mock
    private DefenseEvaluationCriteriaRepository defenseEvaluationCriteriaRepository;
    @Mock
    private StudentModalityRepository studentModalityRepository;
    @Mock
    private ModalityProcessStatusHistoryRepository historyRepository;
    @Mock
    private ProgramAuthorityRepository programAuthorityRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private ModalityStatusTransition modalityStatusTransition;

    @InjectMocks
    private DefenseEvaluationService service;

    private User committee;
    private StudentModality modality;

    private void setUp(ModalityProcessStatus status, AcademicDistinction distinction, LocalDateTime defenseDate) {
        committee = User.builder().id(600L).name("Comite").lastName("Curriculo").build();
        modality = StudentModality.builder()
                .id(226L)
                .academicProgram(AcademicProgram.builder().id(5L).build())
                .status(status)
                .academicDistinction(distinction)
                .defenseDate(defenseDate)
                .build();
        when(studentModalityRepository.findById(226L)).thenReturn(Optional.of(modality));
        when(programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                600L, 5L, ProgramRole.PROGRAM_CURRICULUM_COMMITTEE)).thenReturn(true);
    }

    @Test
    @DisplayName("Restablece borrando criterios y volviendo a EXAMINERS_ASSIGNED sin fecha de defensa")
    void resetClearsCriteriaAndReturnsToExaminersAssigned() {
        setUp(ModalityProcessStatus.UNDER_EVALUATION_PRIMARY_EXAMINERS, null, null);
        when(defenseEvaluationCriteriaRepository.findByStudentModalityId(226L))
                .thenReturn(List.of(DefenseEvaluationCriteria.builder().id(1L).build()));

        OperationResultResponse result = service.resetDefenseEvaluation(226L, committee);

        verify(defenseEvaluationCriteriaRepository).deleteAll(any());
        verify(modalityStatusTransition).transition(eq(modality), eq(ModalityProcessStatus.EXAMINERS_ASSIGNED), eq(committee), any());
        assertThat(result.success()).isTrue();
        assertThat(result.studentModalityId()).isEqualTo(226L);
    }

    @Test
    @DisplayName("Con fecha de defensa vuelve a DEFENSE_SCHEDULED")
    void resetWithDefenseDateReturnsToDefenseScheduled() {
        setUp(ModalityProcessStatus.UNDER_EVALUATION_TIEBREAKER, null, LocalDateTime.now().plusDays(2));
        when(defenseEvaluationCriteriaRepository.findByStudentModalityId(226L))
                .thenReturn(List.of(DefenseEvaluationCriteria.builder().id(1L).build()));

        service.resetDefenseEvaluation(226L, committee);

        verify(modalityStatusTransition).transition(eq(modality), eq(ModalityProcessStatus.DEFENSE_SCHEDULED), eq(committee), any());
    }

    @Test
    @DisplayName("Limpia la distinción pendiente de desempate al restablecer")
    void resetClearsPendingTiebreakerDistinction() {
        setUp(ModalityProcessStatus.DISAGREEMENT_REQUIRES_TIEBREAKER, AcademicDistinction.DISAGREEMENT_PENDING_TIEBREAKER, null);
        when(defenseEvaluationCriteriaRepository.findByStudentModalityId(226L))
                .thenReturn(List.of(DefenseEvaluationCriteria.builder().id(1L).build()));

        service.resetDefenseEvaluation(226L, committee);

        assertThat(modality.getAcademicDistinction()).isNull();
        verify(studentModalityRepository).save(modality);
    }

    @Test
    @DisplayName("Sin criterios y sin estado atascado lanza ValidationException")
    void resetWithoutPendingEvaluationThrows() {
        setUp(ModalityProcessStatus.DEFENSE_COMPLETED, null, null);
        when(defenseEvaluationCriteriaRepository.findByStudentModalityId(226L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.resetDefenseEvaluation(226L, committee))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("no tiene una evaluación pendiente");

        verify(defenseEvaluationCriteriaRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("Un usuario sin rol de comité del programa recibe ForbiddenException")
    void resetByNonCommitteeThrows() {
        setUp(ModalityProcessStatus.UNDER_EVALUATION_PRIMARY_EXAMINERS, null, null);
        when(programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                600L, 5L, ProgramRole.PROGRAM_CURRICULUM_COMMITTEE)).thenReturn(false);

        assertThatThrownBy(() -> service.resetDefenseEvaluation(226L, committee))
                .isInstanceOf(ForbiddenException.class);

        verify(defenseEvaluationCriteriaRepository, never()).findByStudentModalityId(226L);
        verify(defenseEvaluationCriteriaRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("La modalidad inexistente lanza NotFoundException")
    void resetOfUnknownModalityThrows() {
        when(studentModalityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetDefenseEvaluation(999L, committee))
                .isInstanceOf(com.SIGMA.USCO.common.exception.NotFoundException.class);
    }
}