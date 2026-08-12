package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.Entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.Entity.DegreeModality;
import com.SIGMA.USCO.Modalities.Entity.ModalityRequirements;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.Entity.enums.ExaminerType;
import com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityType;
import com.SIGMA.USCO.Modalities.Entity.enums.RuleType;
import com.SIGMA.USCO.Modalities.Repository.DefenseExaminerRepository;
import com.SIGMA.USCO.Modalities.Repository.DegreeModalityRepository;
import com.SIGMA.USCO.Modalities.Repository.ModalityRequirementsRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Modalities.dto.DocumentReviewDTO;
import com.SIGMA.USCO.Modalities.dto.ValidationItemDTO;
import com.SIGMA.USCO.Modalities.dto.response.ExaminerDocumentReviewResponse;
import com.SIGMA.USCO.Modalities.dto.response.FinalEvaluationInfo;
import com.SIGMA.USCO.Modalities.dto.response.ProposalEvaluationInfo;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.Entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.ProgramDegreeModality;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.ProgramDegreeModalityRepository;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.documents.entity.ExaminerDocumentReview;
import com.SIGMA.USCO.documents.entity.FinalDocumentEvaluation;
import com.SIGMA.USCO.documents.entity.ProposalEvaluation;
import com.SIGMA.USCO.documents.entity.RequiredDocument;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.entity.StudentDocumentStatusHistory;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import com.SIGMA.USCO.documents.entity.enums.ExaminerDocumentDecision;
import com.SIGMA.USCO.documents.entity.enums.FinalDocumentRubricType;
import com.SIGMA.USCO.documents.dto.FinalEvaluationRequest;
import com.SIGMA.USCO.documents.dto.ProposalEvaluationRequest;
import com.SIGMA.USCO.documents.repository.ExaminerDocumentReviewRepository;
import com.SIGMA.USCO.documents.repository.FinalDocumentEvaluationRepository;
import com.SIGMA.USCO.documents.repository.ProposalEvaluationRepository;
import com.SIGMA.USCO.documents.repository.RequiredDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentStatusHistoryRepository;
import com.SIGMA.USCO.common.exception.ForbiddenException;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.common.util.ResourceAccessPolicy;
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.event.ModalityEvent;
import com.SIGMA.USCO.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentWorkflowService {

    private final DegreeModalityRepository degreeModalityRepository;
    private final DefenseExaminerRepository defenseExaminerRepository;
    private final ModalityRequirementsRepository modalityRequirementsRepository;
    private final RequiredDocumentRepository requiredDocumentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final StudentDocumentStatusHistoryRepository documentHistoryRepository;
    private final ProgramDegreeModalityRepository programDegreeModalityRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;
    private final ProposalEvaluationRepository proposalEvaluationRepository;
    private final FinalDocumentEvaluationRepository secondaryDocumentEvaluationRepository;
    private final ExaminerDocumentReviewRepository examinerDocumentReviewRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ResourceAccessPolicy resourceAccessPolicy;
    private final ModalityStatusTransition modalityStatusTransition;
    private final ModalityDocumentService modalityDocumentService;

    @Transactional
    public Map<String, Object> startStudentModalityIndividual(Long modalityId) {

        User student = SecurityUtils.getCurrentUser();

        StudentProfile profile = studentProfileRepository.findByUserId(student.getId())
                .orElseThrow(() -> new ValidationException("Debe completar su perfil académico antes de seleccionar una modalidad"));

        DegreeModality modality = degreeModalityRepository.findById(modalityId)
                .orElseThrow(() -> new NotFoundException("La modalidad con ID " + modalityId + " no existe"));

        ProgramDegreeModality programDegreeModality =
                programDegreeModalityRepository.findByAcademicProgramIdAndDegreeModalityIdAndActiveTrue(profile.getAcademicProgram().getId(), modalityId)
                        .orElseThrow(() -> new ValidationException("La modalidad no está habilitada para tu programa académico"));

        // Verificar si el estudiante tiene modalidades activas (en proceso)
        // CORRECTIONS_REJECTED_FINAL también es un estado finalizado que permite iniciar nueva modalidad

        List<ModalityProcessStatus> finalizedStatuses = List.of(
                ModalityProcessStatus.MODALITY_CLOSED,
                ModalityProcessStatus.MODALITY_CANCELLED,
                ModalityProcessStatus.GRADED_FAILED,
                ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL
        );

        // Obtener todas las modalidades del estudiante como miembro activo
        List<StudentModalityMember> activeMembers = studentModalityMemberRepository.findByStudentIdAndStatus(
                student.getId(),
                MemberStatus.ACTIVE
        );

        // Verificar si alguna de esas modalidades NO está finalizada
        for (StudentModalityMember member : activeMembers) {
            ModalityProcessStatus currentStatus = member.getStudentModality().getStatus();
            if (!finalizedStatuses.contains(currentStatus)) {
                throw new ValidationException("Ya tienes una modalidad de grado en curso. No puedes iniciar otra.");
            }
        }

        // Verificar si el estudiante tiene una modalidad CERRADA (MODALITY_CLOSED)
        // Si tiene una modalidad cerrada, NO puede volver a iniciar la MISMA modalidad
        List<StudentModality> closedModalities = studentModalityRepository.findByLeaderIdAndStatus(
                student.getId(),
                ModalityProcessStatus.MODALITY_CLOSED
        );

        for (StudentModality closedModality : closedModalities) {
            if (closedModality.getProgramDegreeModality().getDegreeModality().getId().equals(modalityId)) {
                throw new ValidationException("No puedes volver a iniciar esta modalidad porque ya fue cerrada anteriormente. Debes seleccionar una modalidad diferente.");
            }
        }

        List<ModalityRequirements> requirements = modalityRequirementsRepository.findByModalityIdAndActiveTrue(modalityId);

        List<ValidationItemDTO> results = new ArrayList<>();
        boolean allValid = true;

        for (ModalityRequirements req : requirements) {

            if (req.getRuleType() != RuleType.NUMERIC) {
                continue;
            }

            boolean fulfilled = true;
            String studentValue = "";

            if (req.getRequirementName().toLowerCase().contains("crédito")) {

                double percentageRequired = Double.parseDouble(req.getExpectedValue());
                long totalCredits = profile.getAcademicProgram().getTotalCredits();
                long requiredCredits = Math.round(totalCredits * percentageRequired);

                fulfilled = profile.getApprovedCredits() >= requiredCredits;
                studentValue = profile.getApprovedCredits() + " / " + requiredCredits;
            }

            if (req.getRequirementName().toLowerCase().contains("promedio")) {

                fulfilled = profile.getGpa() >= Double.parseDouble(req.getExpectedValue());
                studentValue = String.valueOf(profile.getGpa());
            }

            results.add(
                    ValidationItemDTO.builder()
                            .requirementName(req.getRequirementName())
                            .expectedValue(req.getExpectedValue())
                            .studentValue(studentValue)
                            .fulfilled(fulfilled)
                            .build()
            );

            if (!fulfilled) {
                allValid = false;
            }
        }

        if (!allValid) {
            throw new ValidationException("No cumples los requisitos académicos para esta modalidad");
        }

        StudentModality studentModality = StudentModality.builder()
                .leader(student)
                .modalityType(ModalityType.INDIVIDUAL)
                .academicProgram(profile.getAcademicProgram())
                .programDegreeModality(programDegreeModality)
                .status(ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD)
                .selectionDate(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        studentModalityRepository.save(studentModality);

        StudentModalityMember member = StudentModalityMember.builder()
                .studentModality(studentModality)
                .student(student)
                .isLeader(true)
                .status(MemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();

        studentModalityMemberRepository.save(member);

        modalityStatusTransition.recordHistory(studentModality, ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD, student,
                "Modalidad individual iniciada por el estudiante");

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_STARTED, studentModality.getId(), student.getId(), Map.of())
        );

        return Map.of(
                        "eligible", true,
                        "studentModalityId", studentModality.getId(),
                        "studentModalityName", modality.getName(),
                        "modalityType", "INDIVIDUAL",
                        "message", "Modalidad iniciada correctamente. Puedes subir los documentos."
                );
    }

    @Transactional
    public Map<String, Object> reviewStudentDocument(Long studentDocumentId, DocumentReviewDTO request) {
        User reviewer = SecurityUtils.getCurrentUser();

        StudentDocument document = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new NotFoundException("Document not found"));

        if (document.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD) {
            throw new ValidationException("No se puede cambiar el estado del documento porque está en estado " + document.getStatus() + ". El estudiante debe primero corregir y resubir el documento para que pueda ser revisado por jefatura de programa nuevamente.");
        }

        if (document.getStatus() == DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW){
            throw new ValidationException("No se puede cambiar el estado del documento porque ya fue aceptado por los jurados evaluadores.");
        }

        if (document.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_CURRICULUM_COMMITTEE ||
           document.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER){
            throw new ValidationException("No se puede cambiar el estado del documento porque está en estado " + document.getStatus() + ". El estudiante debe primero corregir y resubir el documento para que pueda ser revisado por el comité de currículo o los jurados evaluadores nuevamente.");
        }

        // Validación de estado permitido
        DocumentStatus currentStatus = document.getStatus();
        if (currentStatus != DocumentStatus.PENDING &&
            currentStatus != DocumentStatus.ACCEPTED_FOR_PROGRAM_HEAD_REVIEW &&
            currentStatus != DocumentStatus.REJECTED_FOR_PROGRAM_HEAD_REVIEW &&
            currentStatus !=  DocumentStatus.CORRECTION_RESUBMITTED) {
            throw new ValidationException("No puedes cambiar el estado de este documento. Estado actual: " + currentStatus);
        }

        ModalityProcessStatus modalityStatus = document.getStudentModality().getStatus();

        if (modalityStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_EXAMINERS ||
             modalityStatus == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE) {
            throw new ValidationException("No se puede cambiar el estado del documento porque la modalidad está en estado " + modalityStatus + ". El estudiante debe primero corregir y resubir el documento para que pueda ser revisado por el comité de currículo o los jurados evaluadores nuevamente.");
        }

        AcademicProgram documentProgram = document.getStudentModality().getAcademicProgram();

        resourceAccessPolicy.requireProgramAuthority(reviewer, documentProgram.getId(), ProgramRole.PROGRAM_HEAD,
                "No tienes permisos para revisar documentos de este programa académico");

        if ((request.getStatus() == DocumentStatus.REJECTED_FOR_PROGRAM_HEAD_REVIEW ||
                request.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD)
                && (request.getNotes() == null || request.getNotes().isBlank())) {

            throw new ValidationException("Debe proporcionar notas al rechazar o solicitar correcciones");
        }

        document.setStatus(request.getStatus());
        document.setNotes(request.getNotes());
        document.setUploadDate(LocalDateTime.now());

        studentDocumentRepository.save(document);

        if (request.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD) {
            StudentModality studentModality = document.getStudentModality();

            LocalDateTime now = LocalDateTime.now();

            studentModality.setCorrectionRequestDate(now);
            studentModality.setCorrectionDeadline(now.plusDays(30));
            studentModality.setCorrectionReminderSent(false);
            modalityStatusTransition.transition(studentModality, ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD, reviewer,
                    "Jefe de programa solicitó correcciones en documento: " +
                            document.getDocumentConfig().getDocumentName() +
                            ". Notas: " + request.getNotes());

            List<StudentModalityMember> activeMembers = studentModalityMemberRepository
                    .findByStudentModalityIdAndStatus(studentModality.getId(), MemberStatus.ACTIVE);

            for (StudentModalityMember member : activeMembers) {
                applicationEventPublisher.publishEvent(
                        new ModalityEvent(NotificationType.DOCUMENT_CORRECTIONS_REQUESTED, studentModality.getId(), reviewer.getId(), Map.of(
                                ModalityEvent.KEY_STUDENT_DOCUMENT_ID, document.getId(),
                                ModalityEvent.KEY_STUDENT_ID, member.getStudent().getId(),
                                ModalityEvent.KEY_OBSERVATIONS, request.getNotes(),
                                ModalityEvent.KEY_REQUESTED_BY, NotificationRecipientType.PROGRAM_HEAD.name()
                        ))
                );
            }
        }

        documentHistoryRepository.save(
                StudentDocumentStatusHistory.builder()
                        .studentDocument(document)
                        .status(request.getStatus())
                        .changeDate(LocalDateTime.now())
                        .responsible(reviewer)
                        .observations(request.getNotes())
                        .build()
        );

        return Map.of(
                        "message", "Documento revisado correctamente",
                        "documentId", document.getId(),
                        "newStatus", document.getStatus()
                );
    }

    @Transactional
    public Map<String, Object> approveModalityByProgramHead(Long studentModalityId) {

        User programHead = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modality not found"));

        Long academicProgramId = studentModality.getAcademicProgram().getId();

        resourceAccessPolicy.requireProgramAuthority(programHead, academicProgramId, ProgramRole.PROGRAM_HEAD,
                "No tienes permisos para aprobar modalidades de este programa académico");

        if (!(studentModality.getStatus() == ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD ||
                studentModality.getStatus() == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD ||
                studentModality.getStatus() == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD ||
                studentModality.getStatus() == ModalityProcessStatus.CANCELLATION_REJECTED
                )) {

            throw new ValidationException("La modalidad no está en un estado válido para ser aprobada por la jefatura de programa. Estado actual: " + studentModality.getStatus());
        }

        Long modalityId =
                studentModality
                        .getProgramDegreeModality()
                        .getDegreeModality()
                        .getId();

        List<RequiredDocument> mandatoryDocuments =
                requiredDocumentRepository.findByModalityIdAndActiveTrueAndDocumentType(modalityId, DocumentType.MANDATORY);

        List<StudentDocument> uploadedDocuments =
                studentDocumentRepository.findByStudentModalityId(studentModalityId);

        Map<Long, StudentDocument> uploadedMap =
                uploadedDocuments.stream()
                        .collect(Collectors.toMap(
                                doc -> doc.getDocumentConfig().getId(),
                                doc -> doc
                        ));

        List<Map<String, Object>> invalidDocuments = new ArrayList<>();

        for (RequiredDocument required : mandatoryDocuments) {

            StudentDocument uploaded = uploadedMap.get(required.getId());

            if (uploaded == null) {
                invalidDocuments.add(
                        Map.of(
                                "documentName", required.getDocumentName(),
                                "status", "NOT_UPLOADED"
                        )
                );
                continue;
            }

            if (uploaded.getStatus() != DocumentStatus.ACCEPTED_FOR_PROGRAM_HEAD_REVIEW) {
                invalidDocuments.add(
                        Map.of(
                                "documentName", required.getDocumentName(),
                                "status", uploaded.getStatus()
                        )
                );
            }
        }

        if (!invalidDocuments.isEmpty()) {
            throw new ValidationException("Para poder aprobar la modalidad, todos los documentos obligatorios deben estar aceptados");
        }

        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.READY_FOR_PROGRAM_CURRICULUM_COMMITTEE, programHead,
                "Modalidad aprobada por jefatura de programa");

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_APPROVED_BY_PROGRAM_HEAD, studentModality.getId(), programHead.getId(), Map.of())
        );

        return Map.of(
                        "approved", true,
                        "newStatus", ModalityProcessStatus.READY_FOR_PROGRAM_CURRICULUM_COMMITTEE,
                        "message", "Modalidad aprobada correctamente y enviada al comité de currículo de programa"
                );
    }

    @Transactional
    public Map<String, Object> approveModalityByCommittee(Long studentModalityId) {
        User committeeMember = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modality not found"));

        Long academicProgramId = studentModality.getAcademicProgram().getId();

        resourceAccessPolicy.requireProgramAuthority(committeeMember, academicProgramId, ProgramRole.PROGRAM_CURRICULUM_COMMITTEE,
                "No tienes permisos para aprobar modalidades de este programa académico");

        if (studentModality.getStatus() != ModalityProcessStatus.READY_FOR_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE) {

            throw new ValidationException("La modalidad no está en estado válido para aprobación por el comité de currículo de programa. Estado actual: " + studentModality.getStatus());
        }

        List<StudentDocument> documents = studentDocumentRepository.findByStudentModalityId(studentModalityId);
        boolean allDocumentsApproved = documents.stream()
                .allMatch(doc -> doc.getStatus() == DocumentStatus.ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW);
        if (!allDocumentsApproved) {
            throw new ValidationException("No se puede aprobar la modalidad. Todos los documentos deben estar aprobados por el comité de currículo de programa.");
        }

        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.READY_FOR_EXAMINERS, committeeMember,
                "Modalidad aprobada por el Comité de currículo de programa");

        return Map.of(
                        "approved", true,
                        "newStatus", ModalityProcessStatus.READY_FOR_EXAMINERS,
                        "message", "Modalidad aprobada definitivamente por el comité de currículo de programa"
                );
    }

    @Transactional
    public Object reviewStudentDocumentByExaminer(Long studentDocumentId, DocumentReviewDTO request) {

        User examiner = SecurityUtils.getCurrentUser();

        boolean hasExaminerRole = examiner.getRoles().stream()
                .anyMatch(role -> role.getName().equals("EXAMINER"));

        if (!hasExaminerRole) {
            throw new ForbiddenException("El usuario no tiene rol de EXAMINER");
        }

        StudentDocument document = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new NotFoundException("Documento no encontrado"));

        StudentModality studentModality = document.getStudentModality();

        DefenseExaminer defenseExaminer = resourceAccessPolicy.requireAssignedExaminer(
                studentModality.getId(), examiner, "No estás asignado como jurado de esta modalidad");

        ExaminerType examinerType = defenseExaminer.getExaminerType();

        // ===== VALIDACIÓN: Solo se pueden evaluar documentos MANDATORY con requiresProposalEvaluation=true =====
        // Documentos MANDATORY sin esta condición (ej: contratos, formularios) no son evaluables por el jurado.
        // Los documentos SECONDARY sí pueden ser evaluados por el jurado (son los documentos finales).
        DocumentType docType = document.getDocumentConfig().getDocumentType();
        if (docType == DocumentType.MANDATORY && !document.getDocumentConfig().isRequiresProposalEvaluation()) {
            throw new ForbiddenException("Este documento obligatorio no requiere evaluación por parte del jurado. " +
                    "Solo los documentos de propuesta de grado marcados para evaluación por jurado pueden ser revisados por este rol.");
        }
        // =================================================================================

        // Validar que el documento no esté bloqueado esperando al estudiante
        if (document.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER) {
            throw new ValidationException("No se puede cambiar el estado del documento porque está en estado " +
                    document.getStatus() + ". El estudiante debe primero corregir y resubir el documento.");
        }

        // ===== VALIDACIÓN: Un jurado no puede cambiar su decisión una vez emitida =====
        // Excepción: si su decisión anterior fue CORRECTIONS_REQUESTED, el estudiante resubió
        // y ahora el jurado debe re-evaluar la nueva versión del documento.
        ExaminerDocumentReview existingReview = examinerDocumentReviewRepository
                .findByStudentDocumentIdAndExaminerId(document.getId(), examiner.getId())
                .orElse(null);

        if (existingReview != null) {
            ExaminerDocumentDecision previousDecision = existingReview.getDecision();

            if (previousDecision == ExaminerDocumentDecision.ACCEPTED) {
                throw new ValidationException("Ya aprobaste este documento. Una vez emitida la aprobación no puede ser modificada.");
            }

            if (previousDecision == ExaminerDocumentDecision.REJECTED) {
                throw new ValidationException("Ya rechazaste este documento. Una vez emitido el rechazo no puede ser modificado.");
            }

            // Si previousDecision == CORRECTIONS_REQUESTED: el jurado puede re-votar
            // porque el estudiante resubió el documento con las correcciones.
            // Verificamos que el documento esté efectivamente en estado de resubmisión.
            if (previousDecision == ExaminerDocumentDecision.CORRECTIONS_REQUESTED) {
                if (document.getStatus() != DocumentStatus.CORRECTION_RESUBMITTED) {
                    throw new ValidationException("Solicitaste correcciones en este documento. Debes esperar a que el estudiante resuba el documento corregido antes de emitir una nueva evaluación.");
                }
            }
        }
        // =============================================================================

        if (request.getStatus() != DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW &&
                request.getStatus() != DocumentStatus.REJECTED_FOR_EXAMINER_REVIEW &&
                request.getStatus() != DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER) {
            throw new ValidationException("Estado de documento inválido para revisión por jurado");
        }

        if ((request.getStatus() == DocumentStatus.REJECTED_FOR_EXAMINER_REVIEW ||
                request.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER)
                && (request.getNotes() == null || request.getNotes().isBlank())) {
            throw new ValidationException("Debe proporcionar notas al rechazar o solicitar correcciones");
        }

        // Determinar la decisión individual del jurado
        ExaminerDocumentDecision individualDecision = switch (request.getStatus()) {
            case ACCEPTED_FOR_EXAMINER_REVIEW -> ExaminerDocumentDecision.ACCEPTED;
            case REJECTED_FOR_EXAMINER_REVIEW -> ExaminerDocumentDecision.REJECTED;
            case CORRECTIONS_REQUESTED_BY_EXAMINER -> ExaminerDocumentDecision.CORRECTIONS_REQUESTED;
            default -> throw new ValidationException("Estado inválido");
        };

        // Guardar/actualizar la review individual del jurado
        ExaminerDocumentReview review = examinerDocumentReviewRepository
                .findByStudentDocumentIdAndExaminerId(document.getId(), examiner.getId())
                .orElse(ExaminerDocumentReview.builder()
                        .studentDocument(document)
                        .examiner(examiner)
                        .isTiebreakerVote(examinerType == ExaminerType.TIEBREAKER_EXAMINER)
                        .build());
        review.setDecision(individualDecision);
        review.setNotes(request.getNotes());
        review.setReviewedAt(LocalDateTime.now());
        review.setIsTiebreakerVote(examinerType == ExaminerType.TIEBREAKER_EXAMINER);
        examinerDocumentReviewRepository.save(review);

        // Guardar historial del documento
        documentHistoryRepository.save(
                StudentDocumentStatusHistory.builder()
                        .studentDocument(document)
                        .status(request.getStatus())
                        .changeDate(LocalDateTime.now())
                        .responsible(examiner)
                        .observations("Jurado " + examiner.getName() + " " + examiner.getLastName() +
                                " (" + examinerType.name() + "): " +
                                (request.getNotes() != null ? request.getNotes() : individualDecision.name()))
                        .build()
        );

        // Manejar ProposalEvaluation si aplica
        if (document.getDocumentConfig().isRequiresProposalEvaluation()
                && request.getProposalEvaluation() != null) {
            ProposalEvaluationRequest evalReq = request.getProposalEvaluation();
            if (evalReq.getSummary() == null || evalReq.getBackgroundJustification() == null
                    || evalReq.getProblemStatement() == null || evalReq.getObjectives() == null
                    || evalReq.getMethodology() == null || evalReq.getBibliographyReferences() == null
                    || evalReq.getDocumentOrganization() == null) {
                throw new ValidationException("Debe proporcionar calificaciones para todos los aspectos de la propuesta de grado");
            }
            ProposalEvaluation proposalEvaluation = proposalEvaluationRepository
                    .findByStudentDocumentIdAndExaminerId(document.getId(), examiner.getId())
                    .orElse(null);
            if (proposalEvaluation != null) {
                proposalEvaluation.setSummary(evalReq.getSummary());
                proposalEvaluation.setBackgroundJustification(evalReq.getBackgroundJustification());
                proposalEvaluation.setProblemStatement(evalReq.getProblemStatement());
                proposalEvaluation.setObjectives(evalReq.getObjectives());
                proposalEvaluation.setMethodology(evalReq.getMethodology());
                proposalEvaluation.setBibliographyReferences(evalReq.getBibliographyReferences());
                proposalEvaluation.setDocumentOrganization(evalReq.getDocumentOrganization());
                proposalEvaluation.setEvaluatedAt(LocalDateTime.now());
            } else {
                proposalEvaluation = ProposalEvaluation.builder()
                        .studentDocument(document)
                        .examiner(examiner)
                        .summary(evalReq.getSummary())
                        .backgroundJustification(evalReq.getBackgroundJustification())
                        .problemStatement(evalReq.getProblemStatement())
                        .objectives(evalReq.getObjectives())
                        .methodology(evalReq.getMethodology())
                        .bibliographyReferences(evalReq.getBibliographyReferences())
                        .documentOrganization(evalReq.getDocumentOrganization())
                        .evaluatedAt(LocalDateTime.now())
                        .build();
            }
            proposalEvaluationRepository.save(proposalEvaluation);
        }

        // ===== LÓGICA DE CONSENSO ENTRE JURADOS =====
        Map<String, Object> consensusResult = processExaminerConsensus(
                document, studentModality, examiner, examinerType, individualDecision, request.getNotes()
        );
        if (consensusResult != null) {
            // Si el consenso retorna un resultado especial (rechazo final), devolvemos ese
            return consensusResult;
        }

        // Construir respuesta
        String message = switch (request.getStatus()) {
            case ACCEPTED_FOR_EXAMINER_REVIEW -> "Documento aceptado por el jurado. Se evaluará el veredicto de todos los jurados.";
            case REJECTED_FOR_EXAMINER_REVIEW -> "Documento rechazado por el jurado. Se evaluará el veredicto de todos los jurados.";
            case CORRECTIONS_REQUESTED_BY_EXAMINER -> "Correcciones solicitadas. El estudiante deberá subir el documento corregido.";
            default -> "Documento revisado correctamente";
        };

        ProposalEvaluation savedProposalEvaluation = proposalEvaluationRepository
                .findByStudentDocumentIdAndExaminerId(document.getId(), examiner.getId())
                .orElse(null);

        return ExaminerDocumentReviewResponse.builder()
                .success(true)
                .documentId(document.getId())
                .documentName(document.getDocumentConfig().getDocumentName())
                .examinerDecision(individualDecision.name())
                .currentDocumentStatus(document.getStatus())
                .examinerName(examiner.getName() + " " + examiner.getLastName())
                .examinerType(examinerType.name())
                .message(message)
                .proposalEvaluation(savedProposalEvaluation != null
                        ? ProposalEvaluationInfo.builder()
                                .id(savedProposalEvaluation.getId())
                                .summary(savedProposalEvaluation.getSummary())
                                .backgroundJustification(savedProposalEvaluation.getBackgroundJustification())
                                .problemStatement(savedProposalEvaluation.getProblemStatement())
                                .objectives(savedProposalEvaluation.getObjectives())
                                .methodology(savedProposalEvaluation.getMethodology())
                                .bibliographyReferences(savedProposalEvaluation.getBibliographyReferences())
                                .documentOrganization(savedProposalEvaluation.getDocumentOrganization())
                                .evaluatedAt(savedProposalEvaluation.getEvaluatedAt())
                                .build()
                        : null)
                .build();
    }

    @Transactional
        public Map<String, Object> reviewFinalDocumentByExaminer(Long studentDocumentId, DocumentReviewDTO request) {

        if (request == null || request.getFinalEvaluation() == null) {
            throw new ValidationException("Debe enviar la evaluación detallada del documento final en el campo finalEvaluation");
        }

        if (request.getStatus() != DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW &&
                request.getStatus() != DocumentStatus.REJECTED_FOR_EXAMINER_REVIEW &&
                request.getStatus() != DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER) {
            throw new ValidationException("Estado de documento inválido para revisión por jurado");
        }

        if ((request.getStatus() == DocumentStatus.REJECTED_FOR_EXAMINER_REVIEW ||
                request.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER)
                && (request.getNotes() == null || request.getNotes().isBlank())) {
            throw new ValidationException("Debe proporcionar notas al rechazar o solicitar correcciones");
        }

        User examiner = SecurityUtils.getCurrentUser();

        StudentDocument document = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new NotFoundException("Documento no encontrado"));

        StudentModality studentModality = document.getStudentModality();

        if (studentModality.getStatus() != ModalityProcessStatus.READY_FOR_DEFENSE &&
              studentModality.getStatus() != ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_EXAMINERS) {
            throw new ValidationException("La modalidad no está en un estado válido para revisión de documentos finales por parte del jurado. Estado actual: " + studentModality.getStatus().name());
        }

        if (document.getDocumentConfig().getDocumentType() != DocumentType.SECONDARY ||
                !document.getDocumentConfig().isRequiresProposalEvaluation()) {
            throw new ForbiddenException("Solo se permite evaluar documentos finales que requieran evaluación por parte del jurado");
        }

        FinalEvaluationRequest evalReq = request.getFinalEvaluation();
        FinalDocumentRubricType rubricType = resolveFinalDocumentRubricType(studentModality);
        String validationError = validateFinalEvaluationByRubric(evalReq, rubricType);
        if (validationError != null) {
            throw new ValidationException(validationError);
        }

        DocumentStatus previousDocumentStatus = document.getStatus();
        String previousDocumentNotes = document.getNotes();
        ModalityProcessStatus previousModalityStatus = studentModality.getStatus();

        Object reviewResult = reviewStudentDocumentByExaminer(studentDocumentId, request);

        // Releer para reflejar estados resultantes del consenso entre jurados.
        StudentDocument updatedDocument = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new NotFoundException("Documento no encontrado"));
        StudentModality updatedModality = studentModalityRepository.findById(studentModality.getId())
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        // Consenso positivo en documento final: asegurar transición de cierre de revisión final.
        // Se invoca cuando el documento fue aprobado Y la modalidad está en fase de revisión final
        // (READY_FOR_DEFENSE o CORRECTIONS_SUBMITTED_TO_EXAMINERS)
        if (updatedDocument.getStatus() == DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW) {
            boolean isInFinalReviewPhase = updatedModality.getStatus() == ModalityProcessStatus.READY_FOR_DEFENSE ||
                    updatedModality.getStatus() == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_EXAMINERS;
            if (isInFinalReviewPhase) {
                checkAndTransitionIfAllSecondaryApprovedByExaminers(updatedModality, examiner);
                updatedModality = studentModalityRepository.findById(updatedModality.getId())
                        .orElse(updatedModality);
            }
        }

        FinalDocumentEvaluation secondaryEvaluation = secondaryDocumentEvaluationRepository
                .findByStudentDocumentIdAndExaminerId(document.getId(), examiner.getId())
                .orElse(FinalDocumentEvaluation.builder()
                        .studentDocument(document)
                        .examiner(examiner)
                        .build());

        applyFinalEvaluationByRubric(secondaryEvaluation, evalReq, rubricType);
        secondaryEvaluation.setEvaluatedAt(LocalDateTime.now());
        secondaryDocumentEvaluationRepository.save(secondaryEvaluation);

        // Trazabilidad específica de la rúbrica del documento final (independiente del consenso).
        saveFinalEvaluationTraceability(updatedDocument, examiner, request.getStatus(), request.getNotes(), secondaryEvaluation);

        // Trazabilidad explícita de cambios finales por consenso.
        if (previousDocumentStatus != updatedDocument.getStatus() || !Objects.equals(previousDocumentNotes, updatedDocument.getNotes())) {
            documentHistoryRepository.save(
                    StudentDocumentStatusHistory.builder()
                            .studentDocument(updatedDocument)
                            .status(updatedDocument.getStatus())
                            .changeDate(LocalDateTime.now())
                            .responsible(examiner)
                            .observations("Estado final del documento tras consenso de jurados: " +
                                    previousDocumentStatus + " -> " + updatedDocument.getStatus() +
                                    (updatedDocument.getNotes() != null && !updatedDocument.getNotes().isBlank()
                                            ? ". Notas finales: " + updatedDocument.getNotes()
                                            : ""))
                            .build()
            );
        }

        FinalEvaluationInfo secondaryEvaluationInfo = ModalityServiceUtils.buildFinalEvaluationInfo(secondaryEvaluation);

        Map<String, Object> traceability = new LinkedHashMap<>();
        traceability.put("previousDocumentStatus", previousDocumentStatus != null ? previousDocumentStatus.name() : null);
        traceability.put("currentDocumentStatus", updatedDocument.getStatus() != null ? updatedDocument.getStatus().name() : null);
        traceability.put("previousModalityStatus", previousModalityStatus != null ? previousModalityStatus.name() : null);
        traceability.put("currentModalityStatus", updatedModality.getStatus() != null ? updatedModality.getStatus().name() : null);
        traceability.put("examinerNotes", request.getNotes());

        Map<String, Object> mergedBody = reviewResult instanceof ExaminerDocumentReviewResponse response
                ? response.toMap()
                : new LinkedHashMap<>((Map<String, Object>) reviewResult);
        mergedBody.put("secondaryEvaluation", secondaryEvaluationInfo);
        mergedBody.put("finalEvaluation", secondaryEvaluationInfo);
        mergedBody.put("currentModalityStatus", updatedModality.getStatus().name());
        mergedBody.put("traceability", traceability);
        return mergedBody;
    }

    private void saveFinalEvaluationTraceability(StudentDocument document,
                                                 User examiner,
                                                 DocumentStatus requestedStatus,
                                                 String notes,
                                                 FinalDocumentEvaluation evaluation) {
        String observations = "Rúbrica de documento final registrada por jurado " +
                examiner.getName() + " " + examiner.getLastName() +
                ". Decisión: " + (requestedStatus != null ? requestedStatus.name() : "SIN_ESTADO") +
                ". " + buildFinalEvaluationObservations(evaluation) +
                (notes != null && !notes.isBlank() ? ". Notas del jurado: " + notes : "");

        documentHistoryRepository.save(
                StudentDocumentStatusHistory.builder()
                        .studentDocument(document)
                        .status(document.getStatus())
                        .changeDate(LocalDateTime.now())
                        .responsible(examiner)
                        .observations(observations)
                        .build()
        );
    }

    private String buildFinalEvaluationObservations(FinalDocumentEvaluation evaluation) {
        FinalDocumentRubricType rubricType = evaluation.getRubricType() != null
                ? evaluation.getRubricType()
                : FinalDocumentRubricType.STANDARD;

        if (rubricType == FinalDocumentRubricType.PROFESSIONAL_PRACTICE) {
            return "Rúbrica=PROFESSIONAL_PRACTICE => generalObjective=" + evaluation.getGeneralObjective() +
                    ", activitiesObjectiveCoherence=" + evaluation.getActivitiesObjectiveCoherence() +
                    ", criticalActivitiesDescription=" + evaluation.getCriticalActivitiesDescription() +
                    ", practiceComplianceEvidence=" + evaluation.getPracticeComplianceEvidence() +
                    ", organizationAndWriting=" + evaluation.getOrganizationAndWriting();
        }

        return "Rúbrica=STANDARD => summary=" + evaluation.getSummary() +
                ", introduction=" + evaluation.getIntroduction() +
                ", materialsAndMethods=" + evaluation.getMaterialsAndMethods() +
                ", resultsAndDiscussion=" + evaluation.getResultsAndDiscussion() +
                ", conclusions=" + evaluation.getConclusions() +
                ", bibliographyReferences=" + evaluation.getBibliographyReferences() +
                ", documentOrganization=" + evaluation.getDocumentOrganization() +
                ", prototypeOrSoftware=" + evaluation.getPrototypeOrSoftware();
    }

    private FinalDocumentRubricType resolveFinalDocumentRubricType(StudentModality studentModality) {
        String modalityName = studentModality.getProgramDegreeModality().getDegreeModality().getName();
        String normalizedName = ModalityServiceUtils.normalizeText(modalityName);
        if ("practica profesional".equals(normalizedName)) {
            return FinalDocumentRubricType.PROFESSIONAL_PRACTICE;
        }
        return FinalDocumentRubricType.STANDARD;
    }

    private String validateFinalEvaluationByRubric(FinalEvaluationRequest evalReq, FinalDocumentRubricType rubricType) {
        if (rubricType == FinalDocumentRubricType.PROFESSIONAL_PRACTICE) {
            if (evalReq.getGeneralObjective() == null ||
                    evalReq.getActivitiesObjectiveCoherence() == null ||
                    evalReq.getCriticalActivitiesDescription() == null ||
                    evalReq.getPracticeComplianceEvidence() == null ||
                    evalReq.getOrganizationAndWriting() == null) {
                return "Para la modalidad Práctica Profesional debe proporcionar calificaciones para todos los criterios: " +
                        "objetivo general, coherencia actividades-objetivo, descripción crítica de actividades, " +
                        "evidencia de cumplimiento de la práctica y organización/redacción del documento.";
            }
            return null;
        }

        if (evalReq.getSummary() == null ||
                evalReq.getIntroduction() == null ||
                evalReq.getMaterialsAndMethods() == null ||
                evalReq.getResultsAndDiscussion() == null ||
                evalReq.getConclusions() == null ||
                evalReq.getBibliographyReferences() == null ||
                evalReq.getDocumentOrganization() == null) {
            return "Debe proporcionar calificaciones para todos los aspectos obligatorios del documento final";
        }
        return null;
    }

    private void applyFinalEvaluationByRubric(FinalDocumentEvaluation evaluation,
                                              FinalEvaluationRequest evalReq,
                                              FinalDocumentRubricType rubricType) {
        evaluation.setRubricType(rubricType);

        if (rubricType == FinalDocumentRubricType.PROFESSIONAL_PRACTICE) {
            evaluation.setGeneralObjective(evalReq.getGeneralObjective());
            evaluation.setActivitiesObjectiveCoherence(evalReq.getActivitiesObjectiveCoherence());
            evaluation.setCriticalActivitiesDescription(evalReq.getCriticalActivitiesDescription());
            evaluation.setPracticeComplianceEvidence(evalReq.getPracticeComplianceEvidence());
            evaluation.setOrganizationAndWriting(evalReq.getOrganizationAndWriting());

            // Mapeo legacy para mantener compatibilidad con columnas históricas NOT NULL.
            evaluation.setSummary(evalReq.getGeneralObjective());
            evaluation.setIntroduction(evalReq.getActivitiesObjectiveCoherence());
            evaluation.setMaterialsAndMethods(evalReq.getCriticalActivitiesDescription());
            evaluation.setResultsAndDiscussion(evalReq.getPracticeComplianceEvidence());
            evaluation.setConclusions(evalReq.getOrganizationAndWriting());
            evaluation.setBibliographyReferences(evalReq.getOrganizationAndWriting());
            evaluation.setDocumentOrganization(evalReq.getOrganizationAndWriting());
            evaluation.setPrototypeOrSoftware(null);
            return;
        }

        evaluation.setSummary(evalReq.getSummary());
        evaluation.setIntroduction(evalReq.getIntroduction());
        evaluation.setMaterialsAndMethods(evalReq.getMaterialsAndMethods());
        evaluation.setResultsAndDiscussion(evalReq.getResultsAndDiscussion());
        evaluation.setConclusions(evalReq.getConclusions());
        evaluation.setBibliographyReferences(evalReq.getBibliographyReferences());
        evaluation.setDocumentOrganization(evalReq.getDocumentOrganization());
        evaluation.setPrototypeOrSoftware(evalReq.getPrototypeOrSoftware());

        // Limpiar campos de práctica si se reutiliza la misma fila de evaluación.
        evaluation.setGeneralObjective(null);
        evaluation.setActivitiesObjectiveCoherence(null);
        evaluation.setCriticalActivitiesDescription(null);
        evaluation.setPracticeComplianceEvidence(null);
        evaluation.setOrganizationAndWriting(null);
    }


    /**
     * Procesa el consenso entre jurados sobre un documento.
     * Implementa la lógica:
     * - Ambos jurados primarios aprueban → documento aprobado, verificar si todos los MANDATORY están aprobados
     * - Uno aprueba + otro solicita correcciones → estudiante debe corregir hasta que el jurado que solicitó correcciones apruebe.
     * - Uno aprueba + otro rechaza → se requiere jurado de desempate (DOCUMENT_REVIEW_TIEBREAKER_REQUIRED)
     * - Jurado de desempate decide (cualquier decisión) → se aplica su decisión
     *
     * @return un resultado final especial (rechazo), null si continúa normal
     */
    private Map<String, Object> processExaminerConsensus(StudentDocument document, StudentModality studentModality, User examiner, ExaminerType examinerType, ExaminerDocumentDecision individualDecision, String notes) {

        Long documentId = document.getId();
        Long modalityId = studentModality.getId();

        // Si es el jurado de desempate, su decisión es definitiva
        if (examinerType == ExaminerType.TIEBREAKER_EXAMINER) {
            return processTiebreakerDocumentDecision(document, studentModality, examiner, individualDecision, notes);
        }

        // Obtener los dos jurados primarios
        List<DefenseExaminer> primaryExaminers = defenseExaminerRepository
                .findPrimaryExaminersByStudentModalityId(modalityId);

        if (primaryExaminers.size() < 2) {
            // Solo hay un jurado asignado, su decisión es suficiente
            applyExaminerDecisionToDocument(document, studentModality, examiner, individualDecision, notes, true);
            return null;
        }

        // Recolectar las reviews de los dos jurados primarios para ESTE documento
        List<ExaminerDocumentReview> primaryReviews = new ArrayList<>();
        for (DefenseExaminer pe : primaryExaminers) {
            examinerDocumentReviewRepository
                    .findByStudentDocumentIdAndExaminerId(documentId, pe.getExaminer().getId())
                    .ifPresent(primaryReviews::add);
        }

        // Si aún no han votado ambos jurados primarios en la ronda actual, esperar al segundo
        if (primaryReviews.size() < 2) {
            // Determinar quién ya votó (el que aprobó previamente y conservó su voto)
            boolean hasExistingAccepted = primaryReviews.stream()
                    .anyMatch(r -> r.getDecision() == ExaminerDocumentDecision.ACCEPTED);

            // Si quien ya votó aprobó, el documento está en revisión pendiente del que solicitó correcciones
            if (hasExistingAccepted) {
                // El documento está esperando que el jurado que solicitó correcciones vote de nuevo
                // Conservar el estado actual (CORRECTION_RESUBMITTED) sin sobreescribirlo a PENDING
                document.setNotes("Aprobado por un jurado. Esperando revisión del otro jurado principal (" +
                        primaryReviews.size() + "/2 votos).");
            } else {
                // Primer voto registrado, esperando al segundo jurado primario
                document.setStatus(DocumentStatus.PENDING);
                document.setNotes("En revisión por jurados. Evaluaciones recibidas: " + primaryReviews.size() + "/2");
            }
            studentDocumentRepository.save(document);
            return null;
        }

        // Ambos jurados primarios han votado — analizar el resultado
        ExaminerDocumentDecision decision1 = primaryReviews.get(0).getDecision();
        ExaminerDocumentDecision decision2 = primaryReviews.get(1).getDecision();

        // CASO 1: Ambos aprueban
        if (decision1 == ExaminerDocumentDecision.ACCEPTED && decision2 == ExaminerDocumentDecision.ACCEPTED) {
            document.setStatus(DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW);
            document.setNotes("Aprobado por ambos jurados principales");
            studentDocumentRepository.save(document);

            // Verificar si todos los documentos del mismo tipo están aprobados
            checkAndTransitionIfAllMandatoryApprovedByExaminers(document, studentModality, examiner);
            return null;
        }

        // CASO 2: Ambos solicitan correcciones o ambos rechazan
        if (decision1 == ExaminerDocumentDecision.CORRECTIONS_REQUESTED && decision2 == ExaminerDocumentDecision.CORRECTIONS_REQUESTED) {
            return applyCorrectionsRequestedByPrimaryExaminers(document, studentModality, examiner, primaryReviews, notes);
        }

        if (decision1 == ExaminerDocumentDecision.REJECTED && decision2 == ExaminerDocumentDecision.REJECTED) {
            return applyRejectionByBothPrimaryExaminers(document, studentModality, examiner, notes);
        }

        // CASO 3: Uno aprueba, el otro solicita correcciones → el estudiante debe corregir
        boolean oneApprovedOneCorrected =
                (decision1 == ExaminerDocumentDecision.ACCEPTED && decision2 == ExaminerDocumentDecision.CORRECTIONS_REQUESTED) ||
                (decision1 == ExaminerDocumentDecision.CORRECTIONS_REQUESTED && decision2 == ExaminerDocumentDecision.ACCEPTED);
        if (oneApprovedOneCorrected) {
            String combinedNotes = primaryReviews.stream()
                    .filter(r -> r.getDecision() == ExaminerDocumentDecision.CORRECTIONS_REQUESTED)
                    .map(r -> r.getNotes() != null ? r.getNotes() : "")
                    .collect(Collectors.joining("; "));
            return applyCorrectionsRequestedByPrimaryExaminers(document, studentModality, examiner, primaryReviews, combinedNotes);
        }

        // CASO 4: Uno aprueba, el otro rechaza → DESEMPATE REQUERIDO (único caso de desempate)
        boolean tiebreakerRequired =
                (decision1 == ExaminerDocumentDecision.ACCEPTED && decision2 == ExaminerDocumentDecision.REJECTED) ||
                (decision1 == ExaminerDocumentDecision.REJECTED && decision2 == ExaminerDocumentDecision.ACCEPTED);

        if (tiebreakerRequired) {
            document.setStatus(DocumentStatus.PENDING);
            document.setNotes("Decisión dividida: un jurado aprobó y el otro rechazó. Se requiere jurado de desempate.");
            studentDocumentRepository.save(document);

            modalityStatusTransition.transition(studentModality, ModalityProcessStatus.DOCUMENT_REVIEW_TIEBREAKER_REQUIRED, examiner,
                    "Un jurado aprobó y el otro rechazó el documento '" +
                            document.getDocumentConfig().getDocumentName() +
                            "'. Se requiere jurado de desempate para resolver.");
            return null;
        }

        // CASO 5: Uno rechaza, el otro solicita correcciones → el estudiante corrige;
        // solo el jurado que solicitó correcciones debe re-votar; el que rechazó conserva su voto REJECTED.
        // Cuando el que solicitó correcciones vuelva a votar, se re-evaluará el consenso:
        //   - Si aprueba → REJECTED + ACCEPTED → entra a desempate (CASO 4)
        //   - Si rechaza → REJECTED + REJECTED → rechazo definitivo
        //   - Si vuelve a pedir correcciones → ciclo se repite
        boolean rejectedVsCorrections =
                (decision1 == ExaminerDocumentDecision.REJECTED && decision2 == ExaminerDocumentDecision.CORRECTIONS_REQUESTED) ||
                (decision1 == ExaminerDocumentDecision.CORRECTIONS_REQUESTED && decision2 == ExaminerDocumentDecision.REJECTED);

        if (rejectedVsCorrections) {
            // Obtener las notas del jurado que solicitó correcciones
            String correctionNotes = primaryReviews.stream()
                    .filter(r -> r.getDecision() == ExaminerDocumentDecision.CORRECTIONS_REQUESTED)
                    .map(r -> r.getNotes() != null ? r.getNotes() : "")
                    .findFirst()
                    .orElse(notes != null ? notes : "");

            int currentAttempts = studentModality.getCorrectionAttempts() == null ? 0 : studentModality.getCorrectionAttempts();
            int newAttempts = currentAttempts + 1;

            if (newAttempts > 3) {
                // Agotó intentos → rechazo definitivo
                studentModality.setCorrectionAttempts(newAttempts);
                modalityStatusTransition.transition(studentModality, ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL, examiner,
                        "Rechazado definitivamente tras agotar 3 intentos de corrección. " +
                                "Documento: " + document.getDocumentConfig().getDocumentName());

                document.setStatus(DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER);
                document.setNotes(correctionNotes);
                studentDocumentRepository.save(document);

                List<StudentModalityMember> members = studentModalityMemberRepository
                        .findByStudentModalityIdAndStatus(studentModality.getId(), MemberStatus.ACTIVE);
                for (StudentModalityMember member : members) {
                    applicationEventPublisher.publishEvent(new ModalityEvent(NotificationType.CORRECTION_REJECTED_FINAL, studentModality.getId(), examiner.getId(), Map.of(
                            ModalityEvent.KEY_DOCUMENT_ID, document.getId(),
                            ModalityEvent.KEY_STUDENT_ID, member.getStudent().getId(),
                            ModalityEvent.KEY_DOCUMENT_NAME, document.getDocumentConfig().getDocumentName(),
                            ModalityEvent.KEY_REASON, correctionNotes
                    )));
                }

                return Map.of(
                        "success", true,
                        "message", "La propuesta ha sido rechazada definitivamente. El estudiante agotó las 3 oportunidades.",
                        "documentId", document.getId(),
                        "newModalityStatus", ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL,
                        "attemptsUsed", newAttempts
                );
            }

            // Solicitar correcciones: solo el jurado que las pidió deberá re-votar
            document.setStatus(DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER);
            document.setNotes("Correcciones solicitadas por un jurado (el otro rechazó). " +
                    "Una vez corregido, el jurado que solicitó correcciones decidirá si aprueba o rechaza.\n" +
                    correctionNotes);
            studentDocumentRepository.save(document);

            studentModality.setCorrectionAttempts(newAttempts);
            LocalDateTime now = LocalDateTime.now();
            studentModality.setCorrectionRequestDate(now);
            studentModality.setCorrectionDeadline(now.plusDays(30));
            studentModality.setCorrectionReminderSent(false);
            modalityStatusTransition.transition(studentModality, ModalityProcessStatus.CORRECTIONS_REQUESTED_EXAMINERS, examiner,
                    "Un jurado rechazó y el otro solicitó correcciones (intento " + newAttempts +
                            " de 3). El estudiante debe corregir para que el jurado que solicitó correcciones " +
                            "decida si aprueba o rechaza. Observaciones: " + correctionNotes);

            List<StudentModalityMember> activeMembers = studentModalityMemberRepository
                    .findByStudentModalityIdAndStatus(studentModality.getId(), MemberStatus.ACTIVE);
            for (StudentModalityMember member : activeMembers) {
                applicationEventPublisher.publishEvent(new ModalityEvent(NotificationType.DOCUMENT_CORRECTIONS_REQUESTED, studentModality.getId(), examiner.getId(), Map.of(
                        ModalityEvent.KEY_STUDENT_DOCUMENT_ID, document.getId(),
                        ModalityEvent.KEY_STUDENT_ID, member.getStudent().getId(),
                        ModalityEvent.KEY_OBSERVATIONS, "Un jurado rechazó el documento y el otro solicitó correcciones. " +
                                "Por favor corrija y resuba el documento. Observaciones: " + correctionNotes,
                        ModalityEvent.KEY_REQUESTED_BY, NotificationRecipientType.PROGRAM_CURRICULUM_COMMITTEE.name()
                )));
            }
            return null;
        }

        return null;
    }

    /**
     * Aplica la decisión del jurado de desempate sobre el documento (es definitiva).
     */
    private Map<String, Object> processTiebreakerDocumentDecision(StudentDocument document, StudentModality studentModality, User tiebreaker, ExaminerDocumentDecision decision, String notes) {

        switch (decision) {
            case ACCEPTED -> {
                document.setStatus(DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW);
                document.setNotes("Aprobado por el jurado de desempate");
                studentDocumentRepository.save(document);

                modalityStatusTransition.recordHistory(studentModality, studentModality.getStatus(), tiebreaker,
                        "Jurado de desempate aprobó el documento: " +
                                document.getDocumentConfig().getDocumentName());

                checkAndTransitionIfAllMandatoryApprovedByExaminers(document, studentModality, tiebreaker);
            }
            case CORRECTIONS_REQUESTED -> {
                applyCorrectionsRequestedFromTiebreaker(document, studentModality, tiebreaker, notes);
            }
            case REJECTED -> {
                applyRejectionByTiebreaker(document, studentModality, tiebreaker, notes);
            }
        }
        return null;
    }

    /**
     * Aplica correcciones solicitadas por jurados primarios al estudiante.
     */
    private Map<String, Object> applyCorrectionsRequestedByPrimaryExaminers(StudentDocument document, StudentModality studentModality, User examiner, List<ExaminerDocumentReview> reviews, String notes) {

        // ===== LÓGICA DE CONTADOR DE INTENTOS =====
        // Solo incrementar el contador si la modalidad NO está ya en estado CORRECTIONS_REQUESTED_EXAMINERS
        // Esto evita que si ambos jurados solicitan correcciones, se cuente como 2 intentos en lugar de 1
        boolean shouldIncrementAttempt = studentModality.getStatus() != ModalityProcessStatus.CORRECTIONS_REQUESTED_EXAMINERS;
        
        int currentAttempts = studentModality.getCorrectionAttempts() == null ? 0 : studentModality.getCorrectionAttempts();
        int newAttempts = shouldIncrementAttempt ? currentAttempts + 1 : currentAttempts;

        if (newAttempts > 3) {
            studentModality.setCorrectionAttempts(newAttempts);
            modalityStatusTransition.transition(studentModality, ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL, examiner,
                    "Rechazado definitivamente. El estudiante agotó 3 oportunidades de corrección. Documento: " +
                            document.getDocumentConfig().getDocumentName());

            List<StudentModalityMember> members = studentModalityMemberRepository
                    .findByStudentModalityIdAndStatus(studentModality.getId(), MemberStatus.ACTIVE);
            for (StudentModalityMember member : members) {
                applicationEventPublisher.publishEvent(new ModalityEvent(NotificationType.CORRECTION_REJECTED_FINAL, studentModality.getId(), examiner.getId(), Map.of(
                        ModalityEvent.KEY_DOCUMENT_ID, document.getId(),
                        ModalityEvent.KEY_STUDENT_ID, member.getStudent().getId(),
                        ModalityEvent.KEY_DOCUMENT_NAME, document.getDocumentConfig().getDocumentName(),
                        ModalityEvent.KEY_REASON, notes
                )));
            }

            return Map.of(
                    "success", true,
                    "message", "La propuesta ha sido rechazada definitivamente. El estudiante agotó las 3 oportunidades.",
                    "documentId", document.getId(),
                    "newModalityStatus", ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL,
                    "attemptsUsed", newAttempts
            );
        }

        String combinedNotes = reviews.stream()
                .filter(r -> r.getDecision() == ExaminerDocumentDecision.CORRECTIONS_REQUESTED && r.getNotes() != null)
                .map(ExaminerDocumentReview::getNotes)
                .collect(Collectors.joining(" | "));
        if (combinedNotes.isBlank() && notes != null) combinedNotes = notes;

        document.setStatus(DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER);
        document.setNotes(combinedNotes);
        studentDocumentRepository.save(document);

        studentModality.setCorrectionAttempts(newAttempts);
        LocalDateTime now = LocalDateTime.now();
        studentModality.setCorrectionRequestDate(now);
        studentModality.setCorrectionDeadline(now.plusDays(30));
        studentModality.setCorrectionReminderSent(false);

        // Trazabilidad: indicar si este es un nuevo intento o una solicitud adicional del mismo intento
        String attemptMessage = shouldIncrementAttempt
                ? "Jurados solicitaron correcciones (intento " + newAttempts + " de 3): " + combinedNotes
                : "Jurado adicional solicitó correcciones para el intento " + newAttempts + " (ya en proceso): " + combinedNotes;

        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.CORRECTIONS_REQUESTED_EXAMINERS, examiner, attemptMessage);

        List<StudentModalityMember> activeMembers = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(studentModality.getId(), MemberStatus.ACTIVE);
        for (StudentModalityMember member : activeMembers) {
            applicationEventPublisher.publishEvent(new ModalityEvent(NotificationType.DOCUMENT_CORRECTIONS_REQUESTED, studentModality.getId(), examiner.getId(), Map.of(
                    ModalityEvent.KEY_STUDENT_DOCUMENT_ID, document.getId(),
                    ModalityEvent.KEY_STUDENT_ID, member.getStudent().getId(),
                    ModalityEvent.KEY_OBSERVATIONS, combinedNotes,
                    ModalityEvent.KEY_REQUESTED_BY, NotificationRecipientType.EXAMINER.name()
            )));
        }
        return null;
    }

    /**
     * Verifica si el documento es de tipo SECONDARY (documento final) para aplicar lógica de cancelación.
     * Si es documento SECONDARY y hay rechazo, cancela la modalidad completamente.
     * Si es documento MANDATORY, mantiene la lógica actual.
     *
     * @param document el documento siendo evaluado
     * @return true si es documento final (SECONDARY)
     */
    private boolean isFinalDocument(StudentDocument document) {
        return document.getDocumentConfig().getDocumentType() == DocumentType.SECONDARY;
    }

    /**
     * Cancela la modalidad por consenso de rechazo en documento final.
     * - Cambia estado de modalidad a MODALITY_CANCELLED
     * - Elimina la relación estudiante-modalidad (StudentModalityMember)
     * - Registra el cambio en historial
     * - Notifica al estudiante
     */
    private Map<String, Object> cancelModalityByFinalDocumentRejection(StudentDocument document, StudentModality studentModality, User examiner, String reason) {

        // Cambiar estado de modalidad a MODALITY_CANCELLED
        String observations = "Modalidad cancelada por rechazo de documento final. " +
                "Documento: " + document.getDocumentConfig().getDocumentName() + ". " +
                (reason != null && !reason.isBlank() ? "Motivo: " + reason : "Documento rechazado por los jurados.");

        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.MODALITY_CANCELLED, examiner, observations);

        // Obtener y eliminar miembros activos (relación estudiante-modalidad)
        List<StudentModalityMember> members = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(studentModality.getId(), MemberStatus.ACTIVE);
        
        for (StudentModalityMember member : members) {
            studentModalityMemberRepository.delete(member);
        }

        // Actualizar estado del documento
        document.setStatus(DocumentStatus.REJECTED_FOR_EXAMINER_REVIEW);
        document.setNotes("Documento final rechazado - Modalidad cancelada");
        studentDocumentRepository.save(document);

        // Notificar a los estudiantes
        for (StudentModalityMember member : members) {
            applicationEventPublisher.publishEvent(
                    new ModalityEvent(NotificationType.CORRECTION_REJECTED_FINAL, studentModality.getId(), examiner.getId(), Map.of(
                            ModalityEvent.KEY_DOCUMENT_ID, document.getId(),
                            ModalityEvent.KEY_STUDENT_ID, member.getStudent().getId(),
                            ModalityEvent.KEY_DOCUMENT_NAME, document.getDocumentConfig().getDocumentName(),
                            ModalityEvent.KEY_REASON, "Modalidad cancelada por rechazo de documento final. " +
                                    (reason != null && !reason.isBlank() ? reason : "Documento rechazado por los jurados. Puedes iniciar una nueva modalidad.")
                    ))
            );
        }

        return Map.of(
                "success", true,
                "message", "La modalidad ha sido cancelada por rechazo de documento final. Puedes iniciar una nueva modalidad.",
                "documentId", document.getId(),
                "documentName", document.getDocumentConfig().getDocumentName(),
                "newModalityStatus", ModalityProcessStatus.MODALITY_CANCELLED.name(),
                "deletedMembers", members.size()
        );
    }

    /**
     * Aplica correcciones solicitadas por el jurado de desempate.
     */
    private void applyCorrectionsRequestedFromTiebreaker(StudentDocument document, StudentModality studentModality, User tiebreaker, String notes) {

        // ===== LÓGICA DE CONTADOR DE INTENTOS =====
        // Solo incrementar el contador si la modalidad NO está ya en estado CORRECTIONS_REQUESTED_EXAMINERS
        // Esto evita que si el jurado de desempate solicita correcciones en paralelo con jurados primarios,
        // se cuente como 2 intentos en lugar de 1
        boolean shouldIncrementAttempt = studentModality.getStatus() != ModalityProcessStatus.CORRECTIONS_REQUESTED_EXAMINERS;
        
        int currentAttempts = studentModality.getCorrectionAttempts() == null ? 0 : studentModality.getCorrectionAttempts();
        int newAttempts = shouldIncrementAttempt ? currentAttempts + 1 : currentAttempts;

        document.setStatus(DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER);
        document.setNotes(notes);
        studentDocumentRepository.save(document);

        studentModality.setCorrectionAttempts(newAttempts);
        LocalDateTime now = LocalDateTime.now();
        studentModality.setCorrectionRequestDate(now);
        studentModality.setCorrectionDeadline(now.plusDays(30));
        studentModality.setCorrectionReminderSent(false);

        // Trazabilidad: indicar si este es un nuevo intento o una solicitud adicional del mismo intento
        String attemptMessage = shouldIncrementAttempt
                ? "Jurado de desempate solicitó correcciones (intento " + newAttempts + " de 3): " + notes
                : "Jurado de desempate solicitó correcciones para el intento " + newAttempts + " (ya en proceso): " + notes;

        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.CORRECTIONS_REQUESTED_EXAMINERS, tiebreaker, attemptMessage);

        List<StudentModalityMember> activeMembers = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(studentModality.getId(), MemberStatus.ACTIVE);
        for (StudentModalityMember member : activeMembers) {
            applicationEventPublisher.publishEvent(new ModalityEvent(NotificationType.DOCUMENT_CORRECTIONS_REQUESTED, studentModality.getId(), tiebreaker.getId(), Map.of(
                    ModalityEvent.KEY_STUDENT_DOCUMENT_ID, document.getId(),
                    ModalityEvent.KEY_STUDENT_ID, member.getStudent().getId(),
                    ModalityEvent.KEY_OBSERVATIONS, notes,
                    ModalityEvent.KEY_REQUESTED_BY, NotificationRecipientType.EXAMINER.name()
            )));
        }
    }

    /**
     * Aplica rechazo definitivo por ambos jurados primarios.
     * Si es un documento final (SECONDARY), cancela la modalidad completamente.
     * Si es documento MANDATORY, marca como rechazado para correcciones.
     */
    private Map<String, Object> applyRejectionByBothPrimaryExaminers(StudentDocument document, StudentModality studentModality, User examiner, String notes) {

        // Verificar si es un documento final (SECONDARY)
        if (isFinalDocument(document)) {
            return cancelModalityByFinalDocumentRejection(document, studentModality, examiner, notes);
        }

        // Lógica existente para documentos MANDATORY
        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL, examiner,
                "Ambos jurados principales rechazaron el documento: " +
                        document.getDocumentConfig().getDocumentName());

        document.setStatus(DocumentStatus.REJECTED_FOR_EXAMINER_REVIEW);
        document.setNotes("Rechazado por ambos jurados principales");
        studentDocumentRepository.save(document);

        List<StudentModalityMember> members = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(studentModality.getId(), MemberStatus.ACTIVE);
        for (StudentModalityMember member : members) {
            applicationEventPublisher.publishEvent(new ModalityEvent(NotificationType.CORRECTION_REJECTED_FINAL, studentModality.getId(), examiner.getId(), Map.of(
                    ModalityEvent.KEY_DOCUMENT_ID, document.getId(),
                    ModalityEvent.KEY_STUDENT_ID, member.getStudent().getId(),
                    ModalityEvent.KEY_DOCUMENT_NAME, document.getDocumentConfig().getDocumentName(),
                    ModalityEvent.KEY_REASON, "Rechazado por ambos jurados principales. " + (notes != null ? notes : "")
            )));
        }

        return Map.of(
                "success", true,
                "message", "El documento fue rechazado por ambos jurados principales.",
                "documentId", document.getId(),
                "newModalityStatus", ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL
        );
    }

    /**
     * Aplica rechazo definitivo por el jurado de desempate.
     * Si es un documento final (SECONDARY), cancela la modalidad completamente.
     * Si es documento MANDATORY, marca como rechazado para correcciones.
     */
    private void applyRejectionByTiebreaker(StudentDocument document, StudentModality studentModality, User tiebreaker, String notes) {

        // Verificar si es un documento final (SECONDARY)
        if (isFinalDocument(document)) {
            cancelModalityByFinalDocumentRejection(document, studentModality, tiebreaker, notes);
            return;
        }

        // Lógica existente para documentos MANDATORY
        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL, tiebreaker,
                "Jurado de desempate rechazó el documento: " +
                        document.getDocumentConfig().getDocumentName() + ". " + (notes != null ? notes : ""));

        document.setStatus(DocumentStatus.REJECTED_FOR_EXAMINER_REVIEW);
        document.setNotes("Rechazado por el jurado de desempate");
        studentDocumentRepository.save(document);

        List<StudentModalityMember> members = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(studentModality.getId(), MemberStatus.ACTIVE);
        for (StudentModalityMember member : members) {
            applicationEventPublisher.publishEvent(new ModalityEvent(NotificationType.CORRECTION_REJECTED_FINAL, studentModality.getId(), tiebreaker.getId(), Map.of(
                    ModalityEvent.KEY_DOCUMENT_ID, document.getId(),
                    ModalityEvent.KEY_STUDENT_ID, member.getStudent().getId(),
                    ModalityEvent.KEY_DOCUMENT_NAME, document.getDocumentConfig().getDocumentName(),
                    ModalityEvent.KEY_REASON, "Rechazado por jurado de desempate. " + (notes != null ? notes : "")
            )));
        }
    }

    /**
     * Aplica la decisión del examiner al documento directamente (cuando solo hay un jurado).
     */
    private void applyExaminerDecisionToDocument(StudentDocument document, StudentModality studentModality, User examiner, ExaminerDocumentDecision decision, String notes, boolean singleExaminer) {

        DocumentStatus newStatus = switch (decision) {
            case ACCEPTED -> DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW;
            case REJECTED -> DocumentStatus.REJECTED_FOR_EXAMINER_REVIEW;
            case CORRECTIONS_REQUESTED -> DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER;
        };
        document.setStatus(newStatus);
        document.setNotes(notes);
        studentDocumentRepository.save(document);

        if (decision == ExaminerDocumentDecision.ACCEPTED) {
            checkAndTransitionIfAllMandatoryApprovedByExaminers(document, studentModality, examiner);
        }
    }

    /**
     * Punto de entrada: se invoca cada vez que un documento es aprobado por consenso de jurados.
     * Usa el TIPO del documento (MANDATORY o SECONDARY) para determinar la fase y delegar.
     *
     * - Documento MANDATORY aprobado → verifica si todos los MANDATORY están listos → PROPOSAL_APPROVED
     * - Documento SECONDARY aprobado → verifica si todos los SECONDARY están listos → FINAL_REVIEW_COMPLETED
     */
    private void checkAndTransitionIfAllMandatoryApprovedByExaminers(StudentDocument approvedDocument, StudentModality studentModality, User responsible) {
        DocumentType approvedDocType = approvedDocument.getDocumentConfig().getDocumentType();

        if (approvedDocType == DocumentType.SECONDARY) {
            checkAndTransitionIfAllSecondaryApprovedByExaminers(studentModality, responsible);
        } else {
            // MANDATORY (o cualquier otro tipo en fase de propuesta)
            checkAndTransitionIfAllMandatoryDocs(studentModality, responsible);
        }
    }

    /**
     * Sobrecarga de compatibilidad cuando no se tiene el documento disponible.
     * Usa el estado de la modalidad como heurístico de fase.
     * NOTA: preferir siempre la versión con documento cuando sea posible.
     */
    private void checkAndTransitionIfAllMandatoryApprovedByExaminers(StudentModality studentModality, User responsible) {
        // Heurístico: READY_FOR_DEFENSE indica que la modalidad está en fase de revisión de SECONDARY
        boolean isSecondaryPhase = studentModality.getStatus() == ModalityProcessStatus.READY_FOR_DEFENSE;
        if (isSecondaryPhase) {
            checkAndTransitionIfAllSecondaryApprovedByExaminers(studentModality, responsible);
        } else {
            checkAndTransitionIfAllMandatoryDocs(studentModality, responsible);
        }
    }

    /**
     * Verifica que todos los documentos MANDATORY con requiresProposalEvaluation=true
     * estén en ACCEPTED_FOR_EXAMINER_REVIEW.
     * Solo los documentos de propuesta MANDATORY marcados para evaluación por jurado son verificados aquí.
     * Si todos están aprobados → DOCUMENTS_APPROVED_BY_EXAMINERS → PROPOSAL_APPROVED + notificaciones.
     */
    private void checkAndTransitionIfAllMandatoryDocs(StudentModality studentModality, User responsible) {
        Long modalityId = studentModality.getProgramDegreeModality().getDegreeModality().getId();

        // Solo documentos MANDATORY que requieren evaluación de propuesta (propuesta de grado, etc.)
        List<RequiredDocument> evaluableMandatoryDocs = requiredDocumentRepository
                .findByModalityIdAndActiveTrue(modalityId)
                .stream()
                .filter(req -> req.getDocumentType() == DocumentType.MANDATORY
                        && Boolean.TRUE.equals(req.isRequiresProposalEvaluation()))
                .toList();

        if (evaluableMandatoryDocs.isEmpty()) return;

        for (RequiredDocument reqDoc : evaluableMandatoryDocs) {
            StudentDocument doc = studentDocumentRepository
                    .findByStudentModalityIdAndDocumentConfigId(studentModality.getId(), reqDoc.getId())
                    .orElse(null);
            if (doc == null || doc.getStatus() != DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW) {
                return; // Aún hay documentos MANDATORY evaluables sin aprobar
            }
        }

        // ✅ Todos los documentos MANDATORY evaluables han sido aprobados
        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.DOCUMENTS_APPROVED_BY_EXAMINERS, responsible,
                "Los documentos de propuesta obligatorios han sido aprobados por los jurados.");

        // → PROPOSAL_APPROVED automático
        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.PROPOSAL_APPROVED, responsible,
                "Propuesta aprobada automáticamente por consenso de jurados.");

        List<StudentModalityMember> activeMembers = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(studentModality.getId(), MemberStatus.ACTIVE);
        for (StudentModalityMember member : activeMembers) {
            applicationEventPublisher.publishEvent(
                    new ModalityEvent(NotificationType.MODALITY_APPROVED_BY_EXAMINERS, studentModality.getId(), member.getStudent().getId(), Map.of())
            );
        }
    }

    /**
     * Verifica que todos los documentos SECONDARY con requiresProposalEvaluation=true
     * estén en ACCEPTED_FOR_EXAMINER_REVIEW.
     * Solo los documentos SECONDARY marcados para evaluación por jurado son verificados aquí.
     * Si todos están aprobados → SECONDARY_DOCUMENTS_APPROVED_BY_EXAMINERS → FINAL_REVIEW_COMPLETED
     * + ExaminerFinalReviewCompletedEvent (notifica al director para programar sustentación).
     */
    private void checkAndTransitionIfAllSecondaryApprovedByExaminers(StudentModality studentModality, User responsible) {
        // Válido en fases de revisión final (READY_FOR_DEFENSE o CORRECTIONS_SUBMITTED_TO_EXAMINERS)
        boolean isInFinalReviewPhase = studentModality.getStatus() == ModalityProcessStatus.READY_FOR_DEFENSE ||
                studentModality.getStatus() == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_EXAMINERS;
        if (!isInFinalReviewPhase) {
            return;
        }

        Long modalityId = studentModality.getProgramDegreeModality().getDegreeModality().getId();

        // Solo documentos SECONDARY que requieren evaluación por jurado
        List<RequiredDocument> evaluableSecondaryDocs = requiredDocumentRepository
                .findByModalityIdAndActiveTrue(modalityId)
                .stream()
                .filter(req -> req.getDocumentType() == DocumentType.SECONDARY
                        && Boolean.TRUE.equals(req.isRequiresProposalEvaluation()))
                .toList();

        if (evaluableSecondaryDocs.isEmpty()) return;

        for (RequiredDocument reqDoc : evaluableSecondaryDocs) {
            StudentDocument doc = studentDocumentRepository
                    .findByStudentModalityIdAndDocumentConfigId(studentModality.getId(), reqDoc.getId())
                    .orElse(null);
            if (doc == null || doc.getStatus() != DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW) {
                return; // Aún hay documentos SECONDARY evaluables sin aprobar
            }
        }

        // ✅ Todos los SECONDARY aprobados → estado intermedio trazable
        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.SECONDARY_DOCUMENTS_APPROVED_BY_EXAMINERS, responsible,
                "Todos los documentos finales han sido aprobados por consenso de jurados.");

        // → FINAL_REVIEW_COMPLETED automático
        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.FINAL_REVIEW_COMPLETED, responsible,
                "Revisión final completada automáticamente por aprobación de jurados. " +
                        "Notificando al director de proyecto para programar la sustentación.");

        // → Notificar al director para que programe la sustentación
        User projectDirector = studentModality.getProjectDirector();
        if (projectDirector != null) {
            applicationEventPublisher.publishEvent(
                    new ModalityEvent(NotificationType.EXAMINER_FINAL_REVIEW_COMPLETED, studentModality.getId(), projectDirector.getId(), Map.of(
                            ModalityEvent.KEY_PROJECT_DIRECTOR_ID, projectDirector.getId()
                    ))
            );
        }
    }

    @Transactional
    public Map<String, Object> approveModalityByExaminers(Long studentModalityId) {
        User examiner = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modality not found"));

        Long academicProgramId = studentModality.getAcademicProgram().getId();

        boolean isAuthorized =
                programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                        examiner.getId(),
                        academicProgramId,
                        ProgramRole.EXAMINER
                );

        if (!isAuthorized) {
            throw new ForbiddenException("No tienes permisos para aprobar modalidades de este programa académico");
        }

        if (studentModality.getStatus() != ModalityProcessStatus.EXAMINERS_ASSIGNED &&
            studentModality.getStatus() != ModalityProcessStatus.DOCUMENTS_APPROVED_BY_EXAMINERS &&
            studentModality.getStatus() != ModalityProcessStatus.CANCELLATION_REJECTED) {
            throw new ValidationException("La modalidad debe estar en estado EXAMINERS_ASSIGNED o DOCUMENTS_APPROVED_BY_EXAMINERS. Todos los documentos obligatorios deben haber sido aceptados por los jurados.");
        }

        Long modalityId =
                studentModality
                        .getProgramDegreeModality()
                        .getDegreeModality()
                        .getId();

        // Solo se validan los documentos MANDATORY que requieren evaluación de propuesta por el jurado
        List<RequiredDocument> mandatoryDocuments =
                requiredDocumentRepository.findByModalityIdAndActiveTrueAndDocumentType(modalityId, DocumentType.MANDATORY)
                        .stream()
                        .filter(RequiredDocument::isRequiresProposalEvaluation)
                        .toList();

        List<StudentDocument> uploadedDocuments =
                studentDocumentRepository.findByStudentModalityId(studentModalityId);

        Map<Long, StudentDocument> uploadedMap =
                uploadedDocuments.stream()
                        .collect(Collectors.toMap(
                                doc -> doc.getDocumentConfig().getId(),
                                doc -> doc
                        ));

        List<Map<String, Object>> invalidDocuments = new ArrayList<>();

        for (RequiredDocument required : mandatoryDocuments) {
            StudentDocument uploaded = uploadedMap.get(required.getId());
            if (uploaded == null) {
                invalidDocuments.add(
                        Map.of(
                                "documentName", required.getDocumentName(),
                                "status", "NOT_UPLOADED"
                        )
                );
                continue;
            }
            if (uploaded.getStatus() != DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW) {
                invalidDocuments.add(
                        Map.of(
                                "documentName", required.getDocumentName(),
                                "status", uploaded.getStatus()
                        )
                );
            }
        }

        if (!invalidDocuments.isEmpty()) {
            throw new ValidationException("Para poder aprobar la modalidad, todos los documentos de propuesta de grado evaluables por los jurados deben estar aceptados");
        }

        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.PROPOSAL_APPROVED, examiner,
                "Modalidad aprobada por los jurados");

        // Notificar a todos los estudiantes miembros activos
        List<StudentModalityMember> activeMembers = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(studentModality.getId(), MemberStatus.ACTIVE);
        for (StudentModalityMember member : activeMembers) {
            applicationEventPublisher.publishEvent(
                    new ModalityEvent(NotificationType.MODALITY_APPROVED_BY_EXAMINERS, studentModality.getId(), member.getStudent().getId(), Map.of())
            );
        }

        return Map.of(
                "approved", true,
                "newStatus", ModalityProcessStatus.PROPOSAL_APPROVED,
                "message", "Modalidad aprobada correctamente por los jurados"
        );
    }

    @Transactional
    public Map<String, Object> reviewStudentDocumentByCommittee(Long studentDocumentId, DocumentReviewDTO request) {

        User committeeMember = SecurityUtils.getCurrentUser();

        StudentDocument document = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new NotFoundException("Documento no encontrado"));

        StudentModality studentModality = document.getStudentModality();
        Long academicProgramId = studentModality.getAcademicProgram().getId();
        if (document.getStatus() == DocumentStatus.REJECTED_FOR_PROGRAM_HEAD_REVIEW ||
            document.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD) {
            throw new ValidationException("No se puede cambiar el estado del documento porque está en estado " + document.getStatus() + ". El estudiante debe primero corregir y resubir el documento para que pueda ser revisado por el comité de currículo de programa.");
        }

        if ( document.getStatus() == DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW){
            throw new ValidationException("No se puede cambiar el estado del documento porque ya fue aprobado por los jurados. El comité de currículo de programa solo puede revisar documentos que aún no han sido aprobados por los jurados.");
        }

        // Validación: no permitir revisión si la modalidad está en un estado propio de la jefatura de programa
        ModalityProcessStatus modalityStatus = studentModality.getStatus();
        if (modalityStatus == ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD ||
            modalityStatus == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD ||
            modalityStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD) {
            throw new ValidationException("No se puede revisar el documento en este momento. La modalidad se encuentra en estado '" +
                       ModalityServiceUtils.describeModalityStatus(modalityStatus) + "', que corresponde a una etapa de revisión por parte de la Jefatura de Programa. El comité podrá revisar el documento una vez la jefatura finalice su proceso.");
        }

        if (modalityStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_EXAMINERS) {
            throw new ValidationException("No se puede revisar el documento en este momento. La modalidad se encuentra en estado '" +
                       ModalityServiceUtils.describeModalityStatus(modalityStatus) + "', que corresponde a una etapa de correcciones ya resubmited por parte del estudiante. El comité podrá revisar el documento una vez el estudiante resubmita las correcciones y la modalidad vuelva a un estado de revisión.");
        }

        boolean isAuthorized =
                programAuthorityRepository
                        .existsByUser_IdAndAcademicProgram_IdAndRole(
                                committeeMember.getId(),
                                academicProgramId,
                                ProgramRole.PROGRAM_CURRICULUM_COMMITTEE
                        );

