package com.udea.demo.pedidos.application.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Campos vacíos confirman la dirección actual; los informados la corrigen. */
public record ConfirmarDireccionRequestDTO(
        @Size(min = 8, max = 250, message = "La dirección debe tener entre 8 y 250 caracteres") String direccionDestino,
        @Size(max = 100) String ciudadDestino,
        @Pattern(regexp = "^(|[\\p{L}0-9 -]{3,12})$", message = "El código postal de destino no es válido")
        String codigoPostalDestino
) {}
