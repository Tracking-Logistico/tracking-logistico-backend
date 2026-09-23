package com.udea.demo.usuarios.application.dto;

import jakarta.validation.constraints.*;

public record RegistroClienteRequestDTO(
    @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
    @NotBlank(message = "El email es obligatorio") @Email(message = "El formato de email no es válido") String email,
    @NotBlank(message = "La contraseña es obligatoria") @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9]).*$",
             message = "La contraseña debe contener al menos una mayúscula, una minúscula, un número y un carácter especial")
    String password,
    @NotBlank(message = "La confirmación de contraseña es obligatoria") String confirmarPassword,
    @Pattern(regexp = "^$|^\\+?[1-9][0-9]{7,14}$", message = "El teléfono debe usar un formato internacional válido") String telefono,
    @Size(max = 200, message = "La dirección no puede superar 200 caracteres") String direccion,
    @Size(max = 100, message = "La ciudad no puede superar 100 caracteres") String ciudad,
    @NotNull @AssertTrue(message = "Debe aceptar los términos y condiciones") Boolean aceptoTerminos,
    @NotBlank(message = "La versión de términos es obligatoria") String versionTerminos,
    @NotNull @AssertTrue(message = "Debe aceptar la política de tratamiento de datos") Boolean aceptoPoliticaDatos,
    @NotBlank(message = "La versión de la política de datos es obligatoria") String versionPoliticaDatos
) {
    public RegistroClienteRequestDTO(String nombre, String email, String password, String confirmarPassword,
                                     String telefono, String direccion, String ciudad,
                                     Boolean aceptoTerminos, String versionTerminos) {
        this(nombre, email, password, confirmarPassword, telefono, direccion, ciudad,
                aceptoTerminos, versionTerminos, true, versionTerminos);
    }
}
