package com.udea.demo.pedidos.domain.event;

import com.udea.demo.compartido.eventos.EventoIntegracion;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.MotivoDevolucion;
import com.udea.demo.pedidos.domain.model.Pedido;

import java.time.LocalDateTime;
import java.util.UUID;

/** El envío inicia el proceso de devolución al remitente. */
public record DevolucionIniciadaEvent(
        UUID eventId,
        String tipo,
        int version,
        LocalDateTime ocurridoEn,
        Long pedidoId,
        String numeroTracking,
        Long clienteId,
        EstadoPedido estadoAnterior,
        MotivoDevolucion motivo,
        String descripcionMotivo,
        String mensajeCliente
) implements EventoIntegracion {
    public static final String TIPO = "DEVOLUCION_INICIADA";
    public static final String ROUTING_KEY = "pedido.devolucion.iniciada";

    public static DevolucionIniciadaEvent de(Pedido pedido, EstadoPedido estadoAnterior, MotivoDevolucion motivo) {
        return new DevolucionIniciadaEvent(UUID.randomUUID(), TIPO, 1, LocalDateTime.now(), pedido.getId(),
                pedido.getNumeroTracking(), pedido.getClienteId(), estadoAnterior, motivo, motivo.descripcion(),
                "Tu paquete será devuelto al remitente. Motivo: " + motivo.descripcion() + ".");
    }

    @Override public String routingKey() { return ROUTING_KEY; }
}
