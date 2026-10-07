package com.udea.demo.pedidos.domain.model;

import com.udea.demo.pedidos.domain.exception.CodigoQrInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HU-06 CodigoQrEnvio - validación del QR de la etiqueta")
class CodigoQrEnvioTest {
    private static final String TRACKING = "LT1234567890";

    @Test
    @DisplayName("El QR generado para la etiqueta se interpreta de vuelta al mismo tracking")
    void generarYParsear() {
        CodigoQrEnvio generado = CodigoQrEnvio.para(TRACKING);

        CodigoQrEnvio leido = CodigoQrEnvio.parsear(generado.contenido());

        assertThat(leido.numeroTracking()).isEqualTo(TRACKING);
        assertThat(generado.contenido()).isEqualTo(TRACKING + "|" + generado.checksum());
        assertThat(generado.checksum()).hasSize(8);
    }

    @Test
    @DisplayName("Acepta checksum en minúsculas y espacios alrededor")
    void toleraMayusculasYEspacios() {
        String contenido = "  " + TRACKING + "|" + CodigoQrEnvio.para(TRACKING).checksum().toLowerCase() + " ";

        assertThat(CodigoQrEnvio.parsear(contenido).numeroTracking()).isEqualTo(TRACKING);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"LT1234567890", "LT1234567890|", "|ABCDEF12", "LT1234567890|ABCDEF12",
            "LT1|AAA|BBB", "texto cualquiera"})
    @DisplayName("Rechaza QR no reconocido, sin checksum o con checksum alterado")
    void rechazaQrInvalido(String contenido) {
        assertThatThrownBy(() -> CodigoQrEnvio.parsear(contenido)).isInstanceOf(CodigoQrInvalidoException.class);
    }

    @Test
    @DisplayName("Un checksum de otro tracking no valida")
    void checksumDeOtroTracking() {
        String ajeno = TRACKING + "|" + CodigoQrEnvio.para("LT0000000001").checksum();

        assertThatThrownBy(() -> CodigoQrEnvio.parsear(ajeno)).isInstanceOf(CodigoQrInvalidoException.class);
    }
}
