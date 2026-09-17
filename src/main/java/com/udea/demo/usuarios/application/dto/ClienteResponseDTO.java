package com.udea.demo.usuarios.application.dto;

import java.time.LocalDateTime;

import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.Rol;

public record ClienteResponseDTO(
    Long idCliente,
    String nombre,
    String email,
    String telefono,
    String direccion,
    String ciudad,
    Rol rol,
    EstadoUsuario estado,
    LocalDateTime fechaCreacion
) {}
