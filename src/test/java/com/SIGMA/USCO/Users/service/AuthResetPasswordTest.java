package com.SIGMA.USCO.Users.service;

import com.SIGMA.USCO.Users.dto.request.ResetPasswordRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Regresión T0.9: validación de la nueva contraseña en el restablecimiento")
class AuthResetPasswordTest {

    /*
     * Decisión de enfoque (T1.8):
     * AuthService.resetPassword (AuthService.java:154-168) NO valida la contraseña
     * manualmente (no lanza ValidationException por contraseña corta/nula): solo valida
     * el token y persiste el nuevo password codificado. La validación de la contraseña
     * vive en las constraints del DTO ResetPasswordRequest (@NotBlank + @Size(min=6,max=60))
     * activadas por @Valid en AuthController:56 (POST /auth/reset-password).
     * Por eso este test usa el Validator de Jakarta (hibernate-validator, incluido vía
     * spring-boot-starter-validation) y NO Mockito sobre AuthService.
     */
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("Contraseña corta (menos de 6 caracteres) viola @Size")
    void shortPasswordFailsSize() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("token-valido")
                .newPassword("123")
                .build();

        Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("newPassword");
    }

    @Test
    @DisplayName("Contraseña nula viola @NotBlank")
    void nullPasswordFailsNotBlank() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("token-valido")
                .newPassword(null)
                .build();

        Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("newPassword");
    }

    @Test
    @DisplayName("Contraseña válida (6 o más caracteres) no genera violaciones")
    void validPasswordPasses() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("token-valido")
                .newPassword("123456")
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }
}