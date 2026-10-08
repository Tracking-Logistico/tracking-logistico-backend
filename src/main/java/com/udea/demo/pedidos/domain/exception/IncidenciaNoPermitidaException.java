package com.udea.demo.pedidos.domain.exception;

public class IncidenciaNoPermitidaException extends RuntimeException {
    public IncidenciaNoPermitidaException(String motivo) {
        super(motivo);
    }
}
