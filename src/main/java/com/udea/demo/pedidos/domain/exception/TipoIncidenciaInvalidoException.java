package com.udea.demo.pedidos.domain.exception;

public class TipoIncidenciaInvalidoException extends RuntimeException {
    public TipoIncidenciaInvalidoException(String codigo) {
        super("El tipo de incidencia '" + codigo + "' no pertenece al catálogo de incidencias válidas");
    }
}
