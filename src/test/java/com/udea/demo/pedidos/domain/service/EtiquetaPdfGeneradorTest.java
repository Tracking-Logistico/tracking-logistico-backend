package com.udea.demo.pedidos.domain.service;

import com.udea.demo.pedidos.domain.model.Pedido;
import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EtiquetaPdfGeneradorTest {
    private final EtiquetaPdfGenerador generador = new EtiquetaPdfGenerador();

    @Test void generaDocumentoPdfConTracking() {
        Pedido pedido = Pedido.recibir(10L, "Carrera 7 #71-21, Bogotá", "Bogotá", "110111",
                "Calle 45 #12-30, Bogotá", "Bogotá", "110111", "Caja frágil", 2.5, 30.0, 20.0,
                15.0, TipoServicio.EXPRESS, "PED-TEST-001", Prioridad.ALTA, "Destinatario",
                "+573001234567", "Cliente", "cliente@test.com", "+573109876543");
        pedido.validar(true, Prioridad.ALTA, null, 99L);
        pedido.activarTracking("LT23456789AB");
        byte[] pdf = Base64.getDecoder().decode(generador.generar(pedido));
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(500);
    }
}
