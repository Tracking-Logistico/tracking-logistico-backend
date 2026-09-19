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
 * Pruebas de validación del DTO AsignarEnvioRequestDTO (HU-09).
 *
 * Cubre los campos pedidoId y conductorId con @NotNull.
 */
@DisplayName("AsignarEnvioRequestDTO - validación (HU-09)")
class AsignarEnvioRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Casos válidos")
    class CasosValidos {

        /** DTO con ambos campos correctos no produce violaciones. */
        @Test
        @DisplayName("pedidoId y conductorId presentes → sin violaciones")
        void camposPresentes_sinViolaciones() {
            // Arrange
            Long pedidoId    = 10L;
            Long conductorId = 5L;
            AsignarEnvioRequestDTO dto = new AsignarEnvioRequestDTO(pedidoId, conductorId);

            // Act
            Set<ConstraintViolation<AsignarEnvioRequestDTO>> violations = validator.validate(dto);

            // Assert — no hay violaciones y los valores son los que se pasaron
            assertThat(violations).isEmpty();
            assertThat(dto.pedidoId()).isEqualTo(pedidoId);
            assertThat(dto.conductorId()).isEqualTo(conductorId);
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
            AsignarEnvioRequestDTO dto = new AsignarEnvioRequestDTO(null, 5L);

            // Act
            Set<ConstraintViolation<AsignarEnvioRequestDTO>> violations = validator.validate(dto);

            // Assert — hay al menos una violación sobre pedidoId
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("pedidoId"));
        }
    }

    @Nested
    @DisplayName("Validación de conductorId")
    class ValidacionConductorId {

        /** conductorId nulo → violación @NotNull. */
        @Test
        @DisplayName("conductorId nulo produce violación @NotNull")
        void conductorIdNulo_produceViolacion() {
            // Arrange
            AsignarEnvioRequestDTO dto = new AsignarEnvioRequestDTO(10L, null);

            // Act
            Set<ConstraintViolation<AsignarEnvioRequestDTO>> violations = validator.validate(dto);

            // Assert — hay al menos una violación sobre conductorId
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("conductorId"));
        }
    }

    @Nested
    @DisplayName("Inmutabilidad y métodos de record")
    class MetodosRecord {

        @Test
        @DisplayName("Dos instancias con los mismos valores son iguales y tienen el mismo hashCode")
        void equalsYHashCode() {
            // Arrange
            AsignarEnvioRequestDTO dto1 = new AsignarEnvioRequestDTO(10L, 5L);
            AsignarEnvioRequestDTO dto2 = new AsignarEnvioRequestDTO(10L, 5L);

            // Assert
            assertThat(dto1).isEqualTo(dto2);
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
        }

        @Test
        @DisplayName("Instancias con distintos valores NO son iguales")
        void instanciasDistintas_noIguales() {
            // Arrange
            AsignarEnvioRequestDTO dto1 = new AsignarEnvioRequestDTO(10L, 5L);
            AsignarEnvioRequestDTO dto2 = new AsignarEnvioRequestDTO(20L, 5L);

            // Assert — pedidoId diferente implica desigualdad
            assertThat(dto1).isNotEqualTo(dto2);
        }
    }
}
