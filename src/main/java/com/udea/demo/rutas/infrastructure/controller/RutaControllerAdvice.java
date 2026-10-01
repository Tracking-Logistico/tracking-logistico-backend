package com.udea.demo.rutas.infrastructure.controller;

import com.udea.demo.rutas.domain.exception.EnvioNoAsignadoException;
import com.udea.demo.rutas.domain.exception.EnvioYaAsignadoException;
import com.udea.demo.rutas.domain.exception.OrdenInvalidoException;
import com.udea.demo.rutas.domain.exception.RutaNoEncontradaException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class RutaControllerAdvice {

    @ExceptionHandler(RutaNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> manejarRutaNoEncontrada(RutaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(EnvioYaAsignadoException.class)
    public ResponseEntity<Map<String, String>> manejarEnvioYaAsignado(EnvioYaAsignadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("pedidoId", ex.getMessage()));
    }

    @ExceptionHandler(EnvioNoAsignadoException.class)
    public ResponseEntity<Map<String, String>> manejarEnvioNoAsignado(EnvioNoAsignadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("pedidoId", ex.getMessage()));
    }

    @ExceptionHandler(OrdenInvalidoException.class)
    public ResponseEntity<Map<String, String>> manejarOrdenInvalido(OrdenInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("orden", ex.getMessage()));
    }
}
