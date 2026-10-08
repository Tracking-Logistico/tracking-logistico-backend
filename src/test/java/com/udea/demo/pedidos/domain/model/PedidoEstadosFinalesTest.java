package com.udea.demo.pedidos.domain.model;

import com.udea.demo.pedidos.domain.exception.EnvioFinalizadoException;
import com.udea.demo.pedidos.domain.exception.TransicionEstadoInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HU-06 Pedido - prevención de estados inválidos (criterio 2)")
class PedidoEstadosFinalesTest {

    private Pedido pedidoEn(EstadoPedido estado) {
        return Pedido.builder().id(1L).numeroTracking("LT1").estado(estado).build();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoPedido.class, names = {"ENTREGADO", "DEVOLUCION_AL_REMITENTE", "ENTREGA_FALLIDA_CERRADA"})
    @DisplayName("Un envío en estado final no puede modificar su estado")
    void estadosFinalesRechazanCambios(EstadoPedido finalizado) {
        Pedido pedido = pedidoEn(finalizado);

        assertThatThrownBy(() -> pedido.cambiarEstadoLogistico(EstadoPedido.EN_REPARTO))
                .isInstanceOf(EnvioFinalizadoException.class)
                .hasMessageContaining("no puede modificar su estado");
        assertThat(pedido.getEstado()).isEqualTo(finalizado);
    }

    @Test
    @DisplayName("esFinal identifica únicamente los estados de cierre")
    void esFinal() {
        assertThat(EstadoPedido.ENTREGADO.esFinal()).isTrue();
        assertThat(EstadoPedido.DEVOLUCION_AL_REMITENTE.esFinal()).isTrue();
        assertThat(EstadoPedido.ENTREGA_FALLIDA_CERRADA.esFinal()).isTrue();
        assertThat(EstadoPedido.RECHAZADO.esFinal()).isTrue();
        assertThat(EstadoPedido.EN_REPARTO.esFinal()).isFalse();
        assertThat(EstadoPedido.ENTREGA_FALLIDA.esFinal()).isFalse();
    }

    @Test
    @DisplayName("El flujo Recibido en origen → En tránsito → En reparto → Entregado sigue siendo válido")
    void flujoNormal() {
        Pedido pedido = pedidoEn(EstadoPedido.CREADO);
        pedido.cambiarEstadoLogistico(EstadoPedido.RECIBIDO_EN_ORIGEN);
        pedido.cambiarEstadoLogistico(EstadoPedido.EN_TRANSITO);
        pedido.cambiarEstadoLogistico(EstadoPedido.EN_REPARTO);
        pedido.cambiarEstadoLogistico(EstadoPedido.ENTREGADO);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ENTREGADO);
    }

    @Test
    @DisplayName("Un envío reprogramado vuelve a reparto")
    void reprogramadoVuelveAReparto() {
        Pedido pedido = pedidoEn(EstadoPedido.ENTREGA_REPROGRAMADA);
        pedido.cambiarEstadoLogistico(EstadoPedido.EN_REPARTO);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.EN_REPARTO);
    }

    @Test
    @DisplayName("Retroceder de En reparto a En tránsito es una transición inválida")
    void retrocesoInvalido() {
        Pedido pedido = pedidoEn(EstadoPedido.EN_REPARTO);

        assertThat(pedido.puedeCambiarEstadoLogisticoA(EstadoPedido.EN_TRANSITO)).isFalse();
        assertThatThrownBy(() -> pedido.cambiarEstadoLogistico(EstadoPedido.EN_TRANSITO))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }
}
