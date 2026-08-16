package com.SIGMA.USCO.Users.service;

import com.SIGMA.USCO.Users.entity.*;
import com.SIGMA.USCO.Users.entity.enums.Status;
import com.SIGMA.USCO.Users.dto.request.AuthRequest;
import com.SIGMA.USCO.Users.dto.request.ResetPasswordRequest;
import com.SIGMA.USCO.Users.repository.*;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.common.exception.UnauthorizedException;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.common.security.Roles;
import com.SIGMA.USCO.config.EmailService;
import com.SIGMA.USCO.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    // ponytail: blacklist expiry = expiración del JWT (jwt.expiration=18000000ms=5h)
    private static final Duration BLACKLIST_EXPIRATION = Duration.ofHours(5);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final RoleRepository roleRepository;
    private final BlackListedTokenRepository blackListedTokenRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    public ResponseEntity<?> register(AuthRequest request) {

        if (request.getName().isEmpty() ||
                request.getLastName().isEmpty() ||
                request.getEmail().isEmpty() ||
                request.getPassword().isEmpty()) {

            return ResponseEntity.badRequest()
                    .body("Todos los campos son obligatorios (nombre, apellido, correo y contraseña)");
        }

        String email = request.getEmail().trim().toLowerCase();

        if (!email.endsWith("@usco.edu.co")) {
            return ResponseEntity.badRequest()
                    .body("El correo debe ser institucional con dominio @usco.edu.co");
        }


        String emailPattern = "^u\\d+@usco\\.edu\\.co$";
        if (!email.matches(emailPattern)) {
            return ResponseEntity.badRequest()
                    .body("El formato del correo institucional es inválido. El formato esperado es: u[NUMEROS]@usco.edu.co (ejemplo: u20221204357@usco.edu.co)");
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest()
                    .body("Este correo ya está en uso");
        }

        Role studentRole = roleRepository.findByName(Roles.ROLE_STUDENT)
                .orElseThrow(() -> new NotFoundException("El rol STUDENT no existe en la base de datos."));

        User user = User.builder()
                .name(request.getName())
                .lastName(request.getLastName())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(studentRole))
                .status(Status.ACTIVE)
                .creationDate(LocalDateTime.now())
                .lastUpdateDate(LocalDateTime.now())
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(token);
    }


    public ResponseEntity<?> login(AuthRequest request){
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            User user = (User) auth.getPrincipal();

            String token = jwtService.generateToken(user);

            return ResponseEntity.ok(token);

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Credenciales incorrectas. Por favor, verifica tu correo institucional y contraseña.");
        }
    }

    public void sendResetPasswordLink(AuthRequest request){
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {

            String token = UUID.randomUUID().toString();

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusMinutes(10))
                    .used(false)
                    .build();
            tokenRepository.save(resetToken);

            String resetLink = frontendUrl + "/reset-password";


            String subject = "Restablecimiento de contraseña - SIGMA USCO";
            String message = """
                    Hola %s,

                    Recibimos una solicitud para restablecer tu contraseña.

                    Haz clic en el siguiente enlace para continuar:
                    %s
                    Tu token de restablecimiento de contraseña es: %s

                    Este enlace expirará en 10 minutos.

                    Si no fuiste tú, ignora este mensaje.

                    Equipo SIGMA
                    """.formatted(user.getName(), resetLink, token);

            emailService.sendEmail(user.getEmail(), subject, message);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request){
        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsedFalse(request.getToken())
                .orElseThrow(() -> new UnauthorizedException("El token es inválido o ya ha sido utilizado."));

        if (resetToken.isExpired()){
            throw new UnauthorizedException("El token ha expirado. Por favor, solicita un nuevo restablecimiento de contraseña.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    public String logout(String token){
        if (token == null || token.isEmpty()) {
            throw new ValidationException("Token no proporcionado.");
        }

        if (!blackListedTokenRepository.existsByToken(token)) {
            LocalDateTime expiresAt = jwtService.getExpirationDate(token) != null
                    ? jwtService.getExpirationDate(token).toInstant()
                            .atZone(ZoneId.of("America/Bogota")).toLocalDateTime()
                    : LocalDateTime.now().plus(BLACKLIST_EXPIRATION);
            BlackListedToken blackListedToken = BlackListedToken.builder()
                    .token(token)
                    .expiresAt(expiresAt)
                    .build();
            blackListedTokenRepository.save(blackListedToken);
            return "Cierre de sesión exitoso.";
        } else {
            throw new ValidationException("El token ya ha sido invalidado.");
        }
    }
}
