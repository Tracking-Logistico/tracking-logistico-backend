package com.udea.demo.pedidos.domain.exception;

import com.udea.demo.pedidos.domain.model.EstadoPedido;

public class TransicionEstadoInvalidaException extends RuntimeException {
    public TransicionEstadoInvalidaException(EstadoPedido origen, EstadoPedido destino) {
        super("No se puede pasar el pedido de estado " + origen + " a " + destino);
    }
}
