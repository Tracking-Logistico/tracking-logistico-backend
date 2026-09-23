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

@DisplayName("SolicitarRestablecimientoPasswordDTO - validación y estructura")
class SolicitarRestablecimientoPasswordDTOTest {

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
        @DisplayName("Email válido no produce violaciones de constraint")
        void emailValido_sinViolaciones() {

            String email = "cliente@correo.com";
            SolicitarRestablecimientoPasswordDTO dto = new SolicitarRestablecimientoPasswordDTO(email);

            Set<ConstraintViolation<SolicitarRestablecimientoPasswordDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
            assertThat(dto.email()).isEqualTo(email);
        }
    }

    @Nested
    @DisplayName("Validación de email")
    class ValidacionEmail {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Email en blanco produce violación @NotBlank")
        void emailEnBlanco_produceViolacion(String emailInvalido) {

            SolicitarRestablecimientoPasswordDTO dto = new SolicitarRestablecimientoPasswordDTO(emailInvalido);

            Set<ConstraintViolation<SolicitarRestablecimientoPasswordDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"sin-arroba", "correo@", "@servidor.com", "usuario con espacio@servidor.com"})
        @DisplayName("Email con formato inválido produce violación @Email")
        void emailFormatoInvalido_produceViolacion(String emailMalFormado) {

            SolicitarRestablecimientoPasswordDTO dto = new SolicitarRestablecimientoPasswordDTO(emailMalFormado);

            Set<ConstraintViolation<SolicitarRestablecimientoPasswordDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }
    }

    @Nested
    @DisplayName("Métodos de record")
    class MetodosRecord {

        @Test
        @DisplayName("Verifica equals, hashCode y toString del record")
        void equalsYHashCode() {
            SolicitarRestablecimientoPasswordDTO dto1 = new SolicitarRestablecimientoPasswordDTO("user@test.com");
            SolicitarRestablecimientoPasswordDTO dto2 = new SolicitarRestablecimientoPasswordDTO("user@test.com");

            assertThat(dto1).isEqualTo(dto2);
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
            assertThat(dto1.toString()).contains("user@test.com");
        }
    }
}
