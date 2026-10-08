package com.udea.demo.pedidos.application.dto;

import com.udea.demo.pedidos.domain.model.EstadoPedido;
import java.time.LocalDateTime;

/**
 * Movimiento visible para el cliente. {@code tipo}: ESTADO, PUNTO_CONTROL o NOVEDAD;
 * {@code estado} solo se informa cuando el movimiento cambió el estado del envío.
 */
public record MovimientoSeguimientoResponseDTO(EstadoPedido estado, LocalDateTime fecha, String tipo, String descripcion) {}
