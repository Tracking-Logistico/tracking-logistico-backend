package com.udea.demo.pedidos.application.dto;

import java.time.LocalDate;

/** Fechas disponibles (inclusive) para reprogramar la entrega. */
public record RangoReprogramacionResponseDTO(LocalDate desde, LocalDate hasta) {}
