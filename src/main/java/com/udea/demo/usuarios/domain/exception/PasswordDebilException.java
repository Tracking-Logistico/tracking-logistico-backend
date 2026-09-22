package com.udea.demo.usuarios.domain.exception;


public class PasswordDebilException extends RuntimeException {
    public PasswordDebilException(String mensaje) {
        super(mensaje);
    }
}
