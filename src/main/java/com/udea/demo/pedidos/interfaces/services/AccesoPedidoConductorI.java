package com.udea.demo.pedidos.interfaces.services;
public interface AccesoPedidoConductorI {
    boolean tieneAsignacionActiva(Long pedidoId, Long usuarioId);
}
