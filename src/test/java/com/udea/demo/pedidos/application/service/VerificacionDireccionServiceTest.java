package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.ConfirmarDireccionRequestDTO;
import com.udea.demo.pedidos.domain.event.DevolucionIniciadaEvent;
import com.udea.demo.pedidos.domain.exception.PlazoVerificacionDireccionVencidoException;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.MotivoDevolucion;
import com.udea.demo.pedidos.domain.model.Pedido;
import com.udea.demo.pedidos.domain.model.TipoIncidencia;
import com.udea.demo.pedidos.infrastructure.scheduling.EscalamientoDireccionesVencidasTask;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HU-07 VerificacionDireccionService - criterio 4")
class VerificacionDireccionServiceTest {
    @Mock private AccesoClientePedido acceso;
    @Mock private PedidoRepository pedidos;
    @Mock private RegistroHistorialPedido historial;
    @Mock private SeguimientoClienteService seguimiento;
    @Mock private ActorAuthorizationService actores;
    @Mock private ApplicationEventPublisher eventos;
    @InjectMocks private VerificacionDireccionService service;

    private Pedido pedido;
    private final LocalDateTime registro = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        pedido = Pedido.builder().id(1L).clienteId(10L).numeroTracking("LT1").direccionDestino("Calle 1 #2-3")
                .estado(EstadoPedido.EN_REPARTO).build();
        pedido.registrarIncidencia(TipoIncidencia.DIRECCION_INCORRECTA, 3, registro, registro.plusDays(3));
    }

    @Test
    @DisplayName("El cliente corrige la dirección dentro del plazo y el envío vuelve a reparto")
    void confirmaEnPlazo() {
        when(acceso.pedidoDelClienteActualParaModificar("LT1")).thenReturn(pedido);
        when(actores.actorActual()).thenReturn(Usuario.builder().id(30L).rol(Rol.CLIENTE).build());

        service.confirmar("LT1", new ConfirmarDireccionRequestDTO("Carrera 80 #45-12", null, null));

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.EN_REPARTO);
        assertThat(pedido.getDireccionDestino()).isEqualTo("Carrera 80 #45-12");
        verify(pedidos).saveAndFlush(pedido);
        verify(historial).registrar(eq(1L), eq(30L), eq(RegistroHistorialPedido.ESTADO_LOGISTICO),
                eq("DIRECCION_CONFIRMADA"), eq("EN_REPARTO"), any());
    }

    @Test
    @DisplayName("Escala a devolución al vencer el plazo y notifica el motivo")
    void escalaVencida() {
        when(pedidos.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));

        boolean escalado = service.escalarVencida(1L, registro.plusDays(4));

        assertThat(escalado).isTrue();
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.DEVOLUCION_AL_REMITENTE);
        ArgumentCaptor<DevolucionIniciadaEvent> evento = ArgumentCaptor.forClass(DevolucionIniciadaEvent.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().motivo()).isEqualTo(MotivoDevolucion.PLAZO_DIRECCION_VENCIDO);
        assertThat(evento.getValue().estadoAnterior()).isEqualTo(EstadoPedido.DIRECCION_POR_VERIFICAR);
    }

    @Test
    @DisplayName("No escala si el cliente respondió antes (se re-verifica bajo bloqueo)")
    void noEscalaSiYaRespondio() {
        pedido.confirmarDireccion(null, null, null, registro.plusDays(1));
        when(pedidos.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));

        assertThat(service.escalarVencida(1L, registro.plusDays(4))).isFalse();
        verifyNoInteractions(eventos);
        verify(pedidos, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Fuera del plazo el cliente ya no puede confirmar")
    void confirmaFueraDePlazo() {
        Pedido vencido = Pedido.builder().id(2L).numeroTracking("LT2").estado(EstadoPedido.EN_REPARTO).build();
        vencido.registrarIncidencia(TipoIncidencia.DIRECCION_INCORRECTA, 3, registro.minusDays(10), registro.minusDays(5));
        when(acceso.pedidoDelClienteActualParaModificar("LT2")).thenReturn(vencido);

        assertThatThrownBy(() -> service.confirmar("LT2", new ConfirmarDireccionRequestDTO(null, null, null)))
                .isInstanceOf(PlazoVerificacionDireccionVencidoException.class);
    }

    @Test
    @DisplayName("La tarea programada procesa cada envío vencido y un fallo aislado no detiene a los demás")
    void tareaProgramada() {
        VerificacionDireccionService mock = mock(VerificacionDireccionService.class);
        when(mock.pendientesDeEscalar(any())).thenReturn(List.of(1L, 2L, 3L));
        when(mock.escalarVencida(eq(2L), any())).thenThrow(new IllegalStateException("bloqueo"));
        when(mock.escalarVencida(eq(1L), any())).thenReturn(true);
        when(mock.escalarVencida(eq(3L), any())).thenReturn(true);

        new EscalamientoDireccionesVencidasTask(mock).escalar();

        verify(mock).escalarVencida(eq(1L), any());
        verify(mock).escalarVencida(eq(3L), any());
    }
}
