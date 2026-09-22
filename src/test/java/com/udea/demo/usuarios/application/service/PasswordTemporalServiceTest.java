package com.udea.demo.usuarios.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordTemporalService - HU-01B generación de clave temporal")
class PasswordTemporalServiceTest {

    private final PasswordTemporalService service = new PasswordTemporalService();

    @Test
    @DisplayName("generar() produce 16 caracteres con mayúscula, minúscula, dígito y especial")
    void generar_cumplePoliticaYLongitud() {
        // Act
        String password = service.generar();

        // Assert
        assertThat(password).hasSize(16);
        assertThat(password).matches(".*[A-Z].*");
        assertThat(password).matches(".*[a-z].*");
        assertThat(password).matches(".*[0-9].*");
        assertThat(password).matches(".*[@#$%^&+=!._-].*");
    }

    @Test
    @DisplayName("generar() produce valores distintos en invocaciones sucesivas")
    void generar_noEsDeterminista() {
        // Act
        Set<String> generadas = new HashSet<>();
        for (int i = 0; i < 8; i++) {
            generadas.add(service.generar());
        }

        // Assert
        assertThat(generadas).hasSizeGreaterThan(1);
        assertThat(generadas).allMatch(p -> p.length() == 16);
    }
}
