package com.udea.demo.pedidos.application.dto;

import com.udea.demo.pedidos.domain.model.Prioridad;
import jakarta.validation.constraints.Size;

public record ValidarPedidoRequestDTO(
    Boolean aprobar,
    Prioridad prioridadConfirmada,
    @Size(max = 500) String observaciones,
    @Size(max = 500) String justificacionPrioridad,
    @Size(max = 120) String campoObservado,
    Boolean solicitarCorreccion
) {}
