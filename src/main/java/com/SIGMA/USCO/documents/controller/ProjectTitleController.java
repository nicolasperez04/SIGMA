package com.SIGMA.USCO.documents.controller;

import com.SIGMA.USCO.documents.service.ProjectTitleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Títulos de Proyectos", description = "Gestión de títulos de proyectos de modalidades de grado")
@RestController
@RequestMapping("/modalities")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class ProjectTitleController {

    private final ProjectTitleService projectTitleService;

    @Operation(summary = "Obtener título del proyecto", 
               description = "Retorna el título del proyecto asociado a una modalidad de grado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Título obtenido"),
            @ApiResponse(responseCode = "404", description = "Modalidad no encontrada")
    })
    @GetMapping("/{studentModalityId}/project-title")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN') or hasRole('PROGRAM_HEAD') or hasRole('JURY')")
    public ResponseEntity<?> getProjectTitle(
            @Parameter(description = "ID de la modalidad del estudiante") @PathVariable Long studentModalityId) {
        try {
            String projectTitle = projectTitleService.getProjectTitle(studentModalityId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "projectTitle", projectTitle != null ? projectTitle : "No registrado"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Actualizar título del proyecto manualmente", 
               description = "Permite a administradores actualizar manualmente el título del proyecto de una modalidad")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Título actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Modalidad no encontrada"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PutMapping("/{studentModalityId}/project-title")
    @PreAuthorize("hasAuthority('PERM_UPDATE_MODALITY') or hasRole('ADMIN')")
    public ResponseEntity<?> updateProjectTitle(
            @Parameter(description = "ID de la modalidad del estudiante") @PathVariable Long studentModalityId,
            @RequestBody Map<String, String> request) {
        try {
            String projectTitle = request.get("projectTitle");
            
            if (projectTitle == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "El campo 'projectTitle' es requerido"
                ));
            }

            projectTitleService.updateProjectTitleManually(studentModalityId, projectTitle);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Título del proyecto actualizado exitosamente",
                    "projectTitle", projectTitle
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

}

