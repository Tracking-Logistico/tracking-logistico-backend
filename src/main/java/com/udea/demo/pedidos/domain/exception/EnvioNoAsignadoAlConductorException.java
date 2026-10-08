package com.udea.demo.pedidos.domain.exception;

public class EnvioNoAsignadoAlConductorException extends RuntimeException {
    public EnvioNoAsignadoAlConductorException(Long pedidoId) {
        super("El envío " + pedidoId + " no está asignado al conductor autenticado");
    }
}
