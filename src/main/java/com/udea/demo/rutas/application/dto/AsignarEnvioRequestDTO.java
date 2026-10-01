package com.udea.demo.rutas.application.dto;

import jakarta.validation.constraints.NotNull;

public record AsignarEnvioRequestDTO(

    @NotNull @jakarta.validation.constraints.Positive(message = "El pedido debe ser positivo")
    Long pedidoId,

    @NotNull @jakarta.validation.constraints.Positive(message = "El conductor debe ser positivo")
    Long conductorId
) {}
