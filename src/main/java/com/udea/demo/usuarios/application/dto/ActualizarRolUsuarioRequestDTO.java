package com.udea.demo.usuarios.application.dto;

import com.udea.demo.usuarios.domain.model.Rol;
import jakarta.validation.constraints.NotNull;

public record ActualizarRolUsuarioRequestDTO(
        @NotNull(message = "El rol es obligatorio") Rol rol,
        String licencia,
        String codigoEmpleado
) {}
