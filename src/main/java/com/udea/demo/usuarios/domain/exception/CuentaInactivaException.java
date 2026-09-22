package com.udea.demo.usuarios.domain.exception;


public class CuentaInactivaException extends RuntimeException {
    public CuentaInactivaException() {
        super("La cuenta se encuentra inactiva. Contacte al administrador.");
    }
}