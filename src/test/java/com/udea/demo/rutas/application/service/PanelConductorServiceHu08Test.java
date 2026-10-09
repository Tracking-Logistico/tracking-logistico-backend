package com.udea.demo.rutas.application.service;

import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.interfaces.services.PedidoServiceI;
import com.udea.demo.rutas.domain.model.Ruta;
import com.udea.demo.rutas.interfaces.persistence.RutaRepository;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import com.udea.demo.usuarios.domain.model.Conductor;
import com.udea.demo.usuarios.interfaces.persistence.ConductorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PanelConductorServiceHu08Test {
    @Mock RutaRepository rutas;
    @Mock ConductorRepository conductores;
    @Mock PedidoServiceI pedidos;
    @Mock ActorAuthorizationService actores;

    @Test void entregasAsignadasUsaSoloLaRutaDelConductorAutenticado() {
        PanelConductorService service = new PanelConductorService(rutas, conductores, pedidos, actores);
        ReflectionTestUtils.setField(service, "zonaHoraria", "UTC");
        when(actores.conductorActualUsuarioId()).thenReturn(7L);
        Conductor conductor = Mockito.mock(Conductor.class);
        when(conductor.getId()).thenReturn(11L);
        when(conductores.findByUsuarioId(7L)).thenReturn(Optional.of(conductor));
        Ruta ruta = Ruta.crear(11L, LocalDate.now(ZoneOffset.UTC));
        ruta.agregarParada(1L);
        when(rutas.findByConductorIdAndFecha(eq(11L), any(LocalDate.class))).thenReturn(Optional.of(ruta));
        when(pedidos.obtenerPorIds(any())).thenReturn(Map.of(1L, pedido(1L)));

        assertThat(service.entregasAsignadas()).extracting(p -> p.pedidoId()).containsExactly(1L);
        Mockito.verify(rutas).findByConductorIdAndFecha(eq(11L), any(LocalDate.class));
    }

    private PedidoResponseDTO pedido(Long id) {
        return new PedidoResponseDTO(id, "P-" + id, 1L, "Origen", "Destino", "Paquete",
                1D, 1D, 1D, 1D, TipoServicio.ESTANDAR, Prioridad.MEDIA, Prioridad.MEDIA,
                EstadoPedido.EN_REPARTO, null, null, LocalDateTime.now(), null, "T-" + id,
                null, false, null);
    }
}
