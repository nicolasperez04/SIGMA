package com.SIGMA.USCO.Users.controller;

import com.SIGMA.USCO.Users.dto.request.AuthRequest;
import com.SIGMA.USCO.Users.dto.request.ResetPasswordRequest;
import com.SIGMA.USCO.Users.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticación", description = "Operaciones de registro, login y recuperación de contraseña")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {


    private final AuthService authService;

    @Operation(summary = "Registro de usuario", description = "Registra un nuevo usuario en el sistema.")
    @ApiResponse(responseCode = "200", description = "Usuario registrado correctamente")
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Login de usuario", description = "Autentica un usuario y retorna un token JWT.")
    @ApiResponse(responseCode = "200", description = "Login exitoso")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Solicitar recuperación de contraseña", description = "Envía un enlace de recuperación al correo institucional.")
    @ApiResponse(responseCode = "200", description = "Enlace enviado")
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody AuthRequest request) {
        authService.sendResetPasswordLink(request);
        return ResponseEntity.ok("Se envió un enlace de recuperación al correo institucional ingresado.");
    }

    @Operation(summary = "Restablecer contraseña", description = "Permite restablecer la contraseña usando el enlace enviado al correo.")
    @ApiResponse(responseCode = "200", description = "Contraseña actualizada")
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Tu contraseña fue actualizada correctamente.");
    }

    @Operation(summary = "Cerrar sesión", description = "Cierra la sesión del usuario y revoca el token JWT.")
    @ApiResponse(responseCode = "200", description = "Sesión cerrada correctamente")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return ResponseEntity.ok(authService.logout(token));
    }

}
