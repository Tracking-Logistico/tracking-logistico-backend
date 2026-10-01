package com.udea.demo.usuarios.application.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RestablecerPasswordDTO - validación y estructura")
class RestablecerPasswordDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Casos válidos")
    class CasosValidos {

        @Test
        @DisplayName("Instancia con todos los campos no vacíos pasa la validación")
        void camposValidos_sinViolaciones() {

            String token = "reset-token-xyz";
            String nuevaPassword = "PasswordSegura123!";
            String confirmarPassword = "PasswordSegura123!";
            RestablecerPasswordDTO dto = new RestablecerPasswordDTO(token, nuevaPassword, confirmarPassword);

            Set<ConstraintViolation<RestablecerPasswordDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
            assertThat(dto.token()).isEqualTo(token);
            assertThat(dto.nuevaPassword()).isEqualTo(nuevaPassword);
            assertThat(dto.confirmarPassword()).isEqualTo(confirmarPassword);
        }
    }

    @Nested
    @DisplayName("Validación de campos @NotBlank")
    class ValidacionCamposObligatorios {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Token en blanco produce violación @NotBlank")
        void tokenEnBlanco_produceViolacion(String tokenInvalido) {
            RestablecerPasswordDTO dto = new RestablecerPasswordDTO(tokenInvalido, "Password123!", "Password123!");
            Set<ConstraintViolation<RestablecerPasswordDTO>> violations = validator.validate(dto);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("token"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("nuevaPassword en blanco produce violación @NotBlank")
        void nuevaPasswordEnBlanco_produceViolacion(String passwordInvalida) {
            RestablecerPasswordDTO dto = new RestablecerPasswordDTO("token-123", passwordInvalida, "Password123!");
            Set<ConstraintViolation<RestablecerPasswordDTO>> violations = validator.validate(dto);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("nuevaPassword"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("confirmarPassword en blanco produce violación @NotBlank")
        void confirmarPasswordEnBlanco_produceViolacion(String confirmarInvalido) {
            RestablecerPasswordDTO dto = new RestablecerPasswordDTO("token-123", "Password123!", confirmarInvalido);
            Set<ConstraintViolation<RestablecerPasswordDTO>> violations = validator.validate(dto);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("confirmarPassword"));
        }
    }

    @Nested
    @DisplayName("Métodos de record")
    class MetodosRecord {

        @Test
        @DisplayName("Verifica igualdad, hashCode y toString")
        void equalsYHashCode() {
            RestablecerPasswordDTO dto1 = new RestablecerPasswordDTO("t1", "p1", "p1");
            RestablecerPasswordDTO dto2 = new RestablecerPasswordDTO("t1", "p1", "p1");

            assertThat(dto1).isEqualTo(dto2);
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
            assertThat(dto1.toString()).contains("t1");
        }
    }
}
