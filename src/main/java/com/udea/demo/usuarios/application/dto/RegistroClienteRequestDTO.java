package com.udea.demo.usuarios.application.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegistroClienteRequestDTO(
    @NotBlank(message = "El nombre es obligatorio")
    String nombre,

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato de email no es válido")
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    String password,

    String telefono,
    String direccion,

    @NotNull(message = "Debe especificar la aceptación de términos")
    @AssertTrue(message = "Debe aceptar los términos y condiciones para registrarse")
    Boolean aceptoTerminos,

    @NotBlank(message = "La versión de términos es obligatoria")
    String versionTerminos
) {}