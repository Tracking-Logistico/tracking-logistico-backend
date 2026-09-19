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

/**
 * Pruebas de validación del DTO ReordenarRutaRequestDTO (HU-09).
 *
 * Cubre el campo pedidoIdsEnOrden con @NotEmpty.
 */
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

        /** Lista con al menos un elemento no produce violaciones. */
        @Test
        @DisplayName("Lista con pedidoIds válidos no produce violaciones")
        void listaValida_sinViolaciones() {
            // Arrange
            List<Long> ids = List.of(10L, 20L, 30L);
            ReordenarRutaRequestDTO dto = new ReordenarRutaRequestDTO(ids);

            // Act
            Set<ConstraintViolation<ReordenarRutaRequestDTO>> violations = validator.validate(dto);

            // Assert — sin violaciones y la lista conserva los valores en el mismo orden
            assertThat(violations).isEmpty();
            assertThat(dto.pedidoIdsEnOrden()).containsExactly(10L, 20L, 30L);
        }

        /** Un solo elemento es suficiente para pasar @NotEmpty. */
        @Test
        @DisplayName("Lista con un único pedidoId no produce violaciones")
        void listaConUnElemento_sinViolaciones() {
            // Arrange
            ReordenarRutaRequestDTO dto = new ReordenarRutaRequestDTO(List.of(10L));

            // Act
            Set<ConstraintViolation<ReordenarRutaRequestDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isEmpty();
            assertThat(dto.pedidoIdsEnOrden()).hasSize(1);
            assertThat(dto.pedidoIdsEnOrden().get(0)).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("Validación de pedidoIdsEnOrden")
    class ValidacionPedidoIds {

        /** Lista nula → violación @NotEmpty. */
        @Test
        @DisplayName("Lista nula produce violación @NotEmpty")
        void listaNula_produceViolacion() {
            // Arrange
            ReordenarRutaRequestDTO dto = new ReordenarRutaRequestDTO(null);

            // Act
            Set<ConstraintViolation<ReordenarRutaRequestDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v ->
                    v.getPropertyPath().toString().equals("pedidoIdsEnOrden"));
        }

        /** Lista vacía → violación @NotEmpty. */
        @Test
        @DisplayName("Lista vacía produce violación @NotEmpty")
        void listaVacia_produceViolacion() {
            // Arrange
            ReordenarRutaRequestDTO dto = new ReordenarRutaRequestDTO(List.of());

            // Act
            Set<ConstraintViolation<ReordenarRutaRequestDTO>> violations = validator.validate(dto);

            // Assert
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
            // Arrange
            List<Long> ids = List.of(10L, 20L);
            ReordenarRutaRequestDTO dto1 = new ReordenarRutaRequestDTO(ids);
            ReordenarRutaRequestDTO dto2 = new ReordenarRutaRequestDTO(ids);

            // Assert
            assertThat(dto1).isEqualTo(dto2);
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
        }

        @Test
        @DisplayName("Instancias con listas distintas NO son iguales")
        void instanciasDistintas_noIguales() {
            // Arrange
            ReordenarRutaRequestDTO dto1 = new ReordenarRutaRequestDTO(List.of(10L, 20L));
            ReordenarRutaRequestDTO dto2 = new ReordenarRutaRequestDTO(List.of(20L, 10L)); // distinto orden

            // Assert — el orden importa
            assertThat(dto1).isNotEqualTo(dto2);
        }
    }
}
