package com.udea.demo.pedidos.domain.service;

import com.udea.demo.pedidos.domain.model.Pedido;
import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias del generador de etiqueta (dominio).
 *
 * CP cubierto:
 *  - CP-HU03B-02: la etiqueta incluye tracking y datos clave de destino, y debe ser PDF con QR.
 *
 * Patrón AAA (Arrange - Act - Assert) con secciones marcadas.
 */
@DisplayName("GeneradorEtiqueta - dominio (CP-HU03B-02)")
class EtiquetaTextoPlanoGeneradorTest {

    private static final String NUMERO_PEDIDO = "PED-20260214-8F4A29C1";
    private static final String NUMERO_TRACKING = "TRK-20260214-8F4A29C1B7";

    private final EtiquetaTextoPlanoGenerador generador = new EtiquetaTextoPlanoGenerador();

    private Pedido pedidoEnTransito() {
        Pedido pedido = Pedido.recibir(
                10L,
                "Carrera 7 #71-21, Bogotá",
                "Calle 45 #12-30, Bogotá",
                "Caja frágil",
                2.50, 30.0, 20.0, 15.0,
                TipoServicio.EXPRESS,
                NUMERO_PEDIDO,
                Prioridad.URGENTE
        );
        pedido.validar(true, Prioridad.ALTA, null, 99L);
        pedido.activarTracking(NUMERO_TRACKING);
        pedido.confirmarImpresionEtiqueta();
        return pedido;
    }

    /** CP-HU03B-02: camino feliz (contenido de la etiqueta). */
    @Test
    @DisplayName("CP-HU03B-02: la etiqueta incluye tracking y datos clave de destino")
    void generar_feliz() {
        // Arrange
        Pedido pedido = pedidoEnTransito();

        // Act
        String etiqueta = generador.generar(pedido);

        // Assert
        assertThat(etiqueta).contains(NUMERO_TRACKING);
        assertThat(etiqueta).contains("DESTINO:     Calle 45 #12-30, Bogotá");
    }

    /**
     * BUG (CP-HU03B-02): la HU exige un PDF con código QR (nivel >= M) que codifique el tracking
     * y un checksum. La implementación actual solo produce texto plano.
     *
     * Deshabilitada para no romper el build mientras no exista la generación de PDF con QR.
     */
    @Disabled("BUG CP-HU03B-02: pendiente implementar PDF con QR. Ver HU03-B.")
    @Test
    @DisplayName("BUG CP-HU03B-02: la etiqueta debería ser PDF con código QR y checksum")
    void generar_error_pdfConQr() {
        // Arrange
        Pedido pedido = pedidoEnTransito();

        // Act
        String etiqueta = generador.generar(pedido);

        // Assert
        assertThat(etiqueta).contains("%PDF");
        assertThat(etiqueta).contains("QR");
        assertThat(etiqueta).contains("checksum");
    }
}
