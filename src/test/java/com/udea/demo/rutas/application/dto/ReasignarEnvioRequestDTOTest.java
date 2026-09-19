package com.udea.demo.rutas.application.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de validación del DTO ReasignarEnvioRequestDTO (HU-09).
 *
 * Cubre los campos pedidoId y nuevoConductorId con @NotNull.
 */
@DisplayName("ReasignarEnvioRequestDTO - validación (HU-09)")
class ReasignarEnvioRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Casos válidos")
    class CasosValidos {

        /** DTO con ambos campos presentes no produce violaciones. */
        @Test
        @DisplayName("pedidoId y nuevoConductorId presentes → sin violaciones")
        void camposPresentes_sinViolaciones() {
            // Arrange
            Long pedidoId          = 10L;
            Long nuevoConductorId  = 6L;
            ReasignarEnvioRequestDTO dto = new ReasignarEnvioRequestDTO(pedidoId, nuevoConductorId);

            // Act
            Set<ConstraintViolation<ReasignarEnvioRequestDTO>> violations = validator.validate(dto);

            // Assert — sin violaciones y los valores son los que se pasaron
            assertThat(violations).isEmpty();
            assertThat(dto.pedidoId()).isEqualTo(pedidoId);
            assertThat(dto.nuevoConductorId()).isEqualTo(nuevoConductorId);
        }
    }

    @Nested
    @DisplayName("Validación de pedidoId")
    class ValidacionPedidoId {

        /** pedidoId nulo → violación @NotNull. */
        @Test
        @DisplayName("pedidoId nulo produce violación @NotNull")
        void pedidoIdNulo_produceViolacion() {
            // Arrange
            ReasignarEnvioRequestDTO dto = new ReasignarEnvioRequestDTO(null, 6L);

            // Act
            Set<ConstraintViolation<ReasignarEnvioRequestDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("pedidoId"));
        }
    }

    @Nested
    @DisplayName("Validación de nuevoConductorId")
    class ValidacionNuevoConductorId {

        /** nuevoConductorId nulo → violación @NotNull. */
        @Test
        @DisplayName("nuevoConductorId nulo produce violación @NotNull")
        void nuevoConductorIdNulo_produceViolacion() {
            // Arrange
            ReasignarEnvioRequestDTO dto = new ReasignarEnvioRequestDTO(10L, null);

            // Act
            Set<ConstraintViolation<ReasignarEnvioRequestDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("nuevoConductorId"));
        }
    }

    @Nested
    @DisplayName("Inmutabilidad y métodos de record")
    class MetodosRecord {

        @Test
        @DisplayName("Dos instancias con los mismos valores son iguales y tienen el mismo hashCode")
        void equalsYHashCode() {
            // Arrange
            ReasignarEnvioRequestDTO dto1 = new ReasignarEnvioRequestDTO(10L, 6L);
            ReasignarEnvioRequestDTO dto2 = new ReasignarEnvioRequestDTO(10L, 6L);

            // Assert
            assertThat(dto1).isEqualTo(dto2);
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
        }

        @Test
        @DisplayName("Instancias con distintos nuevoConductorId NO son iguales")
        void instanciasDistintas_noIguales() {
            // Arrange
            ReasignarEnvioRequestDTO dto1 = new ReasignarEnvioRequestDTO(10L, 6L);
            ReasignarEnvioRequestDTO dto2 = new ReasignarEnvioRequestDTO(10L, 7L);

            // Assert — distinto nuevoConductorId → no iguales
            assertThat(dto1).isNotEqualTo(dto2);
        }
    }
}
