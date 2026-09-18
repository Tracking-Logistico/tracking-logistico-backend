package com.udea.demo.usuarios.interfaces.services;

import com.udea.demo.usuarios.application.dto.LoginRequestDTO;
import com.udea.demo.usuarios.application.dto.LoginResponseDTO;
import com.udea.demo.usuarios.application.dto.RefreshTokenRequestDTO;
import com.udea.demo.usuarios.application.dto.RestablecerPasswordDTO;
import com.udea.demo.usuarios.application.dto.SolicitarRestablecimientoPasswordDTO;

public interface AutenticacionServiceI {
    LoginResponseDTO iniciarSesion(LoginRequestDTO request, String ip, String userAgent);
    LoginResponseDTO renovarSesion(RefreshTokenRequestDTO request);
    void cerrarSesion(String accessToken);
    void solicitarRestablecimiento(SolicitarRestablecimientoPasswordDTO request);
    void restablecerPassword(RestablecerPasswordDTO request);
}