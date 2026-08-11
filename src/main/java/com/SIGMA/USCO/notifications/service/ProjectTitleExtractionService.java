package com.SIGMA.USCO.notifications.service;

import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.documents.service.ProjectTitleService;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.event.ModalityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectTitleExtractionService {

    private final ProjectTitleService projectTitleService;
    private final StudentDocumentRepository studentDocumentRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStudentDocumentUpdated(ModalityEvent event) {
        if (event.getType() != NotificationType.DOCUMENT_UPLOADED) return;
        try {
            Long documentId = event.get(ModalityEvent.KEY_STUDENT_DOCUMENT_ID, Long.class);
            if (documentId == null) return;
            StudentDocument document = studentDocumentRepository.findById(documentId).orElse(null);
            if (document == null) {
                log.debug("StudentDocument no encontrado para ID: {}", documentId);
                return;
            }
            boolean titleUpdated = projectTitleService.updateProjectTitleFromDocument(document);
            if (titleUpdated) {
                log.info("Título del proyecto extraído y actualizado automáticamente para documento ID: {}", document.getId());
            }
        } catch (Exception e) {
            log.error("Error en ProjectTitleExtractionService: {}", e.getMessage(), e);
        }
    }
}
