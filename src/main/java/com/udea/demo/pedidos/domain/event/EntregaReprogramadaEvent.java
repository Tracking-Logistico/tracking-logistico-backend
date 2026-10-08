package com.udea.demo.pedidos.domain.event;

import com.udea.demo.compartido.eventos.EventoIntegracion;
import com.udea.demo.pedidos.domain.model.Pedido;
import java.time.LocalDate;

import java.time.LocalDateTime;
import java.util.UUID;

/** El cliente eligió una nueva fecha para la entrega fallida. */
public record EntregaReprogramadaEvent(
        UUID eventId,
        String tipo,
        int version,
        LocalDateTime ocurridoEn,
        Long pedidoId,
        String numeroTracking,
        Long clienteId,
        LocalDate fechaEntregaReprogramada,
        String mensajeCliente
) implements EventoIntegracion {
    public static final String TIPO = "ENTREGA_REPROGRAMADA";
    public static final String ROUTING_KEY = "pedido.entrega.reprogramada";

    public static EntregaReprogramadaEvent de(Pedido pedido) {
        return new EntregaReprogramadaEvent(UUID.randomUUID(), TIPO, 1, LocalDateTime.now(), pedido.getId(),
                pedido.getNumeroTracking(), pedido.getClienteId(), pedido.getFechaEntregaReprogramada(),
                "Tu entrega fue reprogramada para el "
                        + pedido.getFechaEntregaReprogramada().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");
    }

    @Override public String routingKey() { return ROUTING_KEY; }
}
