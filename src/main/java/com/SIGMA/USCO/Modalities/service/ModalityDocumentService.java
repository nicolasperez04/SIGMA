package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.entity.DegreeModality;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.repository.DefenseExaminerRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Modalities.dto.response.AvailableDocumentsResponse;
import com.SIGMA.USCO.Modalities.dto.response.CorrectionDeadlineStatusResponse;
import com.SIGMA.USCO.Modalities.dto.response.DocumentsAcceptedForCommitteeResponse;
import com.SIGMA.USCO.Modalities.dto.response.RequiredDocumentsUploadedResponse;
import com.SIGMA.USCO.Modalities.dto.response.ResubmitDocumentResponse;
import com.SIGMA.USCO.Modalities.dto.response.StudentDocumentResponse;
import com.SIGMA.USCO.Modalities.dto.response.UploadDocumentResponse;
import com.SIGMA.USCO.Modalities.dto.response.ValidateAllDocumentsUploadedResponse;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.entity.enums.ProgramRole;
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
import com.SIGMA.USCO.shared.util.ResourceAccessPolicy;
import com.SIGMA.USCO.shared.util.TranslationUtils;
import com.SIGMA.USCO.common.validation.FileValidator;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.Modalities.event.ModalityEvent;
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
    private final DefenseExaminerRepository defenseExaminerRepository;
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

    // T5.12: la I/O (Files.copy) queda fuera de la tx; la persistencia va en persistUpload
    public UploadDocumentResponse uploadRequiredDocument(Long studentModalityId, Long requiredDocumentId, MultipartFile file, User uploader) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new ValidationException("El archivo es obligatorio");
        }

        // ponytail: filesystem is not rollbackable with @Transactional; DB is atomic, orphan file possible on later failure

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

        // T5.12: consulta por id en lugar de navegar la relación LAZY RequiredDocument.modality fuera de tx
        if (!requiredDocumentRepository.existsByIdAndModalityId(requiredDocumentId, modality.getId())) {
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

        if (requiredDocument.getAllowedFormat() != null) {
            FileValidator.validateExtension(file, requiredDocument.getAllowedFormat());
        }

        FileValidator.validateMime(file, extension);

        if (requiredDocument.getMaxFileSizeMB() != null) {
            FileValidator.validateSize(file, requiredDocument.getMaxFileSizeMB());
        }

        String modalityFolder = TranslationUtils.sanitizeFileName(modality.getName(), "[^a-zA-Z0-9]");

        String studentFolder = TranslationUtils.studentFolder(student.getName(), student.getLastName(), student.getId());

        Path basePath = Paths.get(
                uploadDir,
                modalityFolder,
                studentFolder
        );

        Files.createDirectories(basePath);

        String safeOriginal = TranslationUtils.sanitizeFileName(FilenameUtils.getName(originalFilename != null ? originalFilename : ""));
        if (safeOriginal.isEmpty()) {
            safeOriginal = "documento";
        }
        // ponytail: setFileName persiste el nombre original (visible), solo la ruta en disco se sane
        String finalFileName = UUID.randomUUID() + "_" + safeOriginal;
        Path fullPath = basePath.resolve(finalFileName);

        Files.copy(file.getInputStream(), fullPath, StandardCopyOption.REPLACE_EXISTING);

        return persistUpload(studentModality, uploader, student, isAssignedDirector, isActiveMember, requiredDocumentId, originalFilename, fullPath.toString());
    }

    @Transactional
    public UploadDocumentResponse persistUpload(StudentModality studentModality, User uploader, User student,
                                                boolean isAssignedDirector, boolean isActiveMember,
                                                Long requiredDocumentId, String originalFilename, String filePath) {
        // ponytail: @Transactional es inefectivo aquí por self-invocation desde uploadRequiredDocument;
        // los save por repositorio auto-commitean. Solo molesta si se navega una relación LAZY
        // (por eso se usa requiredDocument cargado por id, no studentDocument.getDocumentConfig()).

        RequiredDocument requiredDocument = requiredDocumentRepository.findById(requiredDocumentId)
                .orElseThrow(() -> new NotFoundException("Documento requerido no existe"));

        StudentDocument studentDocument = studentDocumentRepository
                .findByStudentModalityIdAndDocumentConfigId(studentModality.getId(), requiredDocumentId)
                .orElse(
                        StudentDocument.builder()
                                .studentModality(studentModality)
                                .documentConfig(requiredDocument)
                                .build()
                );

        studentDocument.setFileName(originalFilename);
        studentDocument.setFilePath(filePath);
        studentDocument.setUploadDate(LocalDateTime.now());

        // ========== LÓGICA DE CORRECCIONES ==========
        // Determinar si se trata de una resubida de correcciones según el estado actual de la modalidad
        ModalityProcessStatus currentModalityStatus = studentModality.getStatus();

        boolean isResubmittingCorrection =
                currentModalityStatus == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD ||
                currentModalityStatus == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE ||
                currentModalityStatus == ModalityProcessStatus.CORRECTIONS_REQUESTED_EXAMINERS;

        // ========== RESUBIDA POR EDICIÓN APROBADA ==========
        // T5.12: la consulta duplicada del documento existente se elimina; la misma instancia (persistence context)
        // ya se cargó arriba, así que su estado refleja el del registro en BD
        boolean isResubmittingApprovedEdit =
                studentDocument.getStatus() == DocumentStatus.EDIT_REQUEST_APPROVED;

        if (isResubmittingApprovedEdit) {
              // Cerrar la solicitud de edición aprobada (marcar como completada con el reenvío)
            documentEditRequestRepository
                    .findTopByStudentDocumentIdAndStatusOrderByCreatedAtDesc(
                            studentDocument.getId(), DocumentEditRequestStatus.APPROVED)
                    .ifPresent(req -> {
                        // Los votos ya están registrados; solo guardamos la referencia para trazabilidad
                        documentEditRequestRepository.save(req);
                    });

            // El documento vuelve a PENDING para re-revisión por jurados
            studentDocument.setStatus(DocumentStatus.PENDING);
            studentDocument.setFileName(originalFilename);
            studentDocument.setFilePath(filePath);
            studentDocument.setUploadDate(LocalDateTime.now());
            studentDocumentRepository.save(studentDocument);

            // Limpiar las reviews anteriores de jurados para este documento (ExaminerDocumentReview)
            List<ExaminerDocumentReview> oldReviews = examinerDocumentReviewRepository
                    .findByStudentDocumentId(studentDocument.getId());
            examinerDocumentReviewRepository.deleteAll(oldReviews);

            // Limpiar también los votos de la solicitud de edición aprobada (DocumentEditRequestVote)
            documentEditRequestRepository
                    .findTopByStudentDocumentIdAndStatusOrderByCreatedAtDesc(
                            studentDocument.getId(), DocumentEditRequestStatus.APPROVED)
                    .ifPresent(req -> {
                        List<DocumentEditRequestVote> editVotes = documentEditRequestVoteRepository
                                .findByEditRequestId(req.getId());
                        documentEditRequestVoteRepository.deleteAll(editVotes);
                    });

            // Cambiar el estado de la modalidad a EXAMINERS_ASSIGNED para que los jurados revisen
            modalityStatusTransition.transition(studentModality, ModalityProcessStatus.EXAMINERS_ASSIGNED, uploader,
                    (isAssignedDirector && !isActiveMember ? "Director" : "Estudiante") +
                            " actualizó el documento '" +
                            requiredDocument.getDocumentName() +
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

            return new UploadDocumentResponse(
                    "Documento actualizado correctamente. Los jurados evaluarán la nueva versión.",
                    filePath,
                    studentDocument.getStatus().name(),
                    studentModality.getStatus().name()
            );

        } else if (isResubmittingCorrection) {
            // Marcar el documento como corrección reenviada
            studentDocument.setStatus(DocumentStatus.CORRECTION_RESUBMITTED);
            studentDocumentRepository.save(studentDocument);

            // Jurados que solicitaron las correcciones: se capturan ANTES de borrar sus votos,
            // porque el listener de notificaciones los necesita tras el resubmit
            List<Long> requestingExaminerIds = List.of();
            if (currentModalityStatus == ModalityProcessStatus.CORRECTIONS_REQUESTED_EXAMINERS) {
                List<ExaminerDocumentReview> oldReviews = examinerDocumentReviewRepository
                        .findByStudentDocumentId(studentDocument.getId());
                // Eliminar solo los votos de CORRECTIONS_REQUESTED; los ACCEPTED se conservan
                List<ExaminerDocumentReview> reviewsToDelete = oldReviews.stream()
                        .filter(r -> r.getDecision() == ExaminerDocumentDecision.CORRECTIONS_REQUESTED)
                        .toList();
                requestingExaminerIds = reviewsToDelete.stream()
                        .map(r -> r.getExaminer().getId())
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
                    new ModalityEvent(NotificationType.CORRECTION_RESUBMITTED, studentModality.getId(), student.getId(), Map.of(
                            ModalityEvent.KEY_DOCUMENT_ID, studentDocument.getId(),
                            ModalityEvent.KEY_STUDENT_ID, uploader.getId(),
                            ModalityEvent.KEY_DOCUMENT_NAME, requiredDocument.getDocumentName(),
                            ModalityEvent.KEY_EXAMINER_IDS, requestingExaminerIds
                    ))
            );

        } else {
            // Subida normal: estado PENDING
            DocumentStatus currentDocStatus = studentDocument.getStatus();
            if (currentDocStatus == DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW ||
                    currentDocStatus == DocumentStatus.ACCEPTED_FOR_PROGRAM_HEAD_REVIEW ||
                    currentDocStatus == DocumentStatus.ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW) {
                throw new ValidationException("El documento ya fue aprobado por los revisores. Para modificarlo debes usar la solicitud de edición.");
            }

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

        return new UploadDocumentResponse(
                        isResubmittingCorrection
                                ? "Documento de corrección enviado correctamente. Será revisado por el evaluador correspondiente."
                                : "Documento subido correctamente",
                        filePath,
                        studentDocument.getStatus().name(),
                        studentModality.getStatus().name()
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
    public ValidateAllDocumentsUploadedResponse validateAllDocumentsUploaded(Long studentModalityId, User user) {

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        boolean isActiveMember = studentModalityMemberRepository.isActiveMember(studentModalityId, user.getId());
        boolean isDirector = studentModality.getProjectDirector() != null
                && studentModality.getProjectDirector().getId().equals(user.getId());
        if (!isActiveMember && !isDirector) {
            throw new ForbiddenException("No autorizado para consultar los documentos de esta modalidad");
        }

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

        return new ValidateAllDocumentsUploadedResponse(
                        allUploaded,
                        missingDocuments
                );
    }

    @Transactional(readOnly = true)
    public AvailableDocumentsResponse getAvailableDocumentsForStudent(User student) {

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

            List<AvailableDocumentsResponse.AvailableDocumentDTO> documentList = mandatoryDocuments.stream()
                    .map(requiredDoc -> new AvailableDocumentsResponse.AvailableDocumentDTO(
                            requiredDoc.getId(),
                            requiredDoc.getDocumentName(),
                            requiredDoc.getDescription(),
                            requiredDoc.getDocumentType(),
                            requiredDoc.getAllowedFormat(),
                            requiredDoc.getMaxFileSizeMB(),
                            false,
                            null, null, null, null, null))
                    .toList();

            return new AvailableDocumentsResponse(
                    false,
                    "Tienes documentos obligatorios pendientes por cargar.",
                    missingMandatoryDocs,
                    studentModalityId,
                    documentList,
                    new AvailableDocumentsResponse.DocumentStatistics(
                            documentList.size(), 0, documentList.size(), documentList.size(), 0)
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

        List<AvailableDocumentsResponse.AvailableDocumentDTO> documentList = allDocuments.stream()
                .map(requiredDoc -> {
                    StudentDocument uploaded = uploadedMap.get(requiredDoc.getId());
                    return new AvailableDocumentsResponse.AvailableDocumentDTO(
                            requiredDoc.getId(),
                            requiredDoc.getDocumentName(),
                            requiredDoc.getDescription(),
                            requiredDoc.getDocumentType(),
                            requiredDoc.getAllowedFormat(),
                            requiredDoc.getMaxFileSizeMB(),
                            uploaded != null,
                            uploaded != null ? uploaded.getId() : null,
                            uploaded != null ? uploaded.getFileName() : null,
                            uploaded != null ? uploaded.getStatus() : null,
                            uploaded != null ? uploaded.getNotes() : null,
                            uploaded != null ? uploaded.getUploadDate() : null);
                })
                .toList();

        long totalDocuments = documentList.size();
        long uploadedCount = documentList.stream()
                .filter(AvailableDocumentsResponse.AvailableDocumentDTO::uploaded)
                .count();
        long mandatoryCount = documentList.stream()
                .filter(d -> d.documentType() == DocumentType.MANDATORY)
                .count();
        long secondaryCount = documentList.stream()
                .filter(d -> d.documentType() == DocumentType.SECONDARY)
                .count();

        return new AvailableDocumentsResponse(
                true,
                "Todos los documentos obligatorios han sido cargados.",
                List.of(),
                studentModalityId,
                documentList,
                new AvailableDocumentsResponse.DocumentStatistics(
                        totalDocuments,
                        uploadedCount,
                        totalDocuments - uploadedCount,
                        mandatoryCount,
                        secondaryCount)
        );
    }

    @Transactional(readOnly = true)
    public List<StudentDocumentResponse> getStudentDocuments(Long studentModalityId, User user) {

        StudentModality studentModality = studentModalityRepository
                .findById(studentModalityId)
                .orElseThrow(() ->
                        new NotFoundException("Modalidad del estudiante no encontrada")
                );

        boolean isDirector = studentModality.getProjectDirector() != null
                && studentModality.getProjectDirector().getId().equals(user.getId());
        boolean isProgramAuthority = programAuthorityRepository.existsByUser_IdAndAcademicProgram_Id(
                user.getId(),
                studentModality.getAcademicProgram().getId()
        );
        boolean isAssignedExaminer = defenseExaminerRepository
                .findByStudentModalityIdAndExaminerId(studentModalityId, user.getId())
                .isPresent();

        if (!isDirector && !isProgramAuthority && !isAssignedExaminer) {
            throw new ForbiddenException("No autorizado para consultar los documentos de esta modalidad");
        }

        List<StudentDocument> documents =
                studentDocumentRepository.findByStudentModalityId(studentModalityId);

        List<StudentDocumentResponse> response = documents.stream()
                .map(doc -> new StudentDocumentResponse(
                        doc.getId(),
                        doc.getDocumentConfig().getDocumentName(),
                        doc.getDocumentConfig().getDocumentType(),
                        doc.getStatus(),
                        doc.getNotes(),
                        doc.getUploadDate(),
                        doc.getFilePath()))
                .toList();

        return response;
    }
    @Transactional(readOnly = true)
    public Resource viewStudentDocument(Long studentDocumentId, User currentUser) throws MalformedURLException {

        StudentDocument doc = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new NotFoundException("Document not found"));

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
                || resourceAccessPolicy.tryRequire(() -> resourceAccessPolicy.requireLeader(modality, currentUser, "No tienes permiso para ver este documento"))
                || resourceAccessPolicy.tryRequire(() -> resourceAccessPolicy.requireActiveMember(modality.getId(), currentUser, "No tienes permiso para ver este documento"))
                || resourceAccessPolicy.tryRequire(() -> resourceAccessPolicy.requireAssignedExaminer(modality.getId(), currentUser, "No tienes permiso para ver este documento"));
    }

    /**
     * Valida que todos los documentos MANDATORY y SECONDARY de la modalidad estén subidos.
     * Usado por el workflow (aprobación/rechazo final del comité).
     */
    public RequiredDocumentsUploadedResponse validateAllRequiredDocumentsUploaded(Long studentModalityId) {
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

        List<RequiredDocumentsUploadedResponse.MissingDocumentInfo> missingDocuments = new ArrayList<>();

        for (RequiredDocument required : requiredDocuments) {
            if (!uploadedMap.containsKey(required.getId())) {
                missingDocuments.add(new RequiredDocumentsUploadedResponse.MissingDocumentInfo(
                        required.getId(),
                        required.getDocumentName(),
                        required.getDocumentType().toString(),
                        required.getDescription() != null ? required.getDescription() : "Sin descripción"));
            }
        }

        boolean allUploaded = missingDocuments.isEmpty();

        return new RequiredDocumentsUploadedResponse(
                allUploaded,
                requiredDocuments.size(),
                uploadedDocuments.size(),
                missingDocuments,
                missingDocuments.size());
    }

    /**
     * Valida que todos los documentos MANDATORY y SECONDARY de la modalidad
     * tengan estado ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW.
     * Usado por el workflow (aprobación/rechazo final del comité).
     */
    public DocumentsAcceptedForCommitteeResponse validateAllDocumentsAcceptedForCommittee(Long studentModalityId) {
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

        List<DocumentsAcceptedForCommitteeResponse.NotAcceptedDocumentInfo> notAcceptedDocuments = new ArrayList<>();

        for (RequiredDocument required : requiredDocuments) {
            StudentDocument uploaded = uploadedMap.get(required.getId());
            boolean accepted = uploaded != null &&
                    uploaded.getStatus() == DocumentStatus.ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW;
            if (!accepted) {
                notAcceptedDocuments.add(new DocumentsAcceptedForCommitteeResponse.NotAcceptedDocumentInfo(
                        required.getId(),
                        required.getDocumentName(),
                        required.getDocumentType().toString(),
                        uploaded != null ? uploaded.getStatus().toString() : "NO_SUBIDO"));
            }
        }

        return new DocumentsAcceptedForCommitteeResponse(
                notAcceptedDocuments.isEmpty(),
                notAcceptedDocuments,
                notAcceptedDocuments.size(),
                requiredDocuments.size());
    }

    // T5.12: la I/O (Files.copy) queda fuera de la tx; la persistencia va en persistResubmit
    public ResubmitDocumentResponse resubmitCorrectedDocument(Long studentModalityId, Long documentId, MultipartFile file, User student) throws IOException {

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

        String originalFilename = file.getOriginalFilename();
        String safeOriginal = TranslationUtils.sanitizeFileName(FilenameUtils.getName(originalFilename != null ? originalFilename : ""));
        if (safeOriginal.isEmpty()) {
            safeOriginal = "documento";
        }
        String finalFileName = UUID.randomUUID() + "_" + safeOriginal;

        // T5.12: ruta desde la modalidad del parámetro (relaciones EAGER) en lugar de
        // document.getStudentModality() (LAZY, no navegable fuera de tx); misma modalidad ya validada
        String modalityPath = TranslationUtils.sanitizeFileName(
                studentModality.getProgramDegreeModality()
                        .getDegreeModality()
                        .getName(), "[^a-zA-Z0-9]");

        String studentPath = TranslationUtils.studentFolder(student.getName(), student.getLastName(), student.getId());

        Path basePath = Paths.get(uploadDir, modalityPath, studentPath);
        Files.createDirectories(basePath);

        Path fullPath = basePath.resolve(finalFileName);
        Files.copy(file.getInputStream(), fullPath, StandardCopyOption.REPLACE_EXISTING);

        return persistResubmit(studentModality, student, documentId, originalFilename, fullPath.toString());
    }

    @Transactional
    public ResubmitDocumentResponse persistResubmit(StudentModality studentModality, User student,
                                                    Long documentId, String originalFilename, String filePath) {

        StudentDocument document = studentDocumentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Documento no encontrado"));

        if (!document.getStudentModality().getId().equals(studentModality.getId())) {
            throw new ForbiddenException("El documento no pertenece a esta modalidad");
        }

        if (document.getStatus() != DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD &&
                document.getStatus() != DocumentStatus.CORRECTIONS_REQUESTED_BY_PROGRAM_CURRICULUM_COMMITTEE) {
            throw new ValidationException("El documento no está en estado de correcciones solicitadas");
        }

        document.setFileName(originalFilename);
        document.setFilePath(filePath);
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

        // ponytail: self-invocation desde resubmitCorrectedDocument; getDocumentConfig() (LAZY) no es navegable
        // fuera de sesión (OSIV off). El getId() del proxy no inicializa; se recarga el documento requerido por id.
        String documentConfigName = requiredDocumentRepository.findById(document.getDocumentConfig().getId())
                .map(RequiredDocument::getDocumentName)
                .orElse(null);

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.CORRECTION_RESUBMITTED, studentModality.getId(), student.getId(), Map.of(
                        ModalityEvent.KEY_DOCUMENT_ID, documentId,
                        ModalityEvent.KEY_STUDENT_ID, student.getId(),
                        ModalityEvent.KEY_DOCUMENT_NAME, documentConfigName
                ))
        );

        return new ResubmitDocumentResponse(
                true,
                "Documento corregido enviado exitosamente. Será revisado por el jurado correspondiente.",
                documentId,
                document.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public CorrectionDeadlineStatusResponse getCorrectionDeadlineStatus(Long studentModalityId, User user) {

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
            return new CorrectionDeadlineStatusResponse(
                    false,
                    studentModality.getStatus(),
                    "No hay correcciones solicitadas actualmente",
                    null, null, null, null, null);
        }

        LocalDateTime now = LocalDateTime.now();
        long daysRemaining = 0;
        boolean isExpired = false;

        if (studentModality.getCorrectionDeadline() != null) {
            daysRemaining = ChronoUnit.DAYS.between(now, studentModality.getCorrectionDeadline());
            isExpired = daysRemaining < 0;
        }

        return new CorrectionDeadlineStatusResponse(
                true,
                studentModality.getStatus(),
                null,
                studentModality.getCorrectionRequestDate(),
                studentModality.getCorrectionDeadline(),
                Math.max(0, daysRemaining),
                isExpired,
                studentModality.getCorrectionReminderSent() != null ? studentModality.getCorrectionReminderSent() : false
        );
    }
}
