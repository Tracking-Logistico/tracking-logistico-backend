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

@DisplayName("RefreshTokenRequestDTO - validación y estructura")
class RefreshTokenRequestDTOTest {

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
        @DisplayName("Token no vacío no genera violaciones")
        void tokenValido_sinViolaciones() {
            // Arrange
            String token = "sample-refresh-token-123456";
            RefreshTokenRequestDTO dto = new RefreshTokenRequestDTO(token);

            // Act
            Set<ConstraintViolation<RefreshTokenRequestDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isEmpty();
            assertThat(dto.refreshToken()).isEqualTo(token);
        }
    }

    @Nested
    @DisplayName("Validación @NotBlank")
    class ValidacionNotBlank {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("RefreshToken nulo o en blanco produce violación @NotBlank")
        void refreshTokenEnBlanco_produceViolacion(String tokenInvalido) {
            // Arrange
            RefreshTokenRequestDTO dto = new RefreshTokenRequestDTO(tokenInvalido);

            // Act
            Set<ConstraintViolation<RefreshTokenRequestDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("refreshToken"));
        }
    }

    @Nested
    @DisplayName("Métodos de record")
    class MetodosRecord {

        @Test
        @DisplayName("Compara igualdad de instancias con el mismo token")
        void equalsYHashCode() {
            RefreshTokenRequestDTO dto1 = new RefreshTokenRequestDTO("token-xyz");
            RefreshTokenRequestDTO dto2 = new RefreshTokenRequestDTO("token-xyz");

            assertThat(dto1).isEqualTo(dto2);
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
            assertThat(dto1.toString()).contains("token-xyz");
        }
    }
}
