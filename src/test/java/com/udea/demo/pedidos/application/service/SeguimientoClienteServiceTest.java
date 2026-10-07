package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.MovimientoSeguimientoResponseDTO;
import com.udea.demo.pedidos.application.dto.SeguimientoClienteResponseDTO;
import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.interfaces.persistence.HistorialPedidoRepository;
import com.udea.demo.pedidos.interfaces.persistence.IncidenciaPedidoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HU-07 SeguimientoClienteService - criterio 2 (visualización para el cliente)")
class SeguimientoClienteServiceTest {
    @Mock private AccesoClientePedido acceso;
    @Mock private HistorialPedidoRepository historial;
    @Mock private IncidenciaPedidoRepository incidencias;
    @InjectMocks private SeguimientoClienteService service;

    private static HistorialPedido h(String tipo, String detalle, LocalDateTime fecha) {
        return HistorialPedido.builder().pedidoId(1L).tipoEvento(tipo).detalle(detalle).fecha(fecha).build();
    }

    @Test
    @DisplayName("Muestra la novedad, el estado actual y la situación en lenguaje para el cliente")
    void muestraNovedad() {
        LocalDateTime t = LocalDateTime.now().minusHours(5);
        Pedido pedido = Pedido.builder().id(1L).numeroPedido("PED-1").numeroTracking("LT1").clienteId(10L)
                .estado(EstadoPedido.EN_REPARTO).build();
        pedido.registrarIncidencia(TipoIncidencia.CLIENTE_AUSENTE, 3, t.plusHours(3), null);
        when(acceso.pedidoDelClienteActual("LT1")).thenReturn(pedido);
        when(historial.findByPedidoIdOrderByFechaAsc(1L)).thenReturn(List.of(
                h("TRACKING_ACTIVADO", "LT1", t),
                h("ESTADO_LOGISTICO", "EN_REPARTO", t.plusHours(1)),
                h("CHECKPOINT", "Punto de control registrado en etapa EN_REPARTO", t.plusHours(2)),
                h("INCIDENCIA", "CLIENTE_AUSENTE", t.plusHours(3)),
                h("ESTADO_LOGISTICO", "ENTREGA_FALLIDA", t.plusHours(3)),
                h("CORRECCION_SOLICITADA", "interno", t.plusHours(4))));
        when(incidencias.findFirstByPedidoIdOrderByFechaDescIdDesc(1L)).thenReturn(Optional.of(new IncidenciaPedido(1L, 50L,
                TipoIncidencia.CLIENTE_AUSENTE, null, new UbicacionReportada(null, null, null, false),
                EstadoPedido.EN_REPARTO, EstadoPedido.ENTREGA_FALLIDA, 1, t.plusHours(3))));

        SeguimientoClienteResponseDTO r = service.obtener("LT1");

        assertThat(r.estado()).isEqualTo(EstadoPedido.ENTREGA_FALLIDA);
        assertThat(r.descripcionEstado()).contains("No pudimos entregar tu paquete");
        assertThat(r.novedad().titulo()).isEqualTo("Cliente ausente");
        assertThat(r.novedad().mensaje()).isEqualTo(TipoIncidencia.CLIENTE_AUSENTE.mensajeCliente());
        assertThat(r.movimientos()).extracting(MovimientoSeguimientoResponseDTO::tipo)
                .containsExactly("ESTADO", "ESTADO", "PUNTO_CONTROL", "NOVEDAD", "ESTADO");
        assertThat(r.movimientos()).extracting(MovimientoSeguimientoResponseDTO::descripcion)
                .allSatisfy(d -> assertThat(d).isNotBlank().doesNotContain("_"));
    }

    @Test
    @DisplayName("Sin incidencias no informa novedad")
    void sinNovedad() {
        Pedido pedido = Pedido.builder().id(2L).numeroTracking("LT2").clienteId(10L).estado(EstadoPedido.EN_TRANSITO).build();
        when(acceso.pedidoDelClienteActual("LT2")).thenReturn(pedido);
        when(historial.findByPedidoIdOrderByFechaAsc(2L)).thenReturn(List.of());
        when(incidencias.findFirstByPedidoIdOrderByFechaDescIdDesc(2L)).thenReturn(Optional.empty());

        SeguimientoClienteResponseDTO r = service.obtener("LT2");

        assertThat(r.novedad()).isNull();
        assertThat(r.descripcionEstado()).isEqualTo("Tu paquete está en camino hacia la ciudad de destino");
    }
}
