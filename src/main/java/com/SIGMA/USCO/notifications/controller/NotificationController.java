package com.SIGMA.USCO.notifications.controller;

import com.SIGMA.USCO.common.web.OperationResultResponse;
import com.SIGMA.USCO.notifications.dto.NotificationResponse;
import com.SIGMA.USCO.notifications.dto.UnreadCountResponse;
import com.SIGMA.USCO.notifications.service.NotificationService;
import com.SIGMA.USCO.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Notificaciones", description = "Operaciones sobre notificaciones del usuario")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Obtener notificaciones", description = "Obtiene las notificaciones del usuario autenticado (paginación opcional)")
    @ApiResponse(responseCode = "200", description = "Lista de notificaciones")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @Parameter(description = "Número de página (0-based), requiere 'size'") @RequestParam(required = false) Integer page,
            @Parameter(description = "Tamaño de página, requiere 'page'") @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(notificationService.getMyNotifications(SecurityUtils.getCurrentUser(), page, size));
    }

    @Operation(summary = "Obtener cantidad de no leídas", description = "Obtiene el número de notificaciones no leídas")
    @ApiResponse(responseCode = "200", description = "Cantidad de no leídas")
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadCount(SecurityUtils.getCurrentUser()));
    }

    @Operation(summary = "Detalle de notificación", description = "Obtiene el detalle de una notificación específica")
    @ApiResponse(responseCode = "200", description = "Detalle de la notificación")
    @GetMapping("/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationResponse> getNotificationDetail(@Parameter(description = "ID de la notificación") @PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.getNotificationDetail(SecurityUtils.getCurrentUser(), notificationId));
    }

    @Operation(summary = "Marcar como leída", description = "Marca una notificación como leída")
    @ApiResponse(responseCode = "200", description = "Notificación marcada como leída")
    @PutMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OperationResultResponse> markAsRead(@Parameter(description = "ID de la notificación") @PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(SecurityUtils.getCurrentUser(), notificationId));
    }
}
