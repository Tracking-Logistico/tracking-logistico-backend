package com.udea.demo.rutas.application.dto;
import java.time.LocalDateTime;
public record NotificacionRutaDTO(Long id, Long pedidoId, String mensaje, LocalDateTime fecha, boolean leida) {}
