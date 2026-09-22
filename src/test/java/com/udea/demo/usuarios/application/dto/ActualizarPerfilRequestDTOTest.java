package com.udea.demo.usuarios.application.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ActualizarPerfilRequestDTO - HU-01A validación")
class ActualizarPerfilRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("DTO con nombre válido no produce violaciones")
    void dtoValido_sinViolaciones() {
        ActualizarPerfilRequestDTO dto = new ActualizarPerfilRequestDTO("Ana María", "300999", "Calle 1");

        Set<ConstraintViolation<ActualizarPerfilRequestDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
        assertThat(dto.nombre()).isEqualTo("Ana María");
        assertThat(dto.telefono()).isEqualTo("300999");
        assertThat(dto.direccion()).isEqualTo("Calle 1");
    }

    @Test
    @DisplayName("Teléfono y dirección nulos son opcionales")
    void camposOpcionalesNulos_validos() {
        ActualizarPerfilRequestDTO dto = new ActualizarPerfilRequestDTO("Ana", null, null);

        assertThat(validator.validate(dto)).isEmpty();
        assertThat(dto.telefono()).isNull();
        assertThat(dto.direccion()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Nombre en blanco produce violación @NotBlank")
    void nombreEnBlanco(String nombre) {
        ActualizarPerfilRequestDTO dto = new ActualizarPerfilRequestDTO(nombre, "300", "Dir");

        assertThat(validator.validate(dto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("nombre"));
    }

    @Test
    @DisplayName("equals, hashCode y toString")
    void equalsHashCodeToString() {
        ActualizarPerfilRequestDTO a = new ActualizarPerfilRequestDTO("Ana", "1", "D");
        ActualizarPerfilRequestDTO b = new ActualizarPerfilRequestDTO("Ana", "1", "D");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("Ana");
    }
}
