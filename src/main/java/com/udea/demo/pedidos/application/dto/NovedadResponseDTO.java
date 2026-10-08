package com.udea.demo.pedidos.application.dto;

import java.time.LocalDateTime;

/** Entrada del panel de novedades del operador (checkpoints, cambios de estado e incidencias). */
public record NovedadResponseDTO(Long id, Long pedidoId, Long usuarioId, String tipoEvento, String detalle,
                                 LocalDateTime fecha) {}
