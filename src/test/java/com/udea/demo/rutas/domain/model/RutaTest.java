package com.udea.demo.rutas.domain.model;

import com.udea.demo.rutas.domain.exception.EnvioNoAsignadoException;
import com.udea.demo.rutas.domain.exception.OrdenInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias del dominio Ruta y ParadaRuta (HU-09: Gestión y Asignación de Rutas).
 *
 * Patrón AAA (Arrange - Act - Assert) con secciones marcadas.
 *
 * CP cubiertos:
 *  - CP-HU09-01: Creación de ruta para un conductor (factory method).
 *  - CP-HU09-02: Asignación de envío a la ruta (agregarParada), ordenamiento automático.
 *  - CP-HU09-02b: Capacidad mínima de 2 paradas pendientes.
 *  - CP-HU09-03: Organización / reordenamiento de la ruta (reordenar).
 *  - CP-HU09-03b: Reordenar con pedidoIds inválidos lanza OrdenInvalidoException.
 *  - CP-HU09-04: Cancelar parada (base de la reasignación).
 *  - CP-HU09-04b: Cancelar parada inexistente lanza EnvioNoAsignadoException.
 */
@DisplayName("Ruta - dominio (HU-09)")
class RutaTest {

    private static final Long CONDUCTOR_ID = 1L;
    private static final Long PEDIDO_ID_A = 10L;
    private static final Long PEDIDO_ID_B = 20L;
    private static final Long PEDIDO_ID_C = 30L;

