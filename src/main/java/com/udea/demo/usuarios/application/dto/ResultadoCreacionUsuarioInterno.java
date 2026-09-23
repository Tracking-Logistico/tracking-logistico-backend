package com.udea.demo.usuarios.application.dto;

import com.udea.demo.usuarios.domain.model.Rol;

public record ResultadoCreacionUsuarioInterno(
    Long id,
    String email,
    Rol rol,
    String passwordTemporal
) {}
