package com.udea.demo.rutas.application.service;

import com.udea.demo.pedidos.domain.event.CheckpointRegistradoEvent;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.rutas.domain.model.EstadoParada;
import com.udea.demo.rutas.domain.model.Ruta;
import com.udea.demo.rutas.interfaces.persistence.ParadaRutaRepository;
import com.udea.demo.rutas.interfaces.persistence.RutaRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Cuando el conductor escanea el QR con etapa ENTREGADO, el pedido pasa a ENTREGADO.
 * Este listener marca la parada como ENTREGADO en la misma transacción, para que la
 * ruta del conductor refleje el progreso real de la jornada.
 *
 * Mismo patrón que CierreParadaPorDevolucionListener.
 */
@Component
public class MarcarParadaEntregadaListener {

    private final ParadaRutaRepository paradas;
    private final RutaRepository rutas;

    public MarcarParadaEntregadaListener(ParadaRutaRepository paradas, RutaRepository rutas) {
        this.paradas = paradas;
        this.rutas = rutas;
    }

    @EventListener
    public void alRegistrarCheckpoint(CheckpointRegistradoEvent evento) {
        if (evento.estadoActual() != EstadoPedido.ENTREGADO) return;
        paradas.findByPedidoIdAndEstado(evento.pedidoId(), EstadoParada.PENDIENTE)
                .ifPresent(parada -> {
                    Ruta ruta = parada.getRuta();
                    ruta.marcarParadaEntregada(evento.pedidoId());
                    rutas.save(ruta);
                });
    }
}
