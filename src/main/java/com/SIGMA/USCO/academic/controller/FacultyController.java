package com.SIGMA.USCO.academic.controller;

import com.SIGMA.USCO.academic.dto.FacultyDTO;
import com.SIGMA.USCO.academic.dto.response.FacultyListResponse;
import com.SIGMA.USCO.academic.dto.response.FacultyResponse;
import com.SIGMA.USCO.academic.dto.response.MessageResponse;
import com.SIGMA.USCO.academic.service.FacultyService;
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

@Tag(name = "Facultades", description = "Gestión de facultades: creación, actualización, consulta y desactivación")
@RestController
@RequiredArgsConstructor
@RequestMapping("/faculties")
@SecurityRequirement(name = "bearer-jwt")
public class FacultyController {

    private final FacultyService facultyService;

    @Operation(summary = "Crear facultad", description = "Crea una nueva facultad en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Facultad creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "409", description = "El código o nombre de la facultad ya existe"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_CREATE_FACULTY + "')")
    public ResponseEntity<FacultyResponse> createFaculty(@RequestBody @Valid FacultyDTO request){
        FacultyDTO faculty = facultyService.createFaculty(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new FacultyResponse("Facultad creada exitosamente.", faculty)
        );
    }

    @Operation(summary = "Obtener todas las facultades", description = "Retorna la lista completa de todas las facultades del sistema")
    @ApiResponse(responseCode = "200", description = "Lista de facultades obtenida")
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_FACULTIES + "')")
    public ResponseEntity<FacultyListResponse> getAllFaculties(){
        return ResponseEntity.ok(
                new FacultyListResponse(facultyService.getAllFaculties())
        );

    }

    @Operation(summary = "Obtener facultades activas", description = "Retorna solo las facultades que están activas en el sistema")
    @ApiResponse(responseCode = "200", description = "Lista de facultades activas obtenida")
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FacultyListResponse> getActiveFaculties() {
        return ResponseEntity.ok(
                new FacultyListResponse(facultyService.getActiveFaculties())
        );
    }

    @Operation(summary = "Actualizar facultad", description = "Actualiza la información de una facultad existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Facultad actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Facultad no encontrada"),
            @ApiResponse(responseCode = "409", description = "El código o nombre de la facultad ya existe"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_UPDATE_FACULTY + "')")
    public ResponseEntity<FacultyResponse> updateFaculty(@Parameter(description = "ID de la facultad") @PathVariable Long id, @RequestBody @Valid FacultyDTO request) {
        FacultyDTO updatedFaculty = facultyService.updateFaculty(id, request);
        return ResponseEntity.ok(
                new FacultyResponse("Facultad actualizada exitosamente.", updatedFaculty)
        );
    }

    @Operation(summary = "Desactivar facultad", description = "Desactiva una facultad específica del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Facultad desactivada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Facultad no encontrada"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PutMapping("/desactive/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_DELETE_FACULTY + "')")
    public ResponseEntity<MessageResponse> desactivate(@Parameter(description = "ID de la facultad") @PathVariable Long id) {
        facultyService.deactivateFaculty(id);
        return ResponseEntity.ok(
                new MessageResponse("Facultad desactivada exitosamente.")
        );
    }

    @Operation(summary = "Obtener detalle de facultad", description = "Retorna la información detallada de una facultad incluyendo sus programas académicos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalle de facultad obtenido"),
            @ApiResponse(responseCode = "404", description = "Facultad no encontrada"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_FACULTIES + "')")
    public ResponseEntity<FacultyDTO> getFacultyDetail(@Parameter(description = "ID de la facultad") @PathVariable Long id) {
        return ResponseEntity.ok(facultyService.getFacultyDetail(id));
    }

}
