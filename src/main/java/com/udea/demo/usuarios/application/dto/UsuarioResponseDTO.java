package com.udea.demo.usuarios.application.dto;

import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.Rol;

import java.time.LocalDateTime;

public record UsuarioResponseDTO(
    Long id,
    String nombre,
    String email,
    String telefono,
    String direccion,
    Rol rol,
    EstadoUsuario estado,
    LocalDateTime fechaCreacion
) {}