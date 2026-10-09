package com.udea.demo.rutas.application.dto;

import java.time.LocalDate;

public record ProgresoPanelDTO(
        LocalDate fecha,
        int totalEntregas,
        int entregadas,
        int pendientes,
        int fallidas,
        int canceladas
) {}