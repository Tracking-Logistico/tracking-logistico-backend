package com.udea.demo.pedidos.infrastructure.controller;

import com.udea.demo.pedidos.application.dto.EtiquetaEnvioResponseDTO;
import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.application.dto.RecibirPedidoRequestDTO;
import com.udea.demo.pedidos.application.dto.ValidarPedidoRequestDTO;
import com.udea.demo.pedidos.domain.exception.PedidoNoEncontradoException;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import com.udea.demo.pedidos.interfaces.services.PedidoServiceI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del PedidoController instanciado a mano con el servicio mockeado.
 *
 * No se levanta contexto Spring: se usa "new PedidoController(mock)". Se verifica el mapeo a
 * ResponseEntity (status + body) y la delegación en el servicio.
 *
 * Cada prueba corresponde a un caso de prueba (CP), cubriendo camino feliz y camino de error.
 * Patrón AAA (Arrange - Act - Assert) con secciones marcadas.
 *
 * CP cubiertos:
 *  - CP-HU03A-01: POST /api/v1/pedidos (201) y obtención inexistente (error).
 *  - CP-HU03A-02: PUT /{id}/validar (200).
 *  - CP-HU03A-03: prioridad confirmada en la validación.
 *  - CP-HU03B-01: PUT /{id}/activar-tracking (200).
 *  - CP-HU03B-02: POST /{id}/etiqueta (200).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoController - web (por caso de prueba)")
class PedidoControllerTest {

    @Mock private PedidoServiceI pedidoService;

    @InjectMocks private PedidoController pedidoController;

    private static final Long PEDIDO_ID = 1L;
    private static final String NUMERO_PEDIDO = "PED-20260214-8F4A29C1";
    private static final String NUMERO_TRACKING = "TRK-20260214-8F4A29C1B7";

    private PedidoResponseDTO pedidoDTO(EstadoPedido estado) {
        return new PedidoResponseDTO(
                PEDIDO_ID, NUMERO_PEDIDO, 10L,
                "Carrera 7 #71-21, Bogotá", "Calle 45 #12-30, Bogotá",
                "Caja frágil", 2.50, 30.0, 20.0, 15.0,
                TipoServicio.EXPRESS, Prioridad.URGENTE, Prioridad.ALTA, estado,
                null, 99L, LocalDateTime.now(), null,
                estado == EstadoPedido.EN_TRANSITO ? NUMERO_TRACKING : null,
                null, false, null);
    }

    @Nested
    @DisplayName("CP-HU03A-01: recepción")
    class Recibir {

        /** CP-HU03A-01: camino feliz. */
        @Test
        @DisplayName("recibirPedido() devuelve 201 CREATED con el pedido recibido")
        void recibirPedido_feliz() {
            // Arrange
            RecibirPedidoRequestDTO dto = new RecibirPedidoRequestDTO(
                    10L, "Carrera 7 #71-21, Bogotá", "Calle 45 #12-30, Bogotá",
                    "Caja frágil", 2.50, 30.0, 20.0, 15.0, TipoServicio.EXPRESS);
            when(pedidoService.recibir(dto)).thenReturn(pedidoDTO(EstadoPedido.RECIBIDO));

            // Act
            ResponseEntity<PedidoResponseDTO> respuesta = pedidoController.recibirPedido(dto);

            // Assert
            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(respuesta.getBody().estado()).isEqualTo(EstadoPedido.RECIBIDO);
        }

        /** CP-HU03A-01: camino de error (propaga la excepción del servicio). */
        @Test
        @DisplayName("obtenerPedido() inexistente propaga PedidoNoEncontradoException")
        void obtenerPedido_error() {
            // Arrange
            when(pedidoService.obtener(PEDIDO_ID))
                    .thenThrow(new PedidoNoEncontradoException(PEDIDO_ID));

            // Act & Assert
            assertThatThrownBy(() -> pedidoController.obtenerPedido(PEDIDO_ID))
                    .isInstanceOf(PedidoNoEncontradoException.class);
        }
    }

    /** CP-HU03A-02: camino feliz (validación). */
    @Test
    @DisplayName("CP-HU03A-02: validarPedido() devuelve 200 OK con el pedido validado")
    void validarPedido_feliz() {
        // Arrange
        ValidarPedidoRequestDTO dto = new ValidarPedidoRequestDTO(99L, true, null, "ok");
        when(pedidoService.validar(eq(PEDIDO_ID), any(ValidarPedidoRequestDTO.class)))
                .thenReturn(pedidoDTO(EstadoPedido.VALIDADO));

        // Act
        ResponseEntity<PedidoResponseDTO> respuesta = pedidoController.validarPedido(PEDIDO_ID, dto);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().estado()).isEqualTo(EstadoPedido.VALIDADO);
    }

    /** CP-HU03A-03: camino feliz (ajuste de prioridad). */
    @Test
    @DisplayName("CP-HU03A-03: validarPedido() refleja la prioridad confirmada")
    void validarPedido_prioridad_feliz() {
        // Arrange
        ValidarPedidoRequestDTO dto = new ValidarPedidoRequestDTO(99L, true, Prioridad.ALTA, null);
        when(pedidoService.validar(eq(PEDIDO_ID), any(ValidarPedidoRequestDTO.class)))
                .thenReturn(pedidoDTO(EstadoPedido.VALIDADO));

        // Act
        ResponseEntity<PedidoResponseDTO> respuesta = pedidoController.validarPedido(PEDIDO_ID, dto);

        // Assert
        assertThat(respuesta.getBody().prioridadConfirmada()).isEqualTo(Prioridad.ALTA);
    }

    /** CP-HU03B-01: camino feliz (activación). */
    @Test
    @DisplayName("CP-HU03B-01: activarTracking() devuelve 200 OK con el tracking generado")
    void activarTracking_feliz() {
        // Arrange
        when(pedidoService.activarTracking(PEDIDO_ID)).thenReturn(pedidoDTO(EstadoPedido.EN_TRANSITO));

        // Act
        ResponseEntity<PedidoResponseDTO> respuesta = pedidoController.activarTracking(PEDIDO_ID);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().numeroTracking()).isEqualTo(NUMERO_TRACKING);
        verify(pedidoService).activarTracking(PEDIDO_ID);
    }

    /** CP-HU03B-02: camino feliz (etiqueta). */
    @Test
    @DisplayName("CP-HU03B-02: generarEtiqueta() devuelve 200 OK con la etiqueta")
    void generarEtiqueta_feliz() {
        // Arrange
        EtiquetaEnvioResponseDTO etiqueta = new EtiquetaEnvioResponseDTO(
                NUMERO_PEDIDO, NUMERO_TRACKING, "CONTENIDO-ETIQUETA", LocalDateTime.now());
        when(pedidoService.generarEtiqueta(PEDIDO_ID)).thenReturn(etiqueta);

        // Act
        ResponseEntity<EtiquetaEnvioResponseDTO> respuesta = pedidoController.generarEtiqueta(PEDIDO_ID);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().numeroTracking()).isEqualTo(NUMERO_TRACKING);
    }
}
