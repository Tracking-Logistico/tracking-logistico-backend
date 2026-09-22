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

@DisplayName("CambiarPasswordRequestDTO - HU-01B validación")
class CambiarPasswordRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("DTO con política de complejidad válida no produce violaciones")
    void dtoValido_sinViolaciones() {
        CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO(
                "Temporal1!", "NuevaClave1!", "NuevaClave1!");

        Set<ConstraintViolation<CambiarPasswordRequestDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
        assertThat(dto.passwordActual()).isEqualTo("Temporal1!");
        assertThat(dto.nuevaPassword()).isEqualTo("NuevaClave1!");
        assertThat(dto.confirmarPassword()).isEqualTo("NuevaClave1!");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("passwordActual en blanco produce violación")
    void passwordActualEnBlanco(String actual) {
        CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO(actual, "NuevaClave1!", "NuevaClave1!");

        assertThat(validator.validate(dto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("passwordActual"));
    }

    @Test
    @DisplayName("nuevaPassword corta produce violación @Size")
    void nuevaPasswordCorta() {
        CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO("Actual1!", "Ab1!", "Ab1!");

        assertThat(validator.validate(dto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("nuevaPassword"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sinmayus1!", "SINMINUS1!", "SinNumero!!", "SinEspecial1"})
    @DisplayName("nuevaPassword sin complejidad produce violación @Pattern")
    void nuevaPasswordSinComplejidad(String nueva) {
        CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO("Actual1!", nueva, nueva);

        assertThat(validator.validate(dto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("nuevaPassword"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("confirmarPassword en blanco produce violación")
    void confirmarEnBlanco(String confirmar) {
        CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO("Actual1!", "NuevaClave1!", confirmar);

        assertThat(validator.validate(dto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("confirmarPassword"));
    }

    @Test
    @DisplayName("equals, hashCode y toString")
    void equalsHashCodeToString() {
        CambiarPasswordRequestDTO a = new CambiarPasswordRequestDTO("A1!aaaaa", "B2!bbbbb", "B2!bbbbb");
        CambiarPasswordRequestDTO b = new CambiarPasswordRequestDTO("A1!aaaaa", "B2!bbbbb", "B2!bbbbb");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("A1!aaaaa");
    }
}
