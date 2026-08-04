package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.Entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.Entity.DegreeModality;
import com.SIGMA.USCO.Modalities.Entity.ModalityProcessStatusHistory;
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
import com.SIGMA.USCO.Modalities.Repository.ModalityProcessStatusHistoryRepository;
import com.SIGMA.USCO.Modalities.Repository.ModalityRequirementsRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Modalities.dto.DocumentReviewDTO;
import com.SIGMA.USCO.Modalities.dto.ValidationItemDTO;
import com.SIGMA.USCO.Modalities.dto.ValidationResultDTO;
import com.SIGMA.USCO.Users.Entity.ProgramAuthority;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.Entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.ProgramDegreeModality;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.ProgramDegreeModalityRepository;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.documents.entity.DocumentEditRequest;
import com.SIGMA.USCO.documents.entity.DocumentEditRequestVote;
import com.SIGMA.USCO.documents.entity.ExaminerDocumentReview;
import com.SIGMA.USCO.documents.entity.FinalDocumentEvaluation;
import com.SIGMA.USCO.documents.entity.ProposalEvaluation;
import com.SIGMA.USCO.documents.entity.RequiredDocument;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.entity.StudentDocumentStatusHistory;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import com.SIGMA.USCO.documents.entity.enums.DocumentEditRequestStatus;
import com.SIGMA.USCO.documents.entity.enums.ExaminerDocumentDecision;
import com.SIGMA.USCO.documents.entity.enums.FinalDocumentRubricType;
import com.SIGMA.USCO.documents.dto.FinalEvaluationRequest;
import com.SIGMA.USCO.documents.dto.ProposalEvaluationRequest;
import com.SIGMA.USCO.documents.repository.DocumentEditRequestRepository;
import com.SIGMA.USCO.documents.repository.DocumentEditRequestVoteRepository;
import com.SIGMA.USCO.documents.repository.ExaminerDocumentReviewRepository;
import com.SIGMA.USCO.documents.repository.FinalDocumentEvaluationRepository;
import com.SIGMA.USCO.documents.repository.ProposalEvaluationRepository;
import com.SIGMA.USCO.documents.repository.RequiredDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentStatusHistoryRepository;
import com.SIGMA.USCO.notifications.entity.enums.NotificationRecipientType;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.event.ModalityEvent;
import com.SIGMA.USCO.security.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentModalityService {

    private final DegreeModalityRepository degreeModalityRepository;
    private final DefenseExaminerRepository defenseExaminerRepository;
    private final ModalityRequirementsRepository modalityRequirementsRepository;
    private final RequiredDocumentRepository requiredDocumentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final ModalityProcessStatusHistoryRepository historyRepository;
    private final StudentDocumentStatusHistoryRepository documentHistoryRepository;
    private final ProgramDegreeModalityRepository programDegreeModalityRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;
    private final ProposalEvaluationRepository proposalEvaluationRepository;
    private final FinalDocumentEvaluationRepository secondaryDocumentEvaluationRepository;
    private final ExaminerDocumentReviewRepository examinerDocumentReviewRepository;
    private final DocumentEditRequestRepository documentEditRequestRepository;
    private final DocumentEditRequestVoteRepository documentEditRequestVoteRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Transactional
    public ResponseEntity<?> startStudentModalityIndividual(Long modalityId) {

        User student = SecurityUtils.getCurrentUser();

        StudentProfile profile = studentProfileRepository.findByUserId(student.getId())
                .orElseThrow(() -> new RuntimeException("Debe completar su perfil académico antes de seleccionar una modalidad"));

        DegreeModality modality = degreeModalityRepository.findById(modalityId)
                .orElseThrow(() -> new RuntimeException("La modalidad con ID " + modalityId + " no existe"));

        ProgramDegreeModality programDegreeModality =
                programDegreeModalityRepository.findByAcademicProgramIdAndDegreeModalityIdAndActiveTrue(profile.getAcademicProgram().getId(), modalityId)
                        .orElseThrow(() -> new RuntimeException("La modalidad no está habilitada para tu programa académico"));

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
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "eligible", false,
                                "message", "Ya tienes una modalidad de grado en curso. No puedes iniciar otra."
                        )
                );
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
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "eligible", false,
                                "message", "No puedes volver a iniciar esta modalidad porque ya fue cerrada anteriormente. Debes seleccionar una modalidad diferente."
                        )
                );
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
            return ResponseEntity.badRequest().body(
                    ValidationResultDTO.builder()
                            .eligible(false)
                            .results(results)
                            .message("No cumples los requisitos académicos para esta modalidad")
                            .build()
            );
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

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD)
                        .changeDate(LocalDateTime.now())
                        .responsible(student)
                        .observations("Modalidad individual iniciada por el estudiante")
                        .build()
        );

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_STARTED, studentModality.getId(), student.getId(), Map.of())
        );

        return ResponseEntity.ok(
                Map.of(
                        "eligible", true,
                        "studentModalityId", studentModality.getId(),
                        "studentModalityName", modality.getName(),
                        "modalityType", "INDIVIDUAL",
                        "message", "Modalidad iniciada correctamente. Puedes subir los documentos."
                )
        );
    }

    public ResponseEntity<?> uploadRequiredDocument(Long studentModalityId, Long requiredDocumentId, MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("El archivo es obligatorio");
        }

        User uploader = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new RuntimeException("Modalidad del estudiante no encontrada"));

        // Verificar si es miembro activo (estudiante) o si es el director asignado a la modalidad
        boolean isActiveMember = studentModalityMemberRepository.isActiveMember(
                studentModalityId,
                uploader.getId()
        );

        boolean isAssignedDirector = studentModality.getProjectDirector() != null &&
                studentModality.getProjectDirector().getId().equals(uploader.getId());

        if (!isActiveMember && !isAssignedDirector) {
            return ResponseEntity.status(403).body("No autorizado para subir documentos a esta modalidad");
        }

        // Para efectos de trazabilidad, usamos 'uploader' como responsable.
        // Si es el director, el folder de almacenamiento sigue siendo el del estudiante líder.
        User student = isAssignedDirector && !isActiveMember
                ? studentModality.getLeader()
                : uploader;

        RequiredDocument requiredDocument = requiredDocumentRepository.findById(requiredDocumentId)
                .orElseThrow(() -> new RuntimeException("Documento requerido no existe"));

        DegreeModality modality = studentModality.getProgramDegreeModality().getDegreeModality();

        if (!requiredDocument.getModality().getId().equals(modality.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("El documento no pertenece a la modalidad seleccionada");
        }

        // Validación: Los documentos de tipo SECONDARY solo pueden ser subidos por el director del proyecto
        if (requiredDocument.getDocumentType() == DocumentType.SECONDARY && !isAssignedDirector) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "success", false,
                            "error", "Acceso denegado",
                            "message", "Este documento solo puede ser subido por el director del proyecto. Por favor, póngase en contacto con el director " +
                                    (studentModality.getProjectDirector() != null
                                            ? studentModality.getProjectDirector().getName() + " " + studentModality.getProjectDirector().getLastName()
                                            : "asignado")
                    ));
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();

        if (requiredDocument.getAllowedFormat() != null &&
                !requiredDocument.getAllowedFormat().toLowerCase().contains(extension)) {
            return ResponseEntity.badRequest().body("Formato de archivo no permitido");
        }

        if (requiredDocument.getMaxFileSizeMB() != null &&
                file.getSize() > requiredDocument.getMaxFileSizeMB() * 1024L * 1024L) {
            return ResponseEntity.badRequest().body("El archivo supera el tamaño permitido");
        }

        String modalityFolder = modality.getName()
                .replaceAll("[^a-zA-Z0-9]", "_");

        String studentFolder = student.getName() + student.getLastName() + "_" +
                student.getLastName() + "_" +
                student.getId();

        Path basePath = Paths.get(
                uploadDir,
                modalityFolder,
                studentFolder
        );

        Files.createDirectories(basePath);

        String finalFileName = UUID.randomUUID() + "_" + originalFilename;
        Path fullPath = basePath.resolve(finalFileName);

        Files.copy(file.getInputStream(), fullPath, StandardCopyOption.REPLACE_EXISTING);

        StudentDocument studentDocument = studentDocumentRepository
                .findByStudentModalityIdAndDocumentConfigId(studentModalityId, requiredDocumentId)
                .orElse(
                        StudentDocument.builder()
                                .studentModality(studentModality)
                                .documentConfig(requiredDocument)
                                .build()
                );

        studentDocument.setFileName(originalFilename);
        studentDocument.setFilePath(fullPath.toString());
        studentDocument.setUploadDate(LocalDateTime.now());

        // ========== LÓGICA DE CORRECCIONES ==========
        // Determinar si se trata de una resubida de correcciones según el estado actual de la modalidad
        ModalityProcessStatus currentModalityStatus = studentModality.getStatus();

        boolean isResubmittingCorrection =
                currentModalityStatus == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD ||
                currentModalityStatus == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE ||
                currentModalityStatus == ModalityProcessStatus.CORRECTIONS_REQUESTED_EXAMINERS;

        // ========== RESUBIDA POR EDICIÓN APROBADA ==========
        // Verificar si el documento existente tiene una solicitud de edición aprobada
        boolean isResubmittingApprovedEdit = false;
        StudentDocument existingDoc = studentDocumentRepository
                .findByStudentModalityIdAndDocumentConfigId(studentModalityId, requiredDocumentId)
                .orElse(null);
        if (existingDoc != null && existingDoc.getStatus() == DocumentStatus.EDIT_REQUEST_APPROVED) {
            isResubmittingApprovedEdit = true;
        }

        if (isResubmittingApprovedEdit) {
              // Cerrar la solicitud de edición aprobada (marcar como completada con el reenvío)
            documentEditRequestRepository
                    .findTopByStudentDocumentIdAndStatusOrderByCreatedAtDesc(
                            existingDoc.getId(), DocumentEditRequestStatus.APPROVED)
                    .ifPresent(req -> {
                        // Los votos ya están registrados; solo guardamos la referencia para trazabilidad
                        documentEditRequestRepository.save(req);
                    });

            // El documento vuelve a PENDING para re-revisión por jurados
            studentDocument.setStatus(DocumentStatus.PENDING);
            studentDocument.setFileName(originalFilename);
            studentDocument.setFilePath(fullPath.toString());
            studentDocument.setUploadDate(LocalDateTime.now());
            studentDocumentRepository.save(studentDocument);

            // Limpiar las reviews anteriores de jurados para este documento (ExaminerDocumentReview)
            List<ExaminerDocumentReview> oldReviews = examinerDocumentReviewRepository
                    .findByStudentDocumentId(studentDocument.getId());
            examinerDocumentReviewRepository.deleteAll(oldReviews);

            // Limpiar también los votos de la solicitud de edición aprobada (DocumentEditRequestVote)
            documentEditRequestRepository
                    .findTopByStudentDocumentIdAndStatusOrderByCreatedAtDesc(
                            existingDoc.getId(), DocumentEditRequestStatus.APPROVED)
                    .ifPresent(req -> {
                        List<DocumentEditRequestVote> editVotes = documentEditRequestVoteRepository
                                .findByEditRequestId(req.getId());
                        documentEditRequestVoteRepository.deleteAll(editVotes);
                    });

            // Cambiar el estado de la modalidad a EXAMINERS_ASSIGNED para que los jurados revisen
            studentModality.setStatus(ModalityProcessStatus.EXAMINERS_ASSIGNED);
            studentModality.setUpdatedAt(LocalDateTime.now());
            studentModalityRepository.save(studentModality);

            // Trazabilidad en el historial del DOCUMENTO
            documentHistoryRepository.save(
                    StudentDocumentStatusHistory.builder()
                            .studentDocument(studentDocument)
                            .status(DocumentStatus.PENDING)
                            .changeDate(LocalDateTime.now())
                            .responsible(uploader)
                            .observations((isAssignedDirector && !isActiveMember ? "Director" : "Estudiante") +
                                    " resubió el documento '" +
                                    originalFilename +
                                    "' tras aprobación de solicitud de edición. Pendiente de re-revisión por jurados.")
                            .build()
            );

            // Trazabilidad en el historial de la MODALIDAD
            historyRepository.save(
                    ModalityProcessStatusHistory.builder()
                            .studentModality(studentModality)
                            .status(ModalityProcessStatus.EXAMINERS_ASSIGNED)
                            .changeDate(LocalDateTime.now())
                            .responsible(uploader)
                            .observations((isAssignedDirector && !isActiveMember ? "Director" : "Estudiante") +
                                    " actualizó el documento '" +
                                    studentDocument.getDocumentConfig().getDocumentName() +
                                    "' con los cambios aprobados por los jurados. " +
                                    "La modalidad regresa al estado de revisión por jurados.")
                            .build()
            );

            applicationEventPublisher.publishEvent(
                    new ModalityEvent(NotificationType.DOCUMENT_UPLOADED, studentModality.getId(), uploader.getId(), Map.of(ModalityEvent.KEY_STUDENT_DOCUMENT_ID, studentDocument.getId(), ModalityEvent.KEY_STUDENT_ID, uploader.getId()))
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Documento actualizado correctamente. Los jurados evaluarán la nueva versión.",
                    "path", fullPath.toString(),
                    "documentStatus", studentDocument.getStatus().name(),
                    "modalityStatus", studentModality.getStatus().name()
            ));

        } else if (isResubmittingCorrection) {
            // Marcar el documento como corrección reenviada
            studentDocument.setStatus(DocumentStatus.CORRECTION_RESUBMITTED);
            studentDocumentRepository.save(studentDocument);

            // Si las correcciones venían de jurados, limpiar SOLO el voto del jurado que solicitó
            // correcciones, conservando el voto ACCEPTED del jurado que ya aprobó
            if (currentModalityStatus == ModalityProcessStatus.CORRECTIONS_REQUESTED_EXAMINERS) {
                List<ExaminerDocumentReview> oldReviews = examinerDocumentReviewRepository
                        .findByStudentDocumentId(studentDocument.getId());
                // Eliminar solo los votos de CORRECTIONS_REQUESTED; los ACCEPTED se conservan
                List<ExaminerDocumentReview> reviewsToDelete = oldReviews.stream()
                        .filter(r -> r.getDecision() == ExaminerDocumentDecision.CORRECTIONS_REQUESTED)
                        .toList();
                examinerDocumentReviewRepository.deleteAll(reviewsToDelete);
            }

            documentHistoryRepository.save(
                    StudentDocumentStatusHistory.builder()
                            .studentDocument(studentDocument)
                            .status(DocumentStatus.CORRECTION_RESUBMITTED)
                            .changeDate(LocalDateTime.now())
                            .responsible(uploader)
                            .observations("Documento corregido reenviado por " +
                                    (isAssignedDirector && !isActiveMember ? "el director" : "el estudiante"))
                            .build()
            );

            // Determinar el nuevo estado de la modalidad según quién solicitó las correcciones
            ModalityProcessStatus newModalityStatusAfterResubmit = switch (currentModalityStatus) {
                case CORRECTIONS_REQUESTED_PROGRAM_HEAD ->
                        ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD;
                case CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE ->
                        ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_COMMITTEE;
                case CORRECTIONS_REQUESTED_EXAMINERS ->
                        ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_EXAMINERS;
                default -> ModalityProcessStatus.CORRECTIONS_SUBMITTED;
            };

            String requesterLabel = switch (currentModalityStatus) {
                case CORRECTIONS_REQUESTED_PROGRAM_HEAD -> "Jefatura de Programa y/o coordinación de modalidades";
                case CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE -> "Comité de Currículo de Programa";
                case CORRECTIONS_REQUESTED_EXAMINERS -> "Jurado evaluador";
                default -> "revisor";
            };

            // Cambiar el estado de la modalidad al estado específico
            studentModality.setStatus(newModalityStatusAfterResubmit);
            studentModality.setUpdatedAt(LocalDateTime.now());
            studentModalityRepository.save(studentModality);

            historyRepository.save(
                    ModalityProcessStatusHistory.builder()
                            .studentModality(studentModality)
                            .status(newModalityStatusAfterResubmit)
                            .changeDate(LocalDateTime.now())
                            .responsible(uploader)
                            .observations("Correcciones enviadas por " +
                                    (isAssignedDirector && !isActiveMember ? "el director" : "el estudiante") +
                                    " tras solicitud de: " + requesterLabel)
                            .build()
            );

            applicationEventPublisher.publishEvent(
                    new ModalityEvent(NotificationType.CORRECTION_RESUBMITTED, studentModality.getId(), student.getId(), Map.of(ModalityEvent.KEY_DOCUMENT_ID, studentDocument.getId(), ModalityEvent.KEY_STUDENT_ID, uploader.getId(), ModalityEvent.KEY_DOCUMENT_NAME, studentDocument.getDocumentConfig().getDocumentName()))
            );

        } else {
            // Subida normal: estado PENDING
            studentDocument.setStatus(DocumentStatus.PENDING);
            studentDocumentRepository.save(studentDocument);

            documentHistoryRepository.save(
                    StudentDocumentStatusHistory.builder()
                            .studentDocument(studentDocument)
                            .status(DocumentStatus.PENDING)
                            .changeDate(LocalDateTime.now())
                            .responsible(uploader)
                            .observations("Documento cargado o actualizado por " +
                                    (isAssignedDirector && !isActiveMember ? "el director" : "el estudiante"))
                            .build()
            );

            applicationEventPublisher.publishEvent(
                    new ModalityEvent(NotificationType.DOCUMENT_UPLOADED, studentModality.getId(), uploader.getId(), Map.of(ModalityEvent.KEY_STUDENT_DOCUMENT_ID, studentDocument.getId(), ModalityEvent.KEY_STUDENT_ID, uploader.getId()))
            );

            // ========== VERIFICAR SI TODOS LOS DOCUMENTOS MANDATORY HAN SIDO SUBIDOS ==========
            checkAndUpdateModalityStatusIfAllMandatoryDocsUploaded(studentModality, uploader);
        }

        return ResponseEntity.ok(
                Map.of(
                        "message", isResubmittingCorrection
                                ? "Documento de corrección enviado correctamente. Será revisado por el evaluador correspondiente."
                                : "Documento subido correctamente",
                        "path", fullPath.toString(),
                        "documentStatus", studentDocument.getStatus().name(),
                        "modalityStatus", studentModality.getStatus().name()
                )
        );
    }

    private void checkAndUpdateModalityStatusIfAllMandatoryDocsUploaded(StudentModality studentModality, User responsibleUser) {

        // Solo aplicar esta lógica si la modalidad está en estado MODALITY_SELECTED
        if (studentModality.getStatus() != ModalityProcessStatus.MODALITY_SELECTED) {
            return;
        }

        Long modalityId = studentModality.getProgramDegreeModality().getDegreeModality().getId();

        // Obtener todos los documentos MANDATORY requeridos para esta modalidad
        List<RequiredDocument> mandatoryDocuments = requiredDocumentRepository
                .findByModalityIdAndActiveTrueAndDocumentType(modalityId, DocumentType.MANDATORY);

        if (mandatoryDocuments.isEmpty()) {
            return; // No hay documentos obligatorios configurados
        }

        // Obtener todos los documentos subidos por el estudiante
        List<StudentDocument> uploadedDocuments = studentDocumentRepository
                .findByStudentModalityId(studentModality.getId());

        Set<Long> uploadedDocumentIds = uploadedDocuments.stream()
                .map(doc -> doc.getDocumentConfig().getId())
                .collect(Collectors.toSet());

        // Verificar si TODOS los documentos MANDATORY han sido subidos
        boolean allMandatoryDocsUploaded = mandatoryDocuments.stream()
                .allMatch(doc -> uploadedDocumentIds.contains(doc.getId()));

        if (allMandatoryDocsUploaded) {
            // Cambiar el estado de la modalidad a UNDER_REVIEW_PROGRAM_HEAD
            studentModality.setStatus(ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD);
            studentModality.setUpdatedAt(LocalDateTime.now());
            studentModalityRepository.save(studentModality);

            // Registrar en el historial
            historyRepository.save(
                    ModalityProcessStatusHistory.builder()
                            .studentModality(studentModality)
                            .status(ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD)
                            .changeDate(LocalDateTime.now())
                            .responsible(responsibleUser)
                            .observations("Todos los documentos obligatorios han sido subidos. " +
                                         "La modalidad pasa automáticamente a revisión del jefe de programa.")
                            .build()
            );

            log.info("Modalidad {} cambió automáticamente a UNDER_REVIEW_PROGRAM_HEAD - Todos los documentos MANDATORY subidos",
                    studentModality.getId());
        }
    }
    public ResponseEntity<?> validateAllDocumentsUploaded(Long studentModalityId) {

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        Long modalityId = studentModality.getProgramDegreeModality().getDegreeModality().getId();

        List<RequiredDocument> requiredDocuments =
                requiredDocumentRepository.findByModalityIdAndActiveTrue(modalityId);

        List<StudentDocument> uploadedDocuments =
                studentDocumentRepository.findByStudentModalityId(studentModalityId);

        Set<Long> uploadedIds = uploadedDocuments.stream()
                .map(d -> d.getDocumentConfig().getId())
                .collect(Collectors.toSet());

        List<String> missingDocuments = requiredDocuments.stream()
                .filter(doc -> doc.getDocumentType() == DocumentType.MANDATORY)
                .filter(doc -> !uploadedIds.contains(doc.getId()))
                .map(RequiredDocument::getDocumentName)
                .toList();

        boolean allUploaded = missingDocuments.isEmpty();

        return ResponseEntity.ok(
                Map.of(
                        "canContinue", allUploaded,
                        "missingDocuments", missingDocuments
                )
        );
    }

    public ResponseEntity<?> getAvailableDocumentsForStudent() {

        User student = SecurityUtils.getCurrentUser();

        Optional<StudentModality> studentModalityOpt = studentModalityRepository
                .findTopByStudentIdOrderByUpdatedAtDesc(student.getId());

        if (studentModalityOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "success", false,
                            "message", "No se encontró una modalidad asociada al estudiante"
                    ));
        }

        StudentModality studentModality = studentModalityOpt.get();
        Long studentModalityId = studentModality.getId();
        Long modalityId = studentModality.getProgramDegreeModality().getDegreeModality().getId();

        List<RequiredDocument> mandatoryDocuments = requiredDocumentRepository
                .findByModalityIdAndActiveTrueAndDocumentType(modalityId, DocumentType.MANDATORY);

        List<StudentDocument> uploadedDocuments = studentDocumentRepository
                .findByStudentModalityId(studentModalityId);

        Set<Long> uploadedDocumentIds = uploadedDocuments.stream()
                .map(d -> d.getDocumentConfig().getId())
                .collect(Collectors.toSet());

        List<String> missingMandatoryDocs = mandatoryDocuments.stream()
                .filter(doc -> !uploadedDocumentIds.contains(doc.getId()))
                .map(RequiredDocument::getDocumentName)
                .toList();

        if (!missingMandatoryDocs.isEmpty()) {

            List<RequiredDocument> mandatoryOnly = mandatoryDocuments;

            List<Map<String, Object>> documentList = mandatoryOnly.stream()
                    .map(requiredDoc -> {
                        Map<String, Object> docInfo = new HashMap<>();
                        docInfo.put("requiredDocumentId", requiredDoc.getId());
                        docInfo.put("documentName", requiredDoc.getDocumentName());
                        docInfo.put("description", requiredDoc.getDescription());
                        docInfo.put("documentType", requiredDoc.getDocumentType());
                        docInfo.put("allowedFormat", requiredDoc.getAllowedFormat());
                        docInfo.put("maxFileSizeMB", requiredDoc.getMaxFileSizeMB());
                        docInfo.put("uploaded", false);
                        return docInfo;
                    })
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "studentModalityId", studentModalityId,
                    "documents", documentList,
                    "statistics", Map.of(
                            "totalDocuments", documentList.size(),
                            "uploadedDocuments", 0,
                            "pendingDocuments", documentList.size(),
                            "mandatoryDocuments", documentList.size(),
                            "secondaryDocuments", 0
                    )
            ));
        }

        List<RequiredDocument> allDocuments = requiredDocumentRepository
                .findByModalityIdAndActiveTrueAndDocumentTypeIn(
                        modalityId,
                        List.of(DocumentType.MANDATORY, DocumentType.SECONDARY)
                );

        Map<Long, StudentDocument> uploadedMap = uploadedDocuments.stream()
                .collect(Collectors.toMap(
                        d -> d.getDocumentConfig().getId(),
                        d -> d
                ));

        List<Map<String, Object>> documentList = allDocuments.stream()
                .map(requiredDoc -> {
                    StudentDocument uploaded = uploadedMap.get(requiredDoc.getId());

                    Map<String, Object> docInfo = new HashMap<>();
                    docInfo.put("requiredDocumentId", requiredDoc.getId());
                    docInfo.put("documentName", requiredDoc.getDocumentName());
                    docInfo.put("description", requiredDoc.getDescription());
                    docInfo.put("documentType", requiredDoc.getDocumentType());
                    docInfo.put("allowedFormat", requiredDoc.getAllowedFormat());
                    docInfo.put("maxFileSizeMB", requiredDoc.getMaxFileSizeMB());
                    docInfo.put("uploaded", uploaded != null);

                    if (uploaded != null) {
                        docInfo.put("studentDocumentId", uploaded.getId());
                        docInfo.put("fileName", uploaded.getFileName());
                        docInfo.put("status", uploaded.getStatus());
                        docInfo.put("notes", uploaded.getNotes());
                        docInfo.put("uploadDate", uploaded.getUploadDate());
                    }

                    return docInfo;
                })
                .toList();

        long totalDocuments = documentList.size();
        long uploadedCount = documentList.stream()
                .filter(doc -> (Boolean) doc.get("uploaded"))
                .count();
        long mandatoryCount = documentList.stream()
                .filter(doc -> doc.get("documentType") == DocumentType.MANDATORY)
                .count();
        long secondaryCount = documentList.stream()
                .filter(doc -> doc.get("documentType") == DocumentType.SECONDARY)
                .count();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "studentModalityId", studentModalityId,
                "documents", documentList,
                "statistics", Map.of(
                        "totalDocuments", totalDocuments,
                        "uploadedDocuments", uploadedCount,
                        "pendingDocuments", totalDocuments - uploadedCount,
                        "mandatoryDocuments", mandatoryCount,
                        "secondaryDocuments", secondaryCount
                )
        ));
    }

    public ResponseEntity<?> getStudentDocuments(Long studentModalityId) {

        StudentModality studentModality = studentModalityRepository
                .findById(studentModalityId)
                .orElseThrow(() ->
                        new RuntimeException("Modalidad del estudiante no encontrada")
                );

        List<StudentDocument> documents =
                studentDocumentRepository.findByStudentModalityId(studentModalityId);

        List<Map<String, Object>> response = documents.stream()
                .map(doc -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("studentDocumentId", doc.getId());
                    map.put("documentName", doc.getDocumentConfig().getDocumentName());
                    map.put("documentType", doc.getDocumentConfig().getDocumentType());
                    map.put("status", doc.getStatus());
                    map.put("notes", doc.getNotes());
                    map.put("uploadedAt", doc.getUploadDate());
                    map.put("filePath", doc.getFilePath());
                    return map;
                })
                .toList();

        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> viewStudentDocument(Long studentDocumentId) throws MalformedURLException {

        StudentDocument doc = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        Path path = Paths.get(doc.getFilePath());
        if (!Files.exists(path)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found on server");
        }

        UrlResource resource = new UrlResource(path.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + doc.getFileName() + "\"")
                .body(resource);

    }
    public ResponseEntity<?> reviewStudentDocument(Long studentDocumentId, DocumentReviewDTO request) {
        User reviewer = SecurityUtils.getCurrentUser();

        StudentDocument document = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (document.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "No se puede cambiar el estado del documento porque está en estado " + document.getStatus() + ". El estudiante debe primero corregir y resubir el documento para que pueda ser revisado por jefatura de programa nuevamente."
                    )
            );
        }

        if (document.getStatus() == DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW){
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "No se puede cambiar el estado del documento porque ya fue aceptado por los jurados evaluadores."
                    )
            );
        }

        if (document.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_CURRICULUM_COMMITTEE ||
           document.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER){
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "No se puede cambiar el estado del documento porque está en estado " + document.getStatus() + ". El estudiante debe primero corregir y resubir el documento para que pueda ser revisado por el comité de currículo o los jurados evaluadores nuevamente."
                    )
            );
        }

        // Validación de estado permitido
        DocumentStatus currentStatus = document.getStatus();
        if (currentStatus != DocumentStatus.PENDING &&
            currentStatus != DocumentStatus.ACCEPTED_FOR_PROGRAM_HEAD_REVIEW &&
            currentStatus != DocumentStatus.REJECTED_FOR_PROGRAM_HEAD_REVIEW &&
            currentStatus !=  DocumentStatus.CORRECTION_RESUBMITTED) {
            return ResponseEntity.badRequest().body(
                Map.of(
                    "success", false,
                    "message", "No puedes cambiar el estado de este documento.",
                    "currentStatus", currentStatus
                )
            );
        }

        ModalityProcessStatus modalityStatus = document.getStudentModality().getStatus();

        if (modalityStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_EXAMINERS ||
             modalityStatus == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "No se puede cambiar el estado del documento porque la modalidad está en estado " + modalityStatus + ". El estudiante debe primero corregir y resubir el documento para que pueda ser revisado por el comité de currículo o los jurados evaluadores nuevamente."
                    )
            );
        }

        AcademicProgram documentProgram = document.getStudentModality().getAcademicProgram();

        boolean authorized = programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(reviewer.getId(), documentProgram.getId(), ProgramRole.PROGRAM_HEAD);

        if (!authorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("No tienes permisos para revisar documentos de este programa académico");
        }

        if ((request.getStatus() == DocumentStatus.REJECTED_FOR_PROGRAM_HEAD_REVIEW ||
                request.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD)
                && (request.getNotes() == null || request.getNotes().isBlank())) {

            return ResponseEntity.badRequest().body("Debe proporcionar notas al rechazar o solicitar correcciones");
        }

        document.setStatus(request.getStatus());
        document.setNotes(request.getNotes());
        document.setUploadDate(LocalDateTime.now());

        studentDocumentRepository.save(document);

        if (request.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD) {
            StudentModality studentModality = document.getStudentModality();

            LocalDateTime now = LocalDateTime.now();

            studentModality.setStatus(ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD);
            studentModality.setCorrectionRequestDate(now);
            studentModality.setCorrectionDeadline(now.plusDays(30));
            studentModality.setCorrectionReminderSent(false);
            studentModality.setUpdatedAt(now);
            studentModalityRepository.save(studentModality);

            historyRepository.save(
                    ModalityProcessStatusHistory.builder()
                            .studentModality(studentModality)
                            .status(ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD)
                            .changeDate(now)
                            .responsible(reviewer)
                            .observations("Jefe de programa solicitó correcciones en documento: " +
                                    document.getDocumentConfig().getDocumentName() +
                                    ". Notas: " + request.getNotes())
                            .build()
            );

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

        return ResponseEntity.ok(
                Map.of(
                        "message", "Documento revisado correctamente",
                        "documentId", document.getId(),
                        "newStatus", document.getStatus()
                )
        );
    }

    @Transactional
    public ResponseEntity<?> approveModalityByProgramHead(Long studentModalityId) {

        User programHead = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new RuntimeException("Modality not found"));

        Long academicProgramId = studentModality.getAcademicProgram().getId();

        boolean isAuthorized =
                programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                                programHead.getId(),
                                academicProgramId,
                                ProgramRole.PROGRAM_HEAD
                        );

        if (!isAuthorized) {
            return ResponseEntity.status(403).body(
                    Map.of(
                            "approved", false,
                            "message", "No tienes permisos para aprobar modalidades de este programa académico"
                    )
            );
        }

        if (!(studentModality.getStatus() == ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD ||
                studentModality.getStatus() == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD ||
                studentModality.getStatus() == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD ||
                studentModality.getStatus() == ModalityProcessStatus.CANCELLATION_REJECTED
                )) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "approved", false,
                            "message", "La modalidad no está en un estado válido para ser aprobada por la jefatura de programa",
                            "currentStatus", studentModality.getStatus()
                    )
            );
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
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "approved", false,
                            "message", "Para poder aprobar la modalidad, todos los documentos obligatorios deben estar aceptados",
                            "documents", invalidDocuments
                    )
            );
        }

        studentModality.setStatus(ModalityProcessStatus.READY_FOR_PROGRAM_CURRICULUM_COMMITTEE);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.READY_FOR_PROGRAM_CURRICULUM_COMMITTEE)
                        .changeDate(LocalDateTime.now())
                        .responsible(programHead)
                        .observations("Modalidad aprobada por jefatura de programa")
                        .build()
        );

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_APPROVED_BY_PROGRAM_HEAD, studentModality.getId(), programHead.getId(), Map.of())
        );

        return ResponseEntity.ok(
                Map.of(
                        "approved", true,
                        "newStatus", ModalityProcessStatus.READY_FOR_PROGRAM_CURRICULUM_COMMITTEE,
                        "message", "Modalidad aprobada correctamente y enviada al comité de currículo de programa"
                )
        );
    }

    @Transactional
    public ResponseEntity<?> approveModalityByCommittee(Long studentModalityId) {
        User committeeMember = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new RuntimeException("Modality not found"));

        Long academicProgramId = studentModality.getAcademicProgram().getId();

        boolean isAuthorized =
                programAuthorityRepository
                        .existsByUser_IdAndAcademicProgram_IdAndRole(
                                committeeMember.getId(),
                                academicProgramId,
                                ProgramRole.PROGRAM_CURRICULUM_COMMITTEE
                        );

        if (!isAuthorized) {
            return ResponseEntity.status(403).body(
                    Map.of(
                            "approved", false,
                            "message", "No tienes permisos para aprobar modalidades de este programa académico"
                    )
            );
        }

        if (studentModality.getStatus() != ModalityProcessStatus.READY_FOR_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "approved", false,
                            "message", "La modalidad no está en estado válido para aprobación por el comité de currículo de programa",
                            "currentStatus", studentModality.getStatus()
                    )
            );
        }

        List<StudentDocument> documents = studentDocumentRepository.findByStudentModalityId(studentModalityId);
        boolean allDocumentsApproved = documents.stream()
                .allMatch(doc -> doc.getStatus() == DocumentStatus.ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW);
        if (!allDocumentsApproved) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "approved", false,
                            "message", "No se puede aprobar la modalidad. Todos los documentos deben estar aprobados por el comité de currículo de programa."
                    )
            );
        }

        studentModality.setStatus(ModalityProcessStatus.READY_FOR_EXAMINERS);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.READY_FOR_EXAMINERS)
                        .changeDate(LocalDateTime.now())
                        .responsible(committeeMember)
                        .observations("Modalidad aprobada por el Comité de currículo de programa")
                        .build()
        );

        return ResponseEntity.ok(
                Map.of(
                        "approved", true,
                        "newStatus", ModalityProcessStatus.READY_FOR_EXAMINERS,
                        "message", "Modalidad aprobada definitivamente por el comité de currículo de programa"
                )
        );
    }

    @Transactional
    public ResponseEntity<?> reviewStudentDocumentByExaminer(Long studentDocumentId, DocumentReviewDTO request) {

        User examiner = SecurityUtils.getCurrentUser();

        boolean hasExaminerRole = examiner.getRoles().stream()
                .anyMatch(role -> role.getName().equals("EXAMINER"));

        if (!hasExaminerRole) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of("success", false, "message", "El usuario no tiene rol de EXAMINER")
            );
        }

        StudentDocument document = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        StudentModality studentModality = document.getStudentModality();

        DefenseExaminer defenseExaminer = defenseExaminerRepository
                .findByStudentModalityIdAndExaminerId(studentModality.getId(), examiner.getId())
                .orElse(null);

        if (defenseExaminer == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of("success", false, "message", "No estás asignado como jurado de esta modalidad")
            );
        }

        ExaminerType examinerType = defenseExaminer.getExaminerType();

        // ===== VALIDACIÓN: Solo se pueden evaluar documentos MANDATORY con requiresProposalEvaluation=true =====
        // Documentos MANDATORY sin esta condición (ej: contratos, formularios) no son evaluables por el jurado.
        // Los documentos SECONDARY sí pueden ser evaluados por el jurado (son los documentos finales).
        DocumentType docType = document.getDocumentConfig().getDocumentType();
        if (docType == DocumentType.MANDATORY && !document.getDocumentConfig().isRequiresProposalEvaluation()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "Este documento obligatorio no requiere evaluación por parte del jurado. " +
                               "Solo los documentos de propuesta de grado marcados para evaluación por jurado pueden ser revisados por este rol."
            ));
        }
        // =================================================================================

        // Validar que el documento no esté bloqueado esperando al estudiante
        if (document.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No se puede cambiar el estado del documento porque está en estado " +
                            document.getStatus() + ". El estudiante debe primero corregir y resubir el documento."
            ));
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
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Ya aprobaste este documento. Una vez emitida la aprobación no puede ser modificada.",
                        "yourPreviousDecision", previousDecision.name()
                ));
            }

            if (previousDecision == ExaminerDocumentDecision.REJECTED) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Ya rechazaste este documento. Una vez emitido el rechazo no puede ser modificado.",
                        "yourPreviousDecision", previousDecision.name()
                ));
            }

            // Si previousDecision == CORRECTIONS_REQUESTED: el jurado puede re-votar
            // porque el estudiante resubió el documento con las correcciones.
            // Verificamos que el documento esté efectivamente en estado de resubmisión.
            if (previousDecision == ExaminerDocumentDecision.CORRECTIONS_REQUESTED) {
                if (document.getStatus() != DocumentStatus.CORRECTION_RESUBMITTED) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Solicitaste correcciones en este documento. Debes esperar a que el estudiante resuba el documento corregido antes de emitir una nueva evaluación.",
                            "yourPreviousDecision", previousDecision.name(),
                            "documentStatus", document.getStatus().name()
                    ));
                }
            }
        }
        // =============================================================================

        if (request.getStatus() != DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW &&
                request.getStatus() != DocumentStatus.REJECTED_FOR_EXAMINER_REVIEW &&
                request.getStatus() != DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Estado de documento inválido para revisión por jurado"
            ));
        }

        if ((request.getStatus() == DocumentStatus.REJECTED_FOR_EXAMINER_REVIEW ||
                request.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER)
                && (request.getNotes() == null || request.getNotes().isBlank())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Debe proporcionar notas al rechazar o solicitar correcciones"
            ));
        }

        // Determinar la decisión individual del jurado
        ExaminerDocumentDecision individualDecision = switch (request.getStatus()) {
            case ACCEPTED_FOR_EXAMINER_REVIEW -> ExaminerDocumentDecision.ACCEPTED;
            case REJECTED_FOR_EXAMINER_REVIEW -> ExaminerDocumentDecision.REJECTED;
            case CORRECTIONS_REQUESTED_BY_EXAMINER -> ExaminerDocumentDecision.CORRECTIONS_REQUESTED;
            default -> throw new IllegalArgumentException("Estado inválido");
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
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Debe proporcionar calificaciones para todos los aspectos de la propuesta de grado"
                ));
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
        ResponseEntity<?> consensusResult = processExaminerConsensus(
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

        Map<String, Object> responseBody = new java.util.HashMap<>();
        responseBody.put("success", true);
        responseBody.put("documentId", document.getId());
        responseBody.put("documentName", document.getDocumentConfig().getDocumentName());
        responseBody.put("examinerDecision", individualDecision.name());
        responseBody.put("currentDocumentStatus", document.getStatus());
        responseBody.put("examinerName", examiner.getName() + " " + examiner.getLastName());
        responseBody.put("examinerType", examinerType.name());
        responseBody.put("message", message);

        if (savedProposalEvaluation != null) {
            Map<String, Object> proposalEvaluationInfo = new java.util.HashMap<>();
            proposalEvaluationInfo.put("id", savedProposalEvaluation.getId());
            proposalEvaluationInfo.put("summary", savedProposalEvaluation.getSummary());
            proposalEvaluationInfo.put("backgroundJustification", savedProposalEvaluation.getBackgroundJustification());
            proposalEvaluationInfo.put("problemStatement", savedProposalEvaluation.getProblemStatement());
            proposalEvaluationInfo.put("objectives", savedProposalEvaluation.getObjectives());
            proposalEvaluationInfo.put("methodology", savedProposalEvaluation.getMethodology());
            proposalEvaluationInfo.put("bibliographyReferences", savedProposalEvaluation.getBibliographyReferences());
            proposalEvaluationInfo.put("documentOrganization", savedProposalEvaluation.getDocumentOrganization());
            proposalEvaluationInfo.put("evaluatedAt", savedProposalEvaluation.getEvaluatedAt());
            responseBody.put("proposalEvaluation", proposalEvaluationInfo);
        } else {
            responseBody.put("proposalEvaluation", null);
        }

        return ResponseEntity.ok(responseBody);
    }

    @Transactional
        public ResponseEntity<?> reviewFinalDocumentByExaminer(Long studentDocumentId, DocumentReviewDTO request) {

        if (request == null || request.getFinalEvaluation() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Debe enviar la evaluación detallada del documento final en el campo finalEvaluation"
            ));
        }

        if (request.getStatus() != DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW &&
                request.getStatus() != DocumentStatus.REJECTED_FOR_EXAMINER_REVIEW &&
                request.getStatus() != DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Estado de documento inválido para revisión por jurado"
            ));
        }

        if ((request.getStatus() == DocumentStatus.REJECTED_FOR_EXAMINER_REVIEW ||
                request.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER)
                && (request.getNotes() == null || request.getNotes().isBlank())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Debe proporcionar notas al rechazar o solicitar correcciones"
            ));
        }

        User examiner = SecurityUtils.getCurrentUser();

        StudentDocument document = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        StudentModality studentModality = document.getStudentModality();

        if (studentModality.getStatus() != ModalityProcessStatus.READY_FOR_DEFENSE &&
              studentModality.getStatus() != ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_EXAMINERS) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "currentStatus", studentModality.getStatus().name(),
                    "message", "La modalidad no está en un estado válido para revisión de documentos finales por parte del jurado"
            ));
        }

        if (document.getDocumentConfig().getDocumentType() != DocumentType.SECONDARY ||
                !document.getDocumentConfig().isRequiresProposalEvaluation()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "Solo se permite evaluar documentos finales que requieran evaluación por parte del jurado"
            ));
        }

        FinalEvaluationRequest evalReq = request.getFinalEvaluation();
        FinalDocumentRubricType rubricType = resolveFinalDocumentRubricType(studentModality);
        String validationError = validateFinalEvaluationByRubric(evalReq, rubricType);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "rubricType", rubricType.name(),
                    "message", validationError
            ));
        }

        DocumentStatus previousDocumentStatus = document.getStatus();
        String previousDocumentNotes = document.getNotes();
        ModalityProcessStatus previousModalityStatus = studentModality.getStatus();

        ResponseEntity<?> reviewResult = reviewStudentDocumentByExaminer(studentDocumentId, request);
        if (!reviewResult.getStatusCode().is2xxSuccessful()) {
            return reviewResult;
        }

        // Releer para reflejar estados resultantes del consenso entre jurados.
        StudentDocument updatedDocument = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));
        StudentModality updatedModality = studentModalityRepository.findById(studentModality.getId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

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

        Map<String, Object> secondaryEvaluationInfo = ModalityServiceUtils.buildFinalEvaluationInfoMap(secondaryEvaluation);

        Map<String, Object> traceability = new LinkedHashMap<>();
        traceability.put("previousDocumentStatus", previousDocumentStatus != null ? previousDocumentStatus.name() : null);
        traceability.put("currentDocumentStatus", updatedDocument.getStatus() != null ? updatedDocument.getStatus().name() : null);
        traceability.put("previousModalityStatus", previousModalityStatus != null ? previousModalityStatus.name() : null);
        traceability.put("currentModalityStatus", updatedModality.getStatus() != null ? updatedModality.getStatus().name() : null);
        traceability.put("examinerNotes", request.getNotes());

        Object responseBody = reviewResult.getBody();
        if (responseBody instanceof Map<?, ?> mapBody) {
            Map<String, Object> mergedBody = new LinkedHashMap<>();
            mapBody.forEach((key, value) -> mergedBody.put(String.valueOf(key), value));
            mergedBody.put("secondaryEvaluation", secondaryEvaluationInfo);
            mergedBody.put("finalEvaluation", secondaryEvaluationInfo);
            mergedBody.put("currentModalityStatus", updatedModality.getStatus().name());
            mergedBody.put("traceability", traceability);
            return ResponseEntity.status(reviewResult.getStatusCode()).body(mergedBody);
        }

        Map<String, Object> fallbackBody = new LinkedHashMap<>();
        fallbackBody.put("success", true);
        fallbackBody.put("message", "Documento SECONDARY evaluado correctamente");
        fallbackBody.put("reviewResult", responseBody);
        fallbackBody.put("secondaryEvaluation", secondaryEvaluationInfo);
        fallbackBody.put("finalEvaluation", secondaryEvaluationInfo);
        fallbackBody.put("currentModalityStatus", updatedModality.getStatus().name());
        fallbackBody.put("traceability", traceability);

        return ResponseEntity.status(reviewResult.getStatusCode()).body(fallbackBody);
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
     * @return ResponseEntity si hay un resultado final especial (rechazo), null si continúa normal
     */
    private ResponseEntity<?> processExaminerConsensus(StudentDocument document, StudentModality studentModality, User examiner, ExaminerType examinerType, ExaminerDocumentDecision individualDecision, String notes) {

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

            studentModality.setStatus(ModalityProcessStatus.DOCUMENT_REVIEW_TIEBREAKER_REQUIRED);
            studentModality.setUpdatedAt(LocalDateTime.now());
            studentModalityRepository.save(studentModality);

            historyRepository.save(ModalityProcessStatusHistory.builder()
                    .studentModality(studentModality)
                    .status(ModalityProcessStatus.DOCUMENT_REVIEW_TIEBREAKER_REQUIRED)
                    .changeDate(LocalDateTime.now())
                    .responsible(examiner)
                    .observations("Un jurado aprobó y el otro rechazó el documento '" +
                            document.getDocumentConfig().getDocumentName() +
                            "'. Se requiere jurado de desempate para resolver.")
                    .build());
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
                studentModality.setStatus(ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL);
                studentModality.setCorrectionAttempts(newAttempts);
                studentModality.setUpdatedAt(LocalDateTime.now());
                studentModalityRepository.save(studentModality);

                document.setStatus(DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER);
                document.setNotes(correctionNotes);
                studentDocumentRepository.save(document);

                historyRepository.save(ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL)
                        .changeDate(LocalDateTime.now())
                        .responsible(examiner)
                        .observations("Rechazado definitivamente tras agotar 3 intentos de corrección. " +
                                "Documento: " + document.getDocumentConfig().getDocumentName())
                        .build());

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

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "La propuesta ha sido rechazada definitivamente. El estudiante agotó las 3 oportunidades.",
                        "documentId", document.getId(),
                        "newModalityStatus", ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL,
                        "attemptsUsed", newAttempts
                ));
            }

            // Solicitar correcciones: solo el jurado que las pidió deberá re-votar
            document.setStatus(DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER);
            document.setNotes("Correcciones solicitadas por un jurado (el otro rechazó). " +
                    "Una vez corregido, el jurado que solicitó correcciones decidirá si aprueba o rechaza.\n" +
                    correctionNotes);
            studentDocumentRepository.save(document);

            studentModality.setCorrectionAttempts(newAttempts);
            studentModality.setStatus(ModalityProcessStatus.CORRECTIONS_REQUESTED_EXAMINERS);
            LocalDateTime now = LocalDateTime.now();
            studentModality.setCorrectionRequestDate(now);
            studentModality.setCorrectionDeadline(now.plusDays(30));
            studentModality.setCorrectionReminderSent(false);
            studentModality.setUpdatedAt(now);
            studentModalityRepository.save(studentModality);

            historyRepository.save(ModalityProcessStatusHistory.builder()
                    .studentModality(studentModality)
                    .status(ModalityProcessStatus.CORRECTIONS_REQUESTED_EXAMINERS)
                    .changeDate(now)
                    .responsible(examiner)
                    .observations("Un jurado rechazó y el otro solicitó correcciones (intento " + newAttempts +
                            " de 3). El estudiante debe corregir para que el jurado que solicitó correcciones " +
                            "decida si aprueba o rechaza. Observaciones: " + correctionNotes)
                    .build());

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
    private ResponseEntity<?> processTiebreakerDocumentDecision(StudentDocument document, StudentModality studentModality, User tiebreaker, ExaminerDocumentDecision decision, String notes) {

        switch (decision) {
            case ACCEPTED -> {
                document.setStatus(DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW);
                document.setNotes("Aprobado por el jurado de desempate");
                studentDocumentRepository.save(document);

                historyRepository.save(ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(studentModality.getStatus())
                        .changeDate(LocalDateTime.now())
                        .responsible(tiebreaker)
                        .observations("Jurado de desempate aprobó el documento: " +
                                document.getDocumentConfig().getDocumentName())
                        .build());

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
    private ResponseEntity<?> applyCorrectionsRequestedByPrimaryExaminers(StudentDocument document, StudentModality studentModality, User examiner, List<ExaminerDocumentReview> reviews, String notes) {

        // ===== LÓGICA DE CONTADOR DE INTENTOS =====
        // Solo incrementar el contador si la modalidad NO está ya en estado CORRECTIONS_REQUESTED_EXAMINERS
        // Esto evita que si ambos jurados solicitan correcciones, se cuente como 2 intentos en lugar de 1
        boolean shouldIncrementAttempt = studentModality.getStatus() != ModalityProcessStatus.CORRECTIONS_REQUESTED_EXAMINERS;
        
        int currentAttempts = studentModality.getCorrectionAttempts() == null ? 0 : studentModality.getCorrectionAttempts();
        int newAttempts = shouldIncrementAttempt ? currentAttempts + 1 : currentAttempts;

        if (newAttempts > 3) {
            studentModality.setStatus(ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL);
            studentModality.setCorrectionAttempts(newAttempts);
            studentModality.setUpdatedAt(LocalDateTime.now());
            studentModalityRepository.save(studentModality);

            historyRepository.save(ModalityProcessStatusHistory.builder()
                    .studentModality(studentModality)
                    .status(ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL)
                    .changeDate(LocalDateTime.now())
                    .responsible(examiner)
                    .observations("Rechazado definitivamente. El estudiante agotó 3 oportunidades de corrección. Documento: " +
                            document.getDocumentConfig().getDocumentName())
                    .build());

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

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "La propuesta ha sido rechazada definitivamente. El estudiante agotó las 3 oportunidades.",
                    "documentId", document.getId(),
                    "newModalityStatus", ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL,
                    "attemptsUsed", newAttempts
            ));
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
        studentModality.setStatus(ModalityProcessStatus.CORRECTIONS_REQUESTED_EXAMINERS);
        LocalDateTime now = LocalDateTime.now();
        studentModality.setCorrectionRequestDate(now);
        studentModality.setCorrectionDeadline(now.plusDays(30));
        studentModality.setCorrectionReminderSent(false);
        studentModality.setUpdatedAt(now);
        studentModalityRepository.save(studentModality);

        // Trazabilidad: indicar si este es un nuevo intento o una solicitud adicional del mismo intento
        String attemptMessage = shouldIncrementAttempt
                ? "Jurados solicitaron correcciones (intento " + newAttempts + " de 3): " + combinedNotes
                : "Jurado adicional solicitó correcciones para el intento " + newAttempts + " (ya en proceso): " + combinedNotes;

        historyRepository.save(ModalityProcessStatusHistory.builder()
                .studentModality(studentModality)
                .status(ModalityProcessStatus.CORRECTIONS_REQUESTED_EXAMINERS)
                .changeDate(now)
                .responsible(examiner)
                .observations(attemptMessage)
                .build());

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
    private ResponseEntity<?> cancelModalityByFinalDocumentRejection(StudentDocument document, StudentModality studentModality, User examiner, String reason) {

        // Cambiar estado de modalidad a MODALITY_CANCELLED
        studentModality.setStatus(ModalityProcessStatus.MODALITY_CANCELLED);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        // Obtener y eliminar miembros activos (relación estudiante-modalidad)
        List<StudentModalityMember> members = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(studentModality.getId(), MemberStatus.ACTIVE);
        
        for (StudentModalityMember member : members) {
            studentModalityMemberRepository.delete(member);
        }

        // Registrar en historial
        String observations = "Modalidad cancelada por rechazo de documento final. " +
                "Documento: " + document.getDocumentConfig().getDocumentName() + ". " +
                (reason != null && !reason.isBlank() ? "Motivo: " + reason : "Documento rechazado por los jurados.");

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.MODALITY_CANCELLED)
                        .changeDate(LocalDateTime.now())
                        .responsible(examiner)
                        .observations(observations)
                        .build()
        );

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

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "La modalidad ha sido cancelada por rechazo de documento final. Puedes iniciar una nueva modalidad.",
                "documentId", document.getId(),
                "documentName", document.getDocumentConfig().getDocumentName(),
                "newModalityStatus", ModalityProcessStatus.MODALITY_CANCELLED.name(),
                "deletedMembers", members.size()
        ));
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
        studentModality.setStatus(ModalityProcessStatus.CORRECTIONS_REQUESTED_EXAMINERS);
        LocalDateTime now = LocalDateTime.now();
        studentModality.setCorrectionRequestDate(now);
        studentModality.setCorrectionDeadline(now.plusDays(30));
        studentModality.setCorrectionReminderSent(false);
        studentModality.setUpdatedAt(now);
        studentModalityRepository.save(studentModality);

        // Trazabilidad: indicar si este es un nuevo intento o una solicitud adicional del mismo intento
        String attemptMessage = shouldIncrementAttempt
                ? "Jurado de desempate solicitó correcciones (intento " + newAttempts + " de 3): " + notes
                : "Jurado de desempate solicitó correcciones para el intento " + newAttempts + " (ya en proceso): " + notes;

        historyRepository.save(ModalityProcessStatusHistory.builder()
                .studentModality(studentModality)
                .status(ModalityProcessStatus.CORRECTIONS_REQUESTED_EXAMINERS)
                .changeDate(now)
                .responsible(tiebreaker)
                .observations(attemptMessage)
                .build());

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
    private ResponseEntity<?> applyRejectionByBothPrimaryExaminers(StudentDocument document, StudentModality studentModality, User examiner, String notes) {

        // Verificar si es un documento final (SECONDARY)
        if (isFinalDocument(document)) {
            return cancelModalityByFinalDocumentRejection(document, studentModality, examiner, notes);
        }

        // Lógica existente para documentos MANDATORY
        studentModality.setStatus(ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        document.setStatus(DocumentStatus.REJECTED_FOR_EXAMINER_REVIEW);
        document.setNotes("Rechazado por ambos jurados principales");
        studentDocumentRepository.save(document);

        historyRepository.save(ModalityProcessStatusHistory.builder()
                .studentModality(studentModality)
                .status(ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL)
                .changeDate(LocalDateTime.now())
                .responsible(examiner)
                .observations("Ambos jurados principales rechazaron el documento: " +
                        document.getDocumentConfig().getDocumentName())
                .build());

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

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "El documento fue rechazado por ambos jurados principales.",
                "documentId", document.getId(),
                "newModalityStatus", ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL
        ));
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
        studentModality.setStatus(ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        document.setStatus(DocumentStatus.REJECTED_FOR_EXAMINER_REVIEW);
        document.setNotes("Rechazado por el jurado de desempate");
        studentDocumentRepository.save(document);

        historyRepository.save(ModalityProcessStatusHistory.builder()
                .studentModality(studentModality)
                .status(ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL)
                .changeDate(LocalDateTime.now())
                .responsible(tiebreaker)
                .observations("Jurado de desempate rechazó el documento: " +
                        document.getDocumentConfig().getDocumentName() + ". " + (notes != null ? notes : ""))
                .build());

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
        studentModality.setStatus(ModalityProcessStatus.DOCUMENTS_APPROVED_BY_EXAMINERS);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        historyRepository.save(ModalityProcessStatusHistory.builder()
                .studentModality(studentModality)
                .status(ModalityProcessStatus.DOCUMENTS_APPROVED_BY_EXAMINERS)
                .changeDate(LocalDateTime.now())
                .responsible(responsible)
                .observations("Los documentos de propuesta obligatorios han sido aprobados por los jurados.")
                .build());

        // → PROPOSAL_APPROVED automático
        studentModality.setStatus(ModalityProcessStatus.PROPOSAL_APPROVED);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        historyRepository.save(ModalityProcessStatusHistory.builder()
                .studentModality(studentModality)
                .status(ModalityProcessStatus.PROPOSAL_APPROVED)
                .changeDate(LocalDateTime.now())
                .responsible(responsible)
                .observations("Propuesta aprobada automáticamente por consenso de jurados.")
                .build());

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
        studentModality.setStatus(ModalityProcessStatus.SECONDARY_DOCUMENTS_APPROVED_BY_EXAMINERS);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        historyRepository.save(ModalityProcessStatusHistory.builder()
                .studentModality(studentModality)
                .status(ModalityProcessStatus.SECONDARY_DOCUMENTS_APPROVED_BY_EXAMINERS)
                .changeDate(LocalDateTime.now())
                .responsible(responsible)
                .observations("Todos los documentos finales han sido aprobados por consenso de jurados.")
                .build());

        // → FINAL_REVIEW_COMPLETED automático
        studentModality.setStatus(ModalityProcessStatus.FINAL_REVIEW_COMPLETED);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        historyRepository.save(ModalityProcessStatusHistory.builder()
                .studentModality(studentModality)
                .status(ModalityProcessStatus.FINAL_REVIEW_COMPLETED)
                .changeDate(LocalDateTime.now())
                .responsible(responsible)
                .observations("Revisión final completada automáticamente por aprobación de jurados. " +
                        "Notificando al director de proyecto para programar la sustentación.")
                .build());

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
    public ResponseEntity<?> approveModalityByExaminers(Long studentModalityId) {
        User examiner = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new RuntimeException("Modality not found"));

        Long academicProgramId = studentModality.getAcademicProgram().getId();

        boolean isAuthorized =
                programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                        examiner.getId(),
                        academicProgramId,
                        ProgramRole.EXAMINER
                );

        if (!isAuthorized) {
            return ResponseEntity.status(403).body(
                    Map.of(
                            "approved", false,
                            "message", "No tienes permisos para aprobar modalidades de este programa académico"
                    )
            );
        }

        if (studentModality.getStatus() != ModalityProcessStatus.EXAMINERS_ASSIGNED &&
            studentModality.getStatus() != ModalityProcessStatus.DOCUMENTS_APPROVED_BY_EXAMINERS &&
            studentModality.getStatus() != ModalityProcessStatus.CANCELLATION_REJECTED) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "approved", false,
                            "message", "La modalidad debe estar en estado EXAMINERS_ASSIGNED o DOCUMENTS_APPROVED_BY_EXAMINERS. " +
                                       "Todos los documentos obligatorios deben haber sido aceptados por los jurados.",
                            "currentStatus", studentModality.getStatus().name(),
                            "requiredStatus", ModalityProcessStatus.EXAMINERS_ASSIGNED.name()
                    )
            );
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
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "approved", false,
                            "message", "Para poder aprobar la modalidad, todos los documentos de propuesta de grado evaluables por los jurados deben estar aceptados",
                            "documents", invalidDocuments
                    )
            );
        }

        studentModality.setStatus(ModalityProcessStatus.PROPOSAL_APPROVED);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.PROPOSAL_APPROVED)
                        .changeDate(LocalDateTime.now())
                        .responsible(examiner)
                        .observations("Modalidad aprobada por los jurados")
                        .build()
        );

        // Notificar a todos los estudiantes miembros activos
        List<StudentModalityMember> activeMembers = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(studentModality.getId(), MemberStatus.ACTIVE);
        for (StudentModalityMember member : activeMembers) {
            applicationEventPublisher.publishEvent(
                    new ModalityEvent(NotificationType.MODALITY_APPROVED_BY_EXAMINERS, studentModality.getId(), member.getStudent().getId(), Map.of())
            );
        }

        return ResponseEntity.ok(
                Map.of(
                        "approved", true,
                        "newStatus", ModalityProcessStatus.PROPOSAL_APPROVED,
                        "message", "Modalidad aprobada correctamente por los jurados"
                )
        );
    }

    public ResponseEntity<?> reviewStudentDocumentByCommittee(Long studentDocumentId, DocumentReviewDTO request) {

        User committeeMember = SecurityUtils.getCurrentUser();

        StudentDocument document = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        StudentModality studentModality = document.getStudentModality();
        Long academicProgramId = studentModality.getAcademicProgram().getId();
        if (document.getStatus() == DocumentStatus.REJECTED_FOR_PROGRAM_HEAD_REVIEW ||
            document.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD) {
            return ResponseEntity.badRequest().body(
                Map.of(
                    "success", false,
                    "message", "No se puede cambiar el estado del documento porque está en estado " + document.getStatus() + ". El estudiante debe primero corregir y resubir el documento para que pueda ser revisado por el comité de currículo de programa."
                )
            );
        }

        if ( document.getStatus() == DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW){
            return ResponseEntity.badRequest().body(
                Map.of(
                    "success", false,
                    "message", "No se puede cambiar el estado del documento porque ya fue aprobado por los jurados. El comité de currículo de programa solo puede revisar documentos que aún no han sido aprobados por los jurados."
                )
            );
        }

        // Validación: no permitir revisión si la modalidad está en un estado propio de la jefatura de programa
        ModalityProcessStatus modalityStatus = studentModality.getStatus();
        if (modalityStatus == ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD ||
            modalityStatus == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD ||
            modalityStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD) {
            return ResponseEntity.badRequest().body(
                Map.of(
                    "success", false,
                    "message", "No se puede revisar el documento en este momento. La modalidad se encuentra en estado '" +
                               ModalityServiceUtils.describeModalityStatus(modalityStatus) + "', que corresponde a una etapa de revisión por parte de la Jefatura de Programa. El comité podrá revisar el documento una vez la jefatura finalice su proceso."
                )
            );
        }

        if (modalityStatus == ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_EXAMINERS) {
            return ResponseEntity.badRequest().body(
                Map.of(
                    "success", false,
                    "message", "No se puede revisar el documento en este momento. La modalidad se encuentra en estado '" +
                               ModalityServiceUtils.describeModalityStatus(modalityStatus) + "', que corresponde a una etapa de correcciones ya resubmited por parte del estudiante. El comité podrá revisar el documento una vez el estudiante resubmita las correcciones y la modalidad vuelva a un estado de revisión."
                )
            );
        }

        boolean isAuthorized =
                programAuthorityRepository
                        .existsByUser_IdAndAcademicProgram_IdAndRole(
                                committeeMember.getId(),
                                academicProgramId,
                                ProgramRole.PROGRAM_CURRICULUM_COMMITTEE
                        );

        if (!isAuthorized) {
            return ResponseEntity.status(403).body(
                    Map.of(
                            "success", false,
                            "message", "No tienes permisos para revisar documentos de este programa académico"
                    )
            );
        }

        if ((request.getStatus() == DocumentStatus.REJECTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW ||
                request.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_CURRICULUM_COMMITTEE)
                && (request.getNotes() == null || request.getNotes().isBlank())) {

            return ResponseEntity.badRequest().body(
                    "Debe proporcionar notas al rechazar o solicitar correcciones"
            );
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

            studentModality.setStatus(ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE);
            studentModality.setCorrectionRequestDate(now);
            studentModality.setCorrectionDeadline(now.plusDays(30));
            studentModality.setCorrectionReminderSent(false);
            studentModality.setUpdatedAt(now);
            studentModalityRepository.save(studentModality);

            // Registrar cambio de estado en el historial
            historyRepository.save(
                    ModalityProcessStatusHistory.builder()
                            .studentModality(studentModality)
                            .status(ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE)
                            .changeDate(now)
                            .responsible(committeeMember)
                            .observations("Comité de currículo solicitó correcciones en documento: " +
                                    document.getDocumentConfig().getDocumentName() +
                                    ". Notas: " + request.getNotes())
                            .build()
            );

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
                    studentModality.setStatus(ModalityProcessStatus.READY_FOR_DIRECTOR_ASSIGNMENT);
                    studentModality.setUpdatedAt(LocalDateTime.now());
                    studentModalityRepository.save(studentModality);

                    historyRepository.save(
                            ModalityProcessStatusHistory.builder()
                                    .studentModality(studentModality)
                                    .status(ModalityProcessStatus.READY_FOR_DIRECTOR_ASSIGNMENT)
                                    .changeDate(LocalDateTime.now())
                                    .responsible(committeeMember)
                                    .observations("Todos los documentos obligatorios han sido aprobados por el Comité de Currículo. " +
                                            "La modalidad está lista para la asignación del Director de Proyecto.")
                                    .build()
                    );

                    return ResponseEntity.ok(
                            Map.of(
                                    "success", true,
                                    "documentId", document.getId(),
                                    "documentName", document.getDocumentConfig().getDocumentName(),
                                    "newStatus", document.getStatus(),
                                    "newModalityStatus", ModalityProcessStatus.READY_FOR_DIRECTOR_ASSIGNMENT.name(),
                                    "message", "Documento aprobado. Todos los documentos obligatorios han sido aprobados. " +
                                               "La modalidad está lista para la asignación del Director de Proyecto."
                            )
                    );
                } else {
                    // Flujo simplificado: el comité toma decisión final directamente
                    studentModality.setStatus(ModalityProcessStatus.APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE);
                    studentModality.setUpdatedAt(LocalDateTime.now());
                    studentModalityRepository.save(studentModality);

                    historyRepository.save(
                            ModalityProcessStatusHistory.builder()
                                    .studentModality(studentModality)
                                    .status(ModalityProcessStatus.APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE)
                                    .changeDate(LocalDateTime.now())
                                    .responsible(committeeMember)
                                    .observations("Todos los documentos obligatorios han sido aprobados por el Comité de Currículo. " +
                                            "Puedes continuar con el proceso de la modalidad ")
                                    .build()
                    );

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

                    return ResponseEntity.ok(
                            Map.of(
                                    "success", true,
                                    "documentId", document.getId(),
                                    "documentName", document.getDocumentConfig().getDocumentName(),
                                    "newStatus", document.getStatus(),
                                    "newModalityStatus", ModalityProcessStatus.APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE.name(),
                                    "message", "Documento aprobado. Todos los documentos obligatorios han sido aprobados. " +
                                               "Puedes continuar con el proceso de la modalidad."
                            )
                    );
                }
            }
        }

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "documentId", document.getId(),
                        "documentName", document.getDocumentConfig().getDocumentName(),
                        "newStatus", document.getStatus(),
                        "message", "Documento revisado correctamente por el comité de currículo de programa"
                )
        );
    }

    private boolean checkIfAllMandatoryDocumentsAcceptedByAllExaminers(Long studentModalityId) {

        StudentModality sm = studentModalityRepository.findById(studentModalityId).orElse(null);
        if (sm == null) return false;

        Long modalityId = sm.getProgramDegreeModality().getDegreeModality().getId();

        // Obtener todos los documentos requeridos MANDATORY activos para esta modalidad
        List<RequiredDocument> mandatoryDocuments = requiredDocumentRepository
                .findByModalityIdAndActiveTrueAndDocumentType(modalityId, DocumentType.MANDATORY);

        if (mandatoryDocuments.isEmpty()) {
            return false;
        }

        // Verificar que haya al menos un jurado asignado
        List<DefenseExaminer> assignedExaminers = defenseExaminerRepository
                .findByStudentModalityId(studentModalityId);

        if (assignedExaminers.isEmpty()) {
            return false;
        }

        // Para cada documento MANDATORY verificar que su estado ACTUAL sea ACCEPTED_FOR_EXAMINER_REVIEW
        for (RequiredDocument reqDoc : mandatoryDocuments) {
            StudentDocument document = studentDocumentRepository
                    .findByStudentModalityIdAndDocumentConfigId(studentModalityId, reqDoc.getId())
                    .orElse(null);

            // Si no fue subido aún, no están todos aceptados
            if (document == null) {
                return false;
            }

            // Si el estado actual del documento NO es ACCEPTED_FOR_EXAMINER_REVIEW, no están todos aceptados
            if (document.getStatus() != DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW) {
                return false;
            }
        }

        return true;
    }

    private boolean checkIfAllDocumentsAcceptedByAllExaminers(Long studentModalityId) {

        List<StudentDocument> documents = studentDocumentRepository.findByStudentModalityId(studentModalityId);

        if (documents.isEmpty()) {
            return false;
        }

        List<DefenseExaminer> assignedExaminers = defenseExaminerRepository
                .findByStudentModalityId(studentModalityId);

        if (assignedExaminers.isEmpty()) {
            return false;
        }

        for (StudentDocument document : documents) {

            List<StudentDocumentStatusHistory> documentHistory = documentHistoryRepository
                    .findByStudentDocumentIdOrderByChangeDateDesc(document.getId());

            Set<Long> examinersWhoAccepted = new HashSet<>();

            for (StudentDocumentStatusHistory history : documentHistory) {
                if (history.getStatus() == DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW &&
                    history.getResponsible() != null) {
                    examinersWhoAccepted.add(history.getResponsible().getId());
                }
            }

            for (DefenseExaminer examiner : assignedExaminers) {
                if (!examinersWhoAccepted.contains(examiner.getExaminer().getId())) {

                    return false;
                }
            }
        }

        return true;
    }

    private Map<String, Object> validateAllRequiredDocumentsUploaded(Long studentModalityId) {
        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        Long modalityId = studentModality.getProgramDegreeModality().getDegreeModality().getId();

        List<RequiredDocument> requiredDocuments = requiredDocumentRepository
                .findByModalityIdAndActiveTrueAndDocumentTypeIn(
                        modalityId,
                        List.of(DocumentType.MANDATORY, DocumentType.SECONDARY)
                );

        List<StudentDocument> uploadedDocuments = studentDocumentRepository
                .findByStudentModalityId(studentModalityId);

        Map<Long, StudentDocument> uploadedMap = uploadedDocuments.stream()
                .collect(Collectors.toMap(
                        d -> d.getDocumentConfig().getId(),
                        d -> d
                ));

        List<Map<String, Object>> missingDocuments = new ArrayList<>();

        for (RequiredDocument required : requiredDocuments) {
            if (!uploadedMap.containsKey(required.getId())) {
                Map<String, Object> docInfo = new HashMap<>();
                docInfo.put("documentId", required.getId());
                docInfo.put("documentName", required.getDocumentName());
                docInfo.put("documentType", required.getDocumentType().toString());
                docInfo.put("description", required.getDescription() != null ? required.getDescription() : "Sin descripción");
                missingDocuments.add(docInfo);
            }
        }

        boolean allUploaded = missingDocuments.isEmpty();

        Map<String, Object> result = new HashMap<>();
        result.put("allDocumentsUploaded", allUploaded);
        result.put("totalRequired", requiredDocuments.size());
        result.put("totalUploaded", uploadedDocuments.size());
        result.put("missingDocuments", missingDocuments);
        result.put("missingCount", missingDocuments.size());

        return result;
    }

    /**
     * Valida que todos los documentos MANDATORY y SECONDARY de la modalidad
     * tengan estado ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW.
     */
    private Map<String, Object> validateAllDocumentsAcceptedForCommittee(Long studentModalityId) {
        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        Long modalityId = studentModality.getProgramDegreeModality().getDegreeModality().getId();

        List<RequiredDocument> requiredDocuments = requiredDocumentRepository
                .findByModalityIdAndActiveTrueAndDocumentTypeIn(
                        modalityId,
                        List.of(DocumentType.MANDATORY, DocumentType.SECONDARY)
                );

        List<StudentDocument> uploadedDocuments = studentDocumentRepository
                .findByStudentModalityId(studentModalityId);

        Map<Long, StudentDocument> uploadedMap = uploadedDocuments.stream()
                .collect(Collectors.toMap(
                        d -> d.getDocumentConfig().getId(),
                        d -> d
                ));

        List<Map<String, Object>> notAcceptedDocuments = new ArrayList<>();

        for (RequiredDocument required : requiredDocuments) {
            StudentDocument uploaded = uploadedMap.get(required.getId());
            boolean accepted = uploaded != null &&
                    uploaded.getStatus() == DocumentStatus.ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW;
            if (!accepted) {
                Map<String, Object> docInfo = new HashMap<>();
                docInfo.put("documentId", required.getId());
                docInfo.put("documentName", required.getDocumentName());
                docInfo.put("documentType", required.getDocumentType().toString());
                docInfo.put("currentStatus", uploaded != null ? uploaded.getStatus().toString() : "NO_SUBIDO");
                notAcceptedDocuments.add(docInfo);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("allAccepted", notAcceptedDocuments.isEmpty());
        result.put("notAcceptedDocuments", notAcceptedDocuments);
        result.put("notAcceptedCount", notAcceptedDocuments.size());
        result.put("totalRequired", requiredDocuments.size());

        return result;
    }

    @Transactional
    public ResponseEntity<?> resubmitCorrectedDocument(Long studentModalityId, Long documentId, MultipartFile file) throws IOException {

        User student = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        // Validar que el usuario sea miembro activo de la modalidad
        boolean isActiveMember = studentModalityMemberRepository.isActiveMember(
                studentModalityId,
                student.getId()
        );

        if (!isActiveMember) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "success", false,
                            "message", "No tienes permiso para modificar esta modalidad"
                    ));
        }

        if (studentModality.getStatus() != ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD &&
                studentModality.getStatus() != ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "La modalidad no está en estado de correcciones solicitadas",
                    "currentStatus", studentModality.getStatus()
            ));
        }

        if (studentModality.getCorrectionDeadline() != null &&
                LocalDateTime.now().isAfter(studentModality.getCorrectionDeadline())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "El plazo de 30 días para entregar las correcciones ha vencido. La modalidad ha sido cancelada.",
                    "deadline", studentModality.getCorrectionDeadline()
            ));
        }

        StudentDocument document = studentDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        if (!document.getStudentModality().getId().equals(studentModalityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "success", false,
                            "message", "El documento no pertenece a esta modalidad"
                    ));
        }

        if (document.getStatus() != DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD &&
                document.getStatus() != DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_CURRICULUM_COMMITTEE) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "El documento no está en estado de correcciones solicitadas",
                    "currentStatus", document.getStatus()
            ));
        }

        String originalFilename = file.getOriginalFilename();
        String finalFileName = UUID.randomUUID() + "_" + originalFilename;

        String modalityPath = document.getStudentModality()
                .getProgramDegreeModality()
                .getDegreeModality()
                .getName()
                .replaceAll("[^a-zA-Z0-9]", "_");

        String studentPath = student.getName() + student.getLastName() + "_" +
                student.getLastName() + "_" + studentModalityId;

        Path basePath = Paths.get(uploadDir, modalityPath, studentPath);
        Files.createDirectories(basePath);

        Path fullPath = basePath.resolve(finalFileName);
        Files.copy(file.getInputStream(), fullPath, StandardCopyOption.REPLACE_EXISTING);

        document.setFileName(originalFilename);
        document.setFilePath(fullPath.toString());
        document.setStatus(DocumentStatus.CORRECTION_RESUBMITTED);
        document.setUploadDate(LocalDateTime.now());
        studentDocumentRepository.save(document);

        studentModality.setStatus(ModalityProcessStatus.CORRECTIONS_SUBMITTED);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        documentHistoryRepository.save(
                StudentDocumentStatusHistory.builder()
                        .studentDocument(document)
                        .status(DocumentStatus.CORRECTION_RESUBMITTED)
                        .changeDate(LocalDateTime.now())
                        .responsible(student)
                        .observations("Documento corregido reenviado por el estudiante dentro del plazo establecido")
                        .build()
        );

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.CORRECTIONS_SUBMITTED)
                        .changeDate(LocalDateTime.now())
                        .responsible(student)
                        .observations("Correcciones enviadas por el estudiante para revisión")
                        .build()
        );

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.CORRECTION_RESUBMITTED, studentModalityId, student.getId(), Map.of(
                        ModalityEvent.KEY_DOCUMENT_ID, documentId,
                        ModalityEvent.KEY_STUDENT_ID, student.getId(),
                        ModalityEvent.KEY_DOCUMENT_NAME, document.getDocumentConfig().getDocumentName()
                ))
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Documento corregido enviado exitosamente. Será revisado por el jurado correspondiente.",
                "documentId", documentId,
                "newStatus", document.getStatus()
        ));
    }

    @Transactional
    public ResponseEntity<?> approveCorrectedDocument(Long documentId) {

        User reviewer = SecurityUtils.getCurrentUser();

        StudentDocument document = studentDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

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
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "success", false,
                            "message", "No tienes permiso para aprobar este documento"
                    ));
        }

        document.setStatus(newDocumentStatus);
        document.setUploadDate(LocalDateTime.now());
        studentDocumentRepository.save(document);

        studentModality.setCorrectionRequestDate(null);
        studentModality.setCorrectionDeadline(null);
        studentModality.setCorrectionReminderSent(null);

        if (newDocumentStatus == DocumentStatus.ACCEPTED_FOR_PROGRAM_HEAD_REVIEW) {
            studentModality.setStatus(ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD);
        } else {
            studentModality.setStatus(ModalityProcessStatus.UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE);
        }

        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        documentHistoryRepository.save(
                StudentDocumentStatusHistory.builder()
                        .studentDocument(document)
                        .status(newDocumentStatus)
                        .changeDate(LocalDateTime.now())
                        .responsible(reviewer)
                        .observations("Correcciones aprobadas. El documento cumple con los requisitos.")
                        .build()
        );

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(studentModality.getStatus())
                        .changeDate(LocalDateTime.now())
                        .responsible(reviewer)
                        .observations("Correcciones aprobadas. Continúa el proceso de revisión.")
                        .build()
        );

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.CORRECTION_APPROVED, studentModality.getId(), reviewer.getId(), Map.of(
                        ModalityEvent.KEY_DOCUMENT_ID, documentId,
                        ModalityEvent.KEY_STUDENT_ID, studentModality.getLeader().getId(),
                        ModalityEvent.KEY_DOCUMENT_NAME, document.getDocumentConfig().getDocumentName()
                ))
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Correcciones aprobadas exitosamente. La modalidad continúa su proceso normal.",
                "documentId", documentId,
                "newDocumentStatus", newDocumentStatus,
                "newModalityStatus", studentModality.getStatus()
        ));
    }

    @Transactional
    public ResponseEntity<?> rejectCorrectedDocumentFinal(Long documentId, String reason) {

        if (reason == null || reason.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Debe proporcionar el motivo del rechazo definitivo"
            ));
        }

        User reviewer = SecurityUtils.getCurrentUser();

        StudentDocument document = studentDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

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
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "success", false,
                            "message", "No tienes permiso para rechazar este documento"
                    ));
        }

        document.setStatus(DocumentStatus.REJECTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW);
        document.setNotes(reason);
        document.setUploadDate(LocalDateTime.now());
        studentDocumentRepository.save(document);

        studentModality.setStatus(ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        documentHistoryRepository.save(
                StudentDocumentStatusHistory.builder()
                        .studentDocument(document)
                        .status(DocumentStatus.REJECTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW)
                        .changeDate(LocalDateTime.now())
                        .responsible(reviewer)
                        .observations("Rechazo definitivo: " + reason)
                        .build()
        );

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL)
                        .changeDate(LocalDateTime.now())
                        .responsible(reviewer)
                        .observations("Modalidad cancelada por rechazo definitivo de correcciones. Motivo: " + reason)
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

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Correcciones rechazadas definitivamente. La modalidad ha sido cancelada.",
                "documentId", documentId,
                "finalStatus", ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL
        ));
    }

    public ResponseEntity<?> getCorrectionDeadlineStatus(Long studentModalityId) {

        User user = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        // Validar que el usuario sea miembro activo de la modalidad o un revisor autorizado
        boolean isStudent = studentModalityMemberRepository.isActiveMember(
                studentModalityId,
                user.getId()
        );
        boolean isAuthorizedReviewer = programAuthorityRepository.existsByUser_IdAndAcademicProgram_Id(
                user.getId(),
                studentModality.getAcademicProgram().getId()
        );

        if (!isStudent && !isAuthorizedReviewer) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "success", false,
                            "message", "No tienes permiso para consultar esta información"
                    ));
        }

        if (studentModality.getStatus() != ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD &&
                studentModality.getStatus() != ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE) {
            return ResponseEntity.ok(Map.of(
                    "hasCorrectionRequest", false,
                    "currentStatus", studentModality.getStatus(),
                    "message", "No hay correcciones solicitadas actualmente"
            ));
        }

        LocalDateTime now = LocalDateTime.now();
        long daysRemaining = 0;
        boolean isExpired = false;

        if (studentModality.getCorrectionDeadline() != null) {
            daysRemaining = ChronoUnit.DAYS.between(now, studentModality.getCorrectionDeadline());
            isExpired = daysRemaining < 0;
        }

        return ResponseEntity.ok(Map.of(
                "hasCorrectionRequest", true,
                "currentStatus", studentModality.getStatus(),
                "correctionRequestDate", studentModality.getCorrectionRequestDate(),
                "correctionDeadline", studentModality.getCorrectionDeadline(),
                "daysRemaining", Math.max(0, daysRemaining),
                "isExpired", isExpired,
                "reminderSent", studentModality.getCorrectionReminderSent() != null ? studentModality.getCorrectionReminderSent() : false
        ));
    }

    @Transactional
    public ResponseEntity<?> closeModalityByCommittee(Long studentModalityId, String reason) {

        if (reason == null || reason.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "Debe proporcionar el motivo del cierre de la modalidad"
                    )
            );
        }

        User committeeMember = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        Long academicProgramId = studentModality.getAcademicProgram().getId();

        boolean isAuthorized = programAuthorityRepository
                .existsByUser_IdAndAcademicProgram_IdAndRole(
                        committeeMember.getId(),
                        academicProgramId,
                        ProgramRole.PROGRAM_CURRICULUM_COMMITTEE
                );

        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "success", false,
                            "message", "No tiene permiso para cerrar modalidades de este programa académico. Debe ser miembro del comité de currículo del programa."
                    ));
        }

        if (studentModality.getStatus() == ModalityProcessStatus.MODALITY_CLOSED) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "La modalidad ya se encuentra cerrada",
                            "currentStatus", studentModality.getStatus()
                    )
            );
        }

        ModalityProcessStatus previousStatus = studentModality.getStatus();

        studentModality.setStatus(ModalityProcessStatus.MODALITY_CLOSED);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.MODALITY_CLOSED)
                        .changeDate(LocalDateTime.now())
                        .responsible(committeeMember)
                        .observations(String.format(
                                "Modalidad cerrada por el comité de currículo del programa.  Motivo: %s",
                                previousStatus,
                                reason
                        ))
                        .build()
        );

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_CLOSED_BY_COMMITTEE, studentModality.getId(), committeeMember.getId(), Map.of(
                        ModalityEvent.KEY_STUDENT_ID, studentModality.getLeader().getId(),
                        ModalityEvent.KEY_REASON, reason,
                        ModalityEvent.KEY_COMMITTEE_MEMBER_ID, committeeMember.getId()
                ))
        );

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "studentModalityId", studentModalityId,
                        "previousStatus", previousStatus,
                        "newStatus", ModalityProcessStatus.MODALITY_CLOSED,
                        "closedBy", committeeMember.getName() + " " + committeeMember.getLastName(),
                        "reason", reason,
                        "message", "Modalidad cerrada exitosamente. El estudiante ha sido notificado por correo electrónico."
                )
        );
    }

    @Transactional
    public ResponseEntity<?> approveFinalModalityByCommittee(Long studentModalityId, String observations) {

        User committeeMember = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        Long academicProgramId = studentModality.getAcademicProgram().getId();

        boolean isAuthorized = programAuthorityRepository
                .existsByUser_IdAndAcademicProgram_IdAndRole(
                        committeeMember.getId(),
                        academicProgramId,
                        ProgramRole.PROGRAM_CURRICULUM_COMMITTEE
                );

        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "success", false,
                            "message", "No tiene permiso para aprobar modalidades de este programa académico. Debe ser miembro del comité de currículo del programa."
                    ));
        }

        Map<String, Object> documentValidation = validateAllRequiredDocumentsUploaded(studentModalityId);
        boolean allDocumentsUploaded = (boolean) documentValidation.get("allDocumentsUploaded");

        if (!allDocumentsUploaded) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "No se puede aprobar la modalidad porque faltan documentos por subir",
                            "missingDocumentsCount", documentValidation.get("missingCount"),
                            "totalRequired", documentValidation.get("totalRequired"),
                            "totalUploaded", documentValidation.get("totalUploaded"),
                            "missingDocuments", documentValidation.get("missingDocuments")
                    )
            );
        }

        // Validar que TODOS los documentos MANDATORY y SECONDARY estén en estado ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW
        Map<String, Object> acceptedValidation = validateAllDocumentsAcceptedForCommittee(studentModalityId);
        boolean allAccepted = (boolean) acceptedValidation.get("allAccepted");

        if (!allAccepted) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "No se puede aprobar la modalidad. Todos los documentos iniciales y complementarios deben estar en estado 'ACEPTADO POR COMITÉ'. Revise los documentos del estudiante.",
                            "documentosNoAceptados", acceptedValidation.get("notAcceptedCount"),
                            "totalRequeridos", acceptedValidation.get("totalRequired"),
                            "documentosPendientes", acceptedValidation.get("notAcceptedDocuments")
                    )
            );
        }

        if (!(studentModality.getStatus() == ModalityProcessStatus.READY_FOR_DIRECTOR_ASSIGNMENT ||
              studentModality.getStatus() == ModalityProcessStatus.APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE)) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "La modalidad no está en estado válido para aprobación final por el comité",
                            "currentStatus", studentModality.getStatus()
                    )
            );
        }

        if (studentModality.getStatus() == ModalityProcessStatus.GRADED_APPROVED ||
                studentModality.getStatus() == ModalityProcessStatus.GRADED_FAILED) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "La modalidad ya ha sido calificada definitivamente",
                            "currentStatus", studentModality.getStatus()
                    )
            );
        }

        ModalityProcessStatus previousStatus = studentModality.getStatus();

        studentModality.setStatus(ModalityProcessStatus.GRADED_APPROVED);
        studentModality.setAcademicDistinction(AcademicDistinction.NO_DISTINCTION);
        studentModality.setFinalGrade(null);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.GRADED_APPROVED)
                        .changeDate(LocalDateTime.now())
                        .responsible(committeeMember)
                        .observations(String.format(
                                "Modalidad aprobada definitivamente por el comité de currículo del programa. " +
                                observations != null ? "Observaciones: " + observations : ""
                        ))
                        .build()
        );

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

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "studentModalityId", studentModalityId,
                        "previousStatus", previousStatus,
                        "newStatus", ModalityProcessStatus.GRADED_APPROVED,
                        "academicDistinction", AcademicDistinction.NO_DISTINCTION,
                        "finalGrade", "N/A",
                        "approvedBy", committeeMember.getName() + " " + committeeMember.getLastName(),
                        "observations", observations != null ? observations : "Sin observaciones",
                        "message", "Modalidad aprobada definitivamente. Todos los estudiantes han sido notificados."
                )
        );
    }

    @Transactional
    public ResponseEntity<?> rejectFinalModalityByCommittee(Long studentModalityId, String reason) {

        if (reason == null || reason.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "Debe proporcionar la razón del rechazo de la modalidad"
                    )
            );
        }

        User committeeMember = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        Long academicProgramId = studentModality.getAcademicProgram().getId();

        boolean isAuthorized = programAuthorityRepository
                .existsByUser_IdAndAcademicProgram_IdAndRole(
                        committeeMember.getId(),
                        academicProgramId,
                        ProgramRole.PROGRAM_CURRICULUM_COMMITTEE
                );

        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "success", false,
                            "message", "No tiene permiso para rechazar modalidades de este programa académico. Debe ser miembro del comité de currículo del programa."
                    ));
        }

        // Validar que todos los documentos MANDATORY y SECONDARY estén subidos
        Map<String, Object> documentValidation = validateAllRequiredDocumentsUploaded(studentModalityId);
        boolean allDocumentsUploaded = (boolean) documentValidation.get("allDocumentsUploaded");

        if (!allDocumentsUploaded) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "No se puede rechazar la modalidad porque faltan documentos por subir. " +
                                    "El estudiante debe completar la documentación antes de que el comité pueda tomar una decisión definitiva.",
                            "missingDocumentsCount", documentValidation.get("missingCount"),
                            "totalRequired", documentValidation.get("totalRequired"),
                            "totalUploaded", documentValidation.get("totalUploaded"),
                            "missingDocuments", documentValidation.get("missingDocuments")
                    )
            );
        }

        // Validar que TODOS los documentos MANDATORY y SECONDARY estén en estado ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW
        Map<String, Object> acceptedValidation = validateAllDocumentsAcceptedForCommittee(studentModalityId);
        boolean allAccepted = (boolean) acceptedValidation.get("allAccepted");

        if (!allAccepted) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "No se puede rechazar la modalidad. Todos los documentos obligatorios y complementarios deben estar en estado 'Aceptado para revisión del comité de currículo' (ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW). Revise los documentos indicados.",
                            "documentosNoAceptados", acceptedValidation.get("notAcceptedCount"),
                            "totalRequeridos", acceptedValidation.get("totalRequired"),
                            "documentosPendientes", acceptedValidation.get("notAcceptedDocuments")
                    )
            );
        }

        if (!(studentModality.getStatus() == ModalityProcessStatus.READY_FOR_DIRECTOR_ASSIGNMENT ||
              studentModality.getStatus() == ModalityProcessStatus.APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE)) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "La modalidad no está en estado válido para rechazo por el comité",
                            "currentStatus", studentModality.getStatus(),
                            "validStates", "READY_FOR_DIRECTOR_ASSIGNMENT o PENDING_COMMITTEE_FINAL_DECISION"
                    )
            );
        }

        if (studentModality.getStatus() == ModalityProcessStatus.GRADED_APPROVED ||
                studentModality.getStatus() == ModalityProcessStatus.GRADED_FAILED) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "La modalidad ya ha sido calificada definitivamente",
                            "currentStatus", studentModality.getStatus()
                    )
            );
        }

        ModalityProcessStatus previousStatus = studentModality.getStatus();

        studentModality.setStatus(ModalityProcessStatus.GRADED_FAILED);
        studentModality.setFinalGrade(null);
        studentModality.setAcademicDistinction(AcademicDistinction.REJECTED_BY_COMMITTEE);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.GRADED_FAILED)
                        .changeDate(LocalDateTime.now())
                        .responsible(committeeMember)
                        .observations(String.format(
                                "Modalidad rechazada definitivamente por el comité de currículo del programa. " +
                                " Motivo: %s",
                                previousStatus,
                                reason
                        ))
                        .build()
        );

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

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "studentModalityId", studentModalityId,
                        "previousStatus", previousStatus,
                        "newStatus", ModalityProcessStatus.GRADED_FAILED,
                        "rejectedBy", committeeMember.getName() + " " + committeeMember.getLastName(),
                        "reason", reason,
                        "message", "Modalidad rechazada definitivamente. Todos los estudiantes han sido notificados."
                )
        );
    }
}
