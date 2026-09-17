package com.udea.demo.usuarios.application.dto;


import com.udea.demo.usuarios.domain.model.Rol;
import jakarta.validation.constraints.*;

public record CrearUsuarioInternoCommand(

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    String nombre,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 150, message = "El correo no puede superar 150 caracteres")
    String email,

    @Pattern(regexp = "^[0-9+\\- ]{7,20}$",
             message = "El teléfono no tiene un formato válido")
    String telefono,

    @NotNull(message = "El rol es obligatorio")
    Rol rol,

    @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
    String direccion,

    // Solo aplica si rol = CONDUCTOR
    @Size(max = 50, message = "La licencia no puede superar 50 caracteres")
    String licencia,

    // Solo aplica si rol = OPERADOR
    @Size(max = 50, message = "El código de empleado no puede superar 50 caracteres")
    String codigoEmpleado
) {}
