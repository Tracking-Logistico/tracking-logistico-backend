package com.udea.demo.pedidos.domain.service;

import com.udea.demo.pedidos.domain.model.Pedido;

public interface GeneradorEtiqueta {
    String generar(Pedido pedido);
}
