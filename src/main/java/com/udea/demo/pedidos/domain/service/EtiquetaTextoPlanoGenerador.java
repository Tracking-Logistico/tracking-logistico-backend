package com.udea.demo.pedidos.domain.service;

import com.udea.demo.pedidos.domain.model.Pedido;
import org.springframework.stereotype.Component;

@Component
public class EtiquetaTextoPlanoGenerador extends GeneradorEtiqueta {

    @Override
    protected String encabezado(Pedido pedido) {
        return "==================== ETIQUETA DE ENVÍO ====================\n"
                + "Pedido:    " + pedido.getNumeroPedido() + "\n"
                + "Tracking:  " + pedido.getNumeroTracking() + "\n"
                + "Prioridad: " + pedido.getPrioridadConfirmada() + "\n"
                + "-------------------------------------------------------------\n";
    }

    @Override
    protected String cuerpo(Pedido pedido) {
        return "ORIGEN:      " + pedido.getDireccionOrigen() + "\n"
                + "DESTINO:     " + pedido.getDireccionDestino() + "\n"
                + "PAQUETE:     " + pedido.getDescripcionPaquete() + "\n"
                + "PESO:        " + pedido.getPesoKg() + " kg\n"
                + "DIMENSIONES: " + pedido.getLargoCm() + "x" + pedido.getAnchoCm() + "x" + pedido.getAltoCm() + " cm\n"
                + "SERVICIO:    " + pedido.getTipoServicio() + "\n"
                + "-------------------------------------------------------------\n";
    }

    @Override
    protected String pie(Pedido pedido) {
        return "Impresa: " + pedido.getFechaImpresionEtiqueta() + "\n"
                + "=============================================================\n";
    }
}
