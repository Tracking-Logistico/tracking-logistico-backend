package com.udea.demo.usuarios.application.dto;

import java.time.LocalDateTime;
import com.udea.demo.usuarios.domain.model.Rol;

public record LoginResponseDTO(
        String accessToken,
        String refreshToken,
        LocalDateTime accessTokenExpiresAt,
        LocalDateTime refreshTokenExpiresAt,
        Rol rol,
        String panel,
        Long usuarioId,
        boolean requiereCambioPassword) {
    public LoginResponseDTO(String accessToken, String refreshToken, LocalDateTime accessTokenExpiresAt,
                            LocalDateTime refreshTokenExpiresAt, Rol rol, String panel) {
        this(accessToken, refreshToken, accessTokenExpiresAt, refreshTokenExpiresAt, rol, panel, null, false);
    }
}
