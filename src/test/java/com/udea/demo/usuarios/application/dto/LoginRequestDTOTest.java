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

@DisplayName("LoginRequestDTO - validación y estructura")
class LoginRequestDTOTest {

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
        @DisplayName("LoginRequestDTO con credenciales válidas no produce violaciones")
        void credencialesValidas_sinViolaciones() {

            String email = "operador@tracking.com";
            String password = "Password123!";
            LoginRequestDTO dto = new LoginRequestDTO(email, password);

            Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
            assertThat(dto.email()).isEqualTo(email);
            assertThat(dto.password()).isEqualTo(password);
        }
    }

    @Nested
    @DisplayName("Validación de campo email")
    class ValidacionEmail {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Email nulo o en blanco produce violación @NotBlank")
        void emailEnBlanco_produceViolacion(String emailInvalido) {

            LoginRequestDTO dto = new LoginRequestDTO(emailInvalido, "Password123!");

            Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"correo-sin-arroba", "usuario@", "@dominio.com", "usuario con espacio@dominio.com"})
        @DisplayName("Email con formato incorrecto produce violación @Email")
        void emailFormatoIncorrecto_produceViolacion(String emailMalFormado) {

            LoginRequestDTO dto = new LoginRequestDTO(emailMalFormado, "Password123!");

            Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }
    }

    @Nested
    @DisplayName("Validación de campo password")
    class ValidacionPassword {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Password nula o en blanco produce violación @NotBlank")
        void passwordEnBlanco_produceViolacion(String passwordInvalida) {

            LoginRequestDTO dto = new LoginRequestDTO("usuario@tracking.com", passwordInvalida);

            Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }
    }

    @Nested
    @DisplayName("Inmutabilidad y métodos de record")
    class MetodosRecord {

        @Test
        @DisplayName("Dos instancias con los mismos valores son iguales y tienen mismo hashCode")
        void equalsYHashCode() {
            LoginRequestDTO dto1 = new LoginRequestDTO("test@mail.com", "Secret123!");
            LoginRequestDTO dto2 = new LoginRequestDTO("test@mail.com", "Secret123!");

            assertThat(dto1).isEqualTo(dto2);
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
            assertThat(dto1.toString()).contains("test@mail.com");
        }
    }
}
