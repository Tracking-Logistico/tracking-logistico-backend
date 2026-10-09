package com.udea.demo.usuarios.interfaces.services;

import com.udea.demo.usuarios.application.dto.ActualizarPreferenciaRequestDTO;
import com.udea.demo.usuarios.application.dto.PreferenciaResponseDTO;

public interface PreferenciaServiceI {

    PreferenciaResponseDTO obtener();

    PreferenciaResponseDTO actualizar(ActualizarPreferenciaRequestDTO dto);
}