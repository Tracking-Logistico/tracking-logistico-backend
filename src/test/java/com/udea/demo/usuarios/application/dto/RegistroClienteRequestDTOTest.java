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

@DisplayName("RegistroClienteRequestDTO - HU-01A validación")
class RegistroClienteRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private RegistroClienteRequestDTO valido() {
        return new RegistroClienteRequestDTO(
                "Ana Pérez", "ana@tracking.com", "Password123!", "Password123!",
                "3001234567", "Calle 10", "Medellín", true, "T&C-v1.0");
    }

    @Nested
    @DisplayName("Casos válidos")
    class CasosValidos {

        @Test
        @DisplayName("DTO completo y válido no produce violaciones")
        void dtoValido_sinViolaciones() {
            RegistroClienteRequestDTO dto = valido();

            Set<ConstraintViolation<RegistroClienteRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
            assertThat(dto.nombre()).isEqualTo("Ana Pérez");
            assertThat(dto.email()).isEqualTo("ana@tracking.com");
            assertThat(dto.aceptoTerminos()).isTrue();
            assertThat(dto.versionTerminos()).isEqualTo("T&C-v1.0");
        }
    }

    @Nested
    @DisplayName("Campos obligatorios")
    class CamposObligatorios {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Nombre en blanco produce violación")
        void nombreEnBlanco(String nombre) {
            RegistroClienteRequestDTO dto = new RegistroClienteRequestDTO(
                    nombre, "ana@tracking.com", "Password123!", "Password123!",
                    null, null, null, true, "v1");

            assertThat(validator.validate(dto))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("nombre"));
        }

        @Test
        @DisplayName("Email con formato inválido produce violación")
        void emailInvalido() {
            RegistroClienteRequestDTO dto = new RegistroClienteRequestDTO(
                    "Ana", "no-es-email", "Password123!", "Password123!",
                    null, null, null, true, "v1");

            assertThat(validator.validate(dto))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("Password menor a 8 caracteres produce violación")
        void passwordCorta() {
            RegistroClienteRequestDTO dto = new RegistroClienteRequestDTO(
                    "Ana", "ana@tracking.com", "Ab1!", "Ab1!",
                    null, null, null, true, "v1");

            assertThat(validator.validate(dto))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"password123!", "PASSWORD123!", "Password!!!!", "Passwordaaaa"})
        @DisplayName("Password sin complejidad produce violación")
        void passwordSinComplejidad(String password) {
            RegistroClienteRequestDTO dto = new RegistroClienteRequestDTO(
                    "Ana", "ana@tracking.com", password, password,
                    null, null, null, true, "v1");

            assertThat(validator.validate(dto))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("aceptoTerminos=false produce violación @AssertTrue")
        void noAceptaTerminos() {
            RegistroClienteRequestDTO dto = new RegistroClienteRequestDTO(
                    "Ana", "ana@tracking.com", "Password123!", "Password123!",
                    null, null, null, false, "v1");

            assertThat(validator.validate(dto))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("aceptoTerminos"));
        }

        @Test
        @DisplayName("aceptoTerminos nulo produce violación")
        void terminosNulos() {
            RegistroClienteRequestDTO dto = new RegistroClienteRequestDTO(
                    "Ana", "ana@tracking.com", "Password123!", "Password123!",
                    null, null, null, null, "v1");

            assertThat(validator.validate(dto))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("aceptoTerminos"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("versión de términos en blanco produce violación")
        void versionTerminosEnBlanco(String version) {
            RegistroClienteRequestDTO dto = new RegistroClienteRequestDTO(
                    "Ana", "ana@tracking.com", "Password123!", "Password123!",
                    null, null, null, true, version);

            assertThat(validator.validate(dto))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("versionTerminos"));
        }
    }

    @Nested
    @DisplayName("Métodos de record")
    class MetodosRecord {

        @Test
        @DisplayName("equals, hashCode y toString")
        void equalsHashCodeToString() {
            RegistroClienteRequestDTO a = valido();
            RegistroClienteRequestDTO b = valido();

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
            assertThat(a.toString()).contains("ana@tracking.com");
        }
    }
}
