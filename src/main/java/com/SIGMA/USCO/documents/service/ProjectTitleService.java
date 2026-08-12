package com.SIGMA.USCO.documents.service;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.common.exception.ForbiddenException;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.common.util.ResourceAccessPolicy;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para gestionar el título del proyecto en la modalidad de grado.
 * Responsable de extraer y actualizar el título desde documentos PDF.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectTitleService {

    private final PdfTitleExtractorService pdfTitleExtractorService;
    private final StudentModalityRepository studentModalityRepository;
    private final ResourceAccessPolicy resourceAccessPolicy;

    /**
     * Intenta extraer el título del proyecto desde un documento de propuesta
     * y actualiza la modalidad si es exitoso.
     *
     * @param studentDocument El documento subido
     * @return true si se actualizó exitosamente, false en caso contrario
     */
    @Transactional
    public boolean updateProjectTitleFromDocument(StudentDocument studentDocument) {
        if (studentDocument == null) {
            log.warn("StudentDocument es null");
            return false;
        }

        // Solo procesar documentos que requieren evaluación de propuesta
        if (!studentDocument.getDocumentConfig().isRequiresProposalEvaluation()) {
            log.debug("Documento no requiere evaluación de propuesta, saltando extracción de título");
            return false;
        }

        // Si la modalidad ya tiene título, no sobrescribir (a menos que se fuerze)
        StudentModality modality = studentDocument.getStudentModality();
        if (modality.getModalityTitle() != null && !modality.getModalityTitle().isEmpty()) {
            log.debug("Modalidad ID {} ya tiene título asignado: {}", modality.getId(), modality.getModalityTitle());
            return false;
        }

        // Intentar extraer el título del PDF
        String extractedTitle = pdfTitleExtractorService.extractProjectTitle(studentDocument.getFilePath());

        if (extractedTitle != null && !extractedTitle.isEmpty()) {
            modality.setModalityTitle(extractedTitle);
            studentModalityRepository.save(modality);
            log.info("Título de proyecto actualizado para modalidad ID {}: {}", 
                modality.getId(), extractedTitle);
            return true;
        } else {
            log.debug("No se pudo extraer título del documento para modalidad ID {}", modality.getId());
            return false;
        }
    }

    /**
     * Actualiza el título del proyecto manualmente para una modalidad.
     * Útil para correcciones o cuando la extracción automática falla.
     *
     * @param studentModalityId ID de la modalidad
     * @param projectTitle Título del proyecto
     */
    @Transactional
    public void updateProjectTitleManually(Long studentModalityId, String projectTitle) {
        StudentModality modality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        if (projectTitle == null || projectTitle.trim().isEmpty()) {
            log.warn("Intento de asignar título vacío a modalidad ID {}", studentModalityId);
            modality.setModalityTitle(null);
        } else {
            // Limpiar y validar el título
            String cleanTitle = projectTitle.trim();
            if (cleanTitle.length() > 500) {
                cleanTitle = cleanTitle.substring(0, 500);
            }
            modality.setModalityTitle(cleanTitle);
            log.info("Título de proyecto actualizado manualmente para modalidad ID {}: {}", 
                studentModalityId, cleanTitle);
        }

        studentModalityRepository.save(modality);
    }

    /**
     * Obtiene el título del proyecto de una modalidad.
     *
     * @param studentModalityId ID de la modalidad
     * @return Título del proyecto, o null si no está definido
     */
    @Transactional(readOnly = true)
    public String getProjectTitle(Long studentModalityId, User currentUser) {
        StudentModality modality = studentModalityRepository.findById(studentModalityId).orElse(null);
        if (modality != null && !isAuthorized(modality, currentUser)) {
            throw new ForbiddenException("No autorizado");
        }
        return modality != null ? modality.getModalityTitle() : null;
    }

    private boolean isAuthorized(StudentModality modality, User current) {
        boolean isStaff = current.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_PROGRAM_HEAD")
                        || a.getAuthority().equals("PERM_VIEW_REPORT"));
        return isStaff || isParticipant(modality, current);
    }

    private boolean isParticipant(StudentModality modality, User current) {
        return tryRequire(() -> resourceAccessPolicy.requireLeader(modality, current, "No autorizado"))
                || tryRequire(() -> resourceAccessPolicy.requireActiveMember(modality.getId(), current, "No autorizado"))
                || tryRequire(() -> resourceAccessPolicy.requireAssignedExaminer(modality.getId(), current, "No autorizado"))
                || (modality.getProjectDirector() != null
                && modality.getProjectDirector().getId().equals(current.getId()));
    }

    private boolean tryRequire(Runnable check) {
        try {
            check.run();
            return true;
        } catch (ForbiddenException e) {
            return false;
        }
    }

}



