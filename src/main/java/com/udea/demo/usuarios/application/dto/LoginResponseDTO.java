package com.udea.demo.usuarios.application.dto;

import java.time.LocalDateTime;

import com.udea.demo.usuarios.domain.model.Rol;

public record LoginResponseDTO(
        String accessToken,
        String refreshToken,
        LocalDateTime accessTokenExpiresAt,
        LocalDateTime refreshTokenExpiresAt,
        Rol rol,
        String panel) {
}