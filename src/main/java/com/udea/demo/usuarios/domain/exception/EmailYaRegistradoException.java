package com.udea.demo.usuarios.domain.exception;

public class EmailYaRegistradoException extends RuntimeException {
    public EmailYaRegistradoException(String email) {
        super("El correo ya se encuentra registrado: " + email);
    }
}
