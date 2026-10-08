package com.udea.demo.pedidos.domain.exception;

import com.udea.demo.pedidos.domain.model.RangoReprogramacion;

public class FechaReprogramacionInvalidaException extends RuntimeException {
    public FechaReprogramacionInvalidaException(RangoReprogramacion rango) {
        super(rango.vacio()
                ? "El plazo de retención del paquete en bodega venció; no es posible reprogramar la entrega"
                : "La fecha de entrega debe estar entre " + rango.desde() + " y " + rango.hasta());
    }
}
