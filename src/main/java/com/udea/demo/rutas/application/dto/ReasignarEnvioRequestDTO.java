package com.udea.demo.rutas.application.dto;

import jakarta.validation.constraints.NotNull;

public record ReasignarEnvioRequestDTO(

    @NotNull(message = "El envío es obligatorio")
    Long pedidoId,

    @NotNull(message = "El nuevo conductor es obligatorio")
    Long nuevoConductorId
) {}
