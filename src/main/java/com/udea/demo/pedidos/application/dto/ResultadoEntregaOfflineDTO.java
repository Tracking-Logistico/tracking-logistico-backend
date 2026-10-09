package com.udea.demo.pedidos.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ResultadoEntregaOfflineDTO(@NotNull Long pedidoId, @NotNull @Valid ResultadoEntregaRequestDTO evento) {}
