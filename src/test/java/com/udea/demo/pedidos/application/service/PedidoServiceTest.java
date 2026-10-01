package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.EtiquetaEnvioResponseDTO;
import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.application.dto.RecibirPedidoRequestDTO;
import com.udea.demo.pedidos.application.dto.ValidarPedidoRequestDTO;
import com.udea.demo.pedidos.domain.exception.PedidoNoEncontradoException;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.Pedido;
import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import com.udea.demo.pedidos.domain.service.GeneradorEtiqueta;
import com.udea.demo.pedidos.domain.service.PrioridadStrategy;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.pedidos.interfaces.persistence.HistorialPedidoRepository;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoService - aplicación (por caso de prueba)")
class PedidoServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private PrioridadStrategy prioridadStrategy;
    @Mock private GeneradorNumeroPedido generadorNumeroPedido;
    @Mock private GeneradorNumeroTracking generadorNumeroTracking;
    @Mock private GeneradorEtiqueta generadorEtiqueta;
    @Mock private ActorAuthorizationService actorAuthorizationService;
    @Mock private HistorialPedidoRepository historialPedidoRepository;
    @Mock private LimitesServicioService limitesServicioService;
    @Mock private com.udea.demo.pedidos.interfaces.services.AccesoPedidoConductorI accesoConductor;

    @InjectMocks private PedidoService pedidoService;

    private static final String NUMERO_PEDIDO = "PED-20260214-8F4A29C1";
    private static final String NUMERO_TRACKING = "LT1234567890";

    @BeforeEach
    void actorAutenticado() {
        Usuario actor = Usuario.builder().id(1L).email("cliente@test.com").nombre("Cliente").rol(Rol.CLIENTE).activo(true).build();
        org.mockito.Mockito.lenient().when(actorAuthorizationService.actorActual()).thenReturn(actor);
        org.mockito.Mockito.lenient().when(actorAuthorizationService.clienteActualId()).thenReturn(10L);
        org.mockito.Mockito.lenient().when(actorAuthorizationService.operadorActualId()).thenReturn(99L);
    }

    private RecibirPedidoRequestDTO recibirRequest() {
        return new RecibirPedidoRequestDTO(
                "Carrera 7 #71-21, Bogotá", "Bogotá", "110111",
                "Calle 45 #12-30, Bogotá", "Bogotá", "110111", "Caja frágil",
                2.50, 30.0, 20.0, 15.0, TipoServicio.EXPRESS,
                "Destinatario", "+573001234567", "+573109876543");
    }

    private Pedido pedidoConId() {
        return Pedido.recibir(
                10L, "Carrera 7 #71-21, Bogotá", "Bogotá", "110111",
                "Calle 45 #12-30, Bogotá", "Bogotá", "110111", "Caja frágil",
                2.50, 30.0, 20.0, 15.0, TipoServicio.EXPRESS, NUMERO_PEDIDO, Prioridad.ALTA,
                "Destinatario", "+573001234567", "Cliente", "cliente@test.com", "+573109876543");
    }

    @Nested
    @DisplayName("CP-HU03A-01: recepción")
    class Recibir {

        @Test
        @DisplayName("recibir() genera número único, sugiere prioridad y guarda en SOLICITADO")
        void recibir_feliz() {

            RecibirPedidoRequestDTO dto = recibirRequest();
            when(prioridadStrategy.sugerir(dto.tipoServicio(), dto.pesoKg())).thenReturn(Prioridad.ALTA);
            when(generadorNumeroPedido.generar()).thenReturn(NUMERO_PEDIDO);
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoConId());

            PedidoResponseDTO respuesta = pedidoService.recibir(dto);

            assertThat(respuesta.numeroPedido()).isEqualTo(NUMERO_PEDIDO);
            assertThat(respuesta.estado()).isEqualTo(EstadoPedido.SOLICITADO);
            verify(pedidoRepository).save(any(Pedido.class));
        }

        @Test
        @DisplayName("obtener() de un pedido inexistente lanza PedidoNoEncontradoException")
        void obtener_error() {

            when(pedidoRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoService.obtener(1L))
                    .isInstanceOf(PedidoNoEncontradoException.class);
        }
    }

    @Test
    @DisplayName("CP-HU03A-02: validar() aprueba el pedido y guarda los cambios")
    void validar_feliz() {

        Pedido pedido = pedidoConId();
        ValidarPedidoRequestDTO dto = new ValidarPedidoRequestDTO(true, null, "ok", null, null, false);
        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoResponseDTO respuesta = pedidoService.validar(1L, dto);

        assertThat(respuesta.estado()).isEqualTo(EstadoPedido.SOLICITADO);
        verify(pedidoRepository).save(pedido);
    }

    @Test
    @DisplayName("CP-HU03A-03: validar() con prioridad confirmada la ajusta en el pedido")
    void validar_ajustePrioridad_feliz() {

        Pedido pedido = pedidoConId();
        ValidarPedidoRequestDTO dto = new ValidarPedidoRequestDTO(true, Prioridad.ALTA, null, null, null, false);
        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoResponseDTO respuesta = pedidoService.validar(1L, dto);

        assertThat(respuesta.prioridadConfirmada()).isEqualTo(Prioridad.ALTA);
    }

    @Nested
    @DisplayName("CP-HU03B-01: activación de tracking")
    class ActivarTracking {

        @Test
        @DisplayName("activarTracking() genera el tracking y pasa a CREADO")
        void activarTracking_feliz() {

            Pedido pedido = pedidoConId();
            pedido.validar(true, null, null, 99L);
            when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
            when(generadorNumeroTracking.generar()).thenReturn(NUMERO_TRACKING);
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

            PedidoResponseDTO respuesta = pedidoService.activarTracking(1L);

            assertThat(respuesta.estado()).isEqualTo(EstadoPedido.CREADO);
            assertThat(respuesta.numeroTracking()).isEqualTo(NUMERO_TRACKING);
        }

        @Test
        @DisplayName("BUG CP-HU03B-01: activar dos veces debería ser idempotente")
        void activarTracking_error_idempotencia() {

            Pedido pedido = pedidoConId();
            pedido.validar(true, null, null, 99L);
            when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
            when(generadorNumeroTracking.generar()).thenReturn(NUMERO_TRACKING);
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

            PedidoResponseDTO primera = pedidoService.activarTracking(1L);
            PedidoResponseDTO segunda = pedidoService.activarTracking(1L);

            assertThat(segunda.numeroTracking()).isEqualTo(primera.numeroTracking());
        }
    }

    @Nested
    @DisplayName("CP-HU03B-02: etiqueta")
    class GenerarEtiqueta {

        @Test
        @DisplayName("generarEtiqueta() imprime la etiqueta y conserva el tracking")
        void generarEtiqueta_feliz() {

            Pedido pedido = pedidoConId();
            pedido.validar(true, null, null, 99L);
            pedido.activarTracking(NUMERO_TRACKING);
            when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
            when(generadorEtiqueta.generar(pedido)).thenReturn("CONTENIDO-ETIQUETA");

            EtiquetaEnvioResponseDTO respuesta = pedidoService.generarEtiqueta(1L);

            assertThat(respuesta.numeroTracking()).isEqualTo(NUMERO_TRACKING);
            assertThat(respuesta.contenido()).isEqualTo("CONTENIDO-ETIQUETA");
        }

        @Test
        @DisplayName("generarEtiqueta() sin tracking activo lanza TrackingNoActivoException")
        void generarEtiqueta_error() {

            Pedido pedido = pedidoConId();
            when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));

            assertThatThrownBy(() -> pedidoService.generarEtiqueta(1L))
                    .isInstanceOf(com.udea.demo.pedidos.domain.exception.TrackingNoActivoException.class);
            verify(generadorEtiqueta, never()).generar(any());
        }
    }

    @Test
    @DisplayName("Un conductor no accede al envío de otro aunque conozca su ID")
    void conductorNoPuedeLeerPedidoAjeno() {
        Usuario conductor = Usuario.builder().id(77L).email("chofer@ejemplo.com")
                .rol(Rol.CONDUCTOR).activo(true).build();
        when(actorAuthorizationService.actorActual()).thenReturn(conductor);
        Pedido pedido = pedidoConId();
        org.springframework.test.util.ReflectionTestUtils.setField(pedido, "id", 10L);
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        assertThatThrownBy(() -> pedidoService.obtener(10L))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(accesoConductor).tieneAsignacionActiva(10L, 77L);
    }
    @Test
    @DisplayName("Despachos incluye los validados, creados y recibidos en origen sin mostrar solicitudes sin revisar")
    void listarDespachos_incluyeTodasLasEtapasDeDespacho() {
        Pedido pendiente = pedidoConId();
        Pedido validado = pedidoConId();
        validado.validar(true, null, null, 99L);
        Pedido recibidoEnOrigen = pedidoConId();
        recibidoEnOrigen.validar(true, null, null, 99L);
        recibidoEnOrigen.activarTracking(NUMERO_TRACKING);
        recibidoEnOrigen.cambiarEstadoLogistico(EstadoPedido.RECIBIDO_EN_ORIGEN);
        when(pedidoRepository.findByEstadoInOrderByFechaCreacionAsc(any())).thenReturn(
                java.util.List.of(pendiente, validado, recibidoEnOrigen));

        var resultado = pedidoService.listarDespachos();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(PedidoResponseDTO::estado)
                .containsExactly(EstadoPedido.SOLICITADO, EstadoPedido.RECIBIDO_EN_ORIGEN);
    }

}
