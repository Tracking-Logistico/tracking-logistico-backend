package com.udea.demo.pedidos.domain.service;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

/** Calcula plazos en días hábiles (lunes a viernes); no contempla festivos. */
@Component
public class CalendarioHabil {
    public LocalDateTime sumarDiasHabiles(LocalDateTime desde, int dias) {
        LocalDateTime resultado = desde;
        int sumados = 0;
        while (sumados < dias) {
            resultado = resultado.plusDays(1);
            if (resultado.getDayOfWeek() != DayOfWeek.SATURDAY && resultado.getDayOfWeek() != DayOfWeek.SUNDAY) sumados++;
        }
        return resultado;
    }
}
