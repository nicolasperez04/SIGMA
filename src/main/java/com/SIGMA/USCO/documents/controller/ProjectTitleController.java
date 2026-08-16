package com.SIGMA.USCO.documents.controller;

import com.SIGMA.USCO.documents.dto.ProjectTitleRequest;
import com.SIGMA.USCO.documents.dto.ProjectTitleResponse;
import com.SIGMA.USCO.documents.service.ProjectTitleService;
import com.SIGMA.USCO.security.SecurityUtils;
import com.SIGMA.USCO.common.security.Permissions;
import com.SIGMA.USCO.common.security.Roles;
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
    @PreAuthorize("hasRole('" + Roles.ROLE_STUDENT + "') or hasRole('" + Roles.ROLE_ADMIN + "') or hasRole('" + Roles.ROLE_PROGRAM_HEAD + "')")
    public ResponseEntity<ProjectTitleResponse> getProjectTitle(
            @Parameter(description = "ID de la modalidad del estudiante") @PathVariable Long studentModalityId) {
        String projectTitle = projectTitleService.getProjectTitle(studentModalityId, SecurityUtils.getCurrentUser());
        return ResponseEntity.ok(new ProjectTitleResponse(
                true,
                projectTitle != null ? projectTitle : "No registrado",
                null
        ));
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
    @PreAuthorize("hasAuthority('" + Permissions.PERM_UPDATE_MODALITY + "')")
    public ResponseEntity<ProjectTitleResponse> updateProjectTitle(
            @Parameter(description = "ID de la modalidad del estudiante") @PathVariable Long studentModalityId,
            @Valid @RequestBody ProjectTitleRequest request) {
        String projectTitle = request.getProjectTitle();

        projectTitleService.updateProjectTitleManually(studentModalityId, projectTitle);
        
        return ResponseEntity.ok(new ProjectTitleResponse(
                true,
                projectTitle,
                "Título del proyecto actualizado exitosamente"
        ));
    }

}

