package com.udea.demo.pedidos.application.dto;

import com.udea.demo.pedidos.domain.model.EstadoPedido;
import java.time.LocalDateTime;

public record MovimientoSeguimientoResponseDTO(EstadoPedido estado, LocalDateTime fecha) {}
