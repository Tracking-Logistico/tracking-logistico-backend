package com.udea.demo.pedidos.domain.exception;

public class TrackingNoActivoException extends RuntimeException {
    public TrackingNoActivoException(Long pedidoId) {
        super("El pedido " + pedidoId + " no tiene tracking activo; actívelo antes de generar la etiqueta");
    }
}
