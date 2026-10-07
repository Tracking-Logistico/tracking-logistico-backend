package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.RegistrarCheckpointRequestDTO;
import com.udea.demo.pedidos.application.dto.RegistroCheckpointResultadoDTO;
import com.udea.demo.pedidos.domain.event.CheckpointPendienteRevisionEvent;
import com.udea.demo.pedidos.domain.event.CheckpointRegistradoEvent;
import com.udea.demo.pedidos.domain.exception.*;
import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.domain.service.ValidadorUbicacion;
import com.udea.demo.pedidos.interfaces.persistence.CheckpointPedidoRepository;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.pedidos.interfaces.services.AccesoPedidoConductorI;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HU-06 CheckpointService - registro de eventos logísticos")
class CheckpointServiceTest {
    private static final Long PEDIDO_ID = 1L;
    private static final Long CONDUCTOR = 7L;
    private static final String TRACKING = "LT1234567890";

    @Mock private PedidoRepository pedidos;
    @Mock private CheckpointPedidoRepository checkpoints;
    @Mock private RegistroHistorialPedido historial;
    @Mock private ActorAuthorizationService actores;
    @Mock private AccesoPedidoConductorI accesoConductor;
    @Mock private ApplicationEventPublisher eventos;

    private CheckpointService service;
    private Pedido pedido;

    @BeforeEach
    void setUp() {
        service = new CheckpointService(pedidos, checkpoints, historial, actores, accesoConductor,
                new ValidadorUbicacion(100), eventos, 5);
        pedido = pedidoEn(EstadoPedido.EN_REPARTO);
        lenient().when(actores.conductorActualUsuarioId()).thenReturn(CONDUCTOR);
        lenient().when(checkpoints.findByIdEventoCliente(anyString())).thenReturn(Optional.empty());
        lenient().when(pedidos.findByNumeroTracking(TRACKING)).thenAnswer(i -> Optional.of(pedido));
        lenient().when(pedidos.findByIdForUpdate(PEDIDO_ID)).thenAnswer(i -> Optional.of(pedido));
        lenient().when(accesoConductor.tieneAsignacionActiva(PEDIDO_ID, CONDUCTOR)).thenReturn(true);
        lenient().when(accesoConductor.fueAsignado(PEDIDO_ID, CONDUCTOR)).thenReturn(true);
        lenient().when(checkpoints.save(any(CheckpointPedido.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static Pedido pedidoEn(EstadoPedido estado) {
        return Pedido.builder().id(PEDIDO_ID).clienteId(10L).numeroTracking(TRACKING).estado(estado).build();
    }

    private static RegistrarCheckpointRequestDTO request(EtapaCheckpoint etapa, Double lat, Double lon, Double precision) {
        return new RegistrarCheckpointRequestDTO(CodigoQrEnvio.para(TRACKING).contenido(), etapa,
                UUID.randomUUID().toString(), lat, lon, precision, OffsetDateTime.now().minusMinutes(2));
    }

    private static RegistrarCheckpointRequestDTO requestValido() {
        return request(EtapaCheckpoint.EN_REPARTO, 6.2442, -75.5812, 10.0);
    }

    @Nested
    @DisplayName("Criterio 1 y 4: registro exitoso y trazabilidad")
    class RegistroExitoso {

        @Test
        @DisplayName("Registra el checkpoint con fecha, ubicación y usuario, y lo agrega a la línea de tiempo")
        void registraCheckpoint() {
            RegistrarCheckpointRequestDTO dto = requestValido();

            RegistroCheckpointResultadoDTO r = service.registrar(PEDIDO_ID, dto);

            assertThat(r.duplicado()).isFalse();
            assertThat(r.checkpoint().estadoRegistro()).isEqualTo(EstadoRegistroCheckpoint.APLICADO);
            assertThat(r.checkpoint().latitud()).isEqualTo(6.2442);
            assertThat(r.checkpoint().ubicacionConfiable()).isTrue();
            assertThat(r.checkpoint().origen()).isEqualTo(OrigenCheckpoint.EN_LINEA);
            ArgumentCaptor<CheckpointPedido> guardado = ArgumentCaptor.forClass(CheckpointPedido.class);
            verify(checkpoints).save(guardado.capture());
            assertThat(guardado.getValue().getUsuarioId()).isEqualTo(CONDUCTOR);
            assertThat(guardado.getValue().getFechaEvento()).isEqualTo(
                    dto.fechaDispositivo().atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDateTime());
            verify(historial).registrar(eq(PEDIDO_ID), eq(CONDUCTOR), eq(RegistroHistorialPedido.CHECKPOINT),
                    isNull(), contains("EN_REPARTO"), eq(guardado.getValue().getFechaEvento()));
        }

        @Test
        @DisplayName("Actualiza el estado según la etapa cuando es un avance válido")
        void actualizaEstadoSegunEtapa() {
            pedido = pedidoEn(EstadoPedido.EN_TRANSITO);

            RegistroCheckpointResultadoDTO r = service.registrar(PEDIDO_ID,
                    request(EtapaCheckpoint.EN_REPARTO, 6.0, -75.0, 5.0));

            assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.EN_REPARTO);
            assertThat(r.checkpoint().estadoAnterior()).isEqualTo(EstadoPedido.EN_TRANSITO);
            assertThat(r.checkpoint().estadoResultante()).isEqualTo(EstadoPedido.EN_REPARTO);
            verify(pedidos).save(pedido);
            verify(historial).registrar(eq(PEDIDO_ID), eq(CONDUCTOR), eq(RegistroHistorialPedido.ESTADO_LOGISTICO),
                    eq(RegistroHistorialPedido.CHECKPOINT), eq("EN_REPARTO"), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Etapa igual al estado actual solo registra trazabilidad, sin cambiar estado")
        void soloTrazabilidad() {
            service.registrar(PEDIDO_ID, requestValido());

            assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.EN_REPARTO);
            verify(pedidos, never()).save(any());
        }

        @Test
        @DisplayName("Publica el evento CheckpointRegistrado para el módulo de notificaciones")
        void publicaEvento() {
            service.registrar(PEDIDO_ID, requestValido());

            ArgumentCaptor<CheckpointRegistradoEvent> evento = ArgumentCaptor.forClass(CheckpointRegistradoEvent.class);
            verify(eventos).publishEvent(evento.capture());
            assertThat(evento.getValue().routingKey()).isEqualTo("pedido.checkpoint.registrado");
            assertThat(evento.getValue().pedidoId()).isEqualTo(PEDIDO_ID);
            assertThat(evento.getValue().numeroTracking()).isEqualTo(TRACKING);
            assertThat(evento.getValue().clienteId()).isEqualTo(10L);
            assertThat(evento.getValue().eventId()).isNotNull();
            assertThat(evento.getValue().estadoActual()).isEqualTo(EstadoPedido.EN_REPARTO);
        }

        @Test
        @DisplayName("Reenviar el mismo idEventoCliente es idempotente y no duplica el registro")
        void idempotente() {
            RegistrarCheckpointRequestDTO dto = requestValido();
            CheckpointPedido previo = CheckpointPedido.aplicado(PEDIDO_ID, CONDUCTOR, dto.idEventoCliente(),
                    EtapaCheckpoint.EN_REPARTO, EstadoPedido.EN_REPARTO, EstadoPedido.EN_REPARTO,
                    new UbicacionReportada(6.0, -75.0, 5.0, true), OrigenCheckpoint.EN_LINEA, LocalDateTime.now());
            when(checkpoints.findByIdEventoCliente(dto.idEventoCliente())).thenReturn(Optional.of(previo));

            RegistroCheckpointResultadoDTO r = service.registrar(PEDIDO_ID, dto);

            assertThat(r.duplicado()).isTrue();
            verify(checkpoints, never()).save(any());
            verifyNoInteractions(eventos);
        }

        @Test
        @DisplayName("Un idEventoCliente usado por otro conductor se rechaza")
        void idEventoDeOtroRegistro() {
            RegistrarCheckpointRequestDTO dto = requestValido();
            CheckpointPedido ajeno = CheckpointPedido.aplicado(PEDIDO_ID, 99L, dto.idEventoCliente(),
                    EtapaCheckpoint.EN_REPARTO, EstadoPedido.EN_REPARTO, EstadoPedido.EN_REPARTO,
                    new UbicacionReportada(null, null, null, false), OrigenCheckpoint.EN_LINEA, LocalDateTime.now());
            when(checkpoints.findByIdEventoCliente(dto.idEventoCliente())).thenReturn(Optional.of(ajeno));

            assertThatThrownBy(() -> service.registrar(PEDIDO_ID, dto)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Criterio 1: validación del código escaneado y del conductor")
    class ValidacionEscaneo {

        @Test
        @DisplayName("Rechaza un QR con formato o checksum inválido")
        void qrInvalido() {
            RegistrarCheckpointRequestDTO dto = new RegistrarCheckpointRequestDTO("LT1234567890|00000000",
                    EtapaCheckpoint.EN_REPARTO, UUID.randomUUID().toString(), 6.0, -75.0, 5.0, OffsetDateTime.now());

            assertThatThrownBy(() -> service.registrar(PEDIDO_ID, dto)).isInstanceOf(CodigoQrInvalidoException.class);
            verifyNoInteractions(eventos);
        }

        @Test
        @DisplayName("Rechaza un QR íntegro cuyo tracking no existe (código no reconocido)")
        void qrNoReconocido() {
            RegistrarCheckpointRequestDTO dto = new RegistrarCheckpointRequestDTO(
                    CodigoQrEnvio.para("LT0000000000").contenido(), EtapaCheckpoint.EN_REPARTO,
                    UUID.randomUUID().toString(), 6.0, -75.0, 5.0, OffsetDateTime.now());
            when(pedidos.findByNumeroTracking("LT0000000000")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.registrar(PEDIDO_ID, dto)).isInstanceOf(CodigoQrInvalidoException.class);
        }

        @Test
        @DisplayName("Rechaza el QR de otro envío distinto al esperado")
        void qrDeOtroEnvio() {
            Pedido otro = Pedido.builder().id(2L).numeroTracking("LT2222222222").estado(EstadoPedido.EN_REPARTO).build();
            when(pedidos.findByNumeroTracking("LT2222222222")).thenReturn(Optional.of(otro));
            RegistrarCheckpointRequestDTO dto = new RegistrarCheckpointRequestDTO(
                    CodigoQrEnvio.para("LT2222222222").contenido(), EtapaCheckpoint.EN_REPARTO,
                    UUID.randomUUID().toString(), 6.0, -75.0, 5.0, OffsetDateTime.now());

            assertThatThrownBy(() -> service.registrar(PEDIDO_ID, dto)).isInstanceOf(CodigoQrOtroEnvioException.class);
            verify(checkpoints, never()).save(any());
        }

        @Test
        @DisplayName("Rechaza al conductor sin asignación activa del envío")
        void conductorNoAsignado() {
            when(accesoConductor.tieneAsignacionActiva(PEDIDO_ID, CONDUCTOR)).thenReturn(false);

            assertThatThrownBy(() -> service.registrar(PEDIDO_ID, requestValido()))
                    .isInstanceOf(EnvioNoAsignadoAlConductorException.class);
            verify(pedidos, never()).findByIdForUpdate(any());
        }

        @Test
        @DisplayName("Rechaza una marca de tiempo del dispositivo en el futuro")
        void fechaFutura() {
            RegistrarCheckpointRequestDTO dto = new RegistrarCheckpointRequestDTO(CodigoQrEnvio.para(TRACKING).contenido(),
                    EtapaCheckpoint.EN_REPARTO, UUID.randomUUID().toString(), 6.0, -75.0, 5.0,
                    OffsetDateTime.now().plusHours(2));

            assertThatThrownBy(() -> service.registrar(PEDIDO_ID, dto))
                    .isInstanceOf(FechaDispositivoInvalidaException.class);
        }
    }

    @Nested
    @DisplayName("Criterio 2: prevención de estados inválidos")
    class EstadosFinales {

        @ParameterizedTest
        @EnumSource(value = EstadoPedido.class, names = {"ENTREGADO", "DEVOLUCION_AL_REMITENTE", "ENTREGA_FALLIDA_CERRADA"})
        @DisplayName("Rechaza registrar eventos sobre un envío finalizado")
        void rechazaFinalizado(EstadoPedido finalizado) {
            pedido = pedidoEn(finalizado);

            assertThatThrownBy(() -> service.registrar(PEDIDO_ID, requestValido()))
                    .isInstanceOf(EnvioFinalizadoException.class)
                    .hasMessageContaining("no puede modificar su estado");
            verify(checkpoints, never()).save(any());
            verifyNoInteractions(eventos);
        }

        @Test
        @DisplayName("Tras una devolución (parada cerrada) el conductor que lo tuvo asignado recibe 'envío finalizado'")
        void finalizadoConParadaCerrada() {
            pedido = pedidoEn(EstadoPedido.DEVOLUCION_AL_REMITENTE);
            when(accesoConductor.tieneAsignacionActiva(PEDIDO_ID, CONDUCTOR)).thenReturn(false);

            assertThatThrownBy(() -> service.registrar(PEDIDO_ID, requestValido()))
                    .isInstanceOf(EnvioFinalizadoException.class);
        }

        @Test
        @DisplayName("Rechaza en línea una etapa que retrocede el flujo")
        void rechazaRetroceso() {
            assertThatThrownBy(() -> service.registrar(PEDIDO_ID,
                    request(EtapaCheckpoint.RECIBIDO_EN_ORIGEN, 6.0, -75.0, 5.0)))
                    .isInstanceOf(TransicionEstadoInvalidaException.class);
        }
    }

    @Nested
    @DisplayName("Criterio 5: validación de ubicación")
    class Ubicacion {

        @Test
        @DisplayName("Rechaza coordenadas GPS fuera de rango")
        void gpsInvalido() {
            assertThatThrownBy(() -> service.registrar(PEDIDO_ID, request(EtapaCheckpoint.EN_REPARTO, 95.0, -75.0, 5.0)))
                    .isInstanceOf(UbicacionInvalidaException.class);
            verify(checkpoints, never()).save(any());
        }

        @Test
        @DisplayName("Registra pero marca como no confiable una precisión insuficiente")
        void precisionInsuficiente() {
            RegistroCheckpointResultadoDTO r = service.registrar(PEDIDO_ID,
                    request(EtapaCheckpoint.EN_REPARTO, 6.0, -75.0, 800.0));

            assertThat(r.checkpoint().ubicacionConfiable()).isFalse();
            assertThat(r.checkpoint().estadoRegistro()).isEqualTo(EstadoRegistroCheckpoint.APLICADO);
        }

        @Test
        @DisplayName("Registra pero marca como no confiables las coordenadas nulas")
        void coordenadasNulas() {
            RegistroCheckpointResultadoDTO r = service.registrar(PEDIDO_ID,
                    request(EtapaCheckpoint.EN_REPARTO, null, null, null));

            assertThat(r.checkpoint().ubicacionConfiable()).isFalse();
            assertThat(r.checkpoint().latitud()).isNull();
        }
    }

    @Nested
    @DisplayName("Criterio 3: registro sin conexión")
    class Offline {

        @Test
        @DisplayName("Un evento offline válido se aplica conservando la fecha del dispositivo")
        void offlineAplicado() {
            RegistrarCheckpointRequestDTO dto = new RegistrarCheckpointRequestDTO(CodigoQrEnvio.para(TRACKING).contenido(),
                    EtapaCheckpoint.EN_REPARTO, UUID.randomUUID().toString(), 6.0, -75.0, 5.0,
                    OffsetDateTime.now().minusHours(3));

            RegistroCheckpointResultadoDTO r = service.registrarSincronizado(PEDIDO_ID, dto);

            assertThat(r.checkpoint().origen()).isEqualTo(OrigenCheckpoint.OFFLINE);
            assertThat(r.checkpoint().estadoRegistro()).isEqualTo(EstadoRegistroCheckpoint.APLICADO);
            assertThat(r.checkpoint().fechaEvento()).isBefore(LocalDateTime.now().minusHours(2));
        }

        @Test
        @DisplayName("Si el envío ya fue entregado por otro medio, el evento queda pendiente de revisión")
        void conflictoPendienteRevision() {
            pedido = pedidoEn(EstadoPedido.ENTREGADO);

            RegistroCheckpointResultadoDTO r = service.registrarSincronizado(PEDIDO_ID, requestValido());

            assertThat(r.checkpoint().estadoRegistro()).isEqualTo(EstadoRegistroCheckpoint.PENDIENTE_REVISION);
            assertThat(r.checkpoint().motivoRevision()).contains("ENTREGADO");
            assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ENTREGADO);
            verify(pedidos, never()).save(any());
            verify(historial).registrar(eq(PEDIDO_ID), eq(CONDUCTOR), eq(RegistroHistorialPedido.CHECKPOINT_EN_REVISION),
                    isNull(), anyString(), any(LocalDateTime.class));
            ArgumentCaptor<CheckpointPendienteRevisionEvent> evento =
                    ArgumentCaptor.forClass(CheckpointPendienteRevisionEvent.class);
            verify(eventos).publishEvent(evento.capture());
            assertThat(evento.getValue().routingKey()).isEqualTo("pedido.checkpoint.pendiente-revision");
        }

        @Test
        @DisplayName("Una etapa offline incompatible con el estado actual queda pendiente de revisión")
        void transicionInvalidaPendienteRevision() {
            RegistroCheckpointResultadoDTO r = service.registrarSincronizado(PEDIDO_ID,
                    request(EtapaCheckpoint.RECIBIDO_EN_ORIGEN, 6.0, -75.0, 5.0));

            assertThat(r.checkpoint().estadoRegistro()).isEqualTo(EstadoRegistroCheckpoint.PENDIENTE_REVISION);
        }

        @Test
        @DisplayName("Offline acepta al conductor que tuvo la asignación aunque ya no esté activa")
        void offlineConAsignacionHistorica() {
            when(accesoConductor.fueAsignado(PEDIDO_ID, CONDUCTOR)).thenReturn(true);
            pedido = pedidoEn(EstadoPedido.DEVOLUCION_AL_REMITENTE);

            RegistroCheckpointResultadoDTO r = service.registrarSincronizado(PEDIDO_ID, requestValido());

            assertThat(r.checkpoint().estadoRegistro()).isEqualTo(EstadoRegistroCheckpoint.PENDIENTE_REVISION);
            verify(accesoConductor, never()).tieneAsignacionActiva(any(), any());
        }

        @Test
        @DisplayName("Offline rechaza a un conductor que nunca tuvo el envío asignado")
        void offlineSinAsignacion() {
            when(accesoConductor.fueAsignado(PEDIDO_ID, CONDUCTOR)).thenReturn(false);

            assertThatThrownBy(() -> service.registrarSincronizado(PEDIDO_ID, requestValido()))
                    .isInstanceOf(EnvioNoAsignadoAlConductorException.class);
        }
    }

    @Test
    @DisplayName("El conductor consulta cuántos eventos quedaron pendientes de revisión")
    void misPendientes() {
        CheckpointPedido pendiente = CheckpointPedido.pendienteRevision(PEDIDO_ID, CONDUCTOR, UUID.randomUUID().toString(),
                EtapaCheckpoint.EN_REPARTO, EstadoPedido.ENTREGADO, new UbicacionReportada(null, null, null, false),
                "motivo", LocalDateTime.now());
        when(checkpoints.findByUsuarioIdAndEstadoRegistroOrderByFechaEventoAsc(CONDUCTOR,
                EstadoRegistroCheckpoint.PENDIENTE_REVISION)).thenReturn(java.util.List.of(pendiente));

        var r = service.misPendientesRevision();

        assertThat(r.total()).isEqualTo(1);
        assertThat(r.checkpoints().get(0).motivoRevision()).isEqualTo("motivo");
    }
}
