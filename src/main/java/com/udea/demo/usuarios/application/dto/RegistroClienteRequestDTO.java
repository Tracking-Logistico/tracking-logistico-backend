package com.udea.demo.usuarios.application.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroClienteRequestDTO(
    @NotBlank(message = "El nombre es obligatorio")
    String nombre,

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato de email no es válido")
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._-]).*$",
        message = "La contraseña debe contener al menos una mayúscula, una minúscula, un número y un carácter especial"
    )
    String password,

    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    String confirmarPassword,

    String telefono,
    String direccion,
    String ciudad,

    @NotNull(message = "Debe especificar la aceptación de términos")
    @AssertTrue(message = "Debe aceptar los términos y condiciones para registrarse")
    Boolean aceptoTerminos,

    @NotBlank(message = "La versión de términos es obligatoria")
    String versionTerminos
) {}