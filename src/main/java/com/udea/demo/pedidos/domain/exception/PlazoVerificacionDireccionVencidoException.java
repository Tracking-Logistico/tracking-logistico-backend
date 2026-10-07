package com.udea.demo.pedidos.domain.exception;

public class PlazoVerificacionDireccionVencidoException extends RuntimeException {
    public PlazoVerificacionDireccionVencidoException() {
        super("El plazo para confirmar la dirección venció; el envío se gestiona como devolución");
    }
}
