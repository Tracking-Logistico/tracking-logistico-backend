package com.udea.demo.pedidos.domain.model;

import com.udea.demo.pedidos.domain.exception.TipoIncidenciaInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HU-07 TipoIncidencia - catálogo cerrado")
class TipoIncidenciaTest {

    @Test
    @DisplayName("Interpreta códigos del catálogo sin importar mayúsculas ni espacios")
    void desdeCodigoValido() {
        assertThat(TipoIncidencia.desde(" cliente_ausente ")).isEqualTo(TipoIncidencia.CLIENTE_AUSENTE);
        assertThat(TipoIncidencia.desde("DIRECCION_INCORRECTA")).isEqualTo(TipoIncidencia.DIRECCION_INCORRECTA);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"PERDIDO", "Cliente ausente", "1"})
    @DisplayName("Rechaza tipos fuera del catálogo")
    void rechazaTipoInvalido(String codigo) {
        assertThatThrownBy(() -> TipoIncidencia.desde(codigo)).isInstanceOf(TipoIncidenciaInvalidoException.class);
    }

    @Test
    @DisplayName("Solo 'Otro' y 'Paquete dañado' (no estándar) exigen comentario")
    void requiereComentario() {
        assertThat(Arrays.stream(TipoIncidencia.values()).filter(TipoIncidencia::requiereComentario))
                .containsExactlyInAnyOrder(TipoIncidencia.OTRO, TipoIncidencia.PAQUETE_DANADO);
    }

    @Test
    @DisplayName("Cada tipo define su efecto en el estado y un mensaje para el cliente sin códigos internos")
    void efectoYMensaje() {
        assertThat(TipoIncidencia.DIRECCION_INCORRECTA.estadoResultante()).isEqualTo(EstadoPedido.DIRECCION_POR_VERIFICAR);
        assertThat(TipoIncidencia.CLIENTE_AUSENTE.estadoResultante()).isEqualTo(EstadoPedido.ENTREGA_FALLIDA);
        assertThat(TipoIncidencia.PAQUETE_RECHAZADO.estadoResultante()).isEqualTo(EstadoPedido.DEVOLUCION_AL_REMITENTE);
        assertThat(TipoIncidencia.RETRASO_OPERATIVO.cambiaEstado()).isFalse();
        assertThat(TipoIncidencia.CLIENTE_AUSENTE.cuentaComoIntento()).isTrue();
        for (TipoIncidencia tipo : TipoIncidencia.values()) {
            assertThat(tipo.mensajeCliente()).isNotBlank().doesNotContain(tipo.name()).doesNotContain("_");
        }
    }
}
