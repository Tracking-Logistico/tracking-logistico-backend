package com.udea.demo.pedidos.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SincronizarCheckpointsRequestDTO(
        @NotEmpty(message = "Debe enviar al menos un evento")
        @Size(max = 100, message = "Se pueden sincronizar máximo 100 eventos por lote")
        List<@NotNull @Valid ItemCheckpointOfflineDTO> eventos
) {}
