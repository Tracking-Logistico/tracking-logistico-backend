package com.udea.demo.pedidos.domain.model;

import com.udea.demo.pedidos.domain.exception.TrackingNoActivoException;
import com.udea.demo.pedidos.domain.exception.TransicionEstadoInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias del dominio del pedido.
 *
 * Cada prueba corresponde a un caso de prueba (CP) de HU-03, cubriendo como máximo el camino
 * feliz y el camino de error. Patrón AAA (Arrange - Act - Assert) con secciones marcadas.
 *
 * CP cubiertos:
 *  - CP-HU03A-01: recepción con identificador único (estado inicial SOLICITADO, datos conservados).
 *  - CP-HU03A-02: validación (transición a SOLICITADO / error de transición).
 *  - CP-HU03A-03: prioridad confirmada vs. sugerida.
 *  - CP-HU03B-01: activación de tracking + idempotencia.
 *  - CP-HU03B-02: impresión de etiqueta exige tracking activo.
 */
@DisplayName("Pedido - dominio (por caso de prueba)")
class PedidoTest {

    private static final Long CLIENTE_ID = 10L;
    private static final Long OPERADOR_ID = 99L;
    private static final String NUMERO_PEDIDO = "PED-20260214-8F4A29C1";
    private static final String NUMERO_TRACKING = "TRK-20260214-8F4A29C1B7";

    private Pedido pedidoRecibido() {
        return Pedido.recibir(
                CLIENTE_ID,
                "Carrera 7 #71-21, Bogotá", "Bogotá", "110111",
                "Calle 45 #12-30, Bogotá", "Bogotá", "110111",
                "Caja frágil",
                2.50, 30.0, 20.0, 15.0,
                TipoServicio.EXPRESS,
                NUMERO_PEDIDO,
                Prioridad.ALTA, "Destinatario", "+573001234567", "Cliente", "cliente@test.com", "+573109876543"
        );
    }

    /** CP-HU03A-01: camino feliz. */
    @Test
    @DisplayName("CP-HU03A-01: recibir() deja el pedido en SOLICITADO conservando los datos")
    void recibir_feliz() {
        // Act
        Pedido pedido = pedidoRecibido();

        // Assert
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.SOLICITADO);
        assertThat(pedido.getNumeroPedido()).isEqualTo(NUMERO_PEDIDO);
        assertThat(pedido.getDireccionDestino()).isEqualTo("Calle 45 #12-30, Bogotá");
        assertThat(pedido.getPesoKg()).isEqualTo(2.50);
        assertThat(pedido.getFechaCreacion()).isNotNull();
    }

    @Nested
    @DisplayName("CP-HU03A-02: validación")
    class Validar {

        /** CP-HU03A-02: camino feliz. */
        @Test
        @DisplayName("validar() aprobado transiciona a SOLICITADO y registra el operador")
        void validar_feliz() {
            // Arrange
            Pedido pedido = pedidoRecibido();

            // Act
            pedido.validar(true, null, "Datos correctos", OPERADOR_ID);

            // Assert
            assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.SOLICITADO);
            assertThat(pedido.getOperadorValidadorId()).isEqualTo(OPERADOR_ID);
            assertThat(pedido.getFechaValidacion()).isNotNull();
        }

        /** CP-HU03A-02: camino de error. */
        @Test
        @DisplayName("validar() sobre un estado no transicionable lanza TransicionEstadoInvalidaException")
        void validar_error() {
            // Arrange
            Pedido pedido = pedidoRecibido();
            pedido.validar(true, null, null, OPERADOR_ID); // pasa a SOLICITADO

            // Act & Assert
            assertThatThrownBy(() -> pedido.validar(true, null, null, OPERADOR_ID))
                    .isInstanceOf(TransicionEstadoInvalidaException.class);
        }
    }

    /** CP-HU03A-03: camino feliz (confirma/ajusta prioridad). */
    @Test
    @DisplayName("CP-HU03A-03: validar() con prioridad confirmada la ajusta; sin ella conserva la sugerida")
    void validar_prioridad_feliz() {
        // Arrange
        Pedido sinAjuste = pedidoRecibido();
        Pedido conAjuste = pedidoRecibido();

        // Act
        sinAjuste.validar(true, null, null, OPERADOR_ID);
        conAjuste.validar(true, Prioridad.ALTA, null, OPERADOR_ID);

        // Assert
        assertThat(sinAjuste.getPrioridadConfirmada()).isEqualTo(Prioridad.ALTA);
        assertThat(conAjuste.getPrioridadConfirmada()).isEqualTo(Prioridad.ALTA);
    }

    @Nested
    @DisplayName("CP-HU03B-01: activación de tracking")
    class ActivarTracking {

        /** CP-HU03B-01: camino feliz. */
        @Test
        @DisplayName("activarTracking() sobre un pedido SOLICITADO pasa a CREADO y setea el tracking")
        void activarTracking_feliz() {
            // Arrange
            Pedido pedido = pedidoRecibido();
            pedido.validar(true, null, null, OPERADOR_ID);

            // Act
            pedido.activarTracking(NUMERO_TRACKING);

            // Assert
            assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CREADO);
            assertThat(pedido.getNumeroTracking()).isEqualTo(NUMERO_TRACKING);
        }

        @Test
        @DisplayName("BUG CP-HU03B-01: la segunda activación debería ser idempotente")
        void activarTracking_error_idempotencia() {
            // Arrange
            Pedido pedido = pedidoRecibido();
            pedido.validar(true, null, null, OPERADOR_ID);
            pedido.activarTracking(NUMERO_TRACKING);

            // Act
            pedido.activarTracking(NUMERO_TRACKING);

            // Assert
            assertThat(pedido.getNumeroTracking()).isEqualTo(NUMERO_TRACKING);
            assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CREADO);
        }
    }

    /** CP-HU03B-02: camino de error (sin tracking no se imprime etiqueta). */
    @Test
    @DisplayName("CP-HU03B-02: confirmar impresión sin tracking lanza TrackingNoActivoException")
    void confirmarImpresion_error() {
        // Arrange
        Pedido pedido = pedidoRecibido();

        // Act & Assert
        assertThatThrownBy(pedido::confirmarImpresionEtiqueta)
                .isInstanceOf(TrackingNoActivoException.class);
    }
    @Test
    @DisplayName("El despacho permite avanzar de creado a recibido en origen y después a tránsito")
    void puedeAvanzarHastaTransito() {
        Pedido pedido = pedidoRecibido();
        pedido.validar(true, null, null, OPERADOR_ID);
        pedido.activarTracking(NUMERO_TRACKING);
        pedido.cambiarEstadoLogistico(EstadoPedido.RECIBIDO_EN_ORIGEN);
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.RECIBIDO_EN_ORIGEN);
        pedido.cambiarEstadoLogistico(EstadoPedido.EN_TRANSITO);
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.EN_TRANSITO);
    }

    @Test
    @DisplayName("No es posible activar tracking antes de validar un pedido")
    void noActivaTrackingDeSolicitudSinValidar() {
        Pedido pedido = pedidoRecibido();
        assertThatThrownBy(() -> pedido.activarTracking(NUMERO_TRACKING))
                .isInstanceOf(IllegalStateException.class);
        assertThat(pedido.getNumeroTracking()).isNull();
    }

}
