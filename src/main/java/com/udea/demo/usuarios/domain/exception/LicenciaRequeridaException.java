package com.udea.demo.usuarios.domain.exception;


public class LicenciaRequeridaException extends RuntimeException {
    public LicenciaRequeridaException() {
        super("La licencia es obligatoria para usuarios con rol CONDUCTOR");
    }
}