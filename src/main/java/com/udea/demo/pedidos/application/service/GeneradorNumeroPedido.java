package com.udea.demo.pedidos.application.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

// Pure Fabrication (GRASP): no es un concepto del dominio, existe solo para mantener a Pedido cohesionado
@Component
public class GeneradorNumeroPedido {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generar() {
        String fecha = LocalDate.now().format(FORMATO_FECHA);
        String sufijo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "PED-" + fecha + "-" + sufijo;
    }
}
