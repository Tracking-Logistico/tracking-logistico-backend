package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.domain.model.HistorialPedido;
import com.udea.demo.pedidos.interfaces.persistence.HistorialPedidoRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Agrega entradas a la línea de tiempo del envío ({@code historial_pedidos}). */
@Component
public class RegistroHistorialPedido {
    public static final String ESTADO_LOGISTICO = "ESTADO_LOGISTICO";
    public static final String CHECKPOINT = "CHECKPOINT";
    public static final String CHECKPOINT_EN_REVISION = "CHECKPOINT_EN_REVISION";
    public static final String INCIDENCIA = "INCIDENCIA";

    private final HistorialPedidoRepository historial;

    public RegistroHistorialPedido(HistorialPedidoRepository historial) { this.historial = historial; }

    public void registrar(Long pedidoId, Long usuarioId, String tipo, String campo, String detalle, LocalDateTime fecha) {
        historial.save(HistorialPedido.builder().pedidoId(pedidoId).usuarioId(usuarioId).tipoEvento(tipo)
                .campoObservado(campo).detalle(detalle).fecha(fecha).build());
    }
}
