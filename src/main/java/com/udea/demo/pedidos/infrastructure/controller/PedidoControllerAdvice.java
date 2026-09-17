package com.udea.demo.pedidos.infrastructure.controller;

import com.udea.demo.pedidos.domain.exception.PedidoNoEncontradoException;
import com.udea.demo.pedidos.domain.exception.TrackingNoActivoException;
import com.udea.demo.pedidos.domain.exception.TransicionEstadoInvalidaException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class PedidoControllerAdvice {

    @ExceptionHandler(PedidoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> manejarPedidoNoEncontrado(PedidoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(TransicionEstadoInvalidaException.class)
    public ResponseEntity<Map<String, String>> manejarTransicionInvalida(TransicionEstadoInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("estado", ex.getMessage()));
    }

    @ExceptionHandler(TrackingNoActivoException.class)
    public ResponseEntity<Map<String, String>> manejarTrackingNoActivo(TrackingNoActivoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("tracking", ex.getMessage()));
    }
}
