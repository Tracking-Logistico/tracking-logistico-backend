package com.udea.demo.pedidos.application.dto;

import com.udea.demo.pedidos.domain.model.CheckpointPedido;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.EstadoRegistroCheckpoint;
import com.udea.demo.pedidos.domain.model.EtapaCheckpoint;
import com.udea.demo.pedidos.domain.model.OrigenCheckpoint;

import java.time.LocalDateTime;

public record CheckpointResponseDTO(
        Long id,
        Long pedidoId,
        String idEventoCliente,
        EtapaCheckpoint etapa,
        EstadoPedido estadoAnterior,
        EstadoPedido estadoResultante,
        Double latitud,
        Double longitud,
        Double precisionMetros,
        boolean ubicacionConfiable,
        OrigenCheckpoint origen,
        EstadoRegistroCheckpoint estadoRegistro,
        String motivoRevision,
        LocalDateTime fechaEvento,
        LocalDateTime fechaRegistro
) {
    public static CheckpointResponseDTO de(CheckpointPedido c) {
        return new CheckpointResponseDTO(c.getId(), c.getPedidoId(), c.getIdEventoCliente(), c.getEtapa(),
                c.getEstadoAnterior(), c.getEstadoResultante(), c.getLatitud(), c.getLongitud(),
                c.getPrecisionMetros(), c.isUbicacionConfiable(), c.getOrigen(), c.getEstadoRegistro(),
                c.getMotivoRevision(), c.getFechaEvento(), c.getFechaRegistro());
    }
}
