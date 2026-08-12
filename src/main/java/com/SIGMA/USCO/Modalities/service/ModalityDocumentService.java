package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.Entity.DegreeModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.Entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.documents.entity.DocumentEditRequestVote;
import com.SIGMA.USCO.documents.entity.ExaminerDocumentReview;
import com.SIGMA.USCO.documents.entity.RequiredDocument;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.entity.StudentDocumentStatusHistory;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import com.SIGMA.USCO.documents.entity.enums.DocumentEditRequestStatus;
import com.SIGMA.USCO.documents.entity.enums.ExaminerDocumentDecision;
import com.SIGMA.USCO.documents.repository.DocumentEditRequestRepository;
import com.SIGMA.USCO.documents.repository.DocumentEditRequestVoteRepository;
import com.SIGMA.USCO.documents.repository.ExaminerDocumentReviewRepository;
import com.SIGMA.USCO.documents.repository.RequiredDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentStatusHistoryRepository;
import com.SIGMA.USCO.common.exception.ForbiddenException;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.common.util.MimeTypeGuard;
import com.SIGMA.USCO.common.util.ResourceAccessPolicy;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.event.ModalityEvent;
import com.SIGMA.USCO.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModalityDocumentService {

    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final RequiredDocumentRepository requiredDocumentRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final StudentDocumentStatusHistoryRepository documentHistoryRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;
    private final ExaminerDocumentReviewRepository examinerDocumentReviewRepository;
    private final DocumentEditRequestRepository documentEditRequestRepository;
    private final DocumentEditRequestVoteRepository documentEditRequestVoteRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ResourceAccessPolicy resourceAccessPolicy;
    private final ModalityStatusTransition modalityStatusTransition;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Transactional
    public Map<String, Object> uploadRequiredDocument(Long studentModalityId, Long requiredDocumentId, MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new ValidationException("El archivo es obligatorio");
        }

        // ponytail: filesystem is not rollbackable with @Transactional; DB is atomic, orphan file possible on later failure

        User uploader = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad del estudiante no encontrada"));

        // Verificar si es miembro activo (estudiante) o si es el director asignado a la modalidad
        boolean isActiveMember = studentModalityMemberRepository.isActiveMember(
                studentModalityId,
                uploader.getId()
        );

        boolean isAssignedDirector = studentModality.getProjectDirector() != null &&
                studentModality.getProjectDirector().getId().equals(uploader.getId());

        if (!isActiveMember && !isAssignedDirector) {
            throw new ForbiddenException("No autorizado para subir documentos a esta modalidad");
        }

        // Para efectos de trazabilidad, usamos 'uploader' como responsable.
        // Si es el director, el folder de almacenamiento sigue siendo el del estudiante líder.
        User student = isAssignedDirector && !isActiveMember
                ? studentModality.getLeader()
                : uploader;

        RequiredDocument requiredDocument = requiredDocumentRepository.findById(requiredDocumentId)
                .orElseThrow(() -> new NotFoundException("Documento requerido no existe"));

        DegreeModality modality = studentModality.getProgramDegreeModality().getDegreeModality();

        if (!requiredDocument.getModality().getId().equals(modality.getId())) {
            throw new ForbiddenException("El documento no pertenece a la modalidad seleccionada");
        }

        // Validación: Los documentos de tipo SECONDARY solo pueden ser subidos por el director del proyecto
        if (requiredDocument.getDocumentType() == DocumentType.SECONDARY && !isAssignedDirector) {
            String message = "Este documento solo puede ser subido por el director del proyecto. Por favor, póngase en contacto con el director " +
                    (studentModality.getProjectDirector() != null
                            ? studentModality.getProjectDirector().getName() + " " + studentModality.getProjectDirector().getLastName()
                            : "asignado");
            throw new ForbiddenException(message);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename == null ? "" : originalFilename).toLowerCase();

        if (requiredDocument.getAllowedFormat() != null &&
                !requiredDocument.getAllowedFormat().toLowerCase().contains(extension)) {
            throw new ValidationException("Formato de archivo no permitido");
        }

        if (!MimeTypeGuard.isMimeAllowed(file, extension)) {
            throw new ValidationException("Formato de archivo no permitido");
        }

        if (requiredDocument.getMaxFileSizeMB() != null &&
                file.getSize() > requiredDocument.getMaxFileSizeMB() * 1024L * 1024L) {
            throw new ValidationException("El archivo supera el tamaño permitido");
        }

        String modalityFolder = modality.getName()
                .replaceAll("[^a-zA-Z0-9]", "_");

        String studentFolder = (student.getName() + student.getLastName() + "_" +
                student.getLastName() + "_" +
                student.getId()).replaceAll("[^a-zA-Z0-9]", "_");

        Path basePath = Paths.get(
                uploadDir,
                modalityFolder,
                studentFolder
        );

        Files.createDirectories(basePath);

        String safeOriginal = FilenameUtils.getName(originalFilename != null ? originalFilename : "")
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safeOriginal.isEmpty()) {
            safeOriginal = "documento";
        }
        // ponytail: setFileName persiste el nombre original (visible), solo la ruta en disco se sane
        String finalFileName = UUID.randomUUID() + "_" + safeOriginal;
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
            modalityStatusTransition.transition(studentModality, ModalityProcessStatus.EXAMINERS_ASSIGNED, uploader,
                    (isAssignedDirector && !isActiveMember ? "Director" : "Estudiante") +
                            " actualizó el documento '" +
                            studentDocument.getDocumentConfig().getDocumentName() +
                            "' con los cambios aprobados por los jurados. " +
                            "La modalidad regresa al estado de revisión por jurados.");

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

            applicationEventPublisher.publishEvent(
                    new ModalityEvent(NotificationType.DOCUMENT_UPLOADED, studentModality.getId(), uploader.getId(), Map.of(ModalityEvent.KEY_STUDENT_DOCUMENT_ID, studentDocument.getId(), ModalityEvent.KEY_STUDENT_ID, uploader.getId()))
            );

            return Map.of(
                    "message", "Documento actualizado correctamente. Los jurados evaluarán la nueva versión.",
                    "path", fullPath.toString(),
                    "documentStatus", studentDocument.getStatus().name(),
                    "modalityStatus", studentModality.getStatus().name()
            );

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
            modalityStatusTransition.transition(studentModality, newModalityStatusAfterResubmit, uploader,
                    "Correcciones enviadas por " +
                            (isAssignedDirector && !isActiveMember ? "el director" : "el estudiante") +
                            " tras solicitud de: " + requesterLabel);

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

        return Map.of(
                        "message", isResubmittingCorrection
                                ? "Documento de corrección enviado correctamente. Será revisado por el evaluador correspondiente."
                                : "Documento subido correctamente",
                        "path", fullPath.toString(),
                        "documentStatus", studentDocument.getStatus().name(),
                        "modalityStatus", studentModality.getStatus().name()
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
            modalityStatusTransition.transition(studentModality, ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD, responsibleUser,
                    "Todos los documentos obligatorios han sido subidos. " +
                         "La modalidad pasa automáticamente a revisión del jefe de programa.");

            log.info("Modalidad {} cambió automáticamente a UNDER_REVIEW_PROGRAM_HEAD - Todos los documentos MANDATORY subidos",
                    studentModality.getId());
        }
    }
    @Transactional(readOnly = true)
    public Map<String, Object> validateAllDocumentsUploaded(Long studentModalityId) {

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

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

        return Map.of(
                        "canContinue", allUploaded,
                        "missingDocuments", missingDocuments
                );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAvailableDocumentsForStudent() {

        User student = SecurityUtils.getCurrentUser();

        Optional<StudentModality> studentModalityOpt = studentModalityRepository
                .findTopByStudentIdOrderByUpdatedAtDesc(student.getId());

        if (studentModalityOpt.isEmpty()) {
            throw new NotFoundException("No se encontró una modalidad asociada al estudiante");
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

            return Map.of(
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
            );
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

        return Map.of(
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
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStudentDocuments(Long studentModalityId) {

        StudentModality studentModality = studentModalityRepository
                .findById(studentModalityId)
                .orElseThrow(() ->
                        new NotFoundException("Modalidad del estudiante no encontrada")
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

        return response;
    }
    @Transactional(readOnly = true)
    public Resource viewStudentDocument(Long studentDocumentId) throws MalformedURLException {

        StudentDocument doc = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new NotFoundException("Document not found"));

        User currentUser = SecurityUtils.getCurrentUser();
        if (!isAuthorizedForDocument(doc, currentUser)) {
            throw new ForbiddenException("No tienes permiso para ver este documento");
        }

        Path path = Paths.get(doc.getFilePath());
        if (!Files.exists(path)) {
            throw new NotFoundException("File not found on server");
        }

        return new UrlResource(path.toUri());

    }

    private boolean isAuthorizedForDocument(StudentDocument doc, User currentUser) {
        StudentModality modality = doc.getStudentModality();
        if (modality == null || modality.getId() == null) {
            return false;
        }
        boolean isDirector = modality.getProjectDirector() != null
                && modality.getProjectDirector().getId().equals(currentUser.getId());
        boolean isProgramAuthority = modality.getAcademicProgram() != null
                && programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRoleIn(
                        currentUser.getId(),
                        modality.getAcademicProgram().getId(),
                        List.of(ProgramRole.PROGRAM_HEAD, ProgramRole.PROGRAM_CURRICULUM_COMMITTEE));

        return isDirector || isProgramAuthority
                || tryRequire(() -> resourceAccessPolicy.requireLeader(modality, currentUser, "No tienes permiso para ver este documento"))
                || tryRequire(() -> resourceAccessPolicy.requireActiveMember(modality.getId(), currentUser, "No tienes permiso para ver este documento"))
                || tryRequire(() -> resourceAccessPolicy.requireAssignedExaminer(modality.getId(), currentUser, "No tienes permiso para ver este documento"));
    }

    private boolean tryRequire(Runnable check) {
        try {
            check.run();
            return true;
        } catch (ForbiddenException e) {
            return false;
        }
    }

    /**
     * Valida que todos los documentos MANDATORY y SECONDARY de la modalidad estén subidos.
     * Usado por el workflow (aprobación/rechazo final del comité).
     */
    public Map<String, Object> validateAllRequiredDocumentsUploaded(Long studentModalityId) {
        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

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
     * Usado por el workflow (aprobación/rechazo final del comité).
     */
    public Map<String, Object> validateAllDocumentsAcceptedForCommittee(Long studentModalityId) {
        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

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
    public Map<String, Object> resubmitCorrectedDocument(Long studentModalityId, Long documentId, MultipartFile file) throws IOException {

        User student = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        // Validar que el usuario sea miembro activo de la modalidad
        boolean isActiveMember = studentModalityMemberRepository.isActiveMember(
                studentModalityId,
                student.getId()
        );

        if (!isActiveMember) {
            throw new ForbiddenException("No tienes permiso para modificar esta modalidad");
        }

        if (studentModality.getStatus() != ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD &&
                studentModality.getStatus() != ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE) {
            throw new ValidationException("La modalidad no está en estado de correcciones solicitadas");
        }

        if (studentModality.getCorrectionDeadline() != null &&
                LocalDateTime.now().isAfter(studentModality.getCorrectionDeadline())) {
            throw new ValidationException("El plazo de 30 días para entregar las correcciones ha vencido. La modalidad ha sido cancelada.");
        }

        StudentDocument document = studentDocumentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Documento no encontrado"));

        if (!document.getStudentModality().getId().equals(studentModalityId)) {
            throw new ForbiddenException("El documento no pertenece a esta modalidad");
        }

        if (document.getStatus() != DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD &&
                document.getStatus() != DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_CURRICULUM_COMMITTEE) {
            throw new ValidationException("El documento no está en estado de correcciones solicitadas");
        }

        String originalFilename = file.getOriginalFilename();
        String safeOriginal = FilenameUtils.getName(originalFilename != null ? originalFilename : "")
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safeOriginal.isEmpty()) {
            safeOriginal = "documento";
        }
        String finalFileName = UUID.randomUUID() + "_" + safeOriginal;

        String modalityPath = document.getStudentModality()
                .getProgramDegreeModality()
                .getDegreeModality()
                .getName()
                .replaceAll("[^a-zA-Z0-9]", "_");

        String studentPath = (student.getName() + student.getLastName() + "_" +
                student.getLastName() + "_" + studentModalityId).replaceAll("[^a-zA-Z0-9]", "_");

        Path basePath = Paths.get(uploadDir, modalityPath, studentPath);
        Files.createDirectories(basePath);

        Path fullPath = basePath.resolve(finalFileName);
        Files.copy(file.getInputStream(), fullPath, StandardCopyOption.REPLACE_EXISTING);

        document.setFileName(originalFilename);
        document.setFilePath(fullPath.toString());
        document.setStatus(DocumentStatus.CORRECTION_RESUBMITTED);
        document.setUploadDate(LocalDateTime.now());
        studentDocumentRepository.save(document);

        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.CORRECTIONS_SUBMITTED, student,
                "Correcciones enviadas por el estudiante para revisión");

        documentHistoryRepository.save(
                StudentDocumentStatusHistory.builder()
                        .studentDocument(document)
                        .status(DocumentStatus.CORRECTION_RESUBMITTED)
                        .changeDate(LocalDateTime.now())
                        .responsible(student)
                        .observations("Documento corregido reenviado por el estudiante dentro del plazo establecido")
                        .build()
        );

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.CORRECTION_RESUBMITTED, studentModalityId, student.getId(), Map.of(
                        ModalityEvent.KEY_DOCUMENT_ID, documentId,
                        ModalityEvent.KEY_STUDENT_ID, student.getId(),
                        ModalityEvent.KEY_DOCUMENT_NAME, document.getDocumentConfig().getDocumentName()
                ))
        );

        return Map.of(
                "success", true,
                "message", "Documento corregido enviado exitosamente. Será revisado por el jurado correspondiente.",
                "documentId", documentId,
                "newStatus", document.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCorrectionDeadlineStatus(Long studentModalityId) {

        User user = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

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
            throw new ForbiddenException("No tienes permiso para consultar esta información");
        }

        if (studentModality.getStatus() != ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD &&
                studentModality.getStatus() != ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE) {
            return Map.of(
                    "hasCorrectionRequest", false,
                    "currentStatus", studentModality.getStatus(),
                    "message", "No hay correcciones solicitadas actualmente"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        long daysRemaining = 0;
        boolean isExpired = false;

        if (studentModality.getCorrectionDeadline() != null) {
            daysRemaining = ChronoUnit.DAYS.between(now, studentModality.getCorrectionDeadline());
            isExpired = daysRemaining < 0;
        }

        return Map.of(
                "hasCorrectionRequest", true,
                "currentStatus", studentModality.getStatus(),
                "correctionRequestDate", studentModality.getCorrectionRequestDate(),
                "correctionDeadline", studentModality.getCorrectionDeadline(),
                "daysRemaining", Math.max(0, daysRemaining),
                "isExpired", isExpired,
                "reminderSent", studentModality.getCorrectionReminderSent() != null ? studentModality.getCorrectionReminderSent() : false
        );
    }
}
