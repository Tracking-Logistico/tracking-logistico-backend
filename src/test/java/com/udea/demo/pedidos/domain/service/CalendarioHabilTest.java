package com.udea.demo.pedidos.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HU-07 CalendarioHabil - plazo de 3 días hábiles")
class CalendarioHabilTest {
    private final CalendarioHabil calendario = new CalendarioHabil();

    @Test
    @DisplayName("De lunes suma tres días hábiles hasta el jueves")
    void desdeLunes() {
        LocalDateTime lunes = LocalDateTime.of(2026, 10, 5, 10, 0);

        assertThat(calendario.sumarDiasHabiles(lunes, 3)).isEqualTo(LocalDateTime.of(2026, 10, 8, 10, 0));
    }

    @Test
    @DisplayName("De jueves salta el fin de semana y termina el martes")
    void saltaFinDeSemana() {
        LocalDateTime jueves = LocalDateTime.of(2026, 10, 8, 16, 30);

        assertThat(calendario.sumarDiasHabiles(jueves, 3)).isEqualTo(LocalDateTime.of(2026, 10, 13, 16, 30));
    }

    @Test
    @DisplayName("Desde un sábado cuenta a partir del lunes")
    void desdeSabado() {
        LocalDateTime sabado = LocalDateTime.of(2026, 10, 10, 9, 0);

        assertThat(calendario.sumarDiasHabiles(sabado, 3)).isEqualTo(LocalDateTime.of(2026, 10, 14, 9, 0));
    }
}
