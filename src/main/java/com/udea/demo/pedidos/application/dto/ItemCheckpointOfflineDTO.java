package com.udea.demo.pedidos.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Checkpoint almacenado en el dispositivo sin conexión, junto al envío que el conductor esperaba escanear. */
public record ItemCheckpointOfflineDTO(
        @NotNull(message = "El envío es obligatorio") @Positive Long pedidoId,
        @NotNull @Valid RegistrarCheckpointRequestDTO checkpoint
) {}
