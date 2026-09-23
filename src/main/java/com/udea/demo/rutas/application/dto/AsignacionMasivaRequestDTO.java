package com.udea.demo.rutas.application.dto;
import jakarta.validation.constraints.*;
import java.util.List;
public record AsignacionMasivaRequestDTO(
    @NotNull @Positive Long conductorId,
    @NotEmpty @Size(max = 100) List<@NotNull @Positive Long> pedidoIds
) {}
