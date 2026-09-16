package com.udea.demo.usuarios.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ActualizarPerfilRequestDTO(
    @NotBlank(message = "El nombre es obligatorio")
    String nombre,

    String telefono,

    String direccion
) {}