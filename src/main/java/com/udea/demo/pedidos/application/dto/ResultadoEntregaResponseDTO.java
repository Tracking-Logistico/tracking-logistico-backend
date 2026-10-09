package com.udea.demo.pedidos.application.dto;

import com.udea.demo.pedidos.domain.model.ResultadoEntrega;
import java.time.LocalDateTime;

public record ResultadoEntregaResponseDTO(String idEventoCliente, Long pedidoId, ResultadoEntrega resultado,
                                          String estado, LocalDateTime fechaEvento, boolean duplicado) {}
