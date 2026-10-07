package com.udea.demo.pedidos.domain.exception;

public class UbicacionInvalidaException extends RuntimeException {
    public UbicacionInvalidaException(String detalle) {
        super("La ubicación reportada no es válida: " + detalle);
    }
}
