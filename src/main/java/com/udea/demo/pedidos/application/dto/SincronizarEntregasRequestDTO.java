package com.udea.demo.pedidos.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record SincronizarEntregasRequestDTO(
        @NotEmpty @Size(max = 500, message = "El lote no puede superar 500 eventos")
        List<@NotNull @Valid ResultadoEntregaOfflineDTO> eventos) {}
