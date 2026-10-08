package com.udea.demo.rutas.application.service;

import com.udea.demo.pedidos.domain.event.DevolucionIniciadaEvent;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.MotivoDevolucion;
import com.udea.demo.pedidos.domain.model.Pedido;
import com.udea.demo.rutas.domain.model.EstadoParada;
import com.udea.demo.rutas.domain.model.ParadaRuta;
import com.udea.demo.rutas.domain.model.Ruta;
import com.udea.demo.rutas.interfaces.persistence.ParadaRutaRepository;
import com.udea.demo.rutas.interfaces.persistence.RutaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HU-07 Rutas - una devolución cierra la parada pendiente del conductor")
class CierreParadaPorDevolucionListenerTest {
    @Mock private ParadaRutaRepository paradas;
    @Mock private RutaRepository rutas;
    @InjectMocks private CierreParadaPorDevolucionListener listener;

    private DevolucionIniciadaEvent evento() {
        Pedido pedido = Pedido.builder().id(7L).numeroTracking("LT7").estado(EstadoPedido.DEVOLUCION_AL_REMITENTE).build();
        return DevolucionIniciadaEvent.de(pedido, EstadoPedido.EN_REPARTO, MotivoDevolucion.PAQUETE_RECHAZADO);
    }

    @Test
    @DisplayName("Cancela la parada pendiente del envío devuelto")
    void cancelaParada() {
        Ruta ruta = Ruta.crear(1L, LocalDate.now());
        ParadaRuta parada = ruta.agregarParada(7L);
        when(paradas.findByPedidoIdAndEstado(7L, EstadoParada.PENDIENTE)).thenReturn(Optional.of(parada));

        listener.alIniciarDevolucion(evento());

        assertThat(parada.getEstado()).isEqualTo(EstadoParada.CANCELADA);
        verify(rutas).save(ruta);
    }

    @Test
    @DisplayName("Sin parada pendiente no hace nada")
    void sinParada() {
        when(paradas.findByPedidoIdAndEstado(7L, EstadoParada.PENDIENTE)).thenReturn(Optional.empty());

        listener.alIniciarDevolucion(evento());

        verify(rutas, never()).save(any());
    }
}
