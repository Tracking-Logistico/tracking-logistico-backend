package com.udea.demo.usuarios.interfaces.services;

import com.udea.demo.usuarios.application.dto.*;

public interface UsuarioInternoServiceI {

    ResultadoCreacionUsuarioInterno crear(CrearUsuarioInternoCommand command);

    UsuarioResponseDTO editar(Long id, EditarUsuarioInternoCommand command);

    void desactivar(Long id);

    void cambiarPassword(Long id, String passwordActual,
                         String nuevaPassword, String confirmarPassword);
}
