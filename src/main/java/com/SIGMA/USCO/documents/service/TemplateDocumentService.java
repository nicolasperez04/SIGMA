package com.SIGMA.USCO.documents.service;

import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.documents.entity.TemplateDocument;
import com.SIGMA.USCO.documents.repository.TemplateDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TemplateDocumentService {

    private final TemplateDocumentRepository templateDocumentRepository;

    @Transactional(readOnly = true)
    public Resource downloadTemplate(Long templateId) {

        TemplateDocument template = templateDocumentRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Plantilla no encontrada"));

        Resource resource = new ClassPathResource(template.getFilePath());

        if (!resource.exists()) {
            throw new NotFoundException("Archivo no encontrado");
        }

        return resource;
    }
}