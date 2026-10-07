package com.udea.demo.pedidos.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * {@code tipo} debe pertenecer al catálogo de {@code TipoIncidencia}. {@code versionEsperada} es la versión
 * del envío que el operador tenía en pantalla: si otro operador lo modificó antes, la solicitud se rechaza.
 */
public record RegistrarIncidenciaRequestDTO(
        @NotBlank(message = "El tipo de incidencia es obligatorio") @Size(max = 40) String tipo,
        @Size(max = 500, message = "El comentario admite máximo 500 caracteres") String comentario,
        Double latitud,
        Double longitud,
        Double precisionMetros,
        @PositiveOrZero Long versionEsperada
) {}
