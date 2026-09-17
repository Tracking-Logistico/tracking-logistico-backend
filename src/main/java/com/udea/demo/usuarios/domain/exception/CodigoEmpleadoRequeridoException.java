package com.udea.demo.usuarios.domain.exception;


public class CodigoEmpleadoRequeridoException extends RuntimeException {
    public CodigoEmpleadoRequeridoException() {
        super("El código de empleado es obligatorio para usuarios con rol OPERADOR");
    }
}
