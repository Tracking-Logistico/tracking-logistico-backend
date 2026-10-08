package com.udea.demo.pedidos.domain.exception;

import com.udea.demo.pedidos.domain.model.EstadoPedido;

public class EnvioFinalizadoException extends RuntimeException {
    public EnvioFinalizadoException(EstadoPedido estado) {
        super("El envío está finalizado (" + estado + ") y no puede modificar su estado");
    }
}
