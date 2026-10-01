package com.udea.demo.rutas.application.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
public record ReasignarEnvioRequestDTO(
    @NotNull @Positive Long pedidoId,
    @NotNull @Positive Long nuevoConductorId,
    @Size(max = 500) String motivo
) {
    public ReasignarEnvioRequestDTO(Long pedidoId, Long nuevoConductorId) {
        this(pedidoId, nuevoConductorId, null);
    }
}
