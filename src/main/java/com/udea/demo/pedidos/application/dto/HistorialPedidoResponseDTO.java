package com.udea.demo.pedidos.application.dto;

import java.time.LocalDateTime;

public record HistorialPedidoResponseDTO(
        Long id,
        Long usuarioId,
        String tipoEvento,
        String campoObservado,
        String detalle,
        LocalDateTime fecha
) {}
