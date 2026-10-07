package com.udea.demo.pedidos.domain.exception;

public class ConflictoConcurrenciaException extends RuntimeException {
    public ConflictoConcurrenciaException() {
        super("El envío fue modificado por otra operación. Actualiza la información y vuelve a intentar");
    }
}