    private Ruta rutaVacia() {
        return Ruta.crear(CONDUCTOR_ID, LocalDate.now());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CP-HU09-01: Creación de ruta
    // ─────────────────────────────────────────────────────────────────────────

    /** CP-HU09-01: camino feliz. */
    @Test
    @DisplayName("CP-HU09-01: crear() inicializa la ruta sin paradas para el conductor del día")
    void crear_feliz() {
        // Arrange
        LocalDate hoy = LocalDate.now();

        // Act
        Ruta ruta = Ruta.crear(CONDUCTOR_ID, hoy);

        // Assert
        assertThat(ruta.getConductorId()).isEqualTo(CONDUCTOR_ID);
        assertThat(ruta.getFecha()).isEqualTo(hoy);
        assertThat(ruta.getParadas()).isEmpty();
        assertThat(ruta.getFechaCreacion()).isNotNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CP-HU09-02: Asignación de envíos a la ruta
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("CP-HU09-02: agregarParada (asignación de envíos)")
    class AgregarParada {

        /** CP-HU09-02: camino feliz — primer envío recibe orden 1. */
        @Test
        @DisplayName("agregarParada() asigna el envío con orden 1 y estado PENDIENTE")
        void agregarParada_primerEnvio_ordenUno() {
            // Arrange
            Ruta ruta = rutaVacia();

            // Act
            ParadaRuta parada = ruta.agregarParada(PEDIDO_ID_A);

            // Assert
            assertThat(parada).isNotNull();
            assertThat(parada.getPedidoId()).isEqualTo(PEDIDO_ID_A);
            assertThat(parada.getOrden()).isEqualTo(1);
            assertThat(parada.getEstado()).isEqualTo(EstadoParada.PENDIENTE);
            assertThat(parada.getFechaAsignacion()).isNotNull();
        }

        /** CP-HU09-02: camino feliz — el segundo envío recibe orden 2. */
        @Test
        @DisplayName("agregarParada() asigna orden incremental al segundo envío")
        void agregarParada_segundoEnvio_ordenDos() {
            // Arrange
            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);

            // Act
            ParadaRuta segunda = ruta.agregarParada(PEDIDO_ID_B);

            // Assert — el orden del segundo debe ser exactamente 2, no un valor arbitrario
            assertThat(segunda.getOrden()).isEqualTo(2);
            assertThat(ruta.getParadas()).hasSize(2);
        }

        /**
         * CP-HU09-02b: La carga mínima es 2; con 2 paradas la ruta tiene exactamente
         * la capacidad base configurada.
         */
        @Test
        @DisplayName("agregarParada() permite exactamente 2 envíos (carga mínima = 2)")
        void agregarParada_capacidadMinima_dosEnvios() {
            // Arrange
            Ruta ruta = rutaVacia();

            // Act
            ruta.agregarParada(PEDIDO_ID_A);
            ruta.agregarParada(PEDIDO_ID_B);

            // Assert — exactamente 2 paradas pendientes
            long pendientes = ruta.getParadas().stream()
                    .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                    .count();
            assertThat(pendientes).isEqualTo(2);
        }

        /** CP-HU09-02: el envío queda visible en el panel del conductor. */
        @Test
        @DisplayName("agregarParada() hace visible el envío en la lista de paradas de la ruta")
        void agregarParada_envioVisibleEnRuta() {
            // Arrange
            Ruta ruta = rutaVacia();

            // Act
            ruta.agregarParada(PEDIDO_ID_A);

            // Assert — el pedido existe y está PENDIENTE
            boolean encontrado = ruta.getParadas().stream()
                    .anyMatch(p -> p.getPedidoId().equals(PEDIDO_ID_A)
                            && p.getEstado() == EstadoParada.PENDIENTE);
            assertThat(encontrado).isTrue();
        }

        /** CP-HU09-02: la parada se asocia correctamente a la ruta padre. */
        @Test
        @DisplayName("agregarParada() asocia la parada a la ruta correcta")
        void agregarParada_paradaAsociadaARuta() {
            // Arrange
            Ruta ruta = rutaVacia();

            // Act
            ParadaRuta parada = ruta.agregarParada(PEDIDO_ID_A);

            // Assert — la parada tiene referencia a la misma ruta
            assertThat(parada.getRuta()).isSameAs(ruta);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CP-HU09-03: Organización de la ruta (reordenar)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("CP-HU09-03: reordenar (organización de la ruta)")
    class Reordenar {

        /** CP-HU09-03: camino feliz — el operador define el orden manualmente. */
        @Test
        @DisplayName("reordenar() aplica el nuevo orden indicado por el operador a las paradas pendientes")
        void reordenar_feliz() {
            // Arrange
            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A); // orden inicial 1
            ruta.agregarParada(PEDIDO_ID_B); // orden inicial 2
            ruta.agregarParada(PEDIDO_ID_C); // orden inicial 3

            // Act — el operador invierte el orden: C, B, A
            ruta.reordenar(List.of(PEDIDO_ID_C, PEDIDO_ID_B, PEDIDO_ID_A));

            // Assert — cada parada tiene el orden indicado por el operador
            ParadaRuta paradaC = ruta.getParadas().stream()
                    .filter(p -> p.getPedidoId().equals(PEDIDO_ID_C)).findFirst().orElseThrow();
            ParadaRuta paradaB = ruta.getParadas().stream()
                    .filter(p -> p.getPedidoId().equals(PEDIDO_ID_B)).findFirst().orElseThrow();
            ParadaRuta paradaA = ruta.getParadas().stream()
                    .filter(p -> p.getPedidoId().equals(PEDIDO_ID_A)).findFirst().orElseThrow();

            assertThat(paradaC.getOrden()).isEqualTo(1);
            assertThat(paradaB.getOrden()).isEqualTo(2);
            assertThat(paradaA.getOrden()).isEqualTo(3);
        }

        /**
         * CP-HU09-03: prioridad Alta — el operador coloca el envío de mayor
         * prioridad en primera posición.
         */
        @Test
        @DisplayName("reordenar() permite colocar el envío de prioridad Alta en primera posición")
        void reordenar_envioAltaPrioridad_primeraPosicion() {
            // Arrange
            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A); // orden inicial 1 (prioridad baja)
            ruta.agregarParada(PEDIDO_ID_B); // orden inicial 2 (prioridad alta)

            // Act — operador prioriza B (alta prioridad)
            ruta.reordenar(List.of(PEDIDO_ID_B, PEDIDO_ID_A));

            // Assert — B queda en la posición 1
            ParadaRuta paradaB = ruta.getParadas().stream()
                    .filter(p -> p.getPedidoId().equals(PEDIDO_ID_B)).findFirst().orElseThrow();
            assertThat(paradaB.getOrden()).isEqualTo(1);
        }

        /** CP-HU09-03: camino de error — la lista contiene un pedidoId extraño. */
        @Test
        @DisplayName("reordenar() con un pedidoId ajeno a la ruta lanza OrdenInvalidoException")
        void reordenar_listaConPedidoAjeno_lanzaExcepcion() {
            // Arrange
            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);
            ruta.agregarParada(PEDIDO_ID_B);

            // Act & Assert — PEDIDO_ID_C no pertenece a la ruta
            assertThatThrownBy(() -> ruta.reordenar(List.of(PEDIDO_ID_A, PEDIDO_ID_C)))
                    .isInstanceOf(OrdenInvalidoException.class);
        }

        /** CP-HU09-03: camino de error — la lista tiene menos elementos de los que hay. */
        @Test
        @DisplayName("reordenar() con lista incompleta lanza OrdenInvalidoException")
        void reordenar_listaIncompleta_lanzaExcepcion() {
            // Arrange
            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);
            ruta.agregarParada(PEDIDO_ID_B);
            ruta.agregarParada(PEDIDO_ID_C);

            // Act & Assert — solo se incluyen 2 de 3 pedidos
            assertThatThrownBy(() -> ruta.reordenar(List.of(PEDIDO_ID_A, PEDIDO_ID_B)))
                    .isInstanceOf(OrdenInvalidoException.class);
        }

