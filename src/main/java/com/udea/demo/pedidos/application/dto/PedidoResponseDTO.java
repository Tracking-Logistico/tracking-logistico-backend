package com.udea.demo.pedidos.application.dto;

import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;

import java.time.LocalDateTime;

public record PedidoResponseDTO(
    Long id,
    String numeroPedido,
    Long clienteId,
    String direccionOrigen,
    String direccionDestino,
    String descripcionPaquete,
    Double pesoKg,
    Double largoCm,
    Double anchoCm,
    Double altoCm,
    TipoServicio tipoServicio,
    Prioridad prioridadSugerida,
    Prioridad prioridadConfirmada,
    EstadoPedido estado,
    String observacionesValidacion,
    Long operadorValidadorId,
    LocalDateTime fechaCreacion,
    LocalDateTime fechaValidacion,
    String numeroTracking,
    LocalDateTime fechaActivacionTracking,
    Boolean etiquetaImpresa,
    LocalDateTime fechaImpresionEtiqueta
) {}
