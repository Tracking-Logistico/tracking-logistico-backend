package com.udea.demo.pedidos.domain.event;

import com.udea.demo.compartido.eventos.EventoIntegracion;
import com.udea.demo.pedidos.domain.model.CheckpointPedido;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.EtapaCheckpoint;
import com.udea.demo.pedidos.domain.model.Pedido;

import java.time.LocalDateTime;
import java.util.UUID;

/** Un checkpoint sincronizado desde el dispositivo no pudo aplicarse y requiere revisión del operador. */
public record CheckpointPendienteRevisionEvent(
        UUID eventId,
        String tipo,
        int version,
        LocalDateTime ocurridoEn,
        Long pedidoId,
        String numeroTracking,
        Long checkpointId,
        Long conductorUsuarioId,
        EtapaCheckpoint etapa,
        EstadoPedido estadoActual,
        String motivo,
        LocalDateTime fechaEvento
) implements EventoIntegracion {
    public static final String TIPO = "CHECKPOINT_PENDIENTE_REVISION";
    public static final String ROUTING_KEY = "pedido.checkpoint.pendiente-revision";

    public static CheckpointPendienteRevisionEvent de(Pedido pedido, CheckpointPedido checkpoint) {
        return new CheckpointPendienteRevisionEvent(UUID.randomUUID(), TIPO, 1, LocalDateTime.now(), pedido.getId(),
                pedido.getNumeroTracking(), checkpoint.getId(), checkpoint.getUsuarioId(), checkpoint.getEtapa(),
                pedido.getEstado(), checkpoint.getMotivoRevision(), checkpoint.getFechaEvento());
    }

    @Override public String routingKey() { return ROUTING_KEY; }
}
