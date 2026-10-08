package com.udea.demo.pedidos.application.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReprogramarEntregaRequestDTO(@NotNull(message = "La nueva fecha de entrega es obligatoria") LocalDate fecha) {}
