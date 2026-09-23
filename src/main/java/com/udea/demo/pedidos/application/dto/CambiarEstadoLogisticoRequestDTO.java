package com.udea.demo.pedidos.application.dto;

import com.udea.demo.pedidos.domain.model.EstadoPedido;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoLogisticoRequestDTO(@NotNull EstadoPedido estado) {}
