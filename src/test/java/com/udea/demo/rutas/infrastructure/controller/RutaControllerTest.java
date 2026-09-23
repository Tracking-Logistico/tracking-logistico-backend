package com.udea.demo.rutas.infrastructure.controller;

import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import com.udea.demo.rutas.application.dto.AsignarEnvioRequestDTO;
import com.udea.demo.rutas.application.dto.ParadaResponseDTO;
import com.udea.demo.rutas.application.dto.ReasignarEnvioRequestDTO;
import com.udea.demo.rutas.application.dto.ReordenarRutaRequestDTO;
import com.udea.demo.rutas.application.dto.RutaResponseDTO;
import com.udea.demo.rutas.domain.exception.EnvioNoAsignadoException;
import com.udea.demo.rutas.domain.exception.EnvioYaAsignadoException;
import com.udea.demo.rutas.domain.exception.RutaNoEncontradaException;
import com.udea.demo.rutas.interfaces.services.RutaServiceI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RutaController - web (HU-09)")
class RutaControllerTest {

    @Mock private RutaServiceI rutaService;

    @InjectMocks private RutaController rutaController;

    private static final Long CONDUCTOR_ID = 5L;
    private static final Long PEDIDO_ID    = 10L;
    private static final Long RUTA_ID      = 100L;

    private PedidoResponseDTO pedidoDTO(Long pedidoId) {
        return new PedidoResponseDTO(
                pedidoId, "PED-001", 99L,
                "Calle 1", "Calle 2",
                "Paquete", 2.0, 20.0, 15.0, 10.0,
                TipoServicio.EXPRESS, Prioridad.ALTA, Prioridad.ALTA,
                EstadoPedido.EN_TRANSITO,
                null, 1L, LocalDateTime.now(), LocalDateTime.now(),
                "TRK-001", LocalDateTime.now(), false, null);
    }

    private RutaResponseDTO rutaDTO(Long rutaId, Long conductorId, Long... pedidoIds) {
        List<ParadaResponseDTO> paradas = new java.util.ArrayList<>();
        int orden = 1;
        for (Long pid : pedidoIds) {
            paradas.add(new ParadaResponseDTO(orden * 100L, pid, orden, "PENDIENTE", LocalDateTime.now()));
            orden++;
        }
        return new RutaResponseDTO(rutaId, conductorId, LocalDate.now(), paradas);
    }

    @Nested
    @DisplayName("CP-HU09-01: GET /api/v1/rutas/envios-pendientes")
    class ListarEnviosPendientes {

        @Test
        @DisplayName("listarEnviosPendientes() devuelve 200 OK con la lista de envíos en tránsito no asignados")
        void listarEnviosPendientes_feliz() {

            List<PedidoResponseDTO> envios = List.of(pedidoDTO(PEDIDO_ID), pedidoDTO(20L));
            when(rutaService.listarEnviosPendientesDeAsignacion()).thenReturn(envios);

            ResponseEntity<List<PedidoResponseDTO>> respuesta = rutaController.listarEnviosPendientes();

            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(respuesta.getBody()).isNotNull();
            assertThat(respuesta.getBody()).hasSize(2);
            assertThat(respuesta.getBody().get(0).id()).isEqualTo(PEDIDO_ID);
            verify(rutaService).listarEnviosPendientesDeAsignacion();
        }

