package com.udea.demo.pedidos.domain.model;

public enum EstadoPedido {
    SOLICITADO,
    CORRECCION_SOLICITADA,
    CREADO,
    RECIBIDO_EN_ORIGEN,
    EN_TRANSITO,
    EN_REPARTO,
    ENTREGADO,
    RECHAZADO,

    RECIBIDO,
    EN_VALIDACION,
    VALIDADO;

    public boolean puedeValidarse() {
        return this == SOLICITADO || this == CORRECCION_SOLICITADA || this == RECIBIDO || this == EN_VALIDACION;
    }

    public boolean puedeActivarTracking() {
        return this == SOLICITADO || this == VALIDADO;
    }
}
