package com.SIGMA.USCO.documents.service;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Repository.DegreeModalityRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.repository.UserRepository;

import com.SIGMA.USCO.documents.dto.StatusHistoryDTO;
import com.SIGMA.USCO.documents.dto.RequiredDocumentDTO;
import com.SIGMA.USCO.documents.entity.*;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import com.SIGMA.USCO.documents.repository.RequiredDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentStatusHistoryRepository;
import com.SIGMA.USCO.common.exception.ForbiddenException;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.security.SecurityUtils;
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
import java.util.Arrays;
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

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Transactional
    public String createRequiredDocument(RequiredDocumentDTO request) {

        var modality = degreeModalityRepository.findById(request.getModalityId())
                .orElseThrow(() -> new NotFoundException(
                        "La modalidad con ID " + request.getModalityId() + " no existe.")
                );

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

        return "Documento obligatorio registrado correctamente.";
    }

    @Transactional
    public String updateRequiredDocument(Long documentId, RequiredDocumentDTO request) {

        var document = requiredDocumentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        "El documento obligatorio con ID " + documentId + " no existe."
                ));

        document.setDocumentName(request.getDocumentName());
        document.setDescription(request.getDescription());
        document.setAllowedFormat(request.getAllowedFormat());
        document.setMaxFileSizeMB(request.getMaxFileSizeMB());
        document.setDocumentType(request.getDocumentType());
        document.setActive(request.isActive());
        document.setRequiresProposalEvaluation(request.isRequiresProposalEvaluation());
        document.setUpdatedAt(LocalDateTime.now());

        requiredDocumentRepository.save(document);

        return "Documento obligatorio actualizado correctamente.";
    }


    public String deleteRequiredDocument(Long documentId) {

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

        return "Documento obligatorio desactivado correctamente.";
    }

    @Transactional(readOnly = true)
    public List<RequiredDocumentDTO> getRequiredDocumentsByModality(Long modalityId) {

        if (!degreeModalityRepository.existsById(modalityId)) {
            throw new NotFoundException("La modalidad con ID " + modalityId + " no existe.");
        }

        return requiredDocumentRepository
                        .findByModalityIdAndActive(modalityId, true)
                        .stream()
                        .map(doc -> RequiredDocumentDTO.builder()
                                .id(doc.getId())
                                .modalityId( doc.getModality().getId())
                                .documentName(doc.getDocumentName())
                                .description(doc.getDescription())
                                .allowedFormat(doc.getAllowedFormat())
                                .maxFileSizeMB(doc.getMaxFileSizeMB())
                                .documentType(doc.getDocumentType())
                                .active(doc.isActive())
                                .requiresProposalEvaluation(doc.isRequiresProposalEvaluation())
                                .build())
                        .toList();
    }

    public List<RequiredDocumentDTO>
    getRequiredDocumentsByModalityAndStatus(Long modalityId, boolean active) {

        if (!degreeModalityRepository.existsById(modalityId)) {
            throw new NotFoundException("La modalidad con ID " + modalityId + " no existe.");
        }

        return requiredDocumentRepository
                        .findByModalityIdAndActive(modalityId, active)
                        .stream()
                        .map(doc -> RequiredDocumentDTO.builder()
                                .id(doc.getId())
                                .documentName(doc.getDocumentName())
                                .description(doc.getDescription())
                                .allowedFormat(doc.getAllowedFormat())
                                .maxFileSizeMB(doc.getMaxFileSizeMB())
                                .documentType(doc.getDocumentType())
                                .active(doc.isActive())
                                .requiresProposalEvaluation(doc.isRequiresProposalEvaluation())
                                .build())
                        .toList();
    }





    private String describeDocumentStatus(DocumentStatus status) {

        return switch (status) {

            case PENDING ->
                    "Documento cargado y pendiente de revisión.";

            case ACCEPTED_FOR_PROGRAM_HEAD_REVIEW ->
                    "Documento aprobado por la jefatura del programa.";

            case REJECTED_FOR_PROGRAM_HEAD_REVIEW ->
                    "Documento rechazado por la jefatura del programa.";

            case CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD ->
                    "La jefatura del programa solicitó correcciones.";

            case ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW ->
                    "Documento aprobado por el Comité Curricular del programa.";

            case REJECTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW ->
                    "Documento rechazado por el Comité Curricular del programa.";

            case CORRECTIONS_REQUESTED_BY_PROGRAM_CURRICULUM_COMMITTEE ->
                    "El Comité Curricular del programa solicitó correcciones.";

            default ->
                    "Estado del documento no definido.";
        };
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryDTO> getDocumentHistory(Long studentDocumentId) {

        User student = SecurityUtils.getCurrentUser();

        StudentDocument document = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new NotFoundException("Document not found"));

        if (!document.getStudentModality().getLeader().getId().equals(student.getId())) {
            throw new ForbiddenException("No authorized access to document history.");
        }

        return documentHistoryRepository
                        .findByStudentDocumentIdOrderByChangeDateAsc(studentDocumentId)
                        .stream()
                        .map(h -> StatusHistoryDTO.builder()
                                .status(h.getStatus().name())
                                .description(describeDocumentStatus(h.getStatus()))
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
    public StudentDocument getDocumentCancellation(Long studentModalityId) {
        // ponytail: devuelve la entidad solo para servir bytes (filePath/fileName) en el controller;
        // no se serializa al JSON, así que no viola el contrato de DTOs de respuesta.

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

    @Transactional
    public void uploadCancellationDocument(Long studentModalityId, MultipartFile file) {

        User uploader = SecurityUtils.getCurrentUser();
        String email = uploader.getEmail();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        if (!studentModality.getLeader().getEmail().equals(email)) {
            throw new ForbiddenException("No autorizado");
        }

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

        validateFile(file, cancellationDocumentConfig);

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        // Crear estructura de carpetas para documentos de cancelación
        String modalityFolder = studentModality.getProgramDegreeModality()
                .getDegreeModality().getName().replaceAll("[^a-zA-Z0-9]", "_");
        String studentFolder = studentModality.getLeader().getName() +
                studentModality.getLeader().getLastName() + "_" +
                studentModality.getLeader().getLastName() + "_" +
                studentModality.getId();

        Path destination = Paths.get(uploadDir, modalityFolder, studentFolder, "cancelaciones", fileName);

        try {
            Files.createDirectories(destination.getParent());
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Error guardando el archivo: " + e.getMessage());
        }

        // Verificar si ya existe un documento de cancelación
        Optional<StudentDocument> existingDoc = studentDocumentRepository
                .findByStudentModalityIdAndDocumentConfig_DocumentType(
                        studentModalityId,
                        DocumentType.CANCELLATION
                )
                .stream()
                .findFirst();

        StudentDocument studentDocument;

        if (existingDoc.isPresent()) {
            // Actualizar documento existente
            studentDocument = existingDoc.get();
            studentDocument.setFileName(fileName);
            studentDocument.setFilePath(destination.toString());
            studentDocument.setStatus(DocumentStatus.PENDING);
            studentDocument.setUploadDate(LocalDateTime.now());
        } else {
            // Crear nuevo documento
            studentDocument = StudentDocument.builder()
                    .studentModality(studentModality)
                    .documentConfig(cancellationDocumentConfig)
                    .fileName(fileName)
                    .filePath(destination.toString())
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

    private void validateFile(MultipartFile file, RequiredDocument config) {

        if (file.isEmpty()) {
            throw new ValidationException("Archivo vacío");
        }

        String extension = FilenameUtils.getExtension(file.getOriginalFilename())
                .toUpperCase();

        List<String> allowed =
                Arrays.stream(config.getAllowedFormat().split(","))
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .toList();

        if (!allowed.contains(extension)) {
            throw new ValidationException("Formato no permitido");
        }

        long maxSizeBytes = config.getMaxFileSizeMB() * 1024L * 1024L;

        if (file.getSize() > maxSizeBytes) {
            throw new ValidationException("Archivo supera el tamaño permitido");
        }
    }

}
