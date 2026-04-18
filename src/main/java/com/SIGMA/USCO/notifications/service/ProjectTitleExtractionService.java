package com.SIGMA.USCO.notifications.service;

import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.documents.service.ProjectTitleService;
import com.SIGMA.USCO.notifications.event.StudentDocumentUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener que captura eventos de actualización de documentos
 * y extrae automáticamente el título del proyecto si es una propuesta.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectTitleExtractionService {

    private final ProjectTitleService projectTitleService;
    private final StudentDocumentRepository studentDocumentRepository;

    @EventListener
    public void onStudentDocumentUpdated(StudentDocumentUpdatedEvent event) {
        try {
            // Obtener el documento desde la BD
            StudentDocument document = studentDocumentRepository.findById(event.getStudentDocumentId())
                    .orElse(null);
            
            if (document == null) {
                log.debug("StudentDocument no encontrado para ID: {}", event.getStudentDocumentId());
                return;
            }

            // Intentar extraer y actualizar el título del proyecto
            boolean titleUpdated = projectTitleService.updateProjectTitleFromDocument(document);
            
            if (titleUpdated) {
                log.info("Título del proyecto extraído y actualizado automáticamente para documento ID: {}", 
                    document.getId());
            }
        } catch (Exception e) {
            log.error("Error en ProjectTitleExtractionService: {}", e.getMessage(), e);
            // No lanzar excepción para no interrumpir el flujo de carga de documentos
        }
    }

}


