package com.udea.demo.rutas.domain.exception;

public class OrdenInvalidoException extends RuntimeException {
    public OrdenInvalidoException() {
        super("El nuevo orden debe incluir exactamente los mismos envíos que están pendientes en la ruta");
    }
}
