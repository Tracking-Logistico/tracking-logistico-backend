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

/**
 * Pruebas unitarias para RutaControllerAdvice (HU-09: Gestión y Asignación de Rutas).
 *
 * Patrón AAA (Arrange - Act - Assert).
 */
@DisplayName("RutaControllerAdvice - manejo de excepciones (HU-09)")
class RutaControllerAdviceTest {

    private final RutaControllerAdvice advice = new RutaControllerAdvice();

    @Test
    @DisplayName("manejarRutaNoEncontrada() retorna 404 NOT_FOUND con clave 'error'")
    void manejarRutaNoEncontrada_retorna404() {
        // Arrange
        RutaNoEncontradaException ex = RutaNoEncontradaException.porId(100L);

        // Act
        ResponseEntity<Map<String, String>> respuesta = advice.manejarRutaNoEncontrada(ex);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody()).containsKey("error");
        assertThat(respuesta.getBody().get("error")).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("manejarEnvioYaAsignado() retorna 409 CONFLICT con clave 'pedidoId'")
    void manejarEnvioYaAsignado_retorna409() {
        // Arrange
        EnvioYaAsignadoException ex = new EnvioYaAsignadoException(10L);

        // Act
        ResponseEntity<Map<String, String>> respuesta = advice.manejarEnvioYaAsignado(ex);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody()).containsKey("pedidoId");
        assertThat(respuesta.getBody().get("pedidoId")).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("manejarEnvioNoAsignado() retorna 409 CONFLICT con clave 'pedidoId'")
    void manejarEnvioNoAsignado_retorna409() {
        // Arrange
        EnvioNoAsignadoException ex = new EnvioNoAsignadoException(10L);

        // Act
        ResponseEntity<Map<String, String>> respuesta = advice.manejarEnvioNoAsignado(ex);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody()).containsKey("pedidoId");
        assertThat(respuesta.getBody().get("pedidoId")).isEqualTo(ex.getMessage());
    }

    @Test
    @DisplayName("manejarOrdenInvalido() retorna 400 BAD_REQUEST con clave 'orden'")
    void manejarOrdenInvalido_retorna400() {
        // Arrange
        OrdenInvalidoException ex = new OrdenInvalidoException();

        // Act
        ResponseEntity<Map<String, String>> respuesta = advice.manejarOrdenInvalido(ex);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody()).containsKey("orden");
        assertThat(respuesta.getBody().get("orden")).isEqualTo(ex.getMessage());
    }
}
