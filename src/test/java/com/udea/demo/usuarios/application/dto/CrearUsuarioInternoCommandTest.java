package com.udea.demo.usuarios.application.dto;

import com.udea.demo.usuarios.domain.model.Rol;
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

@DisplayName("CrearUsuarioInternoCommand - HU-01B validación")
class CrearUsuarioInternoCommandTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private CrearUsuarioInternoCommand operadorValido() {
        return new CrearUsuarioInternoCommand(
                "Laura Ops", "laura@tracking.com", "3001112233",
                Rol.OPERADOR, "Calle 1", null, "OP-001");
    }

    @Nested
    @DisplayName("Casos válidos")
    class CasosValidos {

        @Test
        @DisplayName("Operador válido no produce violaciones")
        void operadorValido_sinViolaciones() {
            CrearUsuarioInternoCommand cmd = operadorValido();

            Set<ConstraintViolation<CrearUsuarioInternoCommand>> violations = validator.validate(cmd);

            assertThat(violations).isEmpty();
            assertThat(cmd.rol()).isEqualTo(Rol.OPERADOR);
            assertThat(cmd.codigoEmpleado()).isEqualTo("OP-001");
        }

        @Test
        @DisplayName("Conductor válido no produce violaciones")
        void conductorValido_sinViolaciones() {
            CrearUsuarioInternoCommand cmd = new CrearUsuarioInternoCommand(
                    "Carlos", "carlos@tracking.com", "3002223344",
                    Rol.CONDUCTOR, "Dir", "LIC-1", null);

            assertThat(validator.validate(cmd)).isEmpty();
            assertThat(cmd.licencia()).isEqualTo("LIC-1");
        }

        @Test
        @DisplayName("Teléfono nulo es opcional a nivel Bean Validation")
        void telefonoNulo_valido() {
            CrearUsuarioInternoCommand cmd = new CrearUsuarioInternoCommand(
                    "Laura", "laura@tracking.com", null,
                    Rol.OPERADOR, "Dir", null, "OP-001");

            assertThat(validator.validate(cmd)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Campos inválidos")
    class CamposInvalidos {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Nombre en blanco produce violación")
        void nombreEnBlanco(String nombre) {
            CrearUsuarioInternoCommand cmd = new CrearUsuarioInternoCommand(
                    nombre, "laura@tracking.com", "3001112233",
                    Rol.OPERADOR, "Dir", null, "OP-001");

            assertThat(validator.validate(cmd))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("nombre"));
        }

        @Test
        @DisplayName("Email inválido produce violación")
        void emailInvalido() {
            CrearUsuarioInternoCommand cmd = new CrearUsuarioInternoCommand(
                    "Laura", "sin-arroba", "3001112233",
                    Rol.OPERADOR, "Dir", null, "OP-001");

            assertThat(validator.validate(cmd))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("Teléfono con formato inválido produce violación")
        void telefonoInvalido() {
            CrearUsuarioInternoCommand cmd = new CrearUsuarioInternoCommand(
                    "Laura", "laura@tracking.com", "abc",
                    Rol.OPERADOR, "Dir", null, "OP-001");

            assertThat(validator.validate(cmd))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("telefono"));
        }

        @Test
        @DisplayName("Rol nulo produce violación")
        void rolNulo() {
            CrearUsuarioInternoCommand cmd = new CrearUsuarioInternoCommand(
                    "Laura", "laura@tracking.com", "3001112233",
                    null, "Dir", null, "OP-001");

            assertThat(validator.validate(cmd))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("rol"));
        }

        @Test
        @DisplayName("Nombre mayor a 100 caracteres produce violación")
        void nombreDemasiadoLargo() {
            String largo = "A".repeat(101);
            CrearUsuarioInternoCommand cmd = new CrearUsuarioInternoCommand(
                    largo, "laura@tracking.com", "3001112233",
                    Rol.OPERADOR, "Dir", null, "OP-001");

            assertThat(validator.validate(cmd))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("nombre"));
        }
    }

    @Nested
    @DisplayName("Métodos de record")
    class MetodosRecord {

        @Test
        @DisplayName("equals, hashCode y toString")
        void equalsHashCodeToString() {
            CrearUsuarioInternoCommand a = operadorValido();
            CrearUsuarioInternoCommand b = operadorValido();

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
            assertThat(a.toString()).contains("laura@tracking.com");
        }
    }
}