        /** CP-HU09-03: camino de error — la lista tiene más elementos de los que hay. */
        @Test
        @DisplayName("reordenar() con lista con IDs duplicados lanza OrdenInvalidoException")
        void reordenar_listaDuplicados_lanzaExcepcion() {
            // Arrange
            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);
            ruta.agregarParada(PEDIDO_ID_B);

            // Act & Assert — se repite A (duplicado) en vez de incluir B
            assertThatThrownBy(() -> ruta.reordenar(List.of(PEDIDO_ID_A, PEDIDO_ID_A, PEDIDO_ID_B)))
                    .isInstanceOf(OrdenInvalidoException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CP-HU09-04: Cancelar parada (base de la reasignación)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("CP-HU09-04: cancelarParada (base de reasignación)")
    class CancelarParada {

        /** CP-HU09-04: camino feliz — la parada pasa a CANCELADA. */
        @Test
        @DisplayName("cancelarParada() cambia el estado de la parada a CANCELADA")
        void cancelarParada_feliz() {
            // Arrange
            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);

            // Act
            ruta.cancelarParada(PEDIDO_ID_A);

            // Assert
            ParadaRuta parada = ruta.getParadas().stream()
                    .filter(p -> p.getPedidoId().equals(PEDIDO_ID_A))
                    .findFirst().orElseThrow();
            assertThat(parada.getEstado()).isEqualTo(EstadoParada.CANCELADA);
        }

        /** CP-HU09-04: tras cancelar, el envío ya no aparece como pendiente en el panel. */
        @Test
        @DisplayName("cancelarParada() remueve el envío del panel activo (deja de ser PENDIENTE)")
        void cancelarParada_yaNoEsPendiente() {
            // Arrange
            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);
            ruta.agregarParada(PEDIDO_ID_B);

            // Act
            ruta.cancelarParada(PEDIDO_ID_A);

            // Assert — solo B queda pendiente
            long pendientes = ruta.getParadas().stream()
                    .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                    .count();
            assertThat(pendientes).isEqualTo(1);
            assertThat(ruta.getParadas().stream()
                    .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                    .findFirst().orElseThrow().getPedidoId())
                    .isEqualTo(PEDIDO_ID_B);
        }

        /**
         * CP-HU09-04: el historial de la reasignación se conserva —
         * la parada cancelada sigue en la lista con estado CANCELADA.
         */
        @Test
        @DisplayName("cancelarParada() conserva la parada en el historial de la ruta original")
        void cancelarParada_conservaHistorial() {
            // Arrange
            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);

            // Act
            ruta.cancelarParada(PEDIDO_ID_A);

            // Assert — la parada permanece en la lista (historial), solo cambió de estado
            assertThat(ruta.getParadas()).hasSize(1);
            assertThat(ruta.getParadas().get(0).getPedidoId()).isEqualTo(PEDIDO_ID_A);
            assertThat(ruta.getParadas().get(0).getEstado()).isEqualTo(EstadoParada.CANCELADA);
        }

        /** CP-HU09-04: camino de error — pedido que no existe en la ruta. */
        @Test
        @DisplayName("cancelarParada() de un pedidoId no asignado lanza EnvioNoAsignadoException")
        void cancelarParada_pedidoInexistente_lanzaExcepcion() {
            // Arrange
            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);

            // Act & Assert — PEDIDO_ID_B nunca fue agregado
            assertThatThrownBy(() -> ruta.cancelarParada(PEDIDO_ID_B))
                    .isInstanceOf(EnvioNoAsignadoException.class);
        }

        /** CP-HU09-04: camino de error — parada ya CANCELADA no puede cancelarse de nuevo. */
        @Test
        @DisplayName("cancelarParada() de una parada ya CANCELADA lanza EnvioNoAsignadoException")
        void cancelarParada_yaEstaCancelada_lanzaExcepcion() {
            // Arrange
            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);
            ruta.cancelarParada(PEDIDO_ID_A); // primera cancelación

            // Act & Assert — la segunda cancelación debe fallar
            assertThatThrownBy(() -> ruta.cancelarParada(PEDIDO_ID_A))
                    .isInstanceOf(EnvioNoAsignadoException.class);
        }
    }

    @Test
    @DisplayName("La ordenación manual sigue vigente al agregar otra entrega")
    void respetaOrdenManualConNuevaParada() {
        Ruta ruta = Ruta.crear(5L, LocalDate.now());
        ruta.agregarParada(10L);
        ruta.agregarParada(20L);
        ruta.reordenar(java.util.List.of(20L, 10L));
        ruta.agregarParada(30L);
        assertThat(ruta.isOrdenManual()).isTrue();
        assertThat(ruta.getParadas().stream()
            .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
            .sorted(java.util.Comparator.comparing(ParadaRuta::getOrden))
            .map(ParadaRuta::getPedidoId).toList()).containsExactly(20L, 10L, 30L);
    }
}
