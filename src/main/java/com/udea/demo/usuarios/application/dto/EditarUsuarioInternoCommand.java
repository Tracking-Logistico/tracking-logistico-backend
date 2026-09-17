package com.udea.demo.usuarios.application.dto;


import com.udea.demo.usuarios.domain.model.Rol;
import jakarta.validation.constraints.*;

public record EditarUsuarioInternoCommand(

    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    String nombre,

    @Pattern(regexp = "^[0-9+\\- ]{7,20}$",
             message = "El teléfono no tiene un formato válido")
    String telefono,

    @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
    String direccion,

    Rol rol,  // opcional, solo OPERADOR o CONDUCTOR

    @Size(max = 50, message = "La licencia no puede superar 50 caracteres")
    String licencia,

    @Size(max = 50, message = "El código de empleado no puede superar 50 caracteres")
    String codigoEmpleado
) {}