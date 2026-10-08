package com.udea.demo.pedidos.domain.exception;

public class FechaDispositivoInvalidaException extends RuntimeException {
    public FechaDispositivoInvalidaException() {
        super("La fecha del dispositivo no puede ser posterior a la hora actual");
    }
}
