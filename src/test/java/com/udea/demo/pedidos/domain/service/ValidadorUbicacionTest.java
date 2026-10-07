package com.udea.demo.pedidos.domain.service;

import com.udea.demo.pedidos.domain.exception.UbicacionInvalidaException;
import com.udea.demo.pedidos.domain.model.UbicacionReportada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HU-06 ValidadorUbicacion - criterio 5")
class ValidadorUbicacionTest {
    private final ValidadorUbicacion validador = new ValidadorUbicacion(100);

    @Test
    @DisplayName("GPS válido con precisión dentro del margen es confiable")
    void gpsValido() {
        UbicacionReportada u = validador.evaluar(6.2442, -75.5812, 12.0);

        assertThat(u.confiable()).isTrue();
        assertThat(u.latitud()).isEqualTo(6.2442);
        assertThat(u.longitud()).isEqualTo(-75.5812);
    }

    @Test
    @DisplayName("Precisión exactamente en el límite sigue siendo confiable")
    void precisionEnLimite() {
        assertThat(validador.evaluar(6.0, -75.0, 100.0).confiable()).isTrue();
    }

    @Test
    @DisplayName("Precisión insuficiente marca la ubicación como no confiable")
    void precisionInsuficiente() {
        assertThat(validador.evaluar(6.0, -75.0, 350.0).confiable()).isFalse();
    }

    @Test
    @DisplayName("Coordenadas nulas se aceptan pero como ubicación no confiable")
    void coordenadasNulas() {
        UbicacionReportada u = validador.evaluar(null, null, null);

        assertThat(u.confiable()).isFalse();
        assertThat(u.latitud()).isNull();
    }

    @Test
    @DisplayName("Coordenadas sin precisión declarada no son confiables")
    void sinPrecision() {
        assertThat(validador.evaluar(6.0, -75.0, null).confiable()).isFalse();
    }

    @ParameterizedTest
    @CsvSource(value = {"91,0,5", "-90.5,0,5", "0,181,5", "0,-180.1,5", "6,NULL,5", "NULL,-75,5", "6,-75,-1"},
            nullValues = "NULL")
    @DisplayName("Coordenadas imposibles, incompletas o precisión negativa se rechazan")
    void gpsInvalido(Double latitud, Double longitud, Double precision) {
        assertThatThrownBy(() -> validador.evaluar(latitud, longitud, precision))
                .isInstanceOf(UbicacionInvalidaException.class);
    }

    @Test
    @DisplayName("Valores no finitos se rechazan")
    void noFinitos() {
        assertThatThrownBy(() -> validador.evaluar(Double.NaN, 0.0, 5.0)).isInstanceOf(UbicacionInvalidaException.class);
    }
}
