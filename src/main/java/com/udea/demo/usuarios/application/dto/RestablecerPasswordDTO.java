package com.udea.demo.usuarios.application.dto;

import jakarta.validation.constraints.NotBlank;

public record RestablecerPasswordDTO(
        @NotBlank String token,
        @NotBlank String nuevaPassword,
        @NotBlank String confirmarPassword) {
}