package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.*;
import com.udea.demo.pedidos.application.dto.SincronizacionCheckpointsResponseDTO.EstadoSincronizacion;
import com.udea.demo.pedidos.domain.exception.CodigoQrInvalidoException;
import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.interfaces.persistence.CheckpointPedidoRepository;
import com.udea.demo.pedidos.interfaces.services.CheckpointServiceI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HU-06 SincronizacionCheckpointService - criterio 3 (registro sin conexión)")
class SincronizacionCheckpointServiceTest {
    @Mock private CheckpointServiceI checkpoints;
    @Mock private CheckpointPedidoRepository registros;
    @InjectMocks private SincronizacionCheckpointService service;

    private static ItemCheckpointOfflineDTO item(long pedidoId, OffsetDateTime fecha) {
        return new ItemCheckpointOfflineDTO(pedidoId, new RegistrarCheckpointRequestDTO("QR", EtapaCheckpoint.EN_REPARTO,
                UUID.randomUUID().toString(), 6.0, -75.0, 5.0, fecha));
    }

    private static RegistroCheckpointResultadoDTO resultado(EstadoRegistroCheckpoint estado, boolean duplicado) {
        return new RegistroCheckpointResultadoDTO(new CheckpointResponseDTO(5L, 1L, "id", EtapaCheckpoint.EN_REPARTO,
                EstadoPedido.EN_REPARTO, EstadoPedido.EN_REPARTO, 6.0, -75.0, 5.0, true, OrigenCheckpoint.OFFLINE,
                estado, estado == EstadoRegistroCheckpoint.PENDIENTE_REVISION ? "conflicto" : null,
                LocalDateTime.now(), LocalDateTime.now()), duplicado);
    }

    @Test
    @DisplayName("Procesa los eventos en el orden cronológico del dispositivo, no en el de llegada")
    void ordenCronologico() {
        OffsetDateTime base = OffsetDateTime.now().minusHours(1);
        ItemCheckpointOfflineDTO tardio = item(1L, base.plusMinutes(30));
        ItemCheckpointOfflineDTO temprano = item(2L, base);
        when(checkpoints.registrarSincronizado(anyLong(), any()))
                .thenReturn(resultado(EstadoRegistroCheckpoint.APLICADO, false));

        service.sincronizar(new SincronizarCheckpointsRequestDTO(List.of(tardio, temprano)));

        InOrder orden = inOrder(checkpoints);
        orden.verify(checkpoints).registrarSincronizado(eq(2L), eq(temprano.checkpoint()));
        orden.verify(checkpoints).registrarSincronizado(eq(1L), eq(tardio.checkpoint()));
    }

    @Test
    @DisplayName("Resume aplicados, duplicados, pendientes de revisión y rechazados sin abortar el lote")
    void resumen() {
        OffsetDateTime base = OffsetDateTime.now().minusHours(1);
        ItemCheckpointOfflineDTO aplicado = item(1L, base);
        ItemCheckpointOfflineDTO duplicado = item(1L, base.plusMinutes(1));
        ItemCheckpointOfflineDTO conflicto = item(1L, base.plusMinutes(2));
        ItemCheckpointOfflineDTO invalido = item(1L, base.plusMinutes(3));
        when(checkpoints.registrarSincronizado(1L, aplicado.checkpoint()))
                .thenReturn(resultado(EstadoRegistroCheckpoint.APLICADO, false));
        when(checkpoints.registrarSincronizado(1L, duplicado.checkpoint()))
                .thenReturn(resultado(EstadoRegistroCheckpoint.APLICADO, true));
        when(checkpoints.registrarSincronizado(1L, conflicto.checkpoint()))
                .thenReturn(resultado(EstadoRegistroCheckpoint.PENDIENTE_REVISION, false));
        when(checkpoints.registrarSincronizado(1L, invalido.checkpoint())).thenThrow(new CodigoQrInvalidoException());

        SincronizacionCheckpointsResponseDTO r = service.sincronizar(
                new SincronizarCheckpointsRequestDTO(List.of(invalido, conflicto, duplicado, aplicado)));

        assertThat(r.aplicados()).isEqualTo(1);
        assertThat(r.duplicados()).isEqualTo(1);
        assertThat(r.pendientesRevision()).isEqualTo(1);
        assertThat(r.rechazados()).isEqualTo(1);
        assertThat(r.resultados()).extracting(SincronizacionCheckpointsResponseDTO.ResultadoItem::estado)
                .containsExactly(EstadoSincronizacion.APLICADO, EstadoSincronizacion.DUPLICADO,
                        EstadoSincronizacion.PENDIENTE_REVISION, EstadoSincronizacion.RECHAZADO);
        assertThat(r.resultados().get(3).motivo()).contains("QR");
    }

    @Test
    @DisplayName("Una carrera de inserción sobre el mismo idEventoCliente se informa como duplicado")
    void carreraDeInsercion() {
        ItemCheckpointOfflineDTO evento = item(1L, OffsetDateTime.now().minusMinutes(5));
        when(checkpoints.registrarSincronizado(1L, evento.checkpoint()))
                .thenThrow(new DataIntegrityViolationException("uk_checkpoints_evento_cliente"));
        CheckpointPedido existente = CheckpointPedido.aplicado(1L, 7L, evento.checkpoint().idEventoCliente(),
                EtapaCheckpoint.EN_REPARTO, EstadoPedido.EN_REPARTO, EstadoPedido.EN_REPARTO,
                new UbicacionReportada(null, null, null, false), OrigenCheckpoint.OFFLINE, LocalDateTime.now());
        when(registros.findByIdEventoCliente(evento.checkpoint().idEventoCliente())).thenReturn(Optional.of(existente));

        SincronizacionCheckpointsResponseDTO r = service.sincronizar(new SincronizarCheckpointsRequestDTO(List.of(evento)));

        assertThat(r.duplicados()).isEqualTo(1);
    }
}