        @Test
        @DisplayName("listarEnviosPendientes() devuelve 200 OK con lista vacía si no hay envíos pendientes")
        void listarEnviosPendientes_listaVacia() {

            when(rutaService.listarEnviosPendientesDeAsignacion()).thenReturn(List.of());

            ResponseEntity<List<PedidoResponseDTO>> respuesta = rutaController.listarEnviosPendientes();

            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(respuesta.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("CP-HU09-02: POST /api/v1/rutas/asignaciones")
    class AsignarEnvio {

        @Test
        @DisplayName("asignarEnvio() devuelve 201 CREATED con la ruta que contiene el nuevo envío")
        void asignarEnvio_feliz() {

            AsignarEnvioRequestDTO dto = new AsignarEnvioRequestDTO(PEDIDO_ID, CONDUCTOR_ID);
            RutaResponseDTO rutaActualizada = rutaDTO(RUTA_ID, CONDUCTOR_ID, PEDIDO_ID);
            when(rutaService.asignarEnvio(dto)).thenReturn(rutaActualizada);

            ResponseEntity<RutaResponseDTO> respuesta = rutaController.asignarEnvio(dto);

            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(respuesta.getBody()).isNotNull();
            assertThat(respuesta.getBody().id()).isEqualTo(RUTA_ID);
            assertThat(respuesta.getBody().conductorId()).isEqualTo(CONDUCTOR_ID);
            assertThat(respuesta.getBody().paradas()).hasSize(1);
            assertThat(respuesta.getBody().paradas().get(0).pedidoId()).isEqualTo(PEDIDO_ID);
            verify(rutaService).asignarEnvio(dto);
        }

        @Test
        @DisplayName("asignarEnvio() propaga EnvioYaAsignadoException cuando el envío ya está asignado")
        void asignarEnvio_envioYaAsignado_propagaExcepcion() {

            AsignarEnvioRequestDTO dto = new AsignarEnvioRequestDTO(PEDIDO_ID, CONDUCTOR_ID);
            when(rutaService.asignarEnvio(dto)).thenThrow(new EnvioYaAsignadoException(PEDIDO_ID));

            assertThatThrownBy(() -> rutaController.asignarEnvio(dto))
                    .isInstanceOf(EnvioYaAsignadoException.class);
        }
    }

    @Nested
    @DisplayName("CP-HU09-03: PUT /api/v1/rutas/{rutaId}/orden")
    class ReordenarRuta {

        private static final Long PEDIDO_B = 20L;

        @Test
        @DisplayName("reordenarRuta() devuelve 200 OK con el orden actualizado")
        void reordenarRuta_feliz() {

            ReordenarRutaRequestDTO dto = new ReordenarRutaRequestDTO(List.of(PEDIDO_B, PEDIDO_ID));

            RutaResponseDTO rutaReordenada = new RutaResponseDTO(RUTA_ID, CONDUCTOR_ID, LocalDate.now(),
                    List.of(
                            new ParadaResponseDTO(200L, PEDIDO_B, 1, "PENDIENTE", LocalDateTime.now()),
                            new ParadaResponseDTO(100L, PEDIDO_ID, 2, "PENDIENTE", LocalDateTime.now())
                    ));
            when(rutaService.reordenarRuta(RUTA_ID, dto)).thenReturn(rutaReordenada);

            ResponseEntity<RutaResponseDTO> respuesta = rutaController.reordenarRuta(RUTA_ID, dto);

            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(respuesta.getBody()).isNotNull();
            assertThat(respuesta.getBody().paradas().get(0).pedidoId()).isEqualTo(PEDIDO_B);
            assertThat(respuesta.getBody().paradas().get(0).orden()).isEqualTo(1);
            assertThat(respuesta.getBody().paradas().get(1).pedidoId()).isEqualTo(PEDIDO_ID);
            assertThat(respuesta.getBody().paradas().get(1).orden()).isEqualTo(2);
            verify(rutaService).reordenarRuta(RUTA_ID, dto);
        }

        @Test
        @DisplayName("reordenarRuta() propaga RutaNoEncontradaException cuando la ruta no existe")
        void reordenarRuta_rutaNoExiste_propagaExcepcion() {

            ReordenarRutaRequestDTO dto = new ReordenarRutaRequestDTO(List.of(PEDIDO_ID));
            when(rutaService.reordenarRuta(eq(RUTA_ID), any(ReordenarRutaRequestDTO.class)))
                    .thenThrow(RutaNoEncontradaException.porId(RUTA_ID));

            assertThatThrownBy(() -> rutaController.reordenarRuta(RUTA_ID, dto))
                    .isInstanceOf(RutaNoEncontradaException.class);
        }
    }

    @Nested
    @DisplayName("CP-HU09-04: PUT /api/v1/rutas/asignaciones/reasignar")
    class ReasignarEnvio {

        private static final Long NUEVO_CONDUCTOR_ID = 6L;

        @Test
        @DisplayName("reasignarEnvio() devuelve 200 OK con la ruta del nuevo conductor que ya incluye el envío")
        void reasignarEnvio_feliz() {

            ReasignarEnvioRequestDTO dto = new ReasignarEnvioRequestDTO(PEDIDO_ID, NUEVO_CONDUCTOR_ID);
            RutaResponseDTO rutaDestino = rutaDTO(RUTA_ID + 1, NUEVO_CONDUCTOR_ID, PEDIDO_ID);
            when(rutaService.reasignarEnvio(dto)).thenReturn(rutaDestino);

            ResponseEntity<RutaResponseDTO> respuesta = rutaController.reasignarEnvio(dto);

            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(respuesta.getBody()).isNotNull();
            assertThat(respuesta.getBody().conductorId()).isEqualTo(NUEVO_CONDUCTOR_ID);
            assertThat(respuesta.getBody().paradas()).hasSize(1);
            assertThat(respuesta.getBody().paradas().get(0).pedidoId()).isEqualTo(PEDIDO_ID);
            verify(rutaService).reasignarEnvio(dto);
        }

        @Test
        @DisplayName("reasignarEnvio() propaga EnvioNoAsignadoException cuando el envío no está asignado")
        void reasignarEnvio_envioNoAsignado_propagaExcepcion() {

            ReasignarEnvioRequestDTO dto = new ReasignarEnvioRequestDTO(PEDIDO_ID, NUEVO_CONDUCTOR_ID);
            when(rutaService.reasignarEnvio(dto)).thenThrow(new EnvioNoAsignadoException(PEDIDO_ID));

            assertThatThrownBy(() -> rutaController.reasignarEnvio(dto))
                    .isInstanceOf(EnvioNoAsignadoException.class);
        }
    }

    @Nested
    @DisplayName("CP-HU09-05: GET /api/v1/rutas/conductores/{conductorId}")
    class ObtenerRutaDeConductor {

        @Test
        @DisplayName("obtenerRutaDeConductor() devuelve 200 OK con la ruta activa y sus paradas")
        void obtenerRutaDeConductor_feliz() {

            RutaResponseDTO ruta = rutaDTO(RUTA_ID, CONDUCTOR_ID, PEDIDO_ID, 20L);
            when(rutaService.obtenerRutaActivaDeConductor(CONDUCTOR_ID)).thenReturn(ruta);

            ResponseEntity<RutaResponseDTO> respuesta = rutaController.obtenerRutaDeConductor(CONDUCTOR_ID);

            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(respuesta.getBody()).isNotNull();
            assertThat(respuesta.getBody().conductorId()).isEqualTo(CONDUCTOR_ID);
            assertThat(respuesta.getBody().paradas()).hasSize(2);
            verify(rutaService).obtenerRutaActivaDeConductor(CONDUCTOR_ID);
        }

        @Test
        @DisplayName("obtenerRutaDeConductor() propaga RutaNoEncontradaException si no hay ruta activa")
        void obtenerRutaDeConductor_sinRutaActiva_propagaExcepcion() {

            when(rutaService.obtenerRutaActivaDeConductor(CONDUCTOR_ID))
                    .thenThrow(RutaNoEncontradaException.paraConductor(CONDUCTOR_ID));

            assertThatThrownBy(() -> rutaController.obtenerRutaDeConductor(CONDUCTOR_ID))
                    .isInstanceOf(RutaNoEncontradaException.class);
        }
    }
}
