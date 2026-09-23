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

        @Test
        @DisplayName("pedidoId y conductorId presentes → sin violaciones")
        void camposPresentes_sinViolaciones() {

            Long pedidoId    = 10L;
            Long conductorId = 5L;
            AsignarEnvioRequestDTO dto = new AsignarEnvioRequestDTO(pedidoId, conductorId);

            Set<ConstraintViolation<AsignarEnvioRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
            assertThat(dto.pedidoId()).isEqualTo(pedidoId);
            assertThat(dto.conductorId()).isEqualTo(conductorId);
        }
    }

    @Nested
    @DisplayName("Validación de pedidoId")
    class ValidacionPedidoId {

        @Test
        @DisplayName("pedidoId nulo produce violación @NotNull")
        void pedidoIdNulo_produceViolacion() {

            AsignarEnvioRequestDTO dto = new AsignarEnvioRequestDTO(null, 5L);

            Set<ConstraintViolation<AsignarEnvioRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("pedidoId"));
        }
    }

    @Nested
    @DisplayName("Validación de conductorId")
    class ValidacionConductorId {

        @Test
        @DisplayName("conductorId nulo produce violación @NotNull")
        void conductorIdNulo_produceViolacion() {

            AsignarEnvioRequestDTO dto = new AsignarEnvioRequestDTO(10L, null);

            Set<ConstraintViolation<AsignarEnvioRequestDTO>> violations = validator.validate(dto);

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

            AsignarEnvioRequestDTO dto1 = new AsignarEnvioRequestDTO(10L, 5L);
            AsignarEnvioRequestDTO dto2 = new AsignarEnvioRequestDTO(10L, 5L);

            assertThat(dto1).isEqualTo(dto2);
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
        }

        @Test
        @DisplayName("Instancias con distintos valores NO son iguales")
        void instanciasDistintas_noIguales() {

            AsignarEnvioRequestDTO dto1 = new AsignarEnvioRequestDTO(10L, 5L);
            AsignarEnvioRequestDTO dto2 = new AsignarEnvioRequestDTO(20L, 5L);

            assertThat(dto1).isNotEqualTo(dto2);
        }
    }
}
