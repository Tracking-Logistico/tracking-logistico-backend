package com.udea.demo.rutas.application.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReordenarRutaRequestDTO(

    @NotEmpty(message = "Debe indicar el orden de los envíos de la ruta")
    List<Long> pedidoIdsEnOrden
) {}
