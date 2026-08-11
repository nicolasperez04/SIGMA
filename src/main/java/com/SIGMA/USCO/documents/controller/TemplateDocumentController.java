package com.SIGMA.USCO.documents.controller;

import com.SIGMA.USCO.documents.service.TemplateDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Plantillas de Documentos", description = "Descarga de plantillas de documentos para modalidades de grado")
@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class TemplateDocumentController {

    private final TemplateDocumentService templateDocumentService;

    @Operation(summary = "Descargar plantilla de documento", description = "Descarga la plantilla en formato PDF de un documento requerido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plantilla descargada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Plantilla no encontrada")
    })
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadTemplate(@Parameter(description = "ID de la plantilla de documento") @PathVariable Long id) {
        Resource resource = templateDocumentService.downloadTemplate(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

}

