package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.entity.ModalityProcessStatusHistory;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.entity.enums.ExaminerType;
import com.SIGMA.USCO.Modalities.entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.repository.DefenseExaminerRepository;
import com.SIGMA.USCO.Modalities.repository.ModalityProcessStatusHistoryRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Modalities.dto.response.EditRequestCreatedResponse;
import com.SIGMA.USCO.Modalities.dto.response.EditRequestDetailResponse;
import com.SIGMA.USCO.Modalities.dto.response.EditRequestResolutionResponse;
import com.SIGMA.USCO.Modalities.dto.response.ExaminerEditRequestsResponse;
import com.SIGMA.USCO.Modalities.dto.response.ExaminerEvaluationNotFoundResponse;
import com.SIGMA.USCO.Modalities.dto.response.ExaminerFinalDocumentEvaluationResponse;
import com.SIGMA.USCO.Modalities.dto.response.ExaminerListResponse;
import com.SIGMA.USCO.Modalities.dto.response.ExaminerProposalEvaluationResponse;
import com.SIGMA.USCO.Modalities.dto.response.ModalityEditRequestsResponse;
import com.SIGMA.USCO.Modalities.dto.response.MyEditRequestsResponse;
import com.SIGMA.USCO.Modalities.dto.response.PendingEditRequestsResponse;
import com.SIGMA.USCO.Modalities.dto.response.ProposalEvaluationInfo;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.documents.entity.DocumentEditRequest;
import com.SIGMA.USCO.documents.entity.DocumentEditRequestVote;
import com.SIGMA.USCO.documents.entity.ExaminerDocumentReview;
import com.SIGMA.USCO.documents.entity.FinalDocumentEvaluation;
import com.SIGMA.USCO.documents.entity.ProposalEvaluation;
import com.SIGMA.USCO.documents.entity.RequiredDocument;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.entity.StudentDocumentStatusHistory;
import com.SIGMA.USCO.documents.entity.enums.DocumentEditRequestStatus;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import com.SIGMA.USCO.documents.entity.enums.EditRequestVoteDecision;
import com.SIGMA.USCO.documents.repository.DocumentEditRequestRepository;
import com.SIGMA.USCO.documents.repository.DocumentEditRequestVoteRepository;
import com.SIGMA.USCO.documents.repository.ExaminerDocumentReviewRepository;
import com.SIGMA.USCO.documents.repository.FinalDocumentEvaluationRepository;
import com.SIGMA.USCO.documents.repository.ProposalEvaluationRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentStatusHistoryRepository;
import com.SIGMA.USCO.documents.dto.DocumentEditRequestDTO;
import com.SIGMA.USCO.documents.dto.DocumentEditResolutionDTO;
import com.SIGMA.USCO.documents.dto.DocumentEditRequestResponseDTO;
import com.SIGMA.USCO.common.exception.BusinessException;
import com.SIGMA.USCO.common.exception.ForbiddenException;
import com.SIGMA.USCO.common.exception.InternalException;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.shared.util.ResourceAccessPolicy;
import com.SIGMA.USCO.shared.util.TranslationUtils;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.common.security.Roles;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.Modalities.event.ModalityEvent;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentEditRequestService {

    private final DocumentEditRequestRepository documentEditRequestRepository;
    private final DocumentEditRequestVoteRepository documentEditRequestVoteRepository;
    private final StudentModalityRepository studentModalityRepository;
    private final ModalityProcessStatusHistoryRepository historyRepository;
    private final DefenseExaminerRepository defenseExaminerRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final StudentDocumentStatusHistoryRepository documentHistoryRepository;
    private final ExaminerDocumentReviewRepository examinerDocumentReviewRepository;
    private final ProposalEvaluationRepository proposalEvaluationRepository;
    private final FinalDocumentEvaluationRepository secondaryDocumentEvaluationRepository;
    private final ModalityStatusTransition modalityStatusTransition;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ResourceAccessPolicy resourceAccessPolicy;

    /**
     * El jurado autenticado obtiene su veredicto sobre documentos MANDATORY (propuesta de grado).
     * Devuelve la decisión individual del jurado, notas y evaluación de propuesta (si aplica).
     * Ruta: GET /modalities/documents/{studentDocumentId}/examiner-proposal-evaluation
     */
    @Transactional(readOnly = true)
    public Object getMyProposalEvaluation(Long studentDocumentId, User examiner) {

        StudentDocument document = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new NotFoundException("Documento no encontrado"));

        StudentModality studentModality = document.getStudentModality();

        // Validar que sea un documento MANDATORY
        if (document.getDocumentConfig().getDocumentType() != DocumentType.MANDATORY) {
            throw new ValidationException("Este documento no es de tipo inicial.");
        }

        // Validar que el examiner esté asignado a la modalidad
        DefenseExaminer defenseExaminer = resourceAccessPolicy.requireAssignedExaminer(
                studentModality.getId(), examiner, "No estás asignado como jurado a esta modalidad");

        // Obtener la review del jurado para este documento
        ExaminerDocumentReview review = examinerDocumentReviewRepository
                .findByStudentDocumentIdAndExaminerId(studentDocumentId, examiner.getId())
                .orElse(null);

        if (review == null) {
            return new ExaminerEvaluationNotFoundResponse(false, "No has emitido veredicto para este documento aún");
        }

        // Obtener la evaluación de propuesta si existe
        ProposalEvaluation proposalEvaluation = proposalEvaluationRepository
                .findByStudentDocumentIdAndExaminerId(studentDocumentId, examiner.getId())
                .orElse(null);

        return ExaminerProposalEvaluationResponse.builder()
                .success(true)
                .documentId(document.getId())
                .documentName(document.getDocumentConfig().getDocumentName())
                .documentType(DocumentType.MANDATORY.name())
                .examinerName(examiner.getName() + " " + examiner.getLastName())
                .examinerEmail(examiner.getEmail())
                .examinerType(defenseExaminer.getExaminerType().name())
                .isTiebreaker(defenseExaminer.getExaminerType() == ExaminerType.TIEBREAKER_EXAMINER)
                .decision(review.getDecision().name())
                .decisionDescription(ModalityServiceUtils.translateExaminerDocumentDecision(review.getDecision()))
                .notes(review.getNotes())
                .reviewedAt(review.getReviewedAt())
                .proposalEvaluation(proposalEvaluation != null
                        ? ProposalEvaluationInfo.builder()
                                .summary(proposalEvaluation.getSummary())
                                .backgroundJustification(proposalEvaluation.getBackgroundJustification())
                                .problemStatement(proposalEvaluation.getProblemStatement())
                                .objectives(proposalEvaluation.getObjectives())
                                .methodology(proposalEvaluation.getMethodology())
                                .bibliographyReferences(proposalEvaluation.getBibliographyReferences())
                                .documentOrganization(proposalEvaluation.getDocumentOrganization())
                                .evaluatedAt(proposalEvaluation.getEvaluatedAt())
                                .build()
                        : null)
                .documentStatus(document.getStatus().name())
                .documentNotes(document.getNotes())
                .build();
    }

    /**
     * El jurado autenticado obtiene su veredicto sobre documentos SECONDARY (documento final).
     * Devuelve la decisión individual del jurado, notas y evaluación final (si aplica).
     * Ruta: GET /modalities/documents/{studentDocumentId}/examiner-final-evaluation
     */
    @Transactional(readOnly = true)
    public Object getMyFinalDocumentEvaluation(Long studentDocumentId, User examiner) {

        StudentDocument document = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new NotFoundException("Documento no encontrado"));

        StudentModality studentModality = document.getStudentModality();

        // Validar que sea un documento SECONDARY
        if (document.getDocumentConfig().getDocumentType() != DocumentType.SECONDARY) {
            throw new ValidationException("Este documento no es de tipo final.");
        }

        // Validar que el examiner esté asignado a la modalidad
        DefenseExaminer defenseExaminer = resourceAccessPolicy.requireAssignedExaminer(
                studentModality.getId(), examiner, "No estás asignado como jurado a esta modalidad");

        // Obtener la review del jurado para este documento
        ExaminerDocumentReview review = examinerDocumentReviewRepository
                .findByStudentDocumentIdAndExaminerId(studentDocumentId, examiner.getId())
                .orElse(null);

        if (review == null) {
            return new ExaminerEvaluationNotFoundResponse(false, "No has emitido veredicto para este documento aún");
        }

        // Obtener la evaluación final (FinalDocumentEvaluation) si existe
        FinalDocumentEvaluation finalEvaluation = secondaryDocumentEvaluationRepository
                .findByStudentDocumentIdAndExaminerId(studentDocumentId, examiner.getId())
                .orElse(null);

        return ExaminerFinalDocumentEvaluationResponse.builder()
                .success(true)
                .documentId(document.getId())
                .documentName(document.getDocumentConfig().getDocumentName())
                .documentType(DocumentType.SECONDARY.name())
                .examinerName(examiner.getName() + " " + examiner.getLastName())
                .examinerEmail(examiner.getEmail())
                .examinerType(defenseExaminer.getExaminerType().name())
                .isTiebreaker(defenseExaminer.getExaminerType() == ExaminerType.TIEBREAKER_EXAMINER)
                .decision(review.getDecision().name())
                .decisionDescription(ModalityServiceUtils.translateExaminerDocumentDecision(review.getDecision()))
                .notes(review.getNotes())
                .reviewedAt(review.getReviewedAt())
                .finalEvaluation(finalEvaluation != null
                        ? ModalityServiceUtils.buildFinalEvaluationInfo(finalEvaluation)
                        : null)
                .documentStatus(document.getStatus().name())
                .documentNotes(document.getNotes())
                .build();
    }
    // =========================================================================
    // SOLICITUD DE EDICIÓN DE PROPUESTA APROBADA (STUDENT → EXAMINER)
    // =========================================================================

    /**
     * Permite al estudiante autenticado solicitar la edición de un documento
     * que ya fue aprobado por los jurados (estado ACCEPTED_FOR_EXAMINER_REVIEW).
     * Solo se permite si la modalidad no está cerrada/calificada.
     */
    @Transactional
    public EditRequestCreatedResponse requestDocumentEdit(Long studentDocumentId, DocumentEditRequestDTO request, User student) {

        StudentDocument document = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new NotFoundException("Documento no encontrado"));

        StudentModality studentModality = document.getStudentModality();

        // Validar que el estudiante sea miembro activo de la modalidad
        boolean isActiveMember = studentModalityMemberRepository.isActiveMember(
                studentModality.getId(), student.getId());
        if (!isActiveMember) {
            throw new ForbiddenException("No eres miembro activo de esta modalidad");
        }

        // Validar que sea un documento MANDATORY
        if (document.getDocumentConfig().getDocumentType() != DocumentType.MANDATORY) {
            throw new ValidationException("Solo puedes solicitar edición de documentos de tipo MANDATORY (obligatorios)");
        }

        // Validar que el documento esté aprobado por jurados
        if (document.getStatus() != DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW) {
            throw new ValidationException("Solo puedes solicitar edición de documentos que hayan sido aprobados por los jurados. Estado actual: " + document.getStatus());
        }

        // Validar que la modalidad esté en una fase que permita solicitar edición
        ModalityProcessStatus modalityStatus = studentModality.getStatus();
        if (modalityStatus != ModalityProcessStatus.EXAMINERS_ASSIGNED &&
                modalityStatus != ModalityProcessStatus.PROPOSAL_APPROVED) {
            throw new ValidationException("Solo puedes solicitar edición cuando la modalidad está en revisión de jurados o con propuesta aprobada");
        }

        // Validar que no haya ya una solicitud pendiente o en desempate para este documento
        boolean hasPending = documentEditRequestRepository.existsByStudentDocumentIdAndStatus(
                studentDocumentId, DocumentEditRequestStatus.PENDING);
        boolean hasTiebreaker = documentEditRequestRepository.existsByStudentDocumentIdAndStatus(
                studentDocumentId, DocumentEditRequestStatus.TIEBREAKER_REQUIRED);
        if (hasPending || hasTiebreaker) {
            throw new ValidationException("Ya existe una solicitud de edición pendiente para este documento");
        }

        // Guardar el estado previo de la modalidad para trazabilidad
        ModalityProcessStatus previousModalityStatus = studentModality.getStatus();

        // Cambiar el estado del documento a EDIT_REQUESTED
        document.setStatus(DocumentStatus.EDIT_REQUESTED);
        document.setUploadDate(LocalDateTime.now());
        studentDocumentRepository.save(document);

        // Historial del DOCUMENTO
        documentHistoryRepository.save(
                StudentDocumentStatusHistory.builder()
                        .studentDocument(document)
                        .status(DocumentStatus.EDIT_REQUESTED)
                        .changeDate(LocalDateTime.now())
                        .responsible(student)
                        .observations("Estudiante solicitó edición del documento aprobado. Motivo: " + request.getReason())
                        .build()
        );

        // Cambiar el estado de la MODALIDAD a EDIT_REQUESTED_BY_STUDENT
        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.EDIT_REQUESTED_BY_STUDENT, student,
                "Estudiante solicitó edición del documento '" +
                        document.getDocumentConfig().getDocumentName() +
                        ". Motivo: " + request.getReason() +
                        ". La solicitud fue enviada a los jurados para su evaluación.");

        // Crear la solicitud de edición
        DocumentEditRequest editRequest = DocumentEditRequest.builder()
                .studentDocument(document)
                .requester(student)
                .reason(request.getReason())
                .status(DocumentEditRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        documentEditRequestRepository.save(editRequest);

        // Notificar a los jurados
        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.DOCUMENT_EDIT_REQUESTED, studentModality.getId(), student.getId(), Map.of(
                        ModalityEvent.KEY_STUDENT_DOCUMENT_ID, studentDocumentId,
                        ModalityEvent.KEY_EDIT_REQUEST_ID, editRequest.getId(),
                        ModalityEvent.KEY_REASON, request.getReason(),
                        ModalityEvent.KEY_DOCUMENT_NAME, document.getDocumentConfig().getDocumentName()
                ))
        );

        return new EditRequestCreatedResponse(
                true,
                editRequest.getId(),
                document.getId(),
                document.getDocumentConfig().getDocumentName(),
                DocumentStatus.EDIT_REQUESTED.name(),
                ModalityProcessStatus.EDIT_REQUESTED_BY_STUDENT.name(),
                "Solicitud de edición registrada correctamente. Los jurados evaluadores serán notificados para votar."
        );
    }

    /**
     * Permite a un jurado votar sobre una solicitud de edición de documento.
     *
     * Lógica de consenso (igual que revisión de documentos):
     * - AMBOS jurados primarios aprueban → solicitud APPROVED, documento pasa a EDIT_REQUEST_APPROVED
     * - AMBOS rechazan → solicitud REJECTED, documento vuelve a ACCEPTED_FOR_EXAMINER_REVIEW
     * - UNO aprueba + UNO rechaza → TIEBREAKER_REQUIRED, el jurado de desempate decide
     * - JURADO DE DESEMPATE vota → su decisión es definitiva
     */
    @Transactional
    public EditRequestResolutionResponse resolveDocumentEditRequest(Long editRequestId, DocumentEditResolutionDTO request, User examiner) {

        boolean hasExaminerRole = examiner.getRoles().stream()
                .anyMatch(role -> role.getName().equals(Roles.ROLE_EXAMINER));
        if (!hasExaminerRole) {
            throw new ForbiddenException("Solo los jurados pueden votar sobre solicitudes de edición de documentos");
        }

        if (request.getApproved() == null) {
            throw new ValidationException("Debe indicar si aprueba (approved: true) o rechaza (approved: false) la solicitud");
        }

        // Si rechaza, las notas son obligatorias
        if (Boolean.FALSE.equals(request.getApproved()) &&
                (request.getResolutionNotes() == null || request.getResolutionNotes().isBlank())) {
            throw new ValidationException("Debe proporcionar notas al rechazar una solicitud de edición");
        }

        DocumentEditRequest editRequest = documentEditRequestRepository.findById(editRequestId)
                .orElseThrow(() -> new NotFoundException("Solicitud de edición no encontrada"));

        // Solo se puede votar si la solicitud está PENDING o TIEBREAKER_REQUIRED
        if (editRequest.getStatus() != DocumentEditRequestStatus.PENDING &&
                editRequest.getStatus() != DocumentEditRequestStatus.TIEBREAKER_REQUIRED) {
            throw new ValidationException("Esta solicitud ya fue resuelta. Estado actual: " + editRequest.getStatus());
        }

        StudentDocument document = editRequest.getStudentDocument();
        StudentModality studentModality = document.getStudentModality();

        // Validar que el jurado esté asignado a la modalidad
        DefenseExaminer defenseExaminer = resourceAccessPolicy.requireAssignedExaminer(
                studentModality.getId(), examiner, "No estás asignado como jurado a esta modalidad");

        ExaminerType examinerType = defenseExaminer.getExaminerType();
        boolean isTiebreaker = examinerType == ExaminerType.TIEBREAKER_EXAMINER;

        // Validar que no haya ya votado
        if (documentEditRequestVoteRepository.existsByEditRequestIdAndExaminerId(editRequestId, examiner.getId())) {
            throw new ValidationException("Ya registraste tu veredicto sobre esta solicitud de edición");
        }

        // Si es jurado de desempate y la solicitud no está en TIEBREAKER_REQUIRED → no puede votar aún
        if (isTiebreaker && editRequest.getStatus() != DocumentEditRequestStatus.TIEBREAKER_REQUIRED) {
            throw new ValidationException("El jurado de desempate solo interviene cuando los jurados principales tienen veredictos divididos");
        }

        // Si es jurado primario y la solicitud está en TIEBREAKER_REQUIRED → ya no puede votar
        if (!isTiebreaker && editRequest.getStatus() == DocumentEditRequestStatus.TIEBREAKER_REQUIRED) {
            throw new ValidationException("Los jurados principales ya votaron. Esta solicitud está en espera del jurado de desempate");
        }

        EditRequestVoteDecision voteDecision = Boolean.TRUE.equals(request.getApproved())
                ? EditRequestVoteDecision.APPROVED
                : EditRequestVoteDecision.REJECTED;

        // Guardar el voto
        DocumentEditRequestVote vote = DocumentEditRequestVote.builder()
                .editRequest(editRequest)
                .examiner(examiner)
                .decision(voteDecision)
                .notes(request.getResolutionNotes())
                .isTiebreakerVote(isTiebreaker)
                .votedAt(LocalDateTime.now())
                .build();
        documentEditRequestVoteRepository.save(vote);

        // ===== LÓGICA DE CONSENSO =====
        return processEditRequestConsensus(editRequest, document, studentModality, examiner, isTiebreaker, voteDecision, request.getResolutionNotes());
    }

    /**
     * Procesa el consenso de votos sobre una solicitud de edición.
     */
    private EditRequestResolutionResponse processEditRequestConsensus(
            DocumentEditRequest editRequest,
            StudentDocument document,
            StudentModality studentModality,
            User examiner,
            boolean isTiebreaker,
            EditRequestVoteDecision currentVote,
            String notes) {

        Long editRequestId = editRequest.getId();

        // Si es el jurado de desempate → su voto es definitivo
        if (isTiebreaker) {
            return applyFinalEditRequestDecision(
                    editRequest, document, studentModality, examiner,
                    currentVote == EditRequestVoteDecision.APPROVED, notes, true
            );
        }

        // Obtener los jurados primarios de la modalidad
        List<DefenseExaminer> primaryExaminers = defenseExaminerRepository
                .findPrimaryExaminersByStudentModalityId(studentModality.getId());

        if (primaryExaminers.size() < 2) {
            // Solo hay un jurado primario → su voto es suficiente
            return applyFinalEditRequestDecision(
                    editRequest, document, studentModality, examiner,
                    currentVote == EditRequestVoteDecision.APPROVED, notes, false
            );
        }

        // Obtener todos los votos de jurados primarios para esta solicitud
        List<DocumentEditRequestVote> primaryVotes = documentEditRequestVoteRepository
                .findByEditRequestId(editRequestId)
                .stream()
                .filter(v -> !v.getIsTiebreakerVote())
                .toList();

        // Si aún no han votado todos los jurados primarios, esperar
        if (primaryVotes.size() < 2) {
            return new EditRequestResolutionResponse(
                    true,
                    editRequestId,
                    "Veredicto registrado. Esperando el veredicto del otro jurado principal.",
                    primaryVotes.size(), 2,
                    null, null, null, null, null, null, null, null);
        }

        // Ambos han votado — analizar resultado
        long approvedCount = primaryVotes.stream()
                .filter(v -> v.getDecision() == EditRequestVoteDecision.APPROVED)
                .count();
        long rejectedCount = primaryVotes.stream()
                .filter(v -> v.getDecision() == EditRequestVoteDecision.REJECTED)
                .count();

        if (approvedCount == 2) {
            // Ambos aprueban → APPROVED
            return applyFinalEditRequestDecision(editRequest, document, studentModality, examiner, true, null, false);
        }

        if (rejectedCount == 2) {
            // Ambos rechazan → REJECTED
            String combinedNotes = primaryVotes.stream()
                    .filter(v -> v.getNotes() != null && !v.getNotes().isBlank())
                    .map(DocumentEditRequestVote::getNotes)
                    .collect(Collectors.joining(" | "));
            return applyFinalEditRequestDecision(editRequest, document, studentModality, examiner, false,
                    combinedNotes.isBlank() ? notes : combinedNotes, false);
        }

        // Votos divididos (uno aprueba, uno rechaza) → TIEBREAKER_REQUIRED
        editRequest.setStatus(DocumentEditRequestStatus.TIEBREAKER_REQUIRED);
        documentEditRequestRepository.save(editRequest);

        // Trazabilidad en historial del DOCUMENTO
        documentHistoryRepository.save(
                StudentDocumentStatusHistory.builder()
                        .studentDocument(document)
                        .status(DocumentStatus.EDIT_REQUESTED)
                        .changeDate(LocalDateTime.now())
                        .responsible(examiner)
                        .observations("Votos de jurados primarios divididos sobre la solicitud de edición. Se requiere jurado de desempate.")
                        .build()
        );

        // Trazabilidad en historial de la MODALIDAD
        modalityStatusTransition.recordHistory(studentModality, studentModality.getStatus(), examiner,
                "Solicitud de edición del documento '" +
                        document.getDocumentConfig().getDocumentName() +
                        "': votos de jurados principales divididos. Se requiere veredicto del jurado de desempate para resolver.");

        // Notificar al jurado de desempate
        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.DOCUMENT_EDIT_REQUESTED, studentModality.getId(), examiner.getId(), Map.of(
                        ModalityEvent.KEY_STUDENT_DOCUMENT_ID, document.getId(),
                        ModalityEvent.KEY_EDIT_REQUEST_ID, editRequest.getId(),
                        ModalityEvent.KEY_REASON, editRequest.getReason() + " [REQUIERE DESEMPATE: votos divididos entre jurados principales]",
                        ModalityEvent.KEY_DOCUMENT_NAME, document.getDocumentConfig().getDocumentName()
                ))
        );

        return new EditRequestResolutionResponse(
                true,
                editRequestId,
                "Los jurados principales tienen votos divididos. El jurado de desempate deberá resolver la solicitud.",
                null, null,
                DocumentEditRequestStatus.TIEBREAKER_REQUIRED.name(),
                buildVotesSummary(documentEditRequestVoteRepository.findByEditRequestId(editRequestId)),
                null, null, null, null, null, null);
    }

    /**
     * Aplica la decisión final sobre la solicitud de edición (aprobada o rechazada).
     */
    private EditRequestResolutionResponse applyFinalEditRequestDecision(
            DocumentEditRequest editRequest,
            StudentDocument document,
            StudentModality studentModality,
            User responsible,
            boolean approved,
            String finalNotes,
            boolean wasTiebreaker) {

        DocumentEditRequestStatus finalStatus = approved
                ? DocumentEditRequestStatus.APPROVED
                : DocumentEditRequestStatus.REJECTED;

        editRequest.setStatus(finalStatus);
        editRequest.setResolvedBy(responsible);
        editRequest.setResolutionNotes(finalNotes);
        editRequest.setResolvedAt(LocalDateTime.now());
        documentEditRequestRepository.save(editRequest);

        if (approved) {
            document.setStatus(DocumentStatus.EDIT_REQUEST_APPROVED);
            document.setNotes("Solicitud de edición aprobada" + (wasTiebreaker ? " por el jurado de desempate" : " por consenso de jurados") +
                    ". Puedes resubir el documento con los cambios necesarios.");
        } else {
            document.setStatus(DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW);
            document.setNotes("Solicitud de edición rechazada" + (wasTiebreaker ? " por el jurado de desempate" : " por consenso de jurados") +
                    (finalNotes != null ? ". Motivo: " + finalNotes : ""));
        }
        document.setUploadDate(LocalDateTime.now());
        studentDocumentRepository.save(document);

        // Actualizar el estado de la MODALIDAD
        // - Si se APRUEBA: la modalidad permanece en EDIT_REQUESTED_BY_STUDENT hasta que el estudiante resuba
        // - Si se RECHAZA: la modalidad vuelve al estado previo a la solicitud (restaurado desde el historial)
        if (!approved) {
            // ponytail: regla de historial — el primer entry es el EDIT_REQUESTED_BY_STUDENT escrito por
            // requestDocumentEdit; se filtra y se toma el primer status distinto para restaurar el estado
            // previo a la solicitud (fallback EXAMINERS_ASSIGNED). La transición escribe su propia historia.
            List<ModalityProcessStatusHistory> history = historyRepository
                    .findByStudentModalityIdOrderByChangeDateDesc(studentModality.getId());
            ModalityProcessStatus previousStatus = history.stream()
                    .map(ModalityProcessStatusHistory::getStatus)
                    .filter(status -> status != ModalityProcessStatus.EDIT_REQUESTED_BY_STUDENT)
                    .findFirst()
                    .orElse(ModalityProcessStatus.EXAMINERS_ASSIGNED);

            modalityStatusTransition.transition(studentModality, previousStatus, responsible,
                    "Solicitud de edición rechazada" + (wasTiebreaker ? " por el jurado de desempate" : " por consenso de jurados principales") +
                            ". La modalidad vuelve a su estado anterior: " + previousStatus);
        }
        // Si se aprueba, el estado sigue en EDIT_REQUESTED_BY_STUDENT; cambiará a EXAMINERS_ASSIGNED
        // cuando el estudiante resuba el documento (en uploadRequiredDocument)

        // Trazabilidad en historial del DOCUMENTO
        documentHistoryRepository.save(
                StudentDocumentStatusHistory.builder()
                        .studentDocument(document)
                        .status(approved ? DocumentStatus.EDIT_REQUEST_APPROVED : DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW)
                        .changeDate(LocalDateTime.now())
                        .responsible(responsible)
                        .observations("Solicitud de edición " + (approved ? "APROBADA" : "RECHAZADA") +
                                (wasTiebreaker ? " por jurado de desempate" : " por consenso de jurados") +
                                (finalNotes != null ? ". Notas: " + finalNotes : ""))
                        .build()
        );

        // Trazabilidad en historial de la MODALIDAD
        // Aprobación → recordHistory (el estado sigue en EDIT_REQUESTED_BY_STUDENT);
        // rechazo → la historia ya fue escrita por transition (restauración del estado previo)
        if (approved) {
            modalityStatusTransition.recordHistory(studentModality, ModalityProcessStatus.EDIT_REQUESTED_BY_STUDENT, responsible,
                    "Solicitud de edición del documento '" +
                            document.getDocumentConfig().getDocumentName() + "' " +
                            "APROBADA" +
                            (wasTiebreaker ? " por el jurado de desempate" : " por consenso de jurados principales") +
                            ". El estudiante puede resubir el documento con los cambios necesarios." +
                            (finalNotes != null && !finalNotes.isBlank() ? " Observaciones: " + finalNotes : ""));
        }

        // Notificar a los estudiantes del resultado
        applicationEventPublisher.publishEvent(
                new ModalityEvent(approved ? NotificationType.DOCUMENT_EDIT_APPROVED : NotificationType.DOCUMENT_EDIT_REJECTED, studentModality.getId(), responsible.getId(), Map.of(
                        ModalityEvent.KEY_STUDENT_DOCUMENT_ID, document.getId(),
                        ModalityEvent.KEY_EDIT_REQUEST_ID, editRequest.getId(),
                        ModalityEvent.KEY_APPROVED, approved,
                        ModalityEvent.KEY_RESOLUTION_NOTES, finalNotes,
                        ModalityEvent.KEY_DOCUMENT_NAME, document.getDocumentConfig().getDocumentName()
                ))
        );

        List<EditRequestResolutionResponse.EditVoteSummary> votesSummary = buildVotesSummary(
                documentEditRequestVoteRepository.findByEditRequestId(editRequest.getId())
        );

        return new EditRequestResolutionResponse(
                true,
                editRequest.getId(),
                approved
                        ? "Solicitud aprobada. El estudiante puede resubir el documento con los cambios."
                        : "Solicitud rechazada. El documento permanece en estado aprobado.",
                null, null, null, votesSummary,
                document.getId(),
                document.getDocumentConfig().getDocumentName(),
                finalStatus.name(),
                document.getStatus().name(),
                studentModality.getStatus().name(),
                wasTiebreaker);
    }

    /**
     * Construye un resumen de los votos de los jurados.
     */
    private List<EditRequestResolutionResponse.EditVoteSummary> buildVotesSummary(List<DocumentEditRequestVote> votes) {
        return votes.stream().map(v -> new EditRequestResolutionResponse.EditVoteSummary(
                v.getExaminer().getName() + " " + v.getExaminer().getLastName(),
                v.getExaminer().getEmail(),
                v.getDecision().name(),
                v.getNotes(),
                v.getIsTiebreakerVote(),
                v.getVotedAt()
        )).toList();
    }

    /**
     * El jurado autenticado obtiene TODAS las solicitudes de edición de documentos
     * asociadas a una modalidad, con información completa del proceso:
     * - Información del documento y del solicitante
     * - Estado actual de la solicitud
     * - Votos ya registrados por los jurados (nombre, tipo, decisión, notas)
     * - Si el jurado autenticado ya ha votado o no
     * - Resultado final (si ya está resuelto)
     * Visible para todos los estados (PENDING, TIEBREAKER_REQUIRED, APPROVED, REJECTED).
     */
    @Transactional(readOnly = true)
    public ExaminerEditRequestsResponse getAllEditRequestsForExaminer(Long studentModalityId, User examiner) {

        DefenseExaminer defenseExaminer = resourceAccessPolicy.requireAssignedExaminer(
                studentModalityId, examiner, "No estás asignado como jurado a esta modalidad");

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        // Obtener todos los miembros activos
        List<StudentModalityMember> activeMembers = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(studentModalityId, MemberStatus.ACTIVE);

        List<String> studentNames = activeMembers.stream()
                .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName() +
                        " (" + m.getStudent().getEmail() + ")" +
                        (Boolean.TRUE.equals(m.getIsLeader()) ? " – Líder" : ""))
                .toList();

        // Obtener TODAS las solicitudes de edición de la modalidad (todos los estados)
        List<DocumentEditRequest> allRequests = documentEditRequestRepository
                .findByStudentModalityId(studentModalityId);

        // Batch (T5.5): votos de todas las solicitudes en 1 query + jurados de la modalidad en 1 query
        Map<Long, List<DocumentEditRequestVote>> votesByRequest = allRequests.isEmpty() ? Map.of()
                : documentEditRequestVoteRepository
                        .findByEditRequestIdIn(allRequests.stream().map(DocumentEditRequest::getId).toList())
                        .stream()
                        .collect(Collectors.groupingBy(v -> v.getEditRequest().getId()));

        Map<Long, DefenseExaminer> examinerByUserId = defenseExaminerRepository
                .findByStudentModalityId(studentModalityId)
                .stream()
                .collect(Collectors.toMap(de -> de.getExaminer().getId(), de -> de));

        List<ExaminerEditRequestsResponse.EditRequestListItem> requestDTOs = new ArrayList<>();

        for (DocumentEditRequest req : allRequests) {
            StudentDocument doc = req.getStudentDocument();

            List<DocumentEditRequestVote> votes = votesByRequest.getOrDefault(req.getId(), List.of());

            boolean alreadyVoted = votes.stream()
                    .anyMatch(v -> v.getExaminer().getId().equals(examiner.getId()));

            DocumentEditRequestVote myVote = votes.stream()
                    .filter(v -> v.getExaminer().getId().equals(examiner.getId()))
                    .findFirst()
                    .orElse(null);

            List<ExaminerEditRequestsResponse.EditRequestVoteInfo> voteDTOs = votes.stream().map(v -> {
                String examinerTypeLabel = Optional.ofNullable(examinerByUserId.get(v.getExaminer().getId()))
                        .map(de -> TranslationUtils.translateExaminerType(de.getExaminerType()))
                        .orElse("Jurado");
                return new ExaminerEditRequestsResponse.EditRequestVoteInfo(
                        v.getExaminer().getName() + " " + v.getExaminer().getLastName(),
                        v.getExaminer().getEmail(),
                        examinerTypeLabel,
                        v.getDecision().name(),
                        v.getDecision() == EditRequestVoteDecision.APPROVED ? "Aprobado" : "Rechazado",
                        v.getNotes(),
                        v.getIsTiebreakerVote(),
                        v.getVotedAt()
                );
            }).toList();

            String statusDesc = switch (req.getStatus()) {
                case PENDING -> "Pendiente de votación por los jurados principales";
                case TIEBREAKER_REQUIRED -> "Votos de jurados principales divididos – en espera del jurado de desempate";
                case APPROVED -> "Solicitud aprobada – el estudiante puede resubir el documento con los cambios";
                case REJECTED -> "Solicitud rechazada – el documento permanece en estado aprobado";
            };

            boolean canVote;
            if (defenseExaminer.getExaminerType() == ExaminerType.TIEBREAKER_EXAMINER) {
                canVote = req.getStatus() == DocumentEditRequestStatus.TIEBREAKER_REQUIRED && !alreadyVoted;
            } else {
                canVote = req.getStatus() == DocumentEditRequestStatus.PENDING && !alreadyVoted;
            }

            ExaminerEditRequestsResponse.EditRequestMyVote myVoteInfo = myVote != null
                    ? new ExaminerEditRequestsResponse.EditRequestMyVote(
                            myVote.getDecision().name(),
                            myVote.getDecision() == EditRequestVoteDecision.APPROVED ? "Aprobado" : "Rechazado",
                            myVote.getNotes(),
                            myVote.getVotedAt())
                    : null;

            requestDTOs.add(new ExaminerEditRequestsResponse.EditRequestListItem(
                    req.getId(),
                    doc.getId(),
                    doc.getDocumentConfig().getDocumentName(),
                    doc.getDocumentConfig().getDocumentType().name(),
                    doc.getStatus().name(),
                    req.getRequester().getName() + " " + req.getRequester().getLastName(),
                    req.getRequester().getEmail(),
                    req.getReason(),
                    req.getStatus().name(),
                    statusDesc,
                    req.getCreatedAt(),
                    req.getResolvedAt(),
                    req.getResolutionNotes(),
                    votes.size(),
                    voteDTOs,
                    alreadyVoted,
                    canVote,
                    myVoteInfo
            ));
        }

        // Información del jurado autenticado en contexto de esta modalidad
        ExaminerEditRequestsResponse.ExaminerContext examinerContext = new ExaminerEditRequestsResponse.ExaminerContext(
                examiner.getId(),
                examiner.getName() + " " + examiner.getLastName(),
                examiner.getEmail(),
                defenseExaminer.getExaminerType().name(),
                TranslationUtils.translateExaminerType(defenseExaminer.getExaminerType()),
                defenseExaminer.getExaminerType() == ExaminerType.TIEBREAKER_EXAMINER
        );

        // Información de la modalidad
        ExaminerEditRequestsResponse.ModalityContext modalityInfo = new ExaminerEditRequestsResponse.ModalityContext(
                studentModality.getId(),
                studentModality.getProgramDegreeModality().getDegreeModality().getName(),
                studentModality.getProgramDegreeModality().getAcademicProgram().getName(),
                studentModality.getStatus().name(),
                studentNames
        );

        long pending = allRequests.stream().filter(r -> r.getStatus() == DocumentEditRequestStatus.PENDING).count();
        long tiebreakerRequired = allRequests.stream().filter(r -> r.getStatus() == DocumentEditRequestStatus.TIEBREAKER_REQUIRED).count();
        long approvedCount = allRequests.stream().filter(r -> r.getStatus() == DocumentEditRequestStatus.APPROVED).count();
        long rejectedCount = allRequests.stream().filter(r -> r.getStatus() == DocumentEditRequestStatus.REJECTED).count();

        return new ExaminerEditRequestsResponse(
                true,
                examinerContext,
                modalityInfo,
                new ExaminerEditRequestsResponse.EditRequestSummary(
                        allRequests.size(), pending, tiebreakerRequired, approvedCount, rejectedCount),
                requestDTOs
        );
    }

    /**
     * Lista todas las solicitudes de edición pendientes para una modalidad,
     * para que el jurado autenticado pueda revisarlas.
     * Incluye el estado de votación actual y los votos ya registrados.
     */
