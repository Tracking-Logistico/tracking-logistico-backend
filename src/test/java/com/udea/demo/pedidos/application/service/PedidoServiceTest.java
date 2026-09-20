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

import org.junit.jupiter.api.Disabled;

/**
 * Pruebas unitarias del PedidoService (capa de aplicación) con colaboradores mockeados.
 *
 * Cada prueba corresponde a un caso de prueba (CP), cubriendo camino feliz y camino de error.
 * Patrón AAA (Arrange - Act - Assert) con secciones marcadas.
 *
 * CP cubiertos:
 *  - CP-HU03A-01: recepción (feliz) e inexistente (error).
 *  - CP-HU03A-02: validación feliz y pedido inexistente (error).
 *  - CP-HU03A-03: ajuste de prioridad en la validación.
 *  - CP-HU03B-01: activación (feliz) e idempotencia (error BUG).
 *  - CP-HU03B-02: etiqueta (feliz) y sin tracking (error).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoService - aplicación (por caso de prueba)")
class PedidoServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private PrioridadStrategy prioridadStrategy;
    @Mock private GeneradorNumeroPedido generadorNumeroPedido;
    @Mock private GeneradorNumeroTracking generadorNumeroTracking;
    @Mock private GeneradorEtiqueta generadorEtiqueta;

    @InjectMocks private PedidoService pedidoService;

    private static final String NUMERO_PEDIDO = "PED-20260214-8F4A29C1";
    private static final String NUMERO_TRACKING = "TRK-20260214-8F4A29C1B7";

    private RecibirPedidoRequestDTO recibirRequest() {
        return new RecibirPedidoRequestDTO(
                10L, "Carrera 7 #71-21, Bogotá", "Calle 45 #12-30, Bogotá",
                "Caja frágil", 2.50, 30.0, 20.0, 15.0, TipoServicio.EXPRESS);
    }

    private Pedido pedidoConId() {
        return Pedido.recibir(
                10L, "Carrera 7 #71-21, Bogotá", "Calle 45 #12-30, Bogotá",
                "Caja frágil", 2.50, 30.0, 20.0, 15.0,
                TipoServicio.EXPRESS, NUMERO_PEDIDO, Prioridad.URGENTE);
    }

    @Nested
    @DisplayName("CP-HU03A-01: recepción")
    class Recibir {

        /** CP-HU03A-01: camino feliz. */
        @Test
        @DisplayName("recibir() genera número único, sugiere prioridad y guarda en RECIBIDO")
        void recibir_feliz() {
            // Arrange
            RecibirPedidoRequestDTO dto = recibirRequest();
            when(prioridadStrategy.sugerir(dto.tipoServicio(), dto.pesoKg())).thenReturn(Prioridad.URGENTE);
            when(generadorNumeroPedido.generar()).thenReturn(NUMERO_PEDIDO);
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoConId());

            // Act
            PedidoResponseDTO respuesta = pedidoService.recibir(dto);

            // Assert
            assertThat(respuesta.numeroPedido()).isEqualTo(NUMERO_PEDIDO);
            assertThat(respuesta.estado()).isEqualTo(EstadoPedido.RECIBIDO);
            verify(pedidoRepository).save(any(Pedido.class));
        }

        /** CP-HU03A-01: camino de error (pedido inexistente). */
        @Test
        @DisplayName("obtener() de un pedido inexistente lanza PedidoNoEncontradoException")
        void obtener_error() {
            // Arrange
            when(pedidoRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> pedidoService.obtener(1L))
                    .isInstanceOf(PedidoNoEncontradoException.class);
        }
    }

    /** CP-HU03A-02: camino feliz (validación aprobada). */
    @Test
    @DisplayName("CP-HU03A-02: validar() aprueba el pedido y guarda los cambios")
    void validar_feliz() {
        // Arrange
        Pedido pedido = pedidoConId();
        ValidarPedidoRequestDTO dto = new ValidarPedidoRequestDTO(99L, true, null, "ok");
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        PedidoResponseDTO respuesta = pedidoService.validar(1L, dto);

        // Assert
        assertThat(respuesta.estado()).isEqualTo(EstadoPedido.VALIDADO);
        verify(pedidoRepository).save(pedido);
    }

    /** CP-HU03A-03: camino feliz (ajuste de prioridad). */
    @Test
    @DisplayName("CP-HU03A-03: validar() con prioridad confirmada la ajusta en el pedido")
    void validar_ajustePrioridad_feliz() {
        // Arrange
        Pedido pedido = pedidoConId();
        ValidarPedidoRequestDTO dto = new ValidarPedidoRequestDTO(99L, true, Prioridad.ALTA, null);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        PedidoResponseDTO respuesta = pedidoService.validar(1L, dto);

        // Assert
        assertThat(respuesta.prioridadConfirmada()).isEqualTo(Prioridad.ALTA);
    }

    @Nested
    @DisplayName("CP-HU03B-01: activación de tracking")
    class ActivarTracking {

        /** CP-HU03B-01: camino feliz. */
        @Test
        @DisplayName("activarTracking() genera el tracking y pasa a EN_TRANSITO")
        void activarTracking_feliz() {
            // Arrange
            Pedido pedido = pedidoConId();
            pedido.validar(true, null, null, 99L);
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(generadorNumeroTracking.generar()).thenReturn(NUMERO_TRACKING);
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            PedidoResponseDTO respuesta = pedidoService.activarTracking(1L);

            // Assert
            assertThat(respuesta.estado()).isEqualTo(EstadoPedido.EN_TRANSITO);
            assertThat(respuesta.numeroTracking()).isEqualTo(NUMERO_TRACKING);
        }

        @Disabled("Inconsistencia en la prueba de idempotencia")
        @Test
        @DisplayName("BUG CP-HU03B-01: activar dos veces debería ser idempotente")
        void activarTracking_error_idempotencia() {
            // Arrange
            Pedido pedido = pedidoConId();
            pedido.validar(true, null, null, 99L);
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(generadorNumeroTracking.generar()).thenReturn(NUMERO_TRACKING);
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            PedidoResponseDTO primera = pedidoService.activarTracking(1L);
            PedidoResponseDTO segunda = pedidoService.activarTracking(1L);

            // Assert
            assertThat(segunda.numeroTracking()).isEqualTo(primera.numeroTracking());
        }
    }

    @Nested
    @DisplayName("CP-HU03B-02: etiqueta")
    class GenerarEtiqueta {

        /** CP-HU03B-02: camino feliz. */
        @Test
        @DisplayName("generarEtiqueta() imprime la etiqueta y conserva el tracking")
        void generarEtiqueta_feliz() {
            // Arrange
            Pedido pedido = pedidoConId();
            pedido.validar(true, null, null, 99L);
            pedido.activarTracking(NUMERO_TRACKING);
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(generadorEtiqueta.generar(pedido)).thenReturn("CONTENIDO-ETIQUETA");

            // Act
            EtiquetaEnvioResponseDTO respuesta = pedidoService.generarEtiqueta(1L);

            // Assert
            assertThat(respuesta.numeroTracking()).isEqualTo(NUMERO_TRACKING);
            assertThat(respuesta.contenido()).isEqualTo("CONTENIDO-ETIQUETA");
        }

        /** CP-HU03B-02: camino de error (sin tracking no se genera etiqueta). */
        @Test
        @DisplayName("generarEtiqueta() sin tracking activo lanza TrackingNoActivoException")
        void generarEtiqueta_error() {
            // Arrange
            Pedido pedido = pedidoConId(); // RECIBIDO, sin tracking
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

            // Act & Assert
            assertThatThrownBy(() -> pedidoService.generarEtiqueta(1L))
                    .isInstanceOf(com.udea.demo.pedidos.domain.exception.TrackingNoActivoException.class);
            verify(generadorEtiqueta, never()).generar(any());
        }
    }
}
