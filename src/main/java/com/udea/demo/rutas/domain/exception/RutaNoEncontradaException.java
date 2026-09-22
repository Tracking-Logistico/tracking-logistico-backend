package com.udea.demo.rutas.domain.exception;

public class RutaNoEncontradaException extends RuntimeException {

    private RutaNoEncontradaException(String mensaje) {
        super(mensaje);
    }

    public static RutaNoEncontradaException porId(Long id) {
        return new RutaNoEncontradaException("Ruta no encontrada con ID: " + id);
    }

    public static RutaNoEncontradaException paraConductor(Long conductorId) {
        return new RutaNoEncontradaException("No hay ruta activa para el conductor con ID: " + conductorId);
    }
}
