package com.SIGMA.USCO.documents.service;

import com.SIGMA.USCO.documents.entity.TemplateDocument;
import com.SIGMA.USCO.documents.repository.TemplateDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TemplateDocumentService {

    private final TemplateDocumentRepository templateDocumentRepository;

    public ResponseEntity<Resource> downloadTemplate(Long templateId) {

        TemplateDocument template = templateDocumentRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));

        Resource resource = new ClassPathResource(template.getFilePath());

        if (!resource.exists()) {
            throw new RuntimeException("Archivo no encontrado");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + template.getName() + "\"")
                .body(resource);
    }
}

