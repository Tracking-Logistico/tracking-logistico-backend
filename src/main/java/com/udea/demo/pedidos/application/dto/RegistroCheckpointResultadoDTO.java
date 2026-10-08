package com.udea.demo.pedidos.application.dto;

/** Resultado de registrar un checkpoint; {@code duplicado} indica un reenvío ya procesado (idempotencia). */
public record RegistroCheckpointResultadoDTO(CheckpointResponseDTO checkpoint, boolean duplicado) {}
