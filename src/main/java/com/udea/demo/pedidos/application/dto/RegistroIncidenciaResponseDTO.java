package com.udea.demo.pedidos.application.dto;

import com.udea.demo.pedidos.domain.model.EstadoPedido;

/** Incidencia registrada y estado del envío tras aplicarla, con la versión para la siguiente operación. */
public record RegistroIncidenciaResponseDTO(IncidenciaResponseDTO incidencia, EstadoPedido estadoPedido,
                                            Long versionPedido, int intentosEntregaFallidos) {}
