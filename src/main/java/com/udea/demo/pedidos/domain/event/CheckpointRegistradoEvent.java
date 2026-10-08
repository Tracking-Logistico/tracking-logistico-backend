package com.udea.demo.pedidos.domain.event;

import com.udea.demo.compartido.eventos.EventoIntegracion;
import com.udea.demo.pedidos.domain.model.CheckpointPedido;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.EtapaCheckpoint;
import com.udea.demo.pedidos.domain.model.OrigenCheckpoint;
import com.udea.demo.pedidos.domain.model.Pedido;

import java.time.LocalDateTime;
import java.util.UUID;

/** El conductor registró el paso del envío por un punto de control y quedó aplicado en la trazabilidad. */
public record CheckpointRegistradoEvent(
        UUID eventId,
        String tipo,
        int version,
        LocalDateTime ocurridoEn,
        Long pedidoId,
        String numeroTracking,
        Long clienteId,
        Long checkpointId,
        Long conductorUsuarioId,
        EtapaCheckpoint etapa,
        EstadoPedido estadoAnterior,
        EstadoPedido estadoActual,
        Double latitud,
        Double longitud,
        boolean ubicacionConfiable,
        OrigenCheckpoint origen,
        LocalDateTime fechaEvento
) implements EventoIntegracion {
    public static final String TIPO = "CHECKPOINT_REGISTRADO";
    public static final String ROUTING_KEY = "pedido.checkpoint.registrado";

    public static CheckpointRegistradoEvent de(Pedido pedido, CheckpointPedido checkpoint) {
        return new CheckpointRegistradoEvent(UUID.randomUUID(), TIPO, 1, LocalDateTime.now(), pedido.getId(),
                pedido.getNumeroTracking(), pedido.getClienteId(), checkpoint.getId(), checkpoint.getUsuarioId(),
                checkpoint.getEtapa(), checkpoint.getEstadoAnterior(), checkpoint.getEstadoResultante(),
                checkpoint.getLatitud(), checkpoint.getLongitud(), checkpoint.isUbicacionConfiable(),
                checkpoint.getOrigen(), checkpoint.getFechaEvento());
    }

    @Override public String routingKey() { return ROUTING_KEY; }
}
