package com.udea.demo.usuarios.interfaces.services;

import com.udea.demo.usuarios.application.dto.ActualizarPerfilRequestDTO;
import com.udea.demo.usuarios.application.dto.ClienteResponseDTO;
import com.udea.demo.usuarios.application.dto.RegistroClienteRequestDTO;
import com.udea.demo.usuarios.application.dto.UsuarioResponseDTO;

public interface ClienteServiceI {
    ClienteResponseDTO registrarCliente(RegistroClienteRequestDTO dto);
    UsuarioResponseDTO actualizarPerfil(Long id, ActualizarPerfilRequestDTO dto);
    void desactivarCuentaCliente(Long id);
    void verificarCuenta(String token);
}
