package com.udea.demo.pedidos.domain.model;

public enum EstadoPedido {
    RECIBIDO,
    EN_VALIDACION,
    VALIDADO,
    RECHAZADO,
    EN_TRANSITO;

    // Guarda del ciclo de vida del pedido (patrón State vía enum)
    public boolean puedeTransicionarA(EstadoPedido destino) {
        return switch (this) {
            case RECIBIDO -> destino == EN_VALIDACION || destino == VALIDADO || destino == RECHAZADO;
            case EN_VALIDACION -> destino == VALIDADO || destino == RECHAZADO;
            case VALIDADO -> destino == EN_TRANSITO;
            case RECHAZADO, EN_TRANSITO -> false;
        };
    }
}
