package com.udea.demo.usuarios.application.dto;

import com.udea.demo.usuarios.domain.model.CanalNotificacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarPreferenciaRequestDTO(
        @NotNull(message = "El canal es obligatorio") CanalNotificacion canal,
        @Size(max = 20) @Pattern(regexp = "^\\+?[1-9][0-9]{7,14}$",
                message = "El teléfono debe tener entre 8 y 15 dígitos y puede incluir prefijo +")
        String telefonoSms
) {}