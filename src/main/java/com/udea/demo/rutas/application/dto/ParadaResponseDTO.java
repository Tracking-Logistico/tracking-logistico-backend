package com.udea.demo.rutas.application.dto;

import java.time.LocalDateTime;

public record ParadaResponseDTO(
    Long id,
    Long pedidoId,
    Integer orden,
    String estado,
    LocalDateTime fechaAsignacion
) {}
