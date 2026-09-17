package com.udea.demo.rutas.domain.exception;

public class EnvioYaAsignadoException extends RuntimeException {
    public EnvioYaAsignadoException(Long pedidoId) {
        super("El envío " + pedidoId + " ya está asignado a un conductor");
    }
}
