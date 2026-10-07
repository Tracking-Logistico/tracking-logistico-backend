package com.udea.demo.pedidos.domain.exception;

public class CodigoQrOtroEnvioException extends RuntimeException {
    public CodigoQrOtroEnvioException(Long pedidoEsperadoId) {
        super("El código QR escaneado corresponde a otro envío, no al envío " + pedidoEsperadoId);
    }
}
