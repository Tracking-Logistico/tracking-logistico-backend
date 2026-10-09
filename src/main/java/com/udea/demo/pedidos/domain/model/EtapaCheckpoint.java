package com.udea.demo.pedidos.domain.model;

/** Etapa del recorrido declarada por el conductor al escanear; determina el estado logístico esperado. */
public enum EtapaCheckpoint {
    RECIBIDO_EN_ORIGEN(EstadoPedido.RECIBIDO_EN_ORIGEN),
    EN_TRANSITO(EstadoPedido.EN_TRANSITO),
    EN_REPARTO(EstadoPedido.EN_REPARTO),
    ENTREGADO(EstadoPedido.ENTREGADO); 

    private final EstadoPedido estado;

    EtapaCheckpoint(EstadoPedido estado) { this.estado = estado; }

    public EstadoPedido estado() { return estado; }
}
