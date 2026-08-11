package com.SIGMA.USCO.Modalities.Controller;

import com.SIGMA.USCO.Modalities.dto.groups.EligibleStudentDTO;
import com.SIGMA.USCO.Modalities.dto.groups.InviteStudentRequest;
import com.SIGMA.USCO.Modalities.service.ModalityGroupService;
import com.SIGMA.USCO.documents.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Modalidades Grupales", description = "Gestión de modalidades de grado grupales: invitaciones, aceptaciones y rechazos")
@RestController
@RequestMapping("/modality-groups")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class ModalityGroupController {

    private final ModalityGroupService modalityGroupService;
    private final DocumentService documentService;

    @Operation(summary = "Iniciar modalidad grupal", description = "El estudiante inicia una modalidad de grado grupal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modalidad grupal iniciada exitosamente"),
            @ApiResponse(responseCode = "400", description = "No se puede iniciar esta modalidad"),
            @ApiResponse(responseCode = "404", description = "Modalidad no encontrada"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping("/{modalityId}/start-group")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> startGroupModality(@Parameter(description = "ID de la modalidad de grado") @PathVariable Long modalityId) {
        return ResponseEntity.ok(modalityGroupService.startStudentModalityGroup(modalityId));
    }

    @Operation(summary = "Obtener estudiantes elegibles", description = "Obtiene la lista de estudiantes elegibles para ser invitados a la modalidad grupal del usuario actual")
    @ApiResponse(responseCode = "200", description = "Lista de estudiantes elegibles obtenida")
    @GetMapping("/eligible-students")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<EligibleStudentDTO>> getEligibleStudents(@Parameter(description = "Filtro por nombre de estudiante (opcional)") @RequestParam(required = false) String nameFilter) {
        return ResponseEntity.ok(modalityGroupService.getEligibleStudentsForInvitation(nameFilter));
    }

    @Operation(summary = "Invitar estudiante", description = "El estudiante invita a otro estudiante para que se una a su modalidad grupal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitación enviada exitosamente"),
            @ApiResponse(responseCode = "400", description = "No se puede invitar a este estudiante"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping("/invite")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> inviteStudent(@Valid @RequestBody InviteStudentRequest request) {
        return ResponseEntity.ok(modalityGroupService.inviteStudentToModality(request.getStudentModalityId(), request.getInviteeId()));
    }

    @Operation(summary = "Aceptar invitación", description = "El estudiante acepta una invitación para unirse a una modalidad grupal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitación aceptada exitosamente"),
            @ApiResponse(responseCode = "400", description = "No se puede aceptar esta invitación"),
            @ApiResponse(responseCode = "404", description = "Invitación no encontrada"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping("/invitations/{invitationId}/accept")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> acceptInvitation(@Parameter(description = "ID de la invitación") @PathVariable Long invitationId) {
        return ResponseEntity.ok(modalityGroupService.acceptInvitation(invitationId));
    }

    @Operation(summary = "Rechazar invitación", description = "El estudiante rechaza una invitación para unirse a una modalidad grupal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitación rechazada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Invitación no encontrada"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping("/invitations/{invitationId}/reject")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> rejectInvitation(@Parameter(description = "ID de la invitación") @PathVariable Long invitationId) {
        return ResponseEntity.ok(modalityGroupService.rejectInvitation(invitationId));
    }


}
