package com.udea.demo.pedidos.application.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class GeneradorNumeroPedido {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generar() {
        String fecha = LocalDate.now().format(FORMATO_FECHA);
        String sufijo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "PED-" + fecha + "-" + sufijo;
    }
}
