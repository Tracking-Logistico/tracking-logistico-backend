package com.udea.demo.pedidos.domain.exception;

public class PedidoNoEncontradoException extends RuntimeException {
    public PedidoNoEncontradoException(Long id) {
        super("Pedido no encontrado con ID: " + id);
    }
}