@Transactional(readOnly = true)
    public PendingEditRequestsResponse getPendingEditRequestsForExaminer(Long studentModalityId, User examiner) {

        DefenseExaminer defenseExaminer = resourceAccessPolicy.requireAssignedExaminer(
                studentModalityId, examiner, "No estás asignado como jurado a esta modalidad");

        boolean isTiebreaker = defenseExaminer.getExaminerType() == ExaminerType.TIEBREAKER_EXAMINER;

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        List<StudentDocument> docs = studentDocumentRepository.findByStudentModalityId(studentModalityId);
        List<DocumentEditRequestResponseDTO> result = new ArrayList<>();

        for (StudentDocument doc : docs) {
            // Mostrar pendientes y en desempate (el de desempate solo ve las que son TIEBREAKER_REQUIRED)
            List<DocumentEditRequest> requests = documentEditRequestRepository
                    .findByStudentDocumentId(doc.getId())
                    .stream()
                    .filter(req -> {
                        if (isTiebreaker) {
                            return req.getStatus() == DocumentEditRequestStatus.TIEBREAKER_REQUIRED;
                        } else {
                            return req.getStatus() == DocumentEditRequestStatus.PENDING;
                        }
                    })
                    .toList();

            for (DocumentEditRequest req : requests) {
                List<DocumentEditRequestVote> votes = documentEditRequestVoteRepository
                        .findByEditRequestId(req.getId());

                boolean alreadyVoted = votes.stream()
                        .anyMatch(v -> v.getExaminer().getId().equals(examiner.getId()));

                List<DocumentEditRequestResponseDTO.EditVoteDTO> voteDTOs = votes.stream()
                        .map(v -> DocumentEditRequestResponseDTO.EditVoteDTO.builder()
                                .examinerName(v.getExaminer().getName() + " " + v.getExaminer().getLastName())
                                .examinerEmail(v.getExaminer().getEmail())
                                .decision(v.getDecision().name())
                                .notes(v.getNotes())
                                .isTiebreakerVote(v.getIsTiebreakerVote())
                                .votedAt(v.getVotedAt())
                                .build())
                        .toList();

                String statusDesc = switch (req.getStatus()) {
                    case PENDING -> "Pendiente de votación por los jurados principales";
                    case TIEBREAKER_REQUIRED -> "Veredictos divididos – requiere veredicto del jurado de desempate";
                    case APPROVED -> "Solicitud aprobada";
                    case REJECTED -> "Solicitud rechazada";
                };

                result.add(DocumentEditRequestResponseDTO.builder()
                        .editRequestId(req.getId())
                        .studentDocumentId(doc.getId())
                        .documentName(doc.getDocumentConfig().getDocumentName())
                        .documentType(doc.getDocumentConfig().getDocumentType().name())
                        .requesterName(req.getRequester().getName() + " " + req.getRequester().getLastName())
                        .requesterEmail(req.getRequester().getEmail())
                        .reason(req.getReason())
                        .status(req.getStatus().name())
                        .statusDescription(statusDesc)
                        .createdAt(req.getCreatedAt())
                        .resolvedAt(req.getResolvedAt())
                        .votes(voteDTOs)
                        .finalResolutionNotes(req.getResolutionNotes())
                        .build());

                // Agregar flag de si ya votó en la respuesta
                // (lo añadimos al map de la respuesta principal)
                log.info("Solicitud {} - Jurado {} ya votó: {}", req.getId(), examiner.getEmail(), alreadyVoted);
            }
        }

        return new PendingEditRequestsResponse(
                true,
                studentModalityId,
                defenseExaminer.getExaminerType().name(),
                isTiebreaker,
                result
        );
    }

    // =========================================================================
    // MÉTODOS GET PARA EL ESTUDIANTE – SOLICITUDES DE EDICIÓN
    // =========================================================================

    /**
     * El estudiante autenticado obtiene TODAS sus solicitudes de edición de documentos,
     * agrupadas por modalidad, con el estado de votación de cada una.
     */
    @Transactional(readOnly = true)
    public MyEditRequestsResponse getMyDocumentEditRequests(User student) {

        List<DocumentEditRequest> requests = documentEditRequestRepository.findByRequesterId(student.getId());

        List<DocumentEditRequestResponseDTO> result = requests.stream()
                .map(req -> buildEditRequestResponseDTO(req))
                .toList();

        return new MyEditRequestsResponse(true, result.size(), result);
    }

    /**
     * El estudiante autenticado obtiene todas las solicitudes de edición
     * asociadas a una modalidad específica (por studentModalityId).
     * Útil para ver el estado actualizado de sus solicitudes en la modalidad actual.
     */
    @Transactional(readOnly = true)
    public ModalityEditRequestsResponse getMyDocumentEditRequestsByModality(Long studentModalityId, User student) {

        // Validar que el estudiante sea miembro activo de la modalidad
        boolean isActiveMember = studentModalityMemberRepository.isActiveMember(studentModalityId, student.getId());
        if (!isActiveMember) {
            throw new ForbiddenException("No eres miembro activo de esta modalidad");
        }

        List<DocumentEditRequest> requests = documentEditRequestRepository
                .findByStudentModalityId(studentModalityId);

        List<DocumentEditRequestResponseDTO> result = requests.stream()
                .map(req -> buildEditRequestResponseDTO(req))
                .toList();

        return new ModalityEditRequestsResponse(true, studentModalityId, result.size(), result);
    }

    /**
     * El estudiante autenticado obtiene el detalle de una solicitud de edición específica por ID.
     */
    @Transactional(readOnly = true)
    public EditRequestDetailResponse getDocumentEditRequestDetail(Long editRequestId, User student) {

        DocumentEditRequest request = documentEditRequestRepository.findById(editRequestId)
                .orElseThrow(() -> new NotFoundException("Solicitud de edición no encontrada"));

        StudentModality studentModality = request.getStudentDocument().getStudentModality();

        // Validar que el estudiante sea miembro activo de la modalidad o el solicitante
        boolean isActiveMember = studentModalityMemberRepository.isActiveMember(
                studentModality.getId(), student.getId());
        if (!isActiveMember && !request.getRequester().getId().equals(student.getId())) {
            throw new ForbiddenException("No tienes permiso para ver esta solicitud de edición");
        }

        DocumentEditRequestResponseDTO dto = buildEditRequestResponseDTO(request);

        return new EditRequestDetailResponse(true, dto);
    }

    /**
     * Construye el DTO de respuesta para una solicitud de edición.
     */
    private DocumentEditRequestResponseDTO buildEditRequestResponseDTO(DocumentEditRequest req) {

        StudentDocument doc = req.getStudentDocument();

        List<DocumentEditRequestVote> votes = documentEditRequestVoteRepository
                .findByEditRequestId(req.getId());

        List<DocumentEditRequestResponseDTO.EditVoteDTO> voteDTOs = votes.stream()
                .map(v -> DocumentEditRequestResponseDTO.EditVoteDTO.builder()
                        .examinerName(v.getExaminer().getName() + " " + v.getExaminer().getLastName())
                        .examinerEmail(v.getExaminer().getEmail())
                        .decision(v.getDecision().name())
                        .notes(v.getNotes())
                        .isTiebreakerVote(v.getIsTiebreakerVote())
                        .votedAt(v.getVotedAt())
                        .build())
                .toList();

        String statusDesc = switch (req.getStatus()) {
            case PENDING -> "Pendiente de votación por los jurados evaluadores";
            case TIEBREAKER_REQUIRED -> "Votos de jurados principales divididos – en espera del jurado de desempate";
            case APPROVED -> "Solicitud aprobada – puedes resubir el documento con los cambios";
            case REJECTED -> "Solicitud rechazada – el documento permanece en estado aprobado";
        };

        return DocumentEditRequestResponseDTO.builder()
                .editRequestId(req.getId())
                .studentDocumentId(doc.getId())
                .documentName(doc.getDocumentConfig().getDocumentName())
                .documentType(doc.getDocumentConfig().getDocumentType().name())
                .requesterName(req.getRequester().getName() + " " + req.getRequester().getLastName())
                .requesterEmail(req.getRequester().getEmail())
                .reason(req.getReason())
                .status(req.getStatus().name())
                .statusDescription(statusDesc)
                .createdAt(req.getCreatedAt())
                .resolvedAt(req.getResolvedAt())
                .votes(voteDTOs)
                .finalResolutionNotes(req.getResolutionNotes())
                .build();
    }

    /**
     * Obtiene la lista de jurados (examinadores) asociados a una modalidad específica.
     * Retorna información detallada de cada jurado incluyendo su tipo (primario 1, primario 2, desempate).
     *
     * @param studentModalityId ID de la modalidad del estudiante
     * @return lista de jurados o error si la modalidad no existe
     */
    @Transactional(readOnly = true)
    public ExaminerListResponse getExaminersForModality(Long studentModalityId, User user) {
        try {
            StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                    .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

            boolean isProgramAuthority = programAuthorityRepository.existsByUser_IdAndAcademicProgram_Id(
                    user.getId(),
                    studentModality.getAcademicProgram().getId()
            );
            boolean isAssignedExaminer = defenseExaminerRepository
                    .findByStudentModalityIdAndExaminerId(studentModalityId, user.getId())
                    .isPresent();

            if (!isProgramAuthority && !isAssignedExaminer) {
                throw new ForbiddenException("No autorizado para consultar los jurados de esta modalidad");
            }

            List<DefenseExaminer> examiners = defenseExaminerRepository
                    .findByStudentModalityId(studentModalityId);

            if (examiners.isEmpty()) {
                return new ExaminerListResponse(true, studentModalityId, List.of(),
                        "No hay jurados asignados a esta modalidad", null, null, null);
            }

            List<ExaminerListResponse.ExaminerInfo> examinersList = examiners.stream()
                    .map(examiner -> new ExaminerListResponse.ExaminerInfo(
                            examiner.getExaminer().getId(),
                            examiner.getExaminer().getName(),
                            examiner.getExaminer().getLastName(),
                            examiner.getExaminer().getEmail(),
                            examiner.getExaminerType().name(),
                            TranslationUtils.translateExaminerType(examiner.getExaminerType()),
                            examiner.getAssignmentDate()))
                    .toList();

            return new ExaminerListResponse(
                    true,
                    studentModalityId,
                    examinersList,
                    null,
                    studentModality.getProgramDegreeModality().getDegreeModality().getName(),
                    studentModality.getStatus().name(),
                    examinersList.size()
            );

        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new InternalException("Error al obtener los jurados", e);
        } catch (Exception e) {
            throw new InternalException("Error al obtener los jurados", e);
        }
    }
}
