package com.SIGMA.USCO.academic.controller;

import com.SIGMA.USCO.academic.dto.ProgramDegreeModalityDTO;
import com.SIGMA.USCO.academic.dto.ProgramDegreeModalityRequest;
import com.SIGMA.USCO.academic.dto.response.ProgramDegreeModalityDataResponse;
import com.SIGMA.USCO.academic.dto.response.ProgramDegreeModalityListResponse;
import com.SIGMA.USCO.academic.dto.response.ProgramDegreeModalityResponse;
import com.SIGMA.USCO.academic.dto.response.SuccessMessageResponse;
import com.SIGMA.USCO.academic.service.ProgramDegreeModalityService;
import com.SIGMA.USCO.common.security.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Configuración de Modalidades", description = "Gestión de configuración de modalidades de grado para programas académicos")
@RestController
@RequestMapping("/program-degree-modalities")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class ProgramDegreeModalityController {

    private final ProgramDegreeModalityService programDegreeModalityService;

    @Operation(summary = "Crear configuración de modalidad", description = "Crea la configuración de una modalidad de grado para un programa académico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Configuración creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "409", description = "La modalidad no pertenece a la facultad del programa o ya está configurada"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_CREATE_PROGRAM_DEGREE_MODALITY + "')")
    public ResponseEntity<ProgramDegreeModalityResponse> createProgramDegreeModality(@RequestBody @Valid ProgramDegreeModalityRequest request) {
        ProgramDegreeModalityDTO dto = programDegreeModalityService.createProgramModality(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ProgramDegreeModalityResponse(true, "Modalidad de grado del programa creada exitosamente.", dto)
        );
    }

    @Operation(summary = "Obtener configuración por ID", description = "Retorna la configuración de una modalidad específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuración obtenida"),
            @ApiResponse(responseCode = "400", description = "ID inválido"),
            @ApiResponse(responseCode = "404", description = "Configuración no encontrada")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + Permissions.PERM_VIEW_PROGRAM_DEGREE_MODALITY + "', '" + Permissions.PERM_CREATE_PROGRAM_DEGREE_MODALITY + "', '" + Permissions.PERM_UPDATE_PROGRAM_DEGREE_MODALITY + "')")
    public ResponseEntity<ProgramDegreeModalityDataResponse> getProgramModalityById(@Parameter(description = "ID de la configuración") @PathVariable Long id) {
        ProgramDegreeModalityDTO dto = programDegreeModalityService.getProgramModalityById(id);
        return ResponseEntity.ok(
                new ProgramDegreeModalityDataResponse(true, dto)
        );
    }

    @Operation(summary = "Obtener todas las configuraciones", description = "Retorna todas las configuraciones de modalidades con filtros opcionales")
    @ApiResponse(responseCode = "200", description = "Lista de configuraciones obtenida")
    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('" + Permissions.PERM_VIEW_PROGRAM_DEGREE_MODALITY + "', '" + Permissions.PERM_CREATE_PROGRAM_DEGREE_MODALITY + "')")
    public ResponseEntity<ProgramDegreeModalityListResponse> getAllProgramModalities(
            @Parameter(description = "Filtrar por estado (true=activo, false=inactivo)") @RequestParam(required = false) Boolean active,
            @Parameter(description = "ID de tipo de modalidad") @RequestParam(required = false) Long degreeModalityId,
            @Parameter(description = "ID de facultad") @RequestParam(required = false) Long facultyId,
            @Parameter(description = "ID de programa académico") @RequestParam(required = false) Long academicProgramId
    ) {
        List<ProgramDegreeModalityDTO> list = programDegreeModalityService.getAllProgramModalities(
                active, degreeModalityId, facultyId, academicProgramId
        );
        return ResponseEntity.ok(
                new ProgramDegreeModalityListResponse(true, list, list.size())
        );
    }

    @Operation(summary = "Actualizar configuración de modalidad", description = "Actualiza la configuración de una modalidad de grado existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuración actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Configuración no encontrada"),
            @ApiResponse(responseCode = "409", description = "La modalidad no pertenece a la facultad del programa o ya está configurada")
    })
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('" + Permissions.PERM_VIEW_PROGRAM_DEGREE_MODALITY + "', '" + Permissions.PERM_CREATE_PROGRAM_DEGREE_MODALITY + "')")
    public ResponseEntity<ProgramDegreeModalityResponse> updateProgramModality(@Parameter(description = "ID de la configuración") @PathVariable Long id, @RequestBody @Valid ProgramDegreeModalityRequest request) {
        ProgramDegreeModalityDTO dto = programDegreeModalityService.updateProgramModality(id, request);
        return ResponseEntity.ok(
                new ProgramDegreeModalityResponse(true, "Configuración de modalidad actualizada exitosamente.", dto)
        );
    }

    @Operation(summary = "Desactivar configuración de modalidad", description = "Desactiva una configuración de modalidad de grado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuración desactivada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Configuración no encontrada")
    })
    @PutMapping("/desactivate/{id}")
    @PreAuthorize("hasAnyAuthority('" + Permissions.PERM_VIEW_PROGRAM_DEGREE_MODALITY + "', '" + Permissions.PERM_CREATE_PROGRAM_DEGREE_MODALITY + "')")
    public ResponseEntity<SuccessMessageResponse> deactivateProgramModality(@Parameter(description = "ID de la configuración") @PathVariable Long id) {
        programDegreeModalityService.deactivateProgramModality(id);
        return ResponseEntity.ok(
                new SuccessMessageResponse(true, "Configuración de modalidad desactivada exitosamente.")
        );
    }

    @Operation(summary = "Activar configuración de modalidad", description = "Activa una configuración de modalidad de grado previamente desactivada")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuración activada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Configuración no encontrada")
    })
    @PutMapping("/activate/{id}")
    @PreAuthorize("hasAnyAuthority('" + Permissions.PERM_VIEW_PROGRAM_DEGREE_MODALITY + "', '" + Permissions.PERM_CREATE_PROGRAM_DEGREE_MODALITY + "')")
    public ResponseEntity<SuccessMessageResponse> activateProgramModality(@Parameter(description = "ID de la configuración") @PathVariable Long id) {
        programDegreeModalityService.activateProgramModality(id);
        return ResponseEntity.ok(
                new SuccessMessageResponse(true, "Configuración de modalidad activada exitosamente.")
        );
    }


}
