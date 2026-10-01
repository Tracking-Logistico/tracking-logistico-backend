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

@DisplayName("Ruta - dominio (HU-09)")
class RutaTest {

    private static final Long CONDUCTOR_ID = 1L;
    private static final Long PEDIDO_ID_A = 10L;
    private static final Long PEDIDO_ID_B = 20L;
    private static final Long PEDIDO_ID_C = 30L;

    private Ruta rutaVacia() {
        return Ruta.crear(CONDUCTOR_ID, LocalDate.now());
    }

    @Test
    @DisplayName("CP-HU09-01: crear() inicializa la ruta sin paradas para el conductor del día")
    void crear_feliz() {

        LocalDate hoy = LocalDate.now();

        Ruta ruta = Ruta.crear(CONDUCTOR_ID, hoy);

        assertThat(ruta.getConductorId()).isEqualTo(CONDUCTOR_ID);
        assertThat(ruta.getFecha()).isEqualTo(hoy);
        assertThat(ruta.getParadas()).isEmpty();
        assertThat(ruta.getFechaCreacion()).isNotNull();
    }

    @Nested
    @DisplayName("CP-HU09-02: agregarParada (asignación de envíos)")
    class AgregarParada {

        @Test
        @DisplayName("agregarParada() asigna el envío con orden 1 y estado PENDIENTE")
        void agregarParada_primerEnvio_ordenUno() {

            Ruta ruta = rutaVacia();

            ParadaRuta parada = ruta.agregarParada(PEDIDO_ID_A);

            assertThat(parada).isNotNull();
            assertThat(parada.getPedidoId()).isEqualTo(PEDIDO_ID_A);
            assertThat(parada.getOrden()).isEqualTo(1);
            assertThat(parada.getEstado()).isEqualTo(EstadoParada.PENDIENTE);
            assertThat(parada.getFechaAsignacion()).isNotNull();
        }

        @Test
        @DisplayName("agregarParada() asigna orden incremental al segundo envío")
        void agregarParada_segundoEnvio_ordenDos() {

            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);

            ParadaRuta segunda = ruta.agregarParada(PEDIDO_ID_B);

            assertThat(segunda.getOrden()).isEqualTo(2);
            assertThat(ruta.getParadas()).hasSize(2);
        }

        @Test
        @DisplayName("agregarParada() permite exactamente 2 envíos (carga mínima = 2)")
        void agregarParada_capacidadMinima_dosEnvios() {

            Ruta ruta = rutaVacia();

            ruta.agregarParada(PEDIDO_ID_A);
            ruta.agregarParada(PEDIDO_ID_B);

            long pendientes = ruta.getParadas().stream()
                    .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                    .count();
            assertThat(pendientes).isEqualTo(2);
        }

        @Test
        @DisplayName("agregarParada() hace visible el envío en la lista de paradas de la ruta")
        void agregarParada_envioVisibleEnRuta() {

            Ruta ruta = rutaVacia();

            ruta.agregarParada(PEDIDO_ID_A);

            boolean encontrado = ruta.getParadas().stream()
                    .anyMatch(p -> p.getPedidoId().equals(PEDIDO_ID_A)
                            && p.getEstado() == EstadoParada.PENDIENTE);
            assertThat(encontrado).isTrue();
        }