if (!isAuthorized) {
            throw new ForbiddenException("No tienes permisos para aprobar modalidades de este programa académico");
        }

        if ((request.getStatus() == DocumentStatus.REJECTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW ||
                request.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_CURRICULUM_COMMITTEE)
                && (request.getNotes() == null || request.getNotes().isBlank())) {

            throw new ValidationException("Debe proporcionar notas al rechazar o solicitar correcciones");
        }

        document.setStatus(request.getStatus());
        document.setNotes(request.getNotes());
        document.setUploadDate(LocalDateTime.now());
        studentDocumentRepository.save(document);

        documentHistoryRepository.save(
                StudentDocumentStatusHistory.builder()
                        .studentDocument(document)
                        .status(request.getStatus())
                        .changeDate(LocalDateTime.now())
                        .responsible(committeeMember)
                        .observations(request.getNotes())
                        .build()
        );

        if (request.getStatus() ==
                DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_CURRICULUM_COMMITTEE) {

            LocalDateTime now = LocalDateTime.now();

            studentModality.setCorrectionRequestDate(now);
            studentModality.setCorrectionDeadline(now.plusDays(30));
            studentModality.setCorrectionReminderSent(false);
            modalityStatusTransition.transition(studentModality, ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE, committeeMember,
                    "Comité de currículo solicitó correcciones en documento: " +
                            document.getDocumentConfig().getDocumentName() +
                            ". Notas: " + request.getNotes());

            List<StudentModalityMember> activeMembers = studentModalityMemberRepository
                    .findByStudentModalityIdAndStatus(studentModality.getId(), MemberStatus.ACTIVE);

            for (StudentModalityMember member : activeMembers) {
                applicationEventPublisher.publishEvent(
                        new ModalityEvent(NotificationType.DOCUMENT_CORRECTIONS_REQUESTED, studentModality.getId(), committeeMember.getId(), Map.of(
                                ModalityEvent.KEY_STUDENT_DOCUMENT_ID, document.getId(),
                                ModalityEvent.KEY_STUDENT_ID, member.getStudent().getId(),
                                ModalityEvent.KEY_OBSERVATIONS, request.getNotes(),
                                ModalityEvent.KEY_REQUESTED_BY, NotificationRecipientType.PROGRAM_CURRICULUM_COMMITTEE.name()
                        ))
                );
            }
        }

        // ========== VERIFICAR SI TODOS LOS MANDATORY HAN SIDO APROBADOS POR EL COMITÉ ==========
        if (request.getStatus() == DocumentStatus.ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW) {

            DegreeModality degreeModality = studentModality.getProgramDegreeModality().getDegreeModality();

            // Obtener todos los documentos MANDATORY configurados para esta modalidad
            List<RequiredDocument> mandatoryDocs = requiredDocumentRepository
                    .findByModalityIdAndActiveTrueAndDocumentType(degreeModality.getId(), DocumentType.MANDATORY);

            // Obtener los documentos subidos por el estudiante para esta modalidad
            List<StudentDocument> uploadedDocs = studentDocumentRepository
                    .findByStudentModalityId(studentModality.getId());

            // Verificar si todos los MANDATORY están en estado ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW
            boolean allMandatoryApproved = !mandatoryDocs.isEmpty() && mandatoryDocs.stream().allMatch(req ->
                    uploadedDocs.stream().anyMatch(uploaded ->
                            uploaded.getDocumentConfig().getId().equals(req.getId()) &&
                            uploaded.getStatus() == DocumentStatus.ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW
                    )
            );

            if (allMandatoryApproved &&
                studentModality.getStatus() != ModalityProcessStatus.READY_FOR_DIRECTOR_ASSIGNMENT &&
                studentModality.getStatus() != ModalityProcessStatus.APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE &&
                studentModality.getStatus() != ModalityProcessStatus.CANCELLATION_REJECTED) {

                // Determinar el siguiente estado según si la modalidad requiere proceso de sustentación
                boolean requiresDefenseProcess = studentModality.getProgramDegreeModality().isRequiresDefenseProcess();

                if (requiresDefenseProcess) {
                    // Flujo completo: requiere director, jurados y sustentación
                    modalityStatusTransition.transition(studentModality, ModalityProcessStatus.READY_FOR_DIRECTOR_ASSIGNMENT, committeeMember,
                            "Todos los documentos obligatorios han sido aprobados por el Comité de Currículo. " +
                                    "La modalidad está lista para la asignación del Director de Proyecto.");

                    return Map.of(
                                    "success", true,
                                    "documentId", document.getId(),
                                    "documentName", document.getDocumentConfig().getDocumentName(),
                                    "newStatus", document.getStatus(),
                                    "newModalityStatus", ModalityProcessStatus.READY_FOR_DIRECTOR_ASSIGNMENT.name(),
                                    "message", "Documento aprobado. Todos los documentos obligatorios han sido aprobados. " +
                                               "La modalidad está lista para la asignación del Director de Proyecto."
                            );
                } else {
                    // Flujo simplificado: el comité toma decisión final directamente
                    modalityStatusTransition.transition(studentModality, ModalityProcessStatus.APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE, committeeMember,
                            "Todos los documentos obligatorios han sido aprobados por el Comité de Currículo. " +
                                    "Puedes continuar con el proceso de la modalidad ");

                    // Notificar a los estudiantes sobre el nuevo estado
                    List<StudentModalityMember> activeMembers = studentModalityMemberRepository
                            .findByStudentModalityIdAndStatus(studentModality.getId(), MemberStatus.ACTIVE);
                    for (StudentModalityMember member : activeMembers) {
                        applicationEventPublisher.publishEvent(
                                new ModalityEvent(NotificationType.DOCUMENT_CORRECTIONS_REQUESTED, studentModality.getId(), committeeMember.getId(), Map.of(
                                        ModalityEvent.KEY_STUDENT_DOCUMENT_ID, document.getId(),
                                        ModalityEvent.KEY_STUDENT_ID, member.getStudent().getId(),
                                        ModalityEvent.KEY_OBSERVATIONS, "Todos tus documentos han sido aprobados. El Comité de Currículo tomará la decisión final sobre tu modalidad.",
                                        ModalityEvent.KEY_REQUESTED_BY, NotificationRecipientType.PROGRAM_CURRICULUM_COMMITTEE.name()
                                ))
                        );
                    }

                    return Map.of(
                                    "success", true,
                                    "documentId", document.getId(),
                                    "documentName", document.getDocumentConfig().getDocumentName(),
                                    "newStatus", document.getStatus(),
                                    "newModalityStatus", ModalityProcessStatus.APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE.name(),
                                    "message", "Documento aprobado. Todos los documentos obligatorios han sido aprobados. " +
                                               "Puedes continuar con el proceso de la modalidad."
                            );
                }
            }
        }

