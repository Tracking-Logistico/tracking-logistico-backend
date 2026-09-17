package com.udea.demo.rutas.application.dto;

import jakarta.validation.constraints.NotNull;

public record AsignarEnvioRequestDTO(

    @NotNull(message = "El envío es obligatorio")
    Long pedidoId,

    @NotNull(message = "El conductor es obligatorio")
    Long conductorId
) {}
