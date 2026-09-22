package com.udea.demo.pedidos.application.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

// Pure Fabrication (GRASP): aísla la generación del identificador de tracking
@Component
public class GeneradorNumeroTracking {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generar() {
        String fecha = LocalDate.now().format(FORMATO_FECHA);
        String sufijo = UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        return "TRK-" + fecha + "-" + sufijo;
    }
}
