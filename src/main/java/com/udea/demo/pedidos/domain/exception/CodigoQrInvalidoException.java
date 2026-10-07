package com.udea.demo.pedidos.domain.exception;

public class CodigoQrInvalidoException extends RuntimeException {
    public CodigoQrInvalidoException() {
        super("El código QR escaneado no es válido o no corresponde a un envío reconocido");
    }
}
