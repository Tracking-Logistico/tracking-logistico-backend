package com.udea.demo.rutas.infrastructure.controller;

import com.udea.demo.rutas.domain.exception.EnvioNoAsignadoException;
import com.udea.demo.rutas.domain.exception.EnvioYaAsignadoException;
import com.udea.demo.rutas.domain.exception.OrdenInvalidoException;
import com.udea.demo.rutas.domain.exception.RutaNoEncontradaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RutaControllerAdvice - manejo de excepciones (HU-09)")
class RutaControllerAdviceTest {

    private final RutaControllerAdvice advice = new RutaControllerAdvice();

    @Test
    @DisplayName("manejarRutaNoEncontrada() retorna 404 NOT_FOUND con clave 'error'")
    void manejarRutaNoEncontrada_retorna404() {

        RutaNoEncontradaException ex = RutaNoEncontradaException.porId(100L);

        ResponseEntity<Map<String, String>> respuesta = advice.manejarRutaNoEncontrada(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody()).containsKey("error");
        assertThat(respuesta.getBody().get("error")).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("manejarEnvioYaAsignado() retorna 409 CONFLICT con clave 'pedidoId'")
    void manejarEnvioYaAsignado_retorna409() {

        EnvioYaAsignadoException ex = new EnvioYaAsignadoException(10L);

        ResponseEntity<Map<String, String>> respuesta = advice.manejarEnvioYaAsignado(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody()).containsKey("pedidoId");
        assertThat(respuesta.getBody().get("pedidoId")).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("manejarEnvioNoAsignado() retorna 409 CONFLICT con clave 'pedidoId'")
    void manejarEnvioNoAsignado_retorna409() {

        EnvioNoAsignadoException ex = new EnvioNoAsignadoException(10L);

        ResponseEntity<Map<String, String>> respuesta = advice.manejarEnvioNoAsignado(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody()).containsKey("pedidoId");
        assertThat(respuesta.getBody().get("pedidoId")).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("manejarOrdenInvalido() retorna 400 BAD_REQUEST con clave 'orden'")
    void manejarOrdenInvalido_retorna400() {

        OrdenInvalidoException ex = new OrdenInvalidoException();

        ResponseEntity<Map<String, String>> respuesta = advice.manejarOrdenInvalido(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody()).containsKey("orden");
        assertThat(respuesta.getBody().get("orden")).isEqualTo(ex.getMessage());
    }
}
