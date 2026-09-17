package com.udea.demo.usuarios.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitarRestablecimientoPasswordDTO(@NotBlank @Email String email) {
}