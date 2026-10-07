package com.udea.demo.pedidos.domain.exception;

public class ComentarioIncidenciaRequeridoException extends RuntimeException {
    public ComentarioIncidenciaRequeridoException(String tipo) {
        super("La incidencia de tipo " + tipo + " requiere un comentario u observación");
    }
}
