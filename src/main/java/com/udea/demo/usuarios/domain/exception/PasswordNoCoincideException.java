package com.udea.demo.usuarios.domain.exception;

public class PasswordNoCoincideException extends RuntimeException {
    public PasswordNoCoincideException() {
        super("Las contraseñas no coinciden");
    }
}