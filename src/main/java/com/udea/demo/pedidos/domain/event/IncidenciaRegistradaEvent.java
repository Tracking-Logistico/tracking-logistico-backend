package com.udea.demo.pedidos.domain.event;

import com.udea.demo.compartido.eventos.EventoIntegracion;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.IncidenciaPedido;
import com.udea.demo.pedidos.domain.model.Pedido;
import com.udea.demo.pedidos.domain.model.TipoIncidencia;

import java.time.LocalDateTime;
import java.util.UUID;

/** Se registró una novedad sobre el envío; incluye el texto orientado al cliente. */
public record IncidenciaRegistradaEvent(
        UUID eventId,
        String tipo,
        int version,
        LocalDateTime ocurridoEn,
        Long pedidoId,
        String numeroTracking,
        Long clienteId,
        Long incidenciaId,
        Long reportadoPorUsuarioId,
        TipoIncidencia tipoIncidencia,
        String descripcionIncidencia,
        String comentario,
        EstadoPedido estadoAnterior,
        EstadoPedido estadoActual,
        int intentosEntregaFallidos,
        String mensajeCliente
) implements EventoIntegracion {
    public static final String TIPO = "INCIDENCIA_REGISTRADA";
    public static final String ROUTING_KEY = "pedido.incidencia.registrada";

    public static IncidenciaRegistradaEvent de(Pedido pedido, IncidenciaPedido incidencia) {
        return new IncidenciaRegistradaEvent(UUID.randomUUID(), TIPO, 1, LocalDateTime.now(), pedido.getId(),
                pedido.getNumeroTracking(), pedido.getClienteId(), incidencia.getId(), incidencia.getUsuarioId(),
                incidencia.getTipo(), incidencia.getTipo().descripcion(), incidencia.getComentario(),
                incidencia.getEstadoAnterior(), incidencia.getEstadoResultante(), pedido.getIntentosEntregaFallidos(),
                incidencia.getTipo().mensajeCliente());
    }

    @Override public String routingKey() { return ROUTING_KEY; }
}
