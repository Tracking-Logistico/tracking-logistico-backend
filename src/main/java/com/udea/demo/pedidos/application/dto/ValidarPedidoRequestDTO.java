package com.udea.demo.pedidos.application.dto;

import com.udea.demo.pedidos.domain.model.Prioridad;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ValidarPedidoRequestDTO(

    @NotNull(message = "El operador es obligatorio")
    Long operadorId,

    @NotNull(message = "Debe indicar si aprueba o rechaza el pedido")
    Boolean aprobar,

    // Opcional: si es null se conserva la prioridad sugerida por el sistema
    Prioridad prioridadConfirmada,

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    String observaciones
) {}
