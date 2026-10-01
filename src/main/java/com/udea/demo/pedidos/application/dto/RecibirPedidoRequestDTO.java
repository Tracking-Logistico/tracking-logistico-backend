package com.udea.demo.pedidos.application.dto;

import com.udea.demo.pedidos.domain.model.TipoServicio;
import jakarta.validation.constraints.*;

public record RecibirPedidoRequestDTO(
    @NotBlank(message = "La dirección de origen es obligatoria") @Size(min = 8, max = 250) String direccionOrigen,
    @NotBlank(message = "La ciudad de origen es obligatoria") @Size(max = 100) String ciudadOrigen,
    @Pattern(regexp = "^(|[\\p{L}0-9 -]{3,12})$", message = "El código postal de origen no es válido") String codigoPostalOrigen,
    @NotBlank(message = "La dirección de destino es obligatoria") @Size(min = 8, max = 250) String direccionDestino,
    @NotBlank(message = "La ciudad de destino es obligatoria") @Size(max = 100) String ciudadDestino,
    @Pattern(regexp = "^(|[\\p{L}0-9 -]{3,12})$", message = "El código postal de destino no es válido") String codigoPostalDestino,
    @NotBlank(message = "La descripción del paquete es obligatoria") @Size(max = 255) String descripcionPaquete,
    @NotNull @Positive @Digits(integer = 8, fraction = 2) Double pesoKg,
    @NotNull @Positive @Digits(integer = 8, fraction = 2) Double largoCm,
    @NotNull @Positive @Digits(integer = 8, fraction = 2) Double anchoCm,
    @NotNull @Positive @Digits(integer = 8, fraction = 2) Double altoCm,
    @NotNull TipoServicio tipoServicio,
    @NotBlank(message = "El nombre del destinatario es obligatorio") @Size(max = 120) String destinatarioNombre,
    @NotBlank(message = "El teléfono del destinatario es obligatorio")
    @Pattern(regexp = "^\\+?[1-9][0-9]{7,14}$", message = "El teléfono del destinatario no tiene un formato válido") String destinatarioTelefono,
    @NotBlank(message = "El teléfono del remitente es obligatorio")
    @Pattern(regexp = "^\\+?[1-9][0-9]{7,14}$", message = "El teléfono del remitente no tiene un formato válido") String remitenteTelefono
) {}
