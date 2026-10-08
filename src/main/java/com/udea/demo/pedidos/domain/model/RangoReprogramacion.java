package com.udea.demo.pedidos.domain.model;

import java.time.LocalDate;

/** Fechas que el sistema ofrece al cliente para reprogramar una entrega fallida (ambas inclusive). */
public record RangoReprogramacion(LocalDate desde, LocalDate hasta) {
    public boolean contiene(LocalDate fecha) {
        return fecha != null && !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }

    public boolean vacio() { return hasta.isBefore(desde); }
}
