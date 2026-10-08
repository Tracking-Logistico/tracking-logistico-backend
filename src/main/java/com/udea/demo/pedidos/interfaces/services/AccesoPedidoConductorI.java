package com.udea.demo.pedidos.interfaces.services;
public interface AccesoPedidoConductorI {
    boolean tieneAsignacionActiva(Long pedidoId, Long usuarioId);

    /** Asignación vigente o pasada; valida eventos offline capturados mientras el envío estaba asignado. */
    boolean fueAsignado(Long pedidoId, Long usuarioId);
}
