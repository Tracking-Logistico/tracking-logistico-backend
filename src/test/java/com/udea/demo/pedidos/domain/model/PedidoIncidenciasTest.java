package com.udea.demo.pedidos.domain.model;

import com.udea.demo.pedidos.domain.exception.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HU-07 Pedido - reglas de incidencias, reprogramación y devoluciones")
class PedidoIncidenciasTest {
    private static final int MAX_INTENTOS = 3;
    private static final int RETENCION = 7;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 6, 10, 0);
    private static final LocalDate HOY = AHORA.toLocalDate();

    private static Pedido pedidoEn(EstadoPedido estado) {
        return Pedido.builder().id(1L).numeroTracking("LT1").estado(estado).build();
    }

    private static EstadoPedido registrar(Pedido p, TipoIncidencia tipo) {
        return p.registrarIncidencia(tipo, MAX_INTENTOS, AHORA, AHORA.plusDays(3));
    }

    @Nested
    @DisplayName("Criterio 1: actualización del estado según el tipo")
    class EstadoSegunTipo {

        @Test
        @DisplayName("Cliente ausente → Entrega fallida y cuenta un intento")
        void clienteAusente() {
            Pedido p = pedidoEn(EstadoPedido.EN_REPARTO);

            assertThat(registrar(p, TipoIncidencia.CLIENTE_AUSENTE)).isEqualTo(EstadoPedido.ENTREGA_FALLIDA);
            assertThat(p.getIntentosEntregaFallidos()).isEqualTo(1);
            assertThat(p.getFechaEntregaFallida()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("Dirección incorrecta → Dirección por verificar con plazo límite")
        void direccionIncorrecta() {
            Pedido p = pedidoEn(EstadoPedido.EN_REPARTO);

            assertThat(registrar(p, TipoIncidencia.DIRECCION_INCORRECTA)).isEqualTo(EstadoPedido.DIRECCION_POR_VERIFICAR);
            assertThat(p.getFechaLimiteVerificacionDireccion()).isEqualTo(AHORA.plusDays(3));
        }

        @Test
        @DisplayName("Paquete rechazado → Devolución al remitente")
        void paqueteRechazado() {
            Pedido p = pedidoEn(EstadoPedido.EN_REPARTO);

            assertThat(registrar(p, TipoIncidencia.PAQUETE_RECHAZADO)).isEqualTo(EstadoPedido.DEVOLUCION_AL_REMITENTE);
        }

        @ParameterizedTest
        @EnumSource(value = TipoIncidencia.class, names = {"RETRASO_OPERATIVO", "OTRO", "PAQUETE_DANADO"})
        @DisplayName("Incidencias informativas no cambian el estado, pero marcan la última novedad")
        void informativas(TipoIncidencia tipo) {
            Pedido p = pedidoEn(EstadoPedido.EN_TRANSITO);

            assertThat(registrar(p, tipo)).isEqualTo(EstadoPedido.EN_TRANSITO);
            assertThat(p.getFechaUltimaIncidencia()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("Un tipo que cambia estado solo aplica a envíos en proceso de entrega")
        void cambioEstadoFueraDeEntrega() {
            Pedido p = pedidoEn(EstadoPedido.EN_TRANSITO);

            assertThatThrownBy(() -> registrar(p, TipoIncidencia.CLIENTE_AUSENTE))
                    .isInstanceOf(IncidenciaNoPermitidaException.class);
        }

        @Test
        @DisplayName("Un envío finalizado no admite incidencias")
        void envioFinalizado() {
            assertThatThrownBy(() -> registrar(pedidoEn(EstadoPedido.ENTREGADO), TipoIncidencia.RETRASO_OPERATIVO))
                    .isInstanceOf(EnvioFinalizadoException.class);
        }

        @Test
        @DisplayName("Un pedido sin tracking activo no es un envío activo")
        void envioNoActivo() {
            assertThatThrownBy(() -> registrar(pedidoEn(EstadoPedido.SOLICITADO), TipoIncidencia.RETRASO_OPERATIVO))
                    .isInstanceOf(IncidenciaNoPermitidaException.class);
        }
    }

    @Nested
    @DisplayName("Criterio 6: devolución tras múltiples intentos fallidos")
    class Intentos {

        private Pedido conIntentosFallidos(int intentos) {
            Pedido p = pedidoEn(EstadoPedido.EN_REPARTO);
            for (int i = 1; i <= intentos; i++) {
                registrar(p, TipoIncidencia.CLIENTE_AUSENTE);
                if (i < intentos) p.reprogramarEntrega(HOY.plusDays(1), HOY, RETENCION);
            }
            return p;
        }

        @Test
        @DisplayName("El tercer intento fallido pasa a Devolución al remitente")
        void tercerIntento() {
            Pedido p = conIntentosFallidos(3);

            assertThat(p.getEstado()).isEqualTo(EstadoPedido.DEVOLUCION_AL_REMITENTE);
            assertThat(p.getIntentosEntregaFallidos()).isEqualTo(3);
        }

        @Test
        @DisplayName("Con dos intentos el envío todavía puede reprogramarse")
        void dosIntentos() {
            Pedido p = conIntentosFallidos(2);

            assertThat(p.getEstado()).isEqualTo(EstadoPedido.ENTREGA_FALLIDA);
        }

        @Test
        @DisplayName("No se permite un cuarto intento")
        void cuartoIntento() {
            Pedido p = conIntentosFallidos(3);

            assertThatThrownBy(() -> registrar(p, TipoIncidencia.CLIENTE_AUSENTE))
                    .isInstanceOf(EnvioFinalizadoException.class);
            assertThat(p.getIntentosEntregaFallidos()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Criterio 3: reprogramación de una entrega fallida")
    class Reprogramacion {

        private Pedido fallido() {
            Pedido p = pedidoEn(EstadoPedido.EN_REPARTO);
            p.registrarIncidencia(TipoIncidencia.CLIENTE_AUSENTE, MAX_INTENTOS, AHORA, null);
            return p;
        }

        @Test
        @DisplayName("Ofrece desde hoy hasta el límite de retención en bodega")
        void rango() {
            RangoReprogramacion rango = fallido().rangoReprogramacion(HOY, RETENCION);

            assertThat(rango.desde()).isEqualTo(HOY);
            assertThat(rango.hasta()).isEqualTo(HOY.plusDays(RETENCION));
        }

        @Test
        @DisplayName("Reprograma dentro del rango y pasa a Entrega reprogramada")
        void reprograma() {
            Pedido p = fallido();
            p.reprogramarEntrega(HOY.plusDays(2), HOY, RETENCION);

            assertThat(p.getEstado()).isEqualTo(EstadoPedido.ENTREGA_REPROGRAMADA);
            assertThat(p.getFechaEntregaReprogramada()).isEqualTo(HOY.plusDays(2));
        }

        @Test
        @DisplayName("Acepta hoy y el último día de retención (límites inclusivos)")
        void limites() {
            Pedido hoy = fallido();
            hoy.reprogramarEntrega(HOY, HOY, RETENCION);
            Pedido ultimo = fallido();
            ultimo.reprogramarEntrega(HOY.plusDays(RETENCION), HOY, RETENCION);

            assertThat(hoy.getEstado()).isEqualTo(EstadoPedido.ENTREGA_REPROGRAMADA);
            assertThat(ultimo.getEstado()).isEqualTo(EstadoPedido.ENTREGA_REPROGRAMADA);
        }

        @Test
        @DisplayName("Rechaza una fecha anterior a hoy")
        void fechaAnterior() {
            Pedido p = fallido();

            assertThatThrownBy(() -> p.reprogramarEntrega(HOY.minusDays(1), HOY, RETENCION))
                    .isInstanceOf(FechaReprogramacionInvalidaException.class);
            assertThat(p.getEstado()).isEqualTo(EstadoPedido.ENTREGA_FALLIDA);
        }

        @Test
        @DisplayName("Rechaza una fecha posterior al límite de retención")
        void fechaFueraDelLimite() {
            Pedido p = fallido();

            assertThatThrownBy(() -> p.reprogramarEntrega(HOY.plusDays(RETENCION + 1), HOY, RETENCION))
                    .isInstanceOf(FechaReprogramacionInvalidaException.class)
                    .hasMessageContaining(HOY.plusDays(RETENCION).toString());
        }

        @Test
        @DisplayName("Con la retención vencida no hay fechas disponibles")
        void retencionVencida() {
            Pedido p = fallido();
            LocalDate dentroDeDiezDias = HOY.plusDays(10);

            assertThat(p.rangoReprogramacion(dentroDeDiezDias, RETENCION).vacio()).isTrue();
            assertThatThrownBy(() -> p.reprogramarEntrega(dentroDeDiezDias, dentroDeDiezDias, RETENCION))
                    .isInstanceOf(FechaReprogramacionInvalidaException.class)
                    .hasMessageContaining("retención");
        }

        @Test
        @DisplayName("Solo un envío en Entrega fallida puede reprogramarse")
        void estadoIncorrecto() {
            assertThatThrownBy(() -> pedidoEn(EstadoPedido.EN_REPARTO).reprogramarEntrega(HOY, HOY, RETENCION))
                    .isInstanceOf(TransicionEstadoInvalidaException.class);
        }
    }

    @Nested
    @DisplayName("Criterio 4: dirección por verificar")
    class Direccion {

        private Pedido porVerificar() {
            Pedido p = pedidoEn(EstadoPedido.EN_REPARTO);
            registrar(p, TipoIncidencia.DIRECCION_INCORRECTA);
            return p;
        }

        @Test
        @DisplayName("El cliente confirma/corrige la dirección en plazo y el envío vuelve a reparto")
        void confirmaEnPlazo() {
            Pedido p = porVerificar();
            p.confirmarDireccion("Calle 10 #20-30 Apto 401", "Medellín", null, AHORA.plusDays(1));

            assertThat(p.getEstado()).isEqualTo(EstadoPedido.EN_REPARTO);
            assertThat(p.getDireccionDestino()).isEqualTo("Calle 10 #20-30 Apto 401");
            assertThat(p.getFechaLimiteVerificacionDireccion()).isNull();
        }

        @Test
        @DisplayName("Fuera del plazo ya no se puede confirmar")
        void confirmaFueraDePlazo() {
            Pedido p = porVerificar();

            assertThatThrownBy(() -> p.confirmarDireccion(null, null, null, AHORA.plusDays(4)))
                    .isInstanceOf(PlazoVerificacionDireccionVencidoException.class);
        }

        @Test
        @DisplayName("Al vencer el plazo escala automáticamente a devolución")
        void escalaAlVencer() {
            Pedido p = porVerificar();

            assertThat(p.devolverPorPlazoVencido(AHORA.plusDays(2))).isFalse();
            assertThat(p.devolverPorPlazoVencido(AHORA.plusDays(4))).isTrue();
            assertThat(p.getEstado()).isEqualTo(EstadoPedido.DEVOLUCION_AL_REMITENTE);
        }
    }
}
