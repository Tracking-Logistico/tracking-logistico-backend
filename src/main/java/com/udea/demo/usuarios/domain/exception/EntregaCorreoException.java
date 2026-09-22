package com.udea.demo.usuarios.domain.exception;

public class EntregaCorreoException extends RuntimeException {
    public EntregaCorreoException() {
        super("No fue posible enviar el correo. Intenta nuevamente más tarde.");
    }
}
