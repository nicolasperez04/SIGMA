package com.SIGMA.USCO.documents.service;

import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.repository.DegreeModalityRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;

import com.SIGMA.USCO.documents.dto.StatusHistoryDTO;
import com.SIGMA.USCO.documents.dto.RequiredDocumentDTO;
import com.SIGMA.USCO.documents.entity.*;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import com.SIGMA.USCO.documents.repository.RequiredDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentStatusHistoryRepository;
import com.SIGMA.USCO.common.exception.ConflictException;
import com.SIGMA.USCO.common.exception.ForbiddenException;
import com.SIGMA.USCO.common.exception.InternalException;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.common.security.Roles;
import com.SIGMA.USCO.shared.util.ResourceAccessPolicy;
import com.SIGMA.USCO.shared.util.TranslationUtils;
import com.SIGMA.USCO.common.validation.FileValidator;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final RequiredDocumentRepository requiredDocumentRepository;
    private final DegreeModalityRepository degreeModalityRepository;
    private final UserRepository userRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final StudentDocumentStatusHistoryRepository documentHistoryRepository;
    private final StudentModalityRepository studentModalityRepository;
    private final ResourceAccessPolicy resourceAccessPolicy;
    private final ProgramAuthorityRepository programAuthorityRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Transactional
    public void createRequiredDocument(RequiredDocumentDTO request) {

        var modality = degreeModalityRepository.findById(request.getModalityId())
                .orElseThrow(() -> new NotFoundException(
                        "La modalidad con ID " + request.getModalityId() + " no existe.")
                );

        // ponytail: unicidad de nombre dentro de la modalidad (solo docs activos)
        if (requiredDocumentRepository.existsByModality_IdAndDocumentNameIgnoreCaseAndActiveTrue(
                request.getModalityId(), request.getDocumentName())) {
            throw new ValidationException("Ya existe un documento con ese nombre en la modalidad.");
        }

        RequiredDocument document = RequiredDocument.builder()
                .modality(modality)
                .documentName(request.getDocumentName())
                .allowedFormat(request.getAllowedFormat())
                .maxFileSizeMB(request.getMaxFileSizeMB())
                .documentType(request.getDocumentType())
                .description(request.getDescription())
                .active(true)
                .requiresProposalEvaluation(request.isRequiresProposalEvaluation())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        requiredDocumentRepository.save(document);

    }

    @Transactional
    public void updateRequiredDocument(Long documentId, RequiredDocumentDTO request) {

        var document = requiredDocumentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        "El documento obligatorio con ID " + documentId + " no existe."
                ));

        // ponytail: unicidad de nombre dentro de la modalidad, excluyendo este documento
        if (requiredDocumentRepository.existsByModality_IdAndDocumentNameIgnoreCaseAndActiveTrueAndIdNot(
                document.getModality().getId(), request.getDocumentName(), documentId)) {
            throw new ValidationException("Ya existe un documento con ese nombre en la modalidad.");
        }

        document.setDocumentName(request.getDocumentName());
        document.setDescription(request.getDescription());
        document.setAllowedFormat(request.getAllowedFormat());
        document.setMaxFileSizeMB(request.getMaxFileSizeMB());
        document.setDocumentType(request.getDocumentType());
        document.setActive(request.isActive());
        document.setRequiresProposalEvaluation(request.isRequiresProposalEvaluation());
        document.setUpdatedAt(LocalDateTime.now());

        requiredDocumentRepository.save(document);

    }


    @Transactional
    public void deleteRequiredDocument(Long documentId) {

        RequiredDocument document = requiredDocumentRepository.findById(documentId)
                .orElseThrow(() ->
                        new NotFoundException("El documento obligatorio con ID " + documentId + " no existe.")
                );

        if (!document.isActive()) {
            throw new ValidationException("El documento obligatorio ya se encuentra inactivo.");
        }

        document.setActive(false);
        document.setUpdatedAt(LocalDateTime.now());

        requiredDocumentRepository.save(document);

    }

    @Transactional(readOnly = true)
    public List<RequiredDocumentDTO> getRequiredDocumentsByModality(Long modalityId) {

        if (!degreeModalityRepository.existsById(modalityId)) {
            throw new NotFoundException("La modalidad con ID " + modalityId + " no existe.");
        }

        return requiredDocumentRepository
                        .findByModalityIdAndActive(modalityId, true)
                        .stream()
                        .map(RequiredDocumentDTO::from)
                        .toList();
    }

    @Transactional(readOnly = true)
    public List<RequiredDocumentDTO>
    getRequiredDocumentsByModalityAndStatus(Long modalityId, boolean active) {

        if (!degreeModalityRepository.existsById(modalityId)) {
            throw new NotFoundException("La modalidad con ID " + modalityId + " no existe.");
        }

        return requiredDocumentRepository
                        .findByModalityIdAndActive(modalityId, active)
                        .stream()
                        .map(RequiredDocumentDTO::from)
                        .toList();
    }





    @Transactional(readOnly = true)
    public List<StatusHistoryDTO> getDocumentHistory(Long studentDocumentId, User currentUser) {

        User student = currentUser;

        StudentDocument document = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new NotFoundException("Document not found"));

        resourceAccessPolicy.requireLeader(document.getStudentModality(), student,
                "No authorized access to document history.");

        return documentHistoryRepository
                        .findByStudentDocumentIdOrderByChangeDateAsc(studentDocumentId)
                        .stream()
                        .map(h -> StatusHistoryDTO.builder()
                                .status(h.getStatus().name())
                                .description(TranslationUtils.translateDocumentStatus(h.getStatus()))
                                .changeDate(h.getChangeDate())
                                .responsible(
                                        h.getResponsible() != null
                                                ? h.getResponsible().getEmail()
                                                : "Sistema"
                                )
                                .observations(h.getObservations())
                                .build()
                        )
                        .toList();
    }

    @Transactional(readOnly = true)
    public StudentDocument getDocumentCancellation(Long studentModalityId, User currentUser) {
        // ponytail: devuelve la entidad solo para servir bytes (filePath/fileName) en el controller;
        // no se serializa al JSON, así que no viola el contrato de DTOs de respuesta.

        User current = currentUser;

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        boolean isLeader = studentModality.getLeader() != null
                && studentModality.getLeader().getId().equals(current.getId());
        boolean isDirector = studentModality.getProjectDirector() != null
                && studentModality.getProjectDirector().getId().equals(current.getId());
        boolean isProgramAuthority = studentModality.getAcademicProgram() != null
                && programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRoleIn(
                        current.getId(),
                        studentModality.getAcademicProgram().getId(),
                        List.of(ProgramRole.PROGRAM_HEAD, ProgramRole.PROGRAM_CURRICULUM_COMMITTEE));
        // ponytail: SUPERADMIN/EXAMINER entran por rol (poseen PERM_VIEW_CANCELLATIONS y hoy ven
        // cancelaciones de terceros); restringir por programa requeriría inyectar más repos.
        boolean isStaff = current.getRoles().stream()
                .anyMatch(role -> role.getName().equals("SUPERADMIN")
                        || role.getName().equals(Roles.ROLE_EXAMINER));

        if (!(isLeader || isDirector || isProgramAuthority || isStaff)) {
            throw new ForbiddenException("No autorizado");
        }

        return studentDocumentRepository
                .findByStudentModalityIdAndDocumentConfig_DocumentType(
                        studentModalityId,
                        DocumentType.CANCELLATION
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "No se encontró documento de cancelación para esta modalidad. " +
                        "El estudiante debe subirlo primero."
                ));
    }

    // T5.12: la I/O (Files.copy) queda fuera de la tx; solo la persistencia va en el método transaccional
    public void uploadCancellationDocument(Long studentModalityId, MultipartFile file, User currentUser) {

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        resourceAccessPolicy.requireLeader(studentModality, currentUser, "No autorizado");

        // Usar DocumentType.CANCELLATION en lugar de buscar por nombre
        Long modalityId = studentModality.getProgramDegreeModality().getDegreeModality().getId();

        RequiredDocument cancellationDocumentConfig = requiredDocumentRepository
                .findByModalityIdAndActiveTrueAndDocumentType(
                        modalityId,
                        DocumentType.CANCELLATION
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "No se encontró configuración de documento de cancelación para esta modalidad"
                ));

        FileValidator.validateNotEmpty(file);
        FileValidator.validateExtension(file, cancellationDocumentConfig.getAllowedFormat());
        FileValidator.validateMime(file, FilenameUtils.getExtension(file.getOriginalFilename()).toUpperCase());
        FileValidator.validateSize(file, cancellationDocumentConfig.getMaxFileSizeMB());

        String safeOriginal = FilenameUtils.getName(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        safeOriginal = TranslationUtils.sanitizeFileName(safeOriginal);
        if (safeOriginal.isEmpty()) {
            safeOriginal = "documento";
        }
        String fileName = UUID.randomUUID() + "_" + safeOriginal;

        // Crear estructura de carpetas para documentos de cancelación
        String modalityFolder = TranslationUtils.sanitizeFileName(
                studentModality.getProgramDegreeModality()
                        .getDegreeModality().getName(), "[^a-zA-Z0-9]");
        String studentFolder = TranslationUtils.studentFolder(
                studentModality.getLeader().getName(),
                studentModality.getLeader().getLastName(),
                studentModality.getLeader().getId());

        Path destination = Paths.get(uploadDir, modalityFolder, studentFolder, "cancelaciones", fileName);

        try {
            Files.createDirectories(destination.getParent());
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new InternalException("Error guardando el archivo", e);
        }

        persistCancellationDocument(studentModality, cancellationDocumentConfig, fileName, destination.toString());
    }

    @Transactional
    public void persistCancellationDocument(StudentModality studentModality,
                                            RequiredDocument cancellationDocumentConfig,
                                            String fileName, String filePath) {

        // Verificar si ya existe un documento de cancelación
        Optional<StudentDocument> existingDoc = studentDocumentRepository
                .findByStudentModalityIdAndDocumentConfig_DocumentType(
                        studentModality.getId(),
                        DocumentType.CANCELLATION
                )
                .stream()
                .findFirst();

        StudentDocument studentDocument;

        if (existingDoc.isPresent()) {
            // Actualizar documento existente
            studentDocument = existingDoc.get();
            studentDocument.setFileName(fileName);
            studentDocument.setFilePath(filePath);
            studentDocument.setStatus(DocumentStatus.PENDING);
            studentDocument.setUploadDate(LocalDateTime.now());
        } else {
            // Crear nuevo documento
            studentDocument = StudentDocument.builder()
                    .studentModality(studentModality)
                    .documentConfig(cancellationDocumentConfig)
                    .fileName(fileName)
                    .filePath(filePath)
                    .status(DocumentStatus.PENDING)
                    .uploadDate(LocalDateTime.now())
                    .build();
        }

        studentDocumentRepository.save(studentDocument);

        // Registrar en historial
        documentHistoryRepository.save(
            StudentDocumentStatusHistory.builder()
                .studentDocument(studentDocument)
                .status(DocumentStatus.PENDING)
                .changeDate(LocalDateTime.now())
                .responsible(studentModality.getLeader())
                .observations("Documento de cancelación cargado por el estudiante")
                .build()
        );
    }

    }
