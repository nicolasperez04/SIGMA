package com.SIGMA.USCO.documents.controller;

import com.SIGMA.USCO.documents.dto.RequiredDocumentDTO;
import com.SIGMA.USCO.documents.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Documentos Requeridos", description = "Gestión de documentos requeridos para modalidades de grado")
@RestController
@RequestMapping("/required-documents")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "Crear documento requerido", description = "Crea un nuevo documento requerido para una modalidad de grado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documento creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('PERM_CREATE_REQUIRED_DOCUMENT') or hasAuthority('PERM_UPDATE_REQUIRED_DOCUMENT')")
    public ResponseEntity<String> createRequiredDocument(@Valid @RequestBody RequiredDocumentDTO request) {
        documentService.createRequiredDocument(request);
        return ResponseEntity.ok("Documento obligatorio registrado correctamente.");
    }

    @Operation(summary = "Actualizar documento requerido", description = "Actualiza un documento requerido existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documento actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PutMapping("/update/{documentId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_REQUIRED_DOCUMENT')")
    public ResponseEntity<String> updateRequiredDocument(@Parameter(description = "ID del documento requerido") @PathVariable Long documentId, @Valid @RequestBody RequiredDocumentDTO request) {
        documentService.updateRequiredDocument(documentId, request);
        return ResponseEntity.ok("Documento obligatorio actualizado correctamente.");
    }

    @Operation(summary = "Eliminar documento requerido", description = "Elimina un documento requerido del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documento eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PutMapping("/delete/{documentId}")
    @PreAuthorize("hasAuthority('PERM_DELETE_REQUIRED_DOCUMENT')")
    public ResponseEntity<String> deleteRequiredDocument(@Parameter(description = "ID del documento requerido") @PathVariable Long documentId) {
        documentService.deleteRequiredDocument(documentId);
        return ResponseEntity.ok("Documento obligatorio desactivado correctamente.");
    }

    @Operation(summary = "Obtener documentos por modalidad", description = "Retorna todos los documentos requeridos para una modalidad específica")
    @ApiResponse(responseCode = "200", description = "Lista de documentos obtenida")
    @GetMapping("/modality/{modalityId}")
    public ResponseEntity<List<RequiredDocumentDTO>>
    getByModality(@Parameter(description = "ID de la modalidad de grado") @PathVariable Long modalityId) {
        return ResponseEntity.ok(documentService.getRequiredDocumentsByModality(modalityId));
    }

    @Operation(summary = "Obtener documentos por modalidad y estado", description = "Retorna documentos requeridos filtrados por estado (activo/inactivo)")
    @ApiResponse(responseCode = "200", description = "Lista de documentos filtrada obtenida")
    @GetMapping("/modality/{modalityId}/filter")
    @PreAuthorize("hasAuthority('PERM_VIEW_REQUIRED_DOCUMENT')")
    public ResponseEntity<List<RequiredDocumentDTO>>
    getByModalityAndStatus(@Parameter(description = "ID de la modalidad de grado") @PathVariable Long modalityId, 
                           @Parameter(description = "Filtrar por estado activo (true=activos, false=inactivos)") @RequestParam boolean active) {
        return ResponseEntity.ok(documentService.getRequiredDocumentsByModalityAndStatus(modalityId, active));
    }
}
