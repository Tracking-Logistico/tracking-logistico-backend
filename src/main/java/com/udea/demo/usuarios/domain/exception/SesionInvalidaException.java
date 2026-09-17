package com.udea.demo.usuarios.domain.exception;

public class SesionInvalidaException extends RuntimeException {
    public SesionInvalidaException() {
        super("La sesión no es válida o ha expirado");
    }
}