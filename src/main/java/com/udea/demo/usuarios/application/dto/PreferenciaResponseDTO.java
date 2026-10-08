package com.udea.demo.usuarios.application.dto;

import com.udea.demo.usuarios.domain.model.CanalNotificacion;
import java.time.LocalDateTime;

public record PreferenciaResponseDTO(
        CanalNotificacion canal,
        String telefonoSms,
        LocalDateTime fechaActualizacion
) {}
