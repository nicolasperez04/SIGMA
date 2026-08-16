package com.SIGMA.USCO.academic.controller;


import com.SIGMA.USCO.academic.dto.ProgramDTO;
import com.SIGMA.USCO.academic.dto.response.ProgramResponse;
import com.SIGMA.USCO.academic.service.AcademicProgramService;
import com.SIGMA.USCO.common.security.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@Tag(name = "Programas Académicos", description = "Gestión de programas académicos: creación, actualización y consulta")
@RestController
@RequestMapping("/academic-programs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class AcademicProgramController {

    private final AcademicProgramService academicProgramService;

    @Operation(summary = "Crear programa académico", description = "Crea un nuevo programa académico en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Programa creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "409", description = "El nombre o código del programa ya existe"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_CREATE_PROGRAM + "')")
    public ResponseEntity<ProgramResponse> createProgram(@RequestBody @Valid ProgramDTO request) {
        ProgramDTO program = academicProgramService.createProgram(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ProgramResponse("Programa académico creado exitosamente.", program)
        );
    }

    @Operation(summary = "Obtener programa por ID", description = "Retorna la información de un programa académico específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Programa obtenido"),
            @ApiResponse(responseCode = "404", description = "Programa no encontrado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProgramDTO> getProgramById(@Parameter(description = "ID del programa") @PathVariable Long id) {
        ProgramDTO program = academicProgramService.getProgramById(id);
        return ResponseEntity.ok(program);
    }

    @Operation(summary = "Obtener todos los programas", description = "Retorna la lista completa de programas académicos")
    @ApiResponse(responseCode = "200", description = "Lista de programas obtenida")
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_PROGRAMS + "')")
    public ResponseEntity<List<ProgramDTO>> getAllPrograms() {
        return ResponseEntity.ok(academicProgramService.getAllPrograms());
    }

    @Operation(summary = "Obtener programas activos", description = "Retorna solo los programas académicos activos")
    @ApiResponse(responseCode = "200", description = "Lista de programas activos obtenida")
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProgramDTO>> getActivePrograms() {
        return ResponseEntity.ok(academicProgramService.getActivePrograms());
    }

    @Operation(summary = "Actualizar programa académico", description = "Actualiza la información de un programa académico existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Programa actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Programa no encontrado"),
            @ApiResponse(responseCode = "409", description = "El nombre o código del programa ya existe"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_UPDATE_PROGRAM + "')")
    public ResponseEntity<ProgramResponse> updateProgram(@Parameter(description = "ID del programa") @PathVariable Long id, @RequestBody @Valid ProgramDTO request) {
        ProgramDTO program = academicProgramService.updateProgram(id, request);
        return ResponseEntity.ok(
                new ProgramResponse("Programa académico actualizado exitosamente.", program)
        );
    }
}
