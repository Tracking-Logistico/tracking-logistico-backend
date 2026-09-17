package com.udea.demo.rutas.application.dto;

import java.time.LocalDate;
import java.util.List;

public record RutaResponseDTO(
    Long id,
    Long conductorId,
    LocalDate fecha,
    List<ParadaResponseDTO> paradas
) {}
