package com.udea.demo.rutas.application.dto;
import java.time.LocalDateTime;
public record HistorialAsignacionDTO(Long id, Long pedidoId, Long conductorAnteriorId,
    Long conductorNuevoId, Long operadorId, String accion, String motivo, LocalDateTime fecha) {}
