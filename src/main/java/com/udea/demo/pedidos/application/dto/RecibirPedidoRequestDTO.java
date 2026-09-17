package com.udea.demo.pedidos.application.dto;

import com.udea.demo.pedidos.domain.model.TipoServicio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RecibirPedidoRequestDTO(

    @NotNull(message = "El cliente es obligatorio")
    Long clienteId,

    @NotBlank(message = "La dirección de origen es obligatoria")
    @Size(max = 250, message = "La dirección de origen no puede superar 250 caracteres")
    String direccionOrigen,

    @NotBlank(message = "La dirección de destino es obligatoria")
    @Size(max = 250, message = "La dirección de destino no puede superar 250 caracteres")
    String direccionDestino,

    @NotBlank(message = "La descripción del paquete es obligatoria")
    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    String descripcionPaquete,

    @NotNull(message = "El peso es obligatorio")
    @Positive(message = "El peso debe ser mayor a 0")
    Double pesoKg,

    @NotNull(message = "El largo es obligatorio")
    @Positive(message = "El largo debe ser mayor a 0")
    Double largoCm,

    @NotNull(message = "El ancho es obligatorio")
    @Positive(message = "El ancho debe ser mayor a 0")
    Double anchoCm,

    @NotNull(message = "El alto es obligatorio")
    @Positive(message = "El alto debe ser mayor a 0")
    Double altoCm,

    @NotNull(message = "El tipo de servicio es obligatorio")
    TipoServicio tipoServicio
) {}
