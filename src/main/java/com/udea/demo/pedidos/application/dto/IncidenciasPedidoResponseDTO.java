package com.udea.demo.pedidos.application.dto;

import com.udea.demo.pedidos.domain.model.EstadoPedido;

import java.util.List;

public record IncidenciasPedidoResponseDTO(Long pedidoId, EstadoPedido estado, Long versionPedido,
                                           int intentosEntregaFallidos, List<IncidenciaResponseDTO> incidencias) {}
