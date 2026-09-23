package com.udea.demo.rutas.application.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReordenarRutaRequestDTO - validación (HU-09)")
class ReordenarRutaRequestDTOTest {

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
        @DisplayName("Lista con pedidoIds válidos no produce violaciones")
        void listaValida_sinViolaciones() {

            List<Long> ids = List.of(10L, 20L, 30L);
            ReordenarRutaRequestDTO dto = new ReordenarRutaRequestDTO(ids);

            Set<ConstraintViolation<ReordenarRutaRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
            assertThat(dto.pedidoIdsEnOrden()).containsExactly(10L, 20L, 30L);
        }

        @Test
        @DisplayName("Lista con un único pedidoId no produce violaciones")
        void listaConUnElemento_sinViolaciones() {

            ReordenarRutaRequestDTO dto = new ReordenarRutaRequestDTO(List.of(10L));

            Set<ConstraintViolation<ReordenarRutaRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
            assertThat(dto.pedidoIdsEnOrden()).hasSize(1);
            assertThat(dto.pedidoIdsEnOrden().get(0)).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("Validación de pedidoIdsEnOrden")
    class ValidacionPedidoIds {

        @Test
        @DisplayName("Lista nula produce violación @NotEmpty")
        void listaNula_produceViolacion() {

            ReordenarRutaRequestDTO dto = new ReordenarRutaRequestDTO(null);

            Set<ConstraintViolation<ReordenarRutaRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v ->
                    v.getPropertyPath().toString().equals("pedidoIdsEnOrden"));
        }

        @Test
        @DisplayName("Lista vacía produce violación @NotEmpty")
        void listaVacia_produceViolacion() {

            ReordenarRutaRequestDTO dto = new ReordenarRutaRequestDTO(List.of());

            Set<ConstraintViolation<ReordenarRutaRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v ->
                    v.getPropertyPath().toString().equals("pedidoIdsEnOrden"));
        }
    }

    @Nested
    @DisplayName("Inmutabilidad y métodos de record")
    class MetodosRecord {

        @Test
        @DisplayName("Dos instancias con la misma lista son iguales y tienen el mismo hashCode")
        void equalsYHashCode() {

            List<Long> ids = List.of(10L, 20L);
            ReordenarRutaRequestDTO dto1 = new ReordenarRutaRequestDTO(ids);
            ReordenarRutaRequestDTO dto2 = new ReordenarRutaRequestDTO(ids);

            assertThat(dto1).isEqualTo(dto2);
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
        }

        @Test
        @DisplayName("Instancias con listas distintas NO son iguales")
        void instanciasDistintas_noIguales() {

            ReordenarRutaRequestDTO dto1 = new ReordenarRutaRequestDTO(List.of(10L, 20L));
            ReordenarRutaRequestDTO dto2 = new ReordenarRutaRequestDTO(List.of(20L, 10L));

            assertThat(dto1).isNotEqualTo(dto2);
        }
    }
}