return Map.of(
                        "success", true,
                        "documentId", document.getId(),
                        "documentName", document.getDocumentConfig().getDocumentName(),
                        "newStatus", document.getStatus(),
                        "message", "Documento revisado correctamente por el comité de currículo de programa"
                );
    }

    @Transactional
    public Map<String, Object> approveCorrectedDocument(Long documentId) {

        User reviewer = SecurityUtils.getCurrentUser();

        StudentDocument document = studentDocumentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Documento no encontrado"));

        StudentModality studentModality = document.getStudentModality();
        Long academicProgramId = studentModality.getAcademicProgram().getId();

        boolean authorized = false;
        ModalityProcessStatus newModalityStatus = null;
        DocumentStatus newDocumentStatus = null;

        ModalityProcessStatus currentStatus = studentModality.getStatus();
        boolean isCorrectionsSubmitted =
                currentStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED ||
                currentStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD ||
                currentStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_COMMITTEE ||
                currentStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_EXAMINERS;

        if (isCorrectionsSubmitted) {

            if (document.getStatus() == DocumentStatus.CORRECTION_RESUBMITTED) {

                if (currentStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD ||
                    currentStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED) {
                    // Verificar si fue solicitado por program head mediante historial
                    List<StudentDocumentStatusHistory> history =
                            documentHistoryRepository.findByStudentDocumentIdOrderByChangeDateDesc(documentId);
                    boolean wasRequestedByProgramHead = history.stream()
                            .anyMatch(h -> h.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD);

                    if (wasRequestedByProgramHead || currentStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD) {
                        authorized = programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                                reviewer.getId(), academicProgramId, ProgramRole.PROGRAM_HEAD);
                        newModalityStatus = ModalityProcessStatus.CORRECTIONS_APPROVED;
                        newDocumentStatus = DocumentStatus.ACCEPTED_FOR_PROGRAM_HEAD_REVIEW;
                    } else {
                        authorized = programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                                reviewer.getId(), academicProgramId, ProgramRole.PROGRAM_CURRICULUM_COMMITTEE);
                        newModalityStatus = ModalityProcessStatus.CORRECTIONS_APPROVED;
                        newDocumentStatus = DocumentStatus.ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW;
                    }
                } else if (currentStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_COMMITTEE) {
                    authorized = programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                            reviewer.getId(), academicProgramId, ProgramRole.PROGRAM_CURRICULUM_COMMITTEE);
                    newModalityStatus = ModalityProcessStatus.CORRECTIONS_APPROVED;
                    newDocumentStatus = DocumentStatus.ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW;
                } else if (currentStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_EXAMINERS) {
                    // Verificar que el revisor es un jurado asignado a esta modalidad
                    authorized = defenseExaminerRepository
                            .findByStudentModalityIdAndExaminerId(studentModality.getId(), reviewer.getId())
                            .isPresent();
                    newModalityStatus = ModalityProcessStatus.CORRECTIONS_APPROVED;
                    newDocumentStatus = DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW;
                }
            }
        }

        if (!authorized) {
            throw new ForbiddenException("No tienes permiso para aprobar este documento");
        }

        document.setStatus(newDocumentStatus);
        document.setUploadDate(LocalDateTime.now());
        studentDocumentRepository.save(document);

        studentModality.setCorrectionRequestDate(null);
        studentModality.setCorrectionDeadline(null);
        studentModality.setCorrectionReminderSent(null);

        if (newDocumentStatus == DocumentStatus.ACCEPTED_FOR_PROGRAM_HEAD_REVIEW) {
            modalityStatusTransition.transition(studentModality, ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD, reviewer,
                    "Correcciones aprobadas. Continúa el proceso de revisión.");
        } else {
            modalityStatusTransition.transition(studentModality, ModalityProcessStatus.UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE, reviewer,
                    "Correcciones aprobadas. Continúa el proceso de revisión.");
        }

        documentHistoryRepository.save(
                StudentDocumentStatusHistory.builder()
                        .studentDocument(document)
                        .status(newDocumentStatus)
                        .changeDate(LocalDateTime.now())
                        .responsible(reviewer)
                        .observations("Correcciones aprobadas. El documento cumple con los requisitos.")
                        .build()
        );

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.CORRECTION_APPROVED, studentModality.getId(), reviewer.getId(), Map.of(
                        ModalityEvent.KEY_DOCUMENT_ID, documentId,
                        ModalityEvent.KEY_STUDENT_ID, studentModality.getLeader().getId(),
                        ModalityEvent.KEY_DOCUMENT_NAME, document.getDocumentConfig().getDocumentName()
                ))
        );

        return Map.of(
                "success", true,
                "message", "Correcciones aprobadas exitosamente. La modalidad continúa su proceso normal.",
                "documentId", documentId,
                "newDocumentStatus", newDocumentStatus,
                "newModalityStatus", studentModality.getStatus()
        );
    }

    @Transactional
    public Map<String, Object> rejectCorrectedDocumentFinal(Long documentId, String reason) {

        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Debe proporcionar el motivo del rechazo definitivo");
        }

        User reviewer = SecurityUtils.getCurrentUser();

        StudentDocument document = studentDocumentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Documento no encontrado"));

        StudentModality studentModality = document.getStudentModality();
        Long academicProgramId = studentModality.getAcademicProgram().getId();

        boolean authorized = false;
        ModalityProcessStatus currentStatus = studentModality.getStatus();
        boolean isCorrectionsSubmitted =
                currentStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED ||
                currentStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD ||
                currentStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_COMMITTEE ||
                currentStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_EXAMINERS;

        if (isCorrectionsSubmitted) {
            if (currentStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD) {
                authorized = programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                        reviewer.getId(), academicProgramId, ProgramRole.PROGRAM_HEAD);
            } else if (currentStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_COMMITTEE) {
                authorized = programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                        reviewer.getId(), academicProgramId, ProgramRole.PROGRAM_CURRICULUM_COMMITTEE);
            } else if (currentStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_EXAMINERS) {
                authorized = defenseExaminerRepository
                        .findByStudentModalityIdAndExaminerId(studentModality.getId(), reviewer.getId())
                        .isPresent();
            } else {
                // CORRECTIONS_SUBMITTED genérico: verificar por historial
                List<StudentDocumentStatusHistory> history =
                        documentHistoryRepository.findByStudentDocumentIdOrderByChangeDateDesc(documentId);
                boolean wasRequestedByProgramHead = history.stream()
                        .anyMatch(h -> h.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD);
                if (wasRequestedByProgramHead) {
                    authorized = programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                            reviewer.getId(), academicProgramId, ProgramRole.PROGRAM_HEAD);
                } else {
                    authorized = programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                            reviewer.getId(), academicProgramId, ProgramRole.PROGRAM_CURRICULUM_COMMITTEE);
                }
            }
        }

        if (!authorized) {
            throw new ForbiddenException("No tienes permiso para rechazar este documento");
        }

        document.setStatus(DocumentStatus.REJECTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW);
        document.setNotes(reason);
        document.setUploadDate(LocalDateTime.now());
        studentDocumentRepository.save(document);

        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL, reviewer,
                "Modalidad cancelada por rechazo definitivo de correcciones. Motivo: " + reason);

        documentHistoryRepository.save(
                StudentDocumentStatusHistory.builder()
                        .studentDocument(document)
                        .status(DocumentStatus.REJECTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW)
                        .changeDate(LocalDateTime.now())
                        .responsible(reviewer)
                        .observations("Rechazo definitivo: " + reason)
                        .build()
        );

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.CORRECTION_REJECTED_FINAL, studentModality.getId(), reviewer.getId(), Map.of(
                        ModalityEvent.KEY_DOCUMENT_ID, documentId,
                        ModalityEvent.KEY_STUDENT_ID, studentModality.getLeader().getId(),
                        ModalityEvent.KEY_DOCUMENT_NAME, document.getDocumentConfig().getDocumentName(),
                        ModalityEvent.KEY_REASON, reason
                ))
        );

        return Map.of(
                "success", true,
                "message", "Correcciones rechazadas definitivamente. La modalidad ha sido cancelada.",
                "documentId", documentId,
                "finalStatus", ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL
        );
    }

    @Transactional
    public Map<String, Object> closeModalityByCommittee(Long studentModalityId, String reason) {

        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Debe proporcionar el motivo del cierre de la modalidad");
        }

        User committeeMember = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        Long academicProgramId = studentModality.getAcademicProgram().getId();

        boolean isAuthorized = programAuthorityRepository
                .existsByUser_IdAndAcademicProgram_IdAndRole(
                        committeeMember.getId(),
                        academicProgramId,
                        ProgramRole.PROGRAM_CURRICULUM_COMMITTEE
                );

        if (!isAuthorized) {
            throw new ForbiddenException("No tiene permiso para cerrar modalidades de este programa académico. Debe ser miembro del comité de currículo del programa.");
        }

        if (studentModality.getStatus() == ModalityProcessStatus.MODALITY_CLOSED) {
            throw new ValidationException("La modalidad ya se encuentra cerrada");
        }

        ModalityProcessStatus previousStatus = studentModality.getStatus();

        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.MODALITY_CLOSED, committeeMember,
                String.format(
                        "Modalidad cerrada por el comité de currículo del programa.  Motivo: %s",
                        previousStatus,
                        reason
                ));

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_CLOSED_BY_COMMITTEE, studentModality.getId(), committeeMember.getId(), Map.of(
                        ModalityEvent.KEY_STUDENT_ID, studentModality.getLeader().getId(),
                        ModalityEvent.KEY_REASON, reason,
                        ModalityEvent.KEY_COMMITTEE_MEMBER_ID, committeeMember.getId()
                ))
        );

        return Map.of(
                "success", true,
                "studentModalityId", studentModalityId,
                "previousStatus", previousStatus,
                "newStatus", ModalityProcessStatus.MODALITY_CLOSED,
                "closedBy", committeeMember.getName() + " " + committeeMember.getLastName(),
                "reason", reason,
                "message", "Modalidad cerrada exitosamente. El estudiante ha sido notificado por correo electrónico."
        );
    }

    @Transactional
    public Map<String, Object> approveFinalModalityByCommittee(Long studentModalityId, String observations) {

        User committeeMember = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        Long academicProgramId = studentModality.getAcademicProgram().getId();

        boolean isAuthorized = programAuthorityRepository
                .existsByUser_IdAndAcademicProgram_IdAndRole(
                        committeeMember.getId(),
                        academicProgramId,
                        ProgramRole.PROGRAM_CURRICULUM_COMMITTEE
                );

        if (!isAuthorized) {
            throw new ForbiddenException("No tiene permiso para aprobar modalidades de este programa académico. Debe ser miembro del comité de currículo del programa.");
        }

        Map<String, Object> documentValidation = modalityDocumentService.validateAllRequiredDocumentsUploaded(studentModalityId);
        boolean allDocumentsUploaded = (boolean) documentValidation.get("allDocumentsUploaded");

        if (!allDocumentsUploaded) {
            throw new ValidationException("No se puede aprobar la modalidad porque faltan documentos por subir");
        }

        // Validar que TODOS los documentos MANDATORY y SECONDARY estén en estado ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW
        Map<String, Object> acceptedValidation = modalityDocumentService.validateAllDocumentsAcceptedForCommittee(studentModalityId);
        boolean allAccepted = (boolean) acceptedValidation.get("allAccepted");

        if (!allAccepted) {
            throw new ValidationException("No se puede aprobar la modalidad. Todos los documentos iniciales y complementarios deben estar en estado 'ACEPTADO POR COMITÉ'. Revise los documentos del estudiante.");
        }

        if (!(studentModality.getStatus() == ModalityProcessStatus.READY_FOR_DIRECTOR_ASSIGNMENT ||
              studentModality.getStatus() == ModalityProcessStatus.APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE)) {

            throw new ValidationException("La modalidad no está en estado válido para aprobación final por el comité");
        }

        if (studentModality.getStatus() == ModalityProcessStatus.GRADED_APPROVED ||
                studentModality.getStatus() == ModalityProcessStatus.GRADED_FAILED) {

            throw new ValidationException("La modalidad ya ha sido calificada definitivamente");
        }

        ModalityProcessStatus previousStatus = studentModality.getStatus();

        studentModality.setAcademicDistinction(AcademicDistinction.NO_DISTINCTION);
        studentModality.setFinalGrade(null);
        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.GRADED_APPROVED, committeeMember,
                String.format(
                        "Modalidad aprobada definitivamente por el comité de currículo del programa. " +
                        observations != null ? "Observaciones: " + observations : ""
                ));

        List<StudentModalityMember> activeMembers = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(studentModality.getId(), MemberStatus.ACTIVE);

        for (StudentModalityMember member : activeMembers) {
            applicationEventPublisher.publishEvent(
                    new ModalityEvent(NotificationType.MODALITY_FINAL_APPROVED_BY_COMMITTEE, studentModality.getId(), committeeMember.getId(), Map.of(
                            ModalityEvent.KEY_STUDENT_ID, member.getStudent().getId(),
                            ModalityEvent.KEY_OBSERVATIONS, observations,
                            ModalityEvent.KEY_COMMITTEE_MEMBER_ID, committeeMember.getId()
                    ))
            );
        }

        return Map.of(
                "success", true,
                "studentModalityId", studentModalityId,
                "previousStatus", previousStatus,
                "newStatus", ModalityProcessStatus.GRADED_APPROVED,
                "academicDistinction", AcademicDistinction.NO_DISTINCTION,
                "finalGrade", "N/A",
                "approvedBy", committeeMember.getName() + " " + committeeMember.getLastName(),
                "observations", observations != null ? observations : "Sin observaciones",
                "message", "Modalidad aprobada definitivamente. Todos los estudiantes han sido notificados."
        );
    }

    @Transactional
    public Map<String, Object> rejectFinalModalityByCommittee(Long studentModalityId, String reason) {

        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Debe proporcionar la razón del rechazo de la modalidad");
        }

        User committeeMember = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        Long academicProgramId = studentModality.getAcademicProgram().getId();

        boolean isAuthorized = programAuthorityRepository
                .existsByUser_IdAndAcademicProgram_IdAndRole(
                        committeeMember.getId(),
                        academicProgramId,
                        ProgramRole.PROGRAM_CURRICULUM_COMMITTEE
                );

        if (!isAuthorized) {
            throw new ForbiddenException("No tiene permiso para rechazar modalidades de este programa académico. Debe ser miembro del comité de currículo del programa.");
        }

        // Validar que todos los documentos MANDATORY y SECONDARY estén subidos
        Map<String, Object> documentValidation = modalityDocumentService.validateAllRequiredDocumentsUploaded(studentModalityId);
        boolean allDocumentsUploaded = (boolean) documentValidation.get("allDocumentsUploaded");

        if (!allDocumentsUploaded) {
            throw new ValidationException("No se puede rechazar la modalidad porque faltan documentos por subir. " +
                                    "El estudiante debe completar la documentación antes de que el comité pueda tomar una decisión definitiva.");
        }

        // Validar que TODOS los documentos estén en estado ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW
        Map<String, Object> acceptedValidation = modalityDocumentService.validateAllDocumentsAcceptedForCommittee(studentModalityId);
        boolean allAccepted = (boolean) acceptedValidation.get("allAccepted");

        if (!allAccepted) {
            throw new ValidationException("No se puede rechazar la modalidad. Todos los documentos obligatorios y complementarios deben estar en estado 'Aceptado para revisión del comité de currículo' (ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW). Revise los documentos indicados.");
        }

        if (!(studentModality.getStatus() == ModalityProcessStatus.READY_FOR_DIRECTOR_ASSIGNMENT ||
              studentModality.getStatus() == ModalityProcessStatus.APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE)) {

            throw new ValidationException("La modalidad no está en estado válido para rechazo por el comité");
        }

        if (studentModality.getStatus() == ModalityProcessStatus.GRADED_APPROVED ||
                studentModality.getStatus() == ModalityProcessStatus.GRADED_FAILED) {

            throw new ValidationException("La modalidad ya ha sido calificada definitivamente");
        }

        ModalityProcessStatus previousStatus = studentModality.getStatus();

        studentModality.setFinalGrade(null);
        studentModality.setAcademicDistinction(AcademicDistinction.REJECTED_BY_COMMITTEE);
        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.GRADED_FAILED, committeeMember,
                String.format(
                        "Modalidad rechazada definitivamente por el comité de currículo del programa. " +
                        " Motivo: %s",
                        previousStatus,
                        reason
                ));

        List<StudentModalityMember> activeMembers = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(studentModality.getId(), MemberStatus.ACTIVE);

        for (StudentModalityMember member : activeMembers) {
            applicationEventPublisher.publishEvent(
                    new ModalityEvent(NotificationType.MODALITY_REJECTED_BY_COMMITTEE, studentModality.getId(), committeeMember.getId(), Map.of(
                            ModalityEvent.KEY_STUDENT_ID, member.getStudent().getId(),
                            ModalityEvent.KEY_REASON, reason,
                            ModalityEvent.KEY_COMMITTEE_MEMBER_ID, committeeMember.getId()
                    ))
            );
        }

        return Map.of(
                "success", true,
                "studentModalityId", studentModalityId,
                "previousStatus", previousStatus,
                "newStatus", ModalityProcessStatus.GRADED_FAILED,
                "rejectedBy", committeeMember.getName() + " " + committeeMember.getLastName(),
                "reason", reason,
                "message", "Modalidad rechazada definitivamente. Todos los estudiantes han sido notificados."
        );
    }
}
