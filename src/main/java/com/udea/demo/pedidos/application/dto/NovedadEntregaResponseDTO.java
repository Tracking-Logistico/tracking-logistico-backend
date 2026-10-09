package com.udea.demo.pedidos.application.dto;

import com.udea.demo.pedidos.domain.model.ResultadoEntrega;

public record NovedadEntregaResponseDTO(String codigo, ResultadoEntrega resultado, String descripcion) {}
