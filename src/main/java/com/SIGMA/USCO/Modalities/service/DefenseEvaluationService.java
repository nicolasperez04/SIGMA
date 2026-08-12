package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.Entity.DefenseEvaluationCriteria;
import com.SIGMA.USCO.Modalities.Entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.Entity.ModalityProcessStatusHistory;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.Entity.enums.DefenseRubricType;
import com.SIGMA.USCO.Modalities.Entity.enums.ExaminerType;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.ProposedMention;
import com.SIGMA.USCO.Modalities.Repository.DefenseEvaluationCriteriaRepository;
import com.SIGMA.USCO.Modalities.Repository.DefenseExaminerRepository;
import com.SIGMA.USCO.Modalities.Repository.ModalityProcessStatusHistoryRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Modalities.dto.DefenseEvaluationCriteriaDTO;
import com.SIGMA.USCO.Modalities.dto.ExaminerEvaluationDTO;
import com.SIGMA.USCO.Modalities.dto.response.FinalDefenseResponse;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.Entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.event.ModalityEvent;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.security.SecurityUtils;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import com.SIGMA.USCO.common.exception.ForbiddenException;
import com.SIGMA.USCO.common.exception.ValidationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefenseEvaluationService {

    private final DefenseExaminerRepository defenseExaminerRepository;
    private final DefenseEvaluationCriteriaRepository defenseEvaluationCriteriaRepository;
    private final StudentModalityRepository studentModalityRepository;
    private final ModalityProcessStatusHistoryRepository historyRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ModalityStatusTransition modalityStatusTransition;

    @Transactional
    public Map<String, Object> registerFinalDefenseEvaluation(Long studentModalityId, ExaminerEvaluationDTO evaluationDTO) {

        User examiner = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        DefenseExaminer defenseExaminer = defenseExaminerRepository
                .findByStudentModalityIdAndExaminerId(studentModalityId, examiner.getId())
                .orElseThrow(() -> new NotFoundException(
                        "No está asignado como jurado de esta sustentación"
                ));

        if (defenseEvaluationCriteriaRepository.existsByDefenseExaminerId(defenseExaminer.getId())) {
            throw new ValidationException("Ya ha registrado su evaluación para esta sustentación");
        }

        if (studentModality.getStatus() != ModalityProcessStatus.DEFENSE_COMPLETED &&
                studentModality.getStatus() != ModalityProcessStatus.READY_FOR_DEFENSE &&
                studentModality.getStatus() != ModalityProcessStatus.EXAMINERS_ASSIGNED &&
                studentModality.getStatus() != ModalityProcessStatus.UNDER_EVALUATION_PRIMARY_EXAMINERS &&
                studentModality.getStatus() != ModalityProcessStatus.UNDER_EVALUATION_TIEBREAKER &&
                studentModality.getStatus() != ModalityProcessStatus.DEFENSE_SCHEDULED &&
                studentModality.getStatus() != ModalityProcessStatus.DISAGREEMENT_REQUIRES_TIEBREAKER) {

            throw new ValidationException("La modalidad no está en estado válido para registrar evaluaciones");
        }

        // Punto 3: El jurado de desempate SOLO puede evaluar si hay desacuerdo entre primarios
        if (defenseExaminer.getExaminerType() == ExaminerType.TIEBREAKER_EXAMINER &&
                studentModality.getStatus() != ModalityProcessStatus.DISAGREEMENT_REQUIRES_TIEBREAKER) {
            throw new ValidationException("El jurado de desempate solo puede evaluar cuando existe desacuerdo entre los jurados principales (un jurado aprueba y el otro rechaza).");
        }

        // Los jurados primarios no pueden evaluar si ya hay desacuerdo resuelto al desempate
        if (defenseExaminer.getExaminerType() != ExaminerType.TIEBREAKER_EXAMINER &&
                studentModality.getStatus() == ModalityProcessStatus.DISAGREEMENT_REQUIRES_TIEBREAKER) {
            throw new ValidationException("Existe desacuerdo entre los jurados principales. Solo el jurado de desempate puede evaluar en este momento.");
        }

        // Validar nota
        if (evaluationDTO.getGrade() == null || evaluationDTO.getGrade() < 0.0 || evaluationDTO.getGrade() > 5.0) {
            throw new ValidationException("La calificación debe estar entre 0.0 y 5.0");
        }

        // Construir la entidad DefenseEvaluationCriteria con toda la información
        DefenseEvaluationCriteria.DefenseEvaluationCriteriaBuilder criteriaBuilder =
                DefenseEvaluationCriteria.builder()
                        .defenseExaminer(defenseExaminer)
                        .grade(evaluationDTO.getGrade())
                        .observations(evaluationDTO.getObservations())
                        .isFinalDecision(false)
                        .evaluatedAt(LocalDateTime.now());

        DefenseRubricType expectedRubricType = resolveDefenseRubricType(studentModality);
        DefenseEvaluationCriteriaDTO criteriaDTO = evaluationDTO.getEvaluationCriteria();

        if (criteriaDTO == null) {
            throw new ValidationException("Debe enviar la rúbrica de evaluación en el campo evaluationCriteria.");
        }

        if (criteriaDTO.getRubricType() != null && criteriaDTO.getRubricType() != expectedRubricType) {
            throw new ValidationException("El tipo de rúbrica enviado no coincide con la modalidad evaluada.");
        }

        if (expectedRubricType == DefenseRubricType.ENTREPRENEURSHIP) {
            if (criteriaDTO.getEntrepreneurshipPresentationSupportMaterial() == null
                    || criteriaDTO.getEntrepreneurshipCoherentBusinessObjectives() == null
                    || criteriaDTO.getEntrepreneurshipMethodologyTechnicalApproach() == null
                    || criteriaDTO.getEntrepreneurshipAnalyticalCreativeCapacity() == null
                    || criteriaDTO.getEntrepreneurshipDefenseSustentation() == null) {
                throw new ValidationException("Para la modalidad de Emprendimiento y fortalecimiento de empresa debe enviar los 5 criterios específicos de la rúbrica empresarial.");
            }

            criteriaBuilder
                    .rubricType(DefenseRubricType.ENTREPRENEURSHIP)
                    .entrepreneurshipPresentationSupportMaterial(criteriaDTO.getEntrepreneurshipPresentationSupportMaterial())
                    .entrepreneurshipCoherentBusinessObjectives(criteriaDTO.getEntrepreneurshipCoherentBusinessObjectives())
                    .entrepreneurshipMethodologyTechnicalApproach(criteriaDTO.getEntrepreneurshipMethodologyTechnicalApproach())
                    .entrepreneurshipAnalyticalCreativeCapacity(criteriaDTO.getEntrepreneurshipAnalyticalCreativeCapacity())
                    .entrepreneurshipDefenseSustentation(criteriaDTO.getEntrepreneurshipDefenseSustentation())
                    .proposedMention(criteriaDTO.getProposedMention() != null
                            ? criteriaDTO.getProposedMention()
                            : ProposedMention.NONE)
                    // Se mapean también a la rúbrica estándar para mantener compatibilidad histórica
                    // con reportes/queries existentes que leen los 5 campos legacy.
                    .domainAndClarity(criteriaDTO.getEntrepreneurshipCoherentBusinessObjectives())
                    .synthesisAndCommunication(criteriaDTO.getEntrepreneurshipPresentationSupportMaterial())
                    .argumentationAndResponse(criteriaDTO.getEntrepreneurshipDefenseSustentation())
                    .innovationAndImpact(criteriaDTO.getEntrepreneurshipAnalyticalCreativeCapacity())
                    .professionalPresentation(criteriaDTO.getEntrepreneurshipMethodologyTechnicalApproach());
        } else {
            if (criteriaDTO.getDomainAndClarity() == null
                    || criteriaDTO.getSynthesisAndCommunication() == null
                    || criteriaDTO.getArgumentationAndResponse() == null
                    || criteriaDTO.getInnovationAndImpact() == null
                    || criteriaDTO.getProfessionalPresentation() == null) {
                throw new ValidationException("Para esta modalidad debe enviar los 5 criterios estándar de la rúbrica.");
            }

            criteriaBuilder
                    .rubricType(DefenseRubricType.STANDARD)
                    .domainAndClarity(criteriaDTO.getDomainAndClarity())
                    .synthesisAndCommunication(criteriaDTO.getSynthesisAndCommunication())
                    .argumentationAndResponse(criteriaDTO.getArgumentationAndResponse())
                    .innovationAndImpact(criteriaDTO.getInnovationAndImpact())
                    .professionalPresentation(criteriaDTO.getProfessionalPresentation())
                    .proposedMention(criteriaDTO.getProposedMention() != null
                            ? criteriaDTO.getProposedMention()
                            : ProposedMention.NONE);
        }

        DefenseEvaluationCriteria evaluation = criteriaBuilder.build();
        defenseEvaluationCriteriaRepository.save(evaluation);

        if (defenseExaminer.getExaminerType() == ExaminerType.TIEBREAKER_EXAMINER) {
            return processTiebreakerEvaluation(studentModality, evaluation, examiner);
        } else {

            return processPrimaryExaminerEvaluation(studentModality, evaluation, examiner);
        }
    }

    @Transactional
    public Map<String, Object> getFinalDefenseEvaluationForExaminer(Long studentModalityId) {
        User examiner = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        DefenseExaminer defenseExaminer = defenseExaminerRepository
                .findByStudentModalityIdAndExaminerId(studentModalityId, examiner.getId())
                .orElseThrow(() -> new NotFoundException(
                        "No está asignado como jurado de esta sustentación"
                ));

        DefenseEvaluationCriteria evaluation = defenseEvaluationCriteriaRepository
                .findByDefenseExaminerId(defenseExaminer.getId())
                .orElse(null);

        if (evaluation == null) {
            return (
                    Map.of(
                            "success", false,
                            "message", "No hay evaluación registrada para este jurado en esta modalidad"
                    )
            );
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("evaluationId", evaluation.getId());
        response.put("grade", evaluation.getGrade());
        response.put("approved", evaluation.getGrade() != null && evaluation.getGrade() >= 3.5);
        response.put("observations", evaluation.getObservations());
        response.put("evaluationDate", evaluation.getEvaluatedAt());
        response.put("isFinalDecision", evaluation.getIsFinalDecision());
        response.put("examinerType", defenseExaminer.getExaminerType());

        response.put("evaluationCriteria", buildDefenseCriteriaResponse(evaluation));

        return (response);
    }

    private Map<String, Object> processPrimaryExaminerEvaluation(StudentModality studentModality, DefenseEvaluationCriteria currentEvaluation, User examiner) {

        if (studentModality.getStatus() == ModalityProcessStatus.DEFENSE_COMPLETED) {
            studentModality.setStatus(ModalityProcessStatus.UNDER_EVALUATION_PRIMARY_EXAMINERS);
            studentModality.setUpdatedAt(LocalDateTime.now());
            studentModalityRepository.save(studentModality);
        }

        boolean bothEvaluated = defenseEvaluationCriteriaRepository
                .bothPrimaryExaminersHaveEvaluated(studentModality.getId());

        if (!bothEvaluated) {

            return (
                    Map.of(
                            "success", true,
                            "message", "Evaluación registrada correctamente. Esperando evaluación del otro jurado principal.",
                            "grade", currentEvaluation.getGrade(),
                            "approved", currentEvaluation.getGrade() >= 3.5
                    )
            );
        }

        List<DefenseEvaluationCriteria> primaryEvaluations = defenseEvaluationCriteriaRepository
                .findPrimaryEvaluationsByStudentModalityId(studentModality.getId());

        boolean hasConsensus = defenseEvaluationCriteriaRepository
                .primaryExaminersHaveConsensus(studentModality.getId());

        if (hasConsensus) {
            return applyFinalResultWithConsensus(studentModality, primaryEvaluations, examiner);
        } else {

            return requestTiebreakerExaminer(studentModality, primaryEvaluations, examiner);
        }
    }

    private Map<String, Object> applyFinalResultWithConsensus(StudentModality studentModality, List<DefenseEvaluationCriteria> primaryEvaluations, User examiner) {

        // La nota final es el promedio de las dos notas de los jurados principales (punto 4)
        Double averageGrade = defenseEvaluationCriteriaRepository
                .calculateAverageGradeOfPrimaryExaminers(studentModality.getId());

        primaryEvaluations.forEach(eval -> {
            eval.setIsFinalDecision(true);
            defenseEvaluationCriteriaRepository.save(eval);
        });

        // La aprobación se determina por nota: >= 3.5 = aprobado, < 3.5 = reprobado
        boolean approved = averageGrade != null && averageGrade >= 3.5;

        AcademicDistinction distinction;
        ModalityProcessStatus finalStatus;
        boolean pendingDistinctionReview = false;

        if (!approved) {
            distinction = AcademicDistinction.AGREED_REJECTED;
            finalStatus = ModalityProcessStatus.GRADED_FAILED;
        } else {
            // La mención solo se propone si AMBOS jurados coinciden unánimemente
            ProposedMention mention1 = primaryEvaluations.get(0).getProposedMention();
            ProposedMention mention2 = primaryEvaluations.get(1).getProposedMention();

            if (mention1 != null && mention2 != null && mention1 == mention2
                    && mention1 == ProposedMention.LAUREATE) {
                // Los jurados PROPONEN la mención Laureada → el comité debe decidir
                distinction = AcademicDistinction.PENDING_COMMITTEE_LAUREATE;
                finalStatus = ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW;
                pendingDistinctionReview = true;
            } else if (mention1 != null && mention2 != null && mention1 == mention2
                    && mention1 == ProposedMention.MERITORIOUS) {
                // Los jurados PROPONEN la mención Meritoria → el comité debe decidir
                distinction = AcademicDistinction.PENDING_COMMITTEE_MERITORIOUS;
                finalStatus = ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW;
                pendingDistinctionReview = true;
            } else {
                distinction = AcademicDistinction.AGREED_APPROVED;
                finalStatus = ModalityProcessStatus.GRADED_APPROVED;
            }
        }

        // Construir la observación con los argumentos de los jurados sobre la mención
        String mentionNotes = primaryEvaluations.stream()
                .filter(e -> e.getObservations() != null && !e.getObservations().isBlank())
                .map(e -> "Jurado " + (e.getDefenseExaminer().getExaminerType() != null
                        ? e.getDefenseExaminer().getExaminerType().name() : "") + ": " + e.getObservations())
                .collect(Collectors.joining(" | "));

        String observations;
        if (pendingDistinctionReview) {
            observations = String.format(
                    "CONSENSO entre jurados principales. Calificación final (promedio): %.2f. " +
                    "Resultado: APROBADO. Los jurados proponen la distinción: %s. " +
                    "PENDIENTE DE REVISIÓN por el Comité de Currículo. Argumentos: %s",
                    averageGrade,
                    ModalityServiceUtils.translateAcademicDistinction(distinction),
                    mentionNotes.isBlank() ? "Sin argumentos adicionales" : mentionNotes
            );
        } else {
            observations = String.format(
                    "CONSENSO entre jurados principales. Calificación final (promedio): %.2f. " +
                    "Resultado: %s. Distinción: %s",
                    averageGrade,
                    approved ? "APROBADO" : "REPROBADO",
                    ModalityServiceUtils.translateAcademicDistinction(distinction)
            );
        }

        studentModality.setAcademicDistinction(distinction);
        studentModality.setFinalGrade(averageGrade);
        modalityStatusTransition.transition(studentModality, finalStatus, examiner, observations);

        // Publicar siempre: incluso si la distinción queda pendiente de comité,
        // el estudiante debe recibir correo y acta de aprobación inicial.
        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.DEFENSE_COMPLETED, studentModality.getId(), examiner.getId(), Map.of(
                        ModalityEvent.KEY_FINAL_STATUS, finalStatus,
                        ModalityEvent.KEY_ACADEMIC_DISTINCTION, distinction,
                        ModalityEvent.KEY_OBSERVATIONS, observations
                ))
        );

        String message;
        if (pendingDistinctionReview) {
            message = "¡Felicitaciones! Tu modalidad de grado ha sido aprobada por consenso de los jurados. Los jurados han propuesto una distinción honorífica (" +
                    ModalityServiceUtils.translateAcademicDistinction(distinction) + "). El Comité de Currículo debe revisar y decidir si acepta o rechaza la distinción.";
        } else {
            message = approved ? "¡Felicitaciones! Tu modalidad de grado ha sido aprobada por consenso de los jurados." : "Tu modalidad de grado ha sido reprobada por consenso de los jurados.";
        }

        return (
                Map.of(
                        "exito", true,
                        "consenso", true,
                        "estadoFinal", finalStatus.name(),
                        "distincionAcademica", ModalityServiceUtils.translateAcademicDistinction(distinction),
                        "calificacionFinal", averageGrade,
                        "distincionPendienteRevision", pendingDistinctionReview,
                        "mensaje", message
                )
        );
    }

    private Map<String, Object> requestTiebreakerExaminer(StudentModality studentModality, List<DefenseEvaluationCriteria> primaryEvaluations, User examiner) {

        String observations = String.format(
                "DESACUERDO entre jurados principales. Jurado 1: %s (%.2f). Jurado 2: %s (%.2f). " +
                "Se requiere asignar un tercer jurado para desempatar.",
                primaryEvaluations.get(0).getGrade() >= 3.5 ? "APROBADO" : "REPROBADO",
                primaryEvaluations.get(0).getGrade(),
                primaryEvaluations.get(1).getGrade() >= 3.5 ? "APROBADO" : "REPROBADO",
                primaryEvaluations.get(1).getGrade()
        );

        studentModality.setAcademicDistinction(AcademicDistinction.DISAGREEMENT_PENDING_TIEBREAKER);
        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.DISAGREEMENT_REQUIRES_TIEBREAKER, examiner, observations);

        return (
                Map.of(
                        "success", true,
                        "hasConsensus", false,
                        "requiresTiebreaker", true,
                        "status", ModalityProcessStatus.DISAGREEMENT_REQUIRES_TIEBREAKER,
                        "message", "No hay consenso entre los jurados principales. Se requiere asignar un tercer jurado para desempatar."
                )
        );
    }

    private Map<String, Object> processTiebreakerEvaluation(StudentModality studentModality, DefenseEvaluationCriteria tiebreakerEvaluation, User examiner) {

        tiebreakerEvaluation.setIsFinalDecision(true);
        defenseEvaluationCriteriaRepository.save(tiebreakerEvaluation);

        // La aprobación se determina por nota: >= 3.5 = aprobado (punto 2 y 3)
        // La nota final es la del jurado de desempate (punto 5)
        double tiebreakerGrade = tiebreakerEvaluation.getGrade();
        boolean approved = tiebreakerGrade >= 3.5;

        AcademicDistinction distinction;
        ModalityProcessStatus finalStatus;
        boolean pendingDistinctionReview = false;

        if (!approved) {
            distinction = AcademicDistinction.TIEBREAKER_REJECTED;
            finalStatus = ModalityProcessStatus.GRADED_FAILED;
        } else {
            // La mención la determina el proposedMention del jurado de desempate
            ProposedMention tiebreakerMention = tiebreakerEvaluation.getProposedMention();
            if (tiebreakerMention == ProposedMention.LAUREATE) {
                // El jurado de desempate PROPONE la mención Laureada → el comité debe decidir
                distinction = AcademicDistinction.TIEBREAKER_PENDING_COMMITTEE_LAUREATE;
                finalStatus = ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW;
                pendingDistinctionReview = true;
            } else if (tiebreakerMention == ProposedMention.MERITORIOUS) {
                // El jurado de desempate PROPONE la mención Meritoria → el comité debe decidir
                distinction = AcademicDistinction.TIEBREAKER_PENDING_COMMITTEE_MERITORIOUS;
                finalStatus = ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW;
                pendingDistinctionReview = true;
            } else {
                distinction = AcademicDistinction.TIEBREAKER_APPROVED;
                finalStatus = ModalityProcessStatus.GRADED_APPROVED;
            }
        }

        String observations;
        if (pendingDistinctionReview) {
            String mentionNote = tiebreakerEvaluation.getObservations() != null
                    ? tiebreakerEvaluation.getObservations() : "Sin argumentos adicionales";
            observations = String.format(
                    "DESEMPATE resuelto por tercer jurado. Calificación final: %.2f. " +
                    "Resultado: APROBADO. El jurado de desempate propone la distinción: %s. " +
                    "PENDIENTE DE REVISIÓN por el Comité de Currículo. Argumento: %s",
                    tiebreakerGrade,
                    ModalityServiceUtils.translateProposedDistinction(distinction),
                    mentionNote
            );
        } else {
            observations = String.format(
                    "DESEMPATE resuelto por tercer jurado. Calificación final: %.2f. " +
                    "Resultado: %s. Distinción: %s",
                    tiebreakerGrade,
                    approved ? "APROBADO" : "REPROBADO",
                    ModalityServiceUtils.translateProposedDistinction(distinction)
            );
        }

        studentModality.setAcademicDistinction(distinction);
        studentModality.setFinalGrade(tiebreakerGrade);
        modalityStatusTransition.transition(studentModality, finalStatus, examiner, observations);

        // Publicar siempre: si queda pendiente de comité también se debe enviar acta inicial.
        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.DEFENSE_COMPLETED, studentModality.getId(), examiner.getId(), Map.of(
                        ModalityEvent.KEY_FINAL_STATUS, finalStatus,
                        ModalityEvent.KEY_ACADEMIC_DISTINCTION, distinction,
                        ModalityEvent.KEY_OBSERVATIONS, observations
                ))
        );

        String message;
        if (pendingDistinctionReview) {
            message = "Modalidad APROBADA por decisión del jurado de desempate. El jurado ha PROPUESTO la distinción (" +
                    ModalityServiceUtils.translateProposedDistinction(distinction) + "). El Comité de Currículo debe revisar y decidir si acepta o rechaza la distinción.";
        } else {
            message = approved ? "Modalidad APROBADA por decisión del jurado de desempate"
                    : "Modalidad REPROBADA por decisión del jurado de desempate";
        }

        return (
                Map.of(
                        "success", true,
                        "isTiebreaker", true,
                        "finalStatus", finalStatus,
                        "academicDistinction", distinction,
                        "finalGrade", tiebreakerGrade,
                        "pendingDistinctionReview", pendingDistinctionReview,
                        "message", message
                )
        );
    }

    @Transactional(readOnly = true)
    public FinalDefenseResponse getFinalDefenseResult(Long studentModalityId) {

        User user = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        Long academicProgramId =
                studentModality
                        .getProgramDegreeModality()
                        .getAcademicProgram()
                        .getId();

        boolean authorized =
                programAuthorityRepository
                        .existsByUser_IdAndAcademicProgram_IdAndRoleIn(
                                user.getId(),
                                academicProgramId,
                                List.of(
                                        ProgramRole.PROGRAM_HEAD,
                                        ProgramRole.PROGRAM_CURRICULUM_COMMITTEE
                                )
                        );

        if (!authorized) {
            throw new ForbiddenException("No tiene permiso para consultar el resultado final de esta modalidad");
        }

        if (studentModality.getStatus() != ModalityProcessStatus.GRADED_APPROVED &&
                studentModality.getStatus() != ModalityProcessStatus.GRADED_FAILED) {

            throw new ValidationException("La modalidad aún no tiene un resultado final registrado");
        }

        ModalityProcessStatus finalStatus = studentModality.getStatus();

        ModalityProcessStatusHistory history =
                historyRepository
                        .findTopByStudentModalityAndStatusOrderByChangeDateDesc(
                                studentModality,
                                finalStatus
                        )
                        .orElseThrow(() ->
                                new NotFoundException("No se encontró historial de evaluación final")
                        );

        List<DefenseExaminer> defenseExaminers = defenseExaminerRepository
                .findByStudentModalityId(studentModalityId);

        List<FinalDefenseResponse.ExaminerEvaluationDetail> examinerEvaluations = defenseExaminers.stream()
                .map(defenseExaminer -> {
                    DefenseEvaluationCriteria evaluation = defenseEvaluationCriteriaRepository
                            .findByDefenseExaminerId(defenseExaminer.getId())
                            .orElse(null);

                    if (evaluation == null) {
                        return null;
                    }

                    FinalDefenseResponse.CriteriaDetail criteriaDetail = buildFinalDefenseCriteriaDetail(evaluation);

                    return FinalDefenseResponse.ExaminerEvaluationDetail.builder()
                            .examinerName(defenseExaminer.getExaminer().getName() + " " +
                                        defenseExaminer.getExaminer().getLastName())
                            .examinerType(defenseExaminer.getExaminerType().name())
                            .grade(evaluation.getGrade())
                            .approved(evaluation.getGrade() != null && evaluation.getGrade() >= 3.5)
                            .observations(evaluation.getObservations())
                            .evaluationDate(evaluation.getEvaluatedAt())
                            .isFinalDecision(evaluation.getIsFinalDecision())
                            .evaluationCriteria(criteriaDetail)
                            .build();
                })
                .filter(detail -> detail != null)
                .toList();

        boolean hasConsensus = studentModality.getAcademicDistinction() != null &&
                              (studentModality.getAcademicDistinction().name().startsWith("AGREED_"));

        boolean wasTiebreaker = studentModality.getAcademicDistinction() != null &&
                               (studentModality.getAcademicDistinction().name().startsWith("TIEBREAKER_"));

        return (
                FinalDefenseResponse.builder()
                        .studentModalityId(studentModality.getId())
                        .studentName(
                                studentModality.getLeader().getName() + " " +
                                        studentModality.getLeader().getLastName()
                        )
                        .studentEmail(studentModality.getLeader().getEmail())
                        .finalStatus(finalStatus)
                        .approved(finalStatus == ModalityProcessStatus.GRADED_APPROVED)
                        .academicDistinction(studentModality.getAcademicDistinction())
                        .finalGrade(studentModality.getFinalGrade())
                        .observations(history.getObservations())
                        .evaluationDate(history.getChangeDate())
                        .evaluatedBy(
                                history.getResponsible() != null
                                        ? history.getResponsible().getName()
                                        : "Comité de currículo de programa"
                        )
                        .hasConsensus(hasConsensus)
                        .wasTiebreaker(wasTiebreaker)
                        .examinerEvaluations(examinerEvaluations)
                        .build()
        );
    }

    private DefenseRubricType resolveDefenseRubricType(StudentModality studentModality) {
        String modalityName = studentModality.getProgramDegreeModality().getDegreeModality().getName();
        String normalizedName = ModalityServiceUtils.normalizeText(modalityName);
        if ("emprendimiento y fortalecimiento de empresa".equals(normalizedName)) {
            return DefenseRubricType.ENTREPRENEURSHIP;
        }
        return DefenseRubricType.STANDARD;
    }

    private Map<String, Object> buildDefenseCriteriaResponse(DefenseEvaluationCriteria evaluation) {
        if (evaluation == null) {
            return null;
        }

        Map<String, Object> criteriaMap = new LinkedHashMap<>();
        DefenseRubricType rubricType = evaluation.getRubricType() != null
                ? evaluation.getRubricType()
                : DefenseRubricType.STANDARD;

        criteriaMap.put("id", evaluation.getId());
        criteriaMap.put("rubricType", rubricType.name());
        criteriaMap.put("proposedMention", evaluation.getProposedMention());
        criteriaMap.put("evaluatedAt", evaluation.getEvaluatedAt());

        if (rubricType == DefenseRubricType.ENTREPRENEURSHIP) {
            criteriaMap.put("entrepreneurshipPresentationSupportMaterial", evaluation.getEntrepreneurshipPresentationSupportMaterial());
            criteriaMap.put("entrepreneurshipCoherentBusinessObjectives", evaluation.getEntrepreneurshipCoherentBusinessObjectives());
            criteriaMap.put("entrepreneurshipMethodologyTechnicalApproach", evaluation.getEntrepreneurshipMethodologyTechnicalApproach());
            criteriaMap.put("entrepreneurshipAnalyticalCreativeCapacity", evaluation.getEntrepreneurshipAnalyticalCreativeCapacity());
            criteriaMap.put("entrepreneurshipDefenseSustentation", evaluation.getEntrepreneurshipDefenseSustentation());
        } else {
            criteriaMap.put("domainAndClarity", evaluation.getDomainAndClarity());
            criteriaMap.put("synthesisAndCommunication", evaluation.getSynthesisAndCommunication());
            criteriaMap.put("argumentationAndResponse", evaluation.getArgumentationAndResponse());
            criteriaMap.put("innovationAndImpact", evaluation.getInnovationAndImpact());
            criteriaMap.put("professionalPresentation", evaluation.getProfessionalPresentation());
        }

        return criteriaMap;
    }

    private FinalDefenseResponse.CriteriaDetail buildFinalDefenseCriteriaDetail(DefenseEvaluationCriteria evaluation) {
        if (evaluation == null) {
            return null;
        }

        return FinalDefenseResponse.CriteriaDetail.builder()
                .rubricType(evaluation.getRubricType() != null ? evaluation.getRubricType() : DefenseRubricType.STANDARD)
                .domainAndClarity(evaluation.getDomainAndClarity())
                .synthesisAndCommunication(evaluation.getSynthesisAndCommunication())
                .argumentationAndResponse(evaluation.getArgumentationAndResponse())
                .innovationAndImpact(evaluation.getInnovationAndImpact())
                .professionalPresentation(evaluation.getProfessionalPresentation())
                .entrepreneurshipPresentationSupportMaterial(evaluation.getEntrepreneurshipPresentationSupportMaterial())
                .entrepreneurshipCoherentBusinessObjectives(evaluation.getEntrepreneurshipCoherentBusinessObjectives())
                .entrepreneurshipMethodologyTechnicalApproach(evaluation.getEntrepreneurshipMethodologyTechnicalApproach())
                .entrepreneurshipAnalyticalCreativeCapacity(evaluation.getEntrepreneurshipAnalyticalCreativeCapacity())
                .entrepreneurshipDefenseSustentation(evaluation.getEntrepreneurshipDefenseSustentation())
                .proposedMention(evaluation.getProposedMention())
                .evaluatedAt(evaluation.getEvaluatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Object getMyFinalDefenseResult() {

        User student = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository
                .findByStudent(student)
                .orElseThrow(() -> new NotFoundException(
                        "No se encontró una modalidad asociada al estudiante"
                ));

        if (studentModality.getStatus() != ModalityProcessStatus.GRADED_APPROVED &&
                studentModality.getStatus() != ModalityProcessStatus.GRADED_FAILED) {

            return (
                    Map.of(
                            "hasResult", false,
                            "message", "Tu modalidad aún no tiene un resultado final"
                    )
            );
        }

        ModalityProcessStatus finalStatus = studentModality.getStatus();

        ModalityProcessStatusHistory history = historyRepository
                .findTopByStudentModalityAndStatusOrderByChangeDateDesc(
                        studentModality,
                        finalStatus
                )
                .orElseThrow(() -> new NotFoundException(
                        "No se encontró historial de evaluación final"
                ));

        List<DefenseExaminer> defenseExaminers = defenseExaminerRepository
                .findByStudentModalityId(studentModality.getId());

        List<FinalDefenseResponse.ExaminerEvaluationDetail> examinerEvaluations = defenseExaminers.stream()
                .map(defenseExaminer -> {
                    DefenseEvaluationCriteria evaluation = defenseEvaluationCriteriaRepository
                            .findByDefenseExaminerId(defenseExaminer.getId())
                            .orElse(null);

                    if (evaluation == null) {
                        return null;
                    }

                    return FinalDefenseResponse.ExaminerEvaluationDetail.builder()
                            .examinerName(defenseExaminer.getExaminer().getName() + " " +
                                        defenseExaminer.getExaminer().getLastName())
                            .examinerType(defenseExaminer.getExaminerType().name())
                            .grade(evaluation.getGrade())
                            .approved(evaluation.getGrade() != null && evaluation.getGrade() >= 3.5)
                            .observations(evaluation.getObservations())
                            .evaluationDate(evaluation.getEvaluatedAt())
                            .isFinalDecision(evaluation.getIsFinalDecision())
                            .evaluationCriteria(buildFinalDefenseCriteriaDetail(evaluation))
                            .build();
                })
                .filter(detail -> detail != null)
                .toList();

        boolean hasConsensus = studentModality.getAcademicDistinction() != null &&
                              (studentModality.getAcademicDistinction().name().startsWith("AGREED_"));

        boolean wasTiebreaker = studentModality.getAcademicDistinction() != null &&
                               (studentModality.getAcademicDistinction().name().startsWith("TIEBREAKER_"));

        return (
                FinalDefenseResponse.builder()
                        .studentModalityId(studentModality.getId())
                        .studentName(student.getName() + " " + student.getLastName())
                        .studentEmail(student.getEmail())
                        .finalStatus(finalStatus)
                        .approved(finalStatus == ModalityProcessStatus.GRADED_APPROVED)
                        .academicDistinction(studentModality.getAcademicDistinction())
                        .finalGrade(studentModality.getFinalGrade())
                        .observations(history.getObservations())
                        .evaluationDate(history.getChangeDate())
                        .evaluatedBy(
                                history.getResponsible() != null
                                        ? history.getResponsible().getName()
                                        : "Comité de currículo de programa"
                        )
                        .hasConsensus(hasConsensus)
                        .wasTiebreaker(wasTiebreaker)
                        .examinerEvaluations(examinerEvaluations)
                        .build()
        );
    }

    // =========================================================================
    // GESTIÓN DE DISTINCIONES HONORÍFICAS PROPUESTAS POR JURADOS
    // =========================================================================

    /**
     * Lista las modalidades en las que los jurados han propuesto unánimemente
     * una distinción honorífica (Meritoria o Laureada) y que están pendientes
     * de revisión y decisión por parte del Comité de Currículo.
     *
     * Solo el comité del programa académico correspondiente puede ver estas modalidades.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPendingDistinctionProposals() {
        User committeeMember = SecurityUtils.getCurrentUser();

        List<Long> programIds = programAuthorityRepository
                .findByUser_Id(committeeMember.getId())
                .stream()
                .filter(pa -> pa.getRole() == ProgramRole.PROGRAM_CURRICULUM_COMMITTEE)
                .map(pa -> pa.getAcademicProgram().getId())
                .toList();

        if (programIds.isEmpty()) {
            throw new ForbiddenException("El usuario no tiene el rol de Comité de Currículo en ningún programa académico.");
        }

        // Buscar modalidades con estado PENDING_DISTINCTION_COMMITTEE_REVIEW en los programas del comité
        List<StudentModality> pendingModalities = studentModalityRepository
                .findByStatusAndProgramDegreeModality_AcademicProgram_IdIn(
                        ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW,
                        programIds
                );

        List<Map<String, Object>> result = pendingModalities.stream()
                .sorted(Comparator.comparing(StudentModality::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(sm -> {
                    User leader = sm.getLeader();
                    StudentProfile leaderProfile = studentProfileRepository.findByUserId(leader.getId()).orElse(null);

                    // Obtener las evaluaciones de los jurados para ver los argumentos
                    List<DefenseExaminer> examiners = defenseExaminerRepository.findByStudentModalityId(sm.getId());
                    List<Map<String, Object>> examinerDetails = examiners.stream()
                            .map(de -> {
                                DefenseEvaluationCriteria eval = defenseEvaluationCriteriaRepository
                                        .findByDefenseExaminerId(de.getId())
                                        .orElse(null);
                                Map<String, Object> examinerMap = new LinkedHashMap<>();
                                examinerMap.put("examinerId", de.getExaminer().getId());
                                examinerMap.put("examinerName", de.getExaminer().getName() + " " + de.getExaminer().getLastName());
                                examinerMap.put("examinerType", de.getExaminerType() != null ? de.getExaminerType().name() : null);
                                examinerMap.put("proposedMention", eval != null ? (eval.getProposedMention() != null ? eval.getProposedMention().name() : "NONE") : null);
                                examinerMap.put("grade", eval != null ? eval.getGrade() : null);
                                examinerMap.put("observations", eval != null ? eval.getObservations() : null);
                                return examinerMap;
                            })
                            .toList();

                    // Traducir la distinción propuesta
                    String proposedDistinctionLabel = ModalityServiceUtils.translateProposedDistinction(sm.getAcademicDistinction());

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("studentModalityId", sm.getId());
                    row.put("studentId", leader.getId());
                    row.put("studentName", leader.getName() + " " + leader.getLastName());
                    row.put("studentEmail", leader.getEmail());
                    row.put("studentCode", leaderProfile != null ? leaderProfile.getStudentCode() : null);
                    row.put("modalityName", sm.getProgramDegreeModality().getDegreeModality().getName());
                    row.put("academicProgram", sm.getAcademicProgram().getName());
                    row.put("finalGrade", sm.getFinalGrade());
                    row.put("currentStatus", sm.getStatus().name());
                    row.put("proposedDistinction", sm.getAcademicDistinction() != null ? sm.getAcademicDistinction().name() : null);
                    row.put("proposedDistinctionLabel", proposedDistinctionLabel);
                    row.put("lastUpdatedAt", sm.getUpdatedAt());
                    row.put("examinerEvaluations", examinerDetails);
                    row.put("projectDirector", sm.getProjectDirector() != null
                            ? sm.getProjectDirector().getName() + " " + sm.getProjectDirector().getLastName()
                            : null);
                    return row;
                })
                .collect(Collectors.toList());

        return (Map.of(
                "success", true,
                "totalPending", result.size(),
                "pendingDistinctionProposals", result
        ));
    }

    /**
     * El Comité de Currículo ACEPTA la distinción honorífica propuesta por los jurados.
     * La modalidad pasa a GRADED_APPROVED con la distinción confirmada.
     *
     * @param studentModalityId ID de la modalidad
     * @param notes             Notas/observaciones del comité al aceptar (opcional)
     */
    @Transactional
    public Map<String, Object> acceptDistinctionProposal(Long studentModalityId, String notes) {
        User committeeMember = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        Long academicProgramId = studentModality.getProgramDegreeModality().getAcademicProgram().getId();

        boolean authorized = programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                committeeMember.getId(), academicProgramId, ProgramRole.PROGRAM_CURRICULUM_COMMITTEE);

        if (!authorized) {
            throw new ForbiddenException("No tiene permiso para revisar distinciones en este programa académico.");
        }

        if (studentModality.getStatus() != ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW) {
            throw new ValidationException("La modalidad no está en estado de revisión de distinción por el comité.");
        }

        // Convertir la distinción propuesta en la distinción definitiva aceptada
        AcademicDistinction proposedDistinction = studentModality.getAcademicDistinction();
        AcademicDistinction confirmedDistinction = resolveAcceptedDistinction(proposedDistinction);

        if (confirmedDistinction == null) {
            throw new ValidationException("No se puede determinar la distinción a confirmar. Estado de distinción inválido: " + proposedDistinction);
        }

        String observations = String.format(
                "El Comité de Currículo ACEPTÓ la distinción honorífica propuesta por los jurados. " +
                "Distinción propuesta: %s → Distinción confirmada: %s. %s",
                ModalityServiceUtils.translateAcademicDistinction(proposedDistinction),
                ModalityServiceUtils.translateAcademicDistinction(confirmedDistinction),
                notes != null && !notes.isBlank() ? "Observaciones del comité: " + notes : ""
        );

        studentModality.setAcademicDistinction(confirmedDistinction);
        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.GRADED_APPROVED, committeeMember, observations);

        // Notificar resultado final definitivo
        applicationEventPublisher.publishEvent(new ModalityEvent(NotificationType.DEFENSE_COMPLETED, studentModality.getId(), committeeMember.getId(), Map.of(
                ModalityEvent.KEY_FINAL_STATUS, ModalityProcessStatus.GRADED_APPROVED,
                ModalityEvent.KEY_ACADEMIC_DISTINCTION, confirmedDistinction,
                ModalityEvent.KEY_OBSERVATIONS, observations
        )));

        return (Map.of(
                "success", true,
                "studentModalityId", studentModalityId,
                "newStatus", ModalityProcessStatus.GRADED_APPROVED,
                "confirmedDistinction", confirmedDistinction,
                "message", "Distinción honorífica aceptada correctamente. La modalidad queda APROBADA con distinción " +
                        ModalityServiceUtils.translateProposedDistinction(confirmedDistinction) + "."
        ));
    }

    /**
     * El Comité de Currículo RECHAZA la distinción honorífica propuesta por los jurados.
     * La modalidad pasa a GRADED_APPROVED sin distinción especial (AGREED_APPROVED o TIEBREAKER_APPROVED).
     *
     * @param studentModalityId ID de la modalidad
     * @param reason            Razón del rechazo (obligatorio)
     */
    @Transactional
    public Map<String, Object> rejectDistinctionProposal(Long studentModalityId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Debe proporcionar una razón para rechazar la distinción propuesta.");
        }

        User committeeMember = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        Long academicProgramId = studentModality.getProgramDegreeModality().getAcademicProgram().getId();

        boolean authorized = programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                committeeMember.getId(), academicProgramId, ProgramRole.PROGRAM_CURRICULUM_COMMITTEE);

        if (!authorized) {
            throw new ForbiddenException("No tiene permiso para revisar distinciones en este programa académico.");
        }

        if (studentModality.getStatus() != ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW) {
            throw new ValidationException("La modalidad no está en estado de revisión de distinción por el comité.");
        }

        // Al rechazar, la distinción se convierte en aprobada sin mención especial
        AcademicDistinction proposedDistinction = studentModality.getAcademicDistinction();
        AcademicDistinction rejectedDistinction = resolveRejectedDistinction(proposedDistinction);

        String observations = String.format(
                "El Comité de Currículo RECHAZÓ la distinción honorífica propuesta por los jurados. " +
                "Distinción propuesta: %s → Distinción final: %s (sin mención especial). " +
                "Razón del rechazo: %s",
                ModalityServiceUtils.translateAcademicDistinction(proposedDistinction),
                ModalityServiceUtils.translateAcademicDistinction(rejectedDistinction),
                reason
        );

        studentModality.setAcademicDistinction(rejectedDistinction);
        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.GRADED_APPROVED, committeeMember, observations);

        // Notificar resultado final definitivo sin mención
        applicationEventPublisher.publishEvent(new ModalityEvent(NotificationType.DEFENSE_COMPLETED, studentModality.getId(), committeeMember.getId(), Map.of(
                ModalityEvent.KEY_FINAL_STATUS, ModalityProcessStatus.GRADED_APPROVED,
                ModalityEvent.KEY_ACADEMIC_DISTINCTION, rejectedDistinction,
                ModalityEvent.KEY_OBSERVATIONS, observations
        )));

        return (Map.of(
                "success", true,
                "studentModalityId", studentModalityId,
                "newStatus", ModalityProcessStatus.GRADED_APPROVED,
                "finalDistinction", rejectedDistinction,
                "reason", reason,
                "message", "Distinción honorífica rechazada. La modalidad queda APROBADA sin distinción especial."
        ));
    }

    /**
     * Resuelve cuál es la distinción definitiva al ACEPTAR la propuesta de los jurados.
     */
    private AcademicDistinction resolveAcceptedDistinction(AcademicDistinction proposed) {
        if (proposed == null) return null;
        return switch (proposed) {
            case PENDING_COMMITTEE_MERITORIOUS -> AcademicDistinction.AGREED_MERITORIOUS;
            case PENDING_COMMITTEE_LAUREATE -> AcademicDistinction.AGREED_LAUREATE;
            case TIEBREAKER_PENDING_COMMITTEE_MERITORIOUS -> AcademicDistinction.TIEBREAKER_MERITORIOUS;
            case TIEBREAKER_PENDING_COMMITTEE_LAUREATE -> AcademicDistinction.TIEBREAKER_LAUREATE;
            default -> null;
        };
    }

    /**
     * Resuelve cuál es la distinción definitiva al RECHAZAR la propuesta de los jurados.
     * La modalidad queda aprobada sin mención especial.
     */
    private AcademicDistinction resolveRejectedDistinction(AcademicDistinction proposed) {
        if (proposed == null) return AcademicDistinction.AGREED_APPROVED;
        return switch (proposed) {
            case TIEBREAKER_PENDING_COMMITTEE_MERITORIOUS, TIEBREAKER_PENDING_COMMITTEE_LAUREATE ->
                    AcademicDistinction.TIEBREAKER_APPROVED;
            default -> AcademicDistinction.AGREED_APPROVED;
        };
    }

    @Transactional
    public Map<String, Object> getExaminerEvaluationForModality(Long studentModalityId) {
        User examiner = SecurityUtils.getCurrentUser();

        DefenseExaminer defenseExaminer = defenseExaminerRepository
                .findByStudentModalityIdAndExaminerId(studentModalityId, examiner.getId())
                .orElse(null);

        if (defenseExaminer == null) {
            throw new ForbiddenException("No está asignado como jurado a esta modalidad");
        }

        DefenseEvaluationCriteria evaluation = defenseEvaluationCriteriaRepository
                .findByDefenseExaminerId(defenseExaminer.getId())
                .orElse(null);

        if (evaluation == null) {
            return (Map.of(
                "success", false,
                "message", "No ha registrado evaluación para esta modalidad"
            ));
        }

        ExaminerEvaluationDTO dto = ExaminerEvaluationDTO.builder()
                .grade(evaluation.getGrade())
                .observations(evaluation.getObservations())
                .evaluationDate(evaluation.getEvaluatedAt())
                .build();

        return (Map.of(
            "success", true,
            "evaluation", dto
        ));
    }
}
