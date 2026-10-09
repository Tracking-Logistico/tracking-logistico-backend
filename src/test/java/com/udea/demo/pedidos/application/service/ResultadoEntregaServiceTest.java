package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.*;
import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.interfaces.persistence.*;
import com.udea.demo.pedidos.interfaces.services.AccesoPedidoConductorI;
import com.udea.demo.rutas.interfaces.persistence.RutaRepository;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import com.udea.demo.usuarios.interfaces.persistence.ConductorRepository;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.access.AccessDeniedException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResultadoEntregaServiceTest {
    @Mock PedidoRepository pedidos;
    @Mock EventoEntregaRepository eventos;
    @Mock CatalogoNovedadEntregaRepository catalogo;
    @Mock HistorialPedidoRepository historial;
    @Mock ActorAuthorizationService actores;
    @Mock AccesoPedidoConductorI acceso;
    @Mock RutaRepository rutas;
    @Mock ConductorRepository conductores;
    ResultadoEntregaService service;

    @BeforeEach void setUp() {
        service = new ResultadoEntregaService(pedidos, eventos, catalogo, historial, actores, acceso, rutas, conductores);
        lenient().when(actores.conductorActualUsuarioId()).thenReturn(7L);
        lenient().when(eventos.findByIdEventoCliente(anyString())).thenReturn(Optional.empty());
    }

    private ResultadoEntregaRequestDTO request(ResultadoEntrega resultado, String codigo, String motivo,
                                               Double latitud, Double longitud) {
        return new ResultadoEntregaRequestDTO(resultado, codigo, motivo, latitud, longitud,
                OffsetDateTime.now().minusMinutes(1), UUID.randomUUID().toString());
    }

    private Pedido pedido(EstadoPedido estado) {
        return Pedido.builder().id(1L).estado(estado).build();
    }

    @Test void motivoEsObligatorioParaFallida() {
        when(acceso.tieneAsignacionActiva(1L, 7L)).thenReturn(true);
        when(pedidos.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido(EstadoPedido.EN_REPARTO)));
        assertThatThrownBy(() -> service.registrar(1L, request(ResultadoEntrega.ENTREGA_FALLIDA, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatorios");
    }

    @Test void conductorEquivocadoEsRechazado() {
        when(acceso.tieneAsignacionActiva(1L, 7L)).thenReturn(false);
        assertThatThrownBy(() -> service.registrar(1L, request(ResultadoEntrega.ENTREGADO, null, null, 6.2, -75.5)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test void envioFueraDeRepartoEsConflicto() {
        when(acceso.tieneAsignacionActiva(1L, 7L)).thenReturn(true);
        when(pedidos.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido(EstadoPedido.EN_TRANSITO)));
        assertThatThrownBy(() -> service.registrar(1L, request(ResultadoEntrega.ENTREGADO, null, null, null, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void ubicacionVaciaNoImpideEntregar() {
        when(acceso.tieneAsignacionActiva(1L, 7L)).thenReturn(true);
        when(pedidos.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido(EstadoPedido.EN_REPARTO)));
        ResultadoEntregaResponseDTO response = service.registrar(
                1L, request(ResultadoEntrega.ENTREGADO, null, null, null, null));
        assertThat(response.estado()).isEqualTo("APLICADO");
        verify(eventos).saveAndFlush(any(EventoEntrega.class));
    }

    @Test void fechaFuturaEsRechazada() {
        assertThatThrownBy(() -> service.registrar(1L, requestConFecha(
                ResultadoEntrega.ENTREGADO, OffsetDateTime.now().plusMinutes(6))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("24 horas");
        verifyNoInteractions(acceso, pedidos);
    }

    @Test void fechaConMasDe24HorasEsRechazada() {
        assertThatThrownBy(() -> service.registrar(1L, requestConFecha(
                ResultadoEntrega.ENTREGADO, OffsetDateTime.now().minusHours(24).minusMinutes(1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("24 horas");
        verifyNoInteractions(acceso, pedidos);
    }

    @Test void fallaHistorialYNoPersisteEvento() {
        when(acceso.tieneAsignacionActiva(1L, 7L)).thenReturn(true);
        Pedido pedido = pedido(EstadoPedido.EN_REPARTO);
        when(pedidos.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        doThrow(new IllegalStateException("fallo de historial"))
                .when(historial).save(any(HistorialPedido.class));

        assertThatThrownBy(() -> service.registrar(
                1L, request(ResultadoEntrega.ENTREGADO, null, null, null, null)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.EN_REPARTO);
        verify(eventos, never()).saveAndFlush(any(EventoEntrega.class));
    }

    private ResultadoEntregaRequestDTO requestConFecha(ResultadoEntrega resultado, OffsetDateTime fecha) {
        return new ResultadoEntregaRequestDTO(resultado, null, null, null, null, fecha, UUID.randomUUID().toString());
    }
}
