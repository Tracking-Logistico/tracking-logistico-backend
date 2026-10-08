package com.udea.demo.pedidos.application.dto;

import java.time.LocalDateTime;

/** Última novedad del envío en lenguaje orientado al cliente. */
public record NovedadClienteDTO(String titulo, String mensaje, LocalDateTime fecha) {}
