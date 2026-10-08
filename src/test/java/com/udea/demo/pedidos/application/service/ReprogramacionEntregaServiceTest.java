package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.ReprogramarEntregaRequestDTO;
import com.udea.demo.pedidos.domain.event.EntregaReprogramadaEvent;
import com.udea.demo.pedidos.domain.exception.FechaReprogramacionInvalidaException;
import com.udea.demo.pedidos.domain.exception.TransicionEstadoInvalidaException;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.Pedido;
import com.udea.demo.pedidos.domain.model.TipoIncidencia;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HU-07 ReprogramacionEntregaService - criterio 3")
class ReprogramacionEntregaServiceTest {
    private static final String TRACKING = "LT1";
    private static final LocalDate HOY = LocalDate.now();

    @Mock private AccesoClientePedido acceso;
    @Mock private PedidoRepository pedidos;
    @Mock private RegistroHistorialPedido historial;
    @Mock private SeguimientoClienteService seguimiento;
    @Mock private ActorAuthorizationService actores;
    @Mock private ApplicationEventPublisher eventos;

    private ReprogramacionEntregaService service;
    private Pedido pedido;

    @BeforeEach
    void setUp() {
        service = new ReprogramacionEntregaService(acceso, pedidos, historial, seguimiento, actores, eventos, 7);
        pedido = Pedido.builder().id(1L).clienteId(10L).numeroTracking(TRACKING).estado(EstadoPedido.EN_REPARTO).build();
        pedido.registrarIncidencia(TipoIncidencia.CLIENTE_AUSENTE, 3, LocalDateTime.now(), null);
        lenient().when(acceso.pedidoDelClienteActualParaModificar(TRACKING)).thenReturn(pedido);
        lenient().when(acceso.pedidoDelClienteActual(TRACKING)).thenReturn(pedido);
        lenient().when(actores.actorActual()).thenReturn(Usuario.builder().id(30L).rol(Rol.CLIENTE).build());
    }

    @Test
    @DisplayName("Ofrece el rango de fechas desde hoy hasta el límite de retención")
    void rango() {
        var rango = service.rangoDisponible(TRACKING);

        assertThat(rango.desde()).isEqualTo(HOY);
        assertThat(rango.hasta()).isEqualTo(HOY.plusDays(7));
    }

    @Test
    @DisplayName("Reprograma: registra la fecha, pasa a Entrega reprogramada e informa al cliente")
    void reprograma() {
        service.reprogramar(TRACKING, new ReprogramarEntregaRequestDTO(HOY.plusDays(2)));

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ENTREGA_REPROGRAMADA);
        assertThat(pedido.getFechaEntregaReprogramada()).isEqualTo(HOY.plusDays(2));
        verify(pedidos).saveAndFlush(pedido);
        verify(historial).registrar(eq(1L), eq(30L), eq(RegistroHistorialPedido.ESTADO_LOGISTICO), eq("REPROGRAMACION"),
                eq("ENTREGA_REPROGRAMADA"), any(LocalDateTime.class));
        ArgumentCaptor<EntregaReprogramadaEvent> evento = ArgumentCaptor.forClass(EntregaReprogramadaEvent.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().fechaEntregaReprogramada()).isEqualTo(HOY.plusDays(2));
        assertThat(evento.getValue().routingKey()).isEqualTo("pedido.entrega.reprogramada");
        verify(seguimiento).construir(pedido);
    }

    @Test
    @DisplayName("Rechaza una fecha anterior a hoy")
    void fechaAnterior() {
        assertThatThrownBy(() -> service.reprogramar(TRACKING, new ReprogramarEntregaRequestDTO(HOY.minusDays(1))))
                .isInstanceOf(FechaReprogramacionInvalidaException.class);
        verifyNoInteractions(eventos);
        verify(pedidos, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Rechaza una fecha posterior al límite de retención en bodega")
    void fechaFueraDelLimite() {
        assertThatThrownBy(() -> service.reprogramar(TRACKING, new ReprogramarEntregaRequestDTO(HOY.plusDays(8))))
                .isInstanceOf(FechaReprogramacionInvalidaException.class);
    }

    @Test
    @DisplayName("Solo se reprograma un envío en Entrega fallida")
    void estadoIncorrecto() {
        Pedido enReparto = Pedido.builder().id(2L).numeroTracking("LT2").estado(EstadoPedido.EN_REPARTO).build();
        when(acceso.pedidoDelClienteActualParaModificar("LT2")).thenReturn(enReparto);

        assertThatThrownBy(() -> service.reprogramar("LT2", new ReprogramarEntregaRequestDTO(HOY)))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    @DisplayName("Un cliente ajeno al envío no puede reprogramarlo")
    void clienteAjeno() {
        when(acceso.pedidoDelClienteActualParaModificar("AJENO")).thenThrow(new AccessDeniedException("ajeno"));

        assertThatThrownBy(() -> service.reprogramar("AJENO", new ReprogramarEntregaRequestDTO(HOY)))
                .isInstanceOf(AccessDeniedException.class);
    }
}