        @Test
        @DisplayName("agregarParada() asocia la parada a la ruta correcta")
        void agregarParada_paradaAsociadaARuta() {

            Ruta ruta = rutaVacia();

            ParadaRuta parada = ruta.agregarParada(PEDIDO_ID_A);

            assertThat(parada.getRuta()).isSameAs(ruta);
        }
    }

    @Nested
    @DisplayName("CP-HU09-03: reordenar (organización de la ruta)")
    class Reordenar {

        @Test
        @DisplayName("reordenar() aplica el nuevo orden indicado por el operador a las paradas pendientes")
        void reordenar_feliz() {

            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);
            ruta.agregarParada(PEDIDO_ID_B);
            ruta.agregarParada(PEDIDO_ID_C);

            ruta.reordenar(List.of(PEDIDO_ID_C, PEDIDO_ID_B, PEDIDO_ID_A));

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

        @Test
        @DisplayName("reordenar() permite colocar el envío de prioridad Alta en primera posición")
        void reordenar_envioAltaPrioridad_primeraPosicion() {

            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);
            ruta.agregarParada(PEDIDO_ID_B);

            ruta.reordenar(List.of(PEDIDO_ID_B, PEDIDO_ID_A));

            ParadaRuta paradaB = ruta.getParadas().stream()
                    .filter(p -> p.getPedidoId().equals(PEDIDO_ID_B)).findFirst().orElseThrow();
            assertThat(paradaB.getOrden()).isEqualTo(1);
        }

        @Test
        @DisplayName("reordenar() con un pedidoId ajeno a la ruta lanza OrdenInvalidoException")
        void reordenar_listaConPedidoAjeno_lanzaExcepcion() {

            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);
            ruta.agregarParada(PEDIDO_ID_B);

            assertThatThrownBy(() -> ruta.reordenar(List.of(PEDIDO_ID_A, PEDIDO_ID_C)))
                    .isInstanceOf(OrdenInvalidoException.class);
        }

        @Test
        @DisplayName("reordenar() con lista incompleta lanza OrdenInvalidoException")
        void reordenar_listaIncompleta_lanzaExcepcion() {

            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);
            ruta.agregarParada(PEDIDO_ID_B);
            ruta.agregarParada(PEDIDO_ID_C);

            assertThatThrownBy(() -> ruta.reordenar(List.of(PEDIDO_ID_A, PEDIDO_ID_B)))
                    .isInstanceOf(OrdenInvalidoException.class);
        }

        @Test
        @DisplayName("reordenar() con lista con IDs duplicados lanza OrdenInvalidoException")
        void reordenar_listaDuplicados_lanzaExcepcion() {

            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);
            ruta.agregarParada(PEDIDO_ID_B);

            assertThatThrownBy(() -> ruta.reordenar(List.of(PEDIDO_ID_A, PEDIDO_ID_A, PEDIDO_ID_B)))
                    .isInstanceOf(OrdenInvalidoException.class);
        }
    }

    @Nested
    @DisplayName("CP-HU09-04: cancelarParada (base de reasignación)")
    class CancelarParada {

        @Test
        @DisplayName("cancelarParada() cambia el estado de la parada a CANCELADA")
        void cancelarParada_feliz() {

            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);

            ruta.cancelarParada(PEDIDO_ID_A);

            ParadaRuta parada = ruta.getParadas().stream()
                    .filter(p -> p.getPedidoId().equals(PEDIDO_ID_A))
                    .findFirst().orElseThrow();
            assertThat(parada.getEstado()).isEqualTo(EstadoParada.CANCELADA);
        }

        @Test
        @DisplayName("cancelarParada() remueve el envío del panel activo (deja de ser PENDIENTE)")
        void cancelarParada_yaNoEsPendiente() {

            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);
            ruta.agregarParada(PEDIDO_ID_B);

            ruta.cancelarParada(PEDIDO_ID_A);

            long pendientes = ruta.getParadas().stream()
                    .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                    .count();
            assertThat(pendientes).isEqualTo(1);
            assertThat(ruta.getParadas().stream()
                    .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                    .findFirst().orElseThrow().getPedidoId())
                    .isEqualTo(PEDIDO_ID_B);
        }

        @Test
        @DisplayName("cancelarParada() conserva la parada en el historial de la ruta original")
        void cancelarParada_conservaHistorial() {

            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);

            ruta.cancelarParada(PEDIDO_ID_A);

            assertThat(ruta.getParadas()).hasSize(1);
            assertThat(ruta.getParadas().get(0).getPedidoId()).isEqualTo(PEDIDO_ID_A);
            assertThat(ruta.getParadas().get(0).getEstado()).isEqualTo(EstadoParada.CANCELADA);
        }

        @Test
        @DisplayName("cancelarParada() de un pedidoId no asignado lanza EnvioNoAsignadoException")
        void cancelarParada_pedidoInexistente_lanzaExcepcion() {

            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);

            assertThatThrownBy(() -> ruta.cancelarParada(PEDIDO_ID_B))
                    .isInstanceOf(EnvioNoAsignadoException.class);
        }

        @Test
        @DisplayName("cancelarParada() de una parada ya CANCELADA lanza EnvioNoAsignadoException")
        void cancelarParada_yaEstaCancelada_lanzaExcepcion() {

            Ruta ruta = rutaVacia();
            ruta.agregarParada(PEDIDO_ID_A);
            ruta.cancelarParada(PEDIDO_ID_A);

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
