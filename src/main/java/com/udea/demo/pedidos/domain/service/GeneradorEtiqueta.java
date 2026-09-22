package com.udea.demo.pedidos.domain.service;

import com.udea.demo.pedidos.domain.model.Pedido;

// Template Method: fija el orden de armado de la etiqueta; las subclases definen cada sección
public abstract class GeneradorEtiqueta {

    public final String generar(Pedido pedido) {
        StringBuilder etiqueta = new StringBuilder();
        etiqueta.append(encabezado(pedido));
        etiqueta.append(cuerpo(pedido));
        etiqueta.append(pie(pedido));
        return etiqueta.toString();
    }

    protected abstract String encabezado(Pedido pedido);

    protected abstract String cuerpo(Pedido pedido);

    protected abstract String pie(Pedido pedido);
}
