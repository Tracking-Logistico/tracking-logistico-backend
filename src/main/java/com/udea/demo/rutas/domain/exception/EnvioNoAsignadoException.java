package com.udea.demo.rutas.domain.exception;

public class EnvioNoAsignadoException extends RuntimeException {
    public EnvioNoAsignadoException(Long pedidoId) {
        super("El envío " + pedidoId + " no está asignado actualmente a ningún conductor");
    }
}
