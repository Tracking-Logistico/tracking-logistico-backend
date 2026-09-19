package com.udea.demo.rutas.application.service;

import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import com.udea.demo.pedidos.interfaces.services.PedidoServiceI;
import com.udea.demo.rutas.application.dto.AsignarEnvioRequestDTO;
import com.udea.demo.rutas.application.dto.ReasignarEnvioRequestDTO;
import com.udea.demo.rutas.application.dto.ReordenarRutaRequestDTO;
import com.udea.demo.rutas.application.dto.RutaResponseDTO;
import com.udea.demo.rutas.domain.exception.EnvioNoAsignadoException;
import com.udea.demo.rutas.domain.exception.EnvioYaAsignadoException;
import com.udea.demo.rutas.domain.exception.RutaNoEncontradaException;
import com.udea.demo.rutas.domain.model.EstadoParada;
import com.udea.demo.rutas.domain.model.ParadaRuta;
import com.udea.demo.rutas.domain.model.Ruta;
import com.udea.demo.rutas.interfaces.persistence.ParadaRutaRepository;
import com.udea.demo.rutas.interfaces.persistence.RutaRepository;
import com.udea.demo.usuarios.domain.model.Conductor;
import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.interfaces.persistence.ConductorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del RutaService (capa de aplicación) con colaboradores mockeados.
 *
 * Patrón AAA (Arrange - Act - Assert) con secciones marcadas.
 *
 * CP cubiertos:
 *  - CP-HU09-01: listarEnviosPendientesDeAsignacion() devuelve sólo los en tránsito no asignados.
 *  - CP-HU09-02: asignarEnvio() asocia el envío al conductor y crea/actualiza la ruta.
 *  - CP-HU09-02b: asignarEnvio() rechaza si el envío ya está asignado (EnvioYaAsignadoException).
 *  - CP-HU09-02c: asignarEnvio() lanza IllegalArgumentException si el usuario no es conductor.
 *  - CP-HU09-03: reordenarRuta() aplica el nuevo orden a las paradas.
 *  - CP-HU09-03b: reordenarRuta() lanza RutaNoEncontradaException si la ruta no existe.
 *  - CP-HU09-04: reasignarEnvio() remueve de la ruta origen y agrega a la ruta destino.
 *  - CP-HU09-04b: reasignarEnvio() lanza EnvioNoAsignadoException si el envío no está asignado.
 *  - CP-HU09-05: obtenerRutaActivaDeConductor() devuelve la ruta activa del conductor.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RutaService - aplicación (HU-09)")
class RutaServiceTest {

    @Mock private RutaRepository rutaRepository;
    @Mock private ParadaRutaRepository paradaRutaRepository;
    @Mock private GestorRutaActiva gestorRutaActiva;
    @Mock private PedidoServiceI pedidoServiceI;
    @Mock private ConductorRepository conductorRepository;

    @InjectMocks private RutaService rutaService;

    // ─── IDs de usuario (externos al módulo) y de conductor (internos) ──────
    private static final Long USUARIO_ID_CONDUCTOR = 5L;
    private static final Long CONDUCTOR_INTERNO_ID  = 1L;
    private static final Long USUARIO_ID_NUEVO      = 6L;
    private static final Long CONDUCTOR_INTERNO_NUEVO = 2L;
    private static final Long PEDIDO_ID             = 10L;
    private static final Long RUTA_ID               = 100L;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Conductor conductorFake(Long conductorId, Long usuarioId) {
        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .nombre("Conductor Test")
                .email("conductor@test.com")
                .password("hash")
                .rol(Rol.CONDUCTOR)
                .estado(EstadoUsuario.ACTIVO)
                .aceptoTerminos(true)
                .build();
        return Conductor.builder()
                .id(conductorId)
                .usuario(usuario)
                .licencia("LIC-001")
                .estado("ACTIVO")
                .build();
    }

    private Ruta rutaConParadas(Long conductorId, Long... pedidoIds) {
        Ruta ruta = Ruta.crear(conductorId, LocalDate.now());
        for (Long pid : pedidoIds) {
            ruta.agregarParada(pid);
        }
        return ruta;
    }

    private PedidoResponseDTO pedidoDTOEnTransito(Long pedidoId) {
        return new PedidoResponseDTO(
                pedidoId, "PED-001", 99L,
                "Calle 1", "Calle 2",
                "Paquete", 2.0, 20.0, 15.0, 10.0,
                TipoServicio.EXPRESS, Prioridad.ALTA, Prioridad.ALTA,
                EstadoPedido.EN_TRANSITO,
                null, 1L, LocalDateTime.now(), LocalDateTime.now(),
                "TRK-001", LocalDateTime.now(), false, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CP-HU09-01: Listar envíos pendientes de asignación
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("CP-HU09-01: listarEnviosPendientesDeAsignacion")
    class ListarEnviosPendientes {

        /**
         * CP-HU09-01: camino feliz — sólo los EN_TRANSITO que no tienen parada
         * PENDIENTE aparecen en la lista.
         */
        @Test
        @DisplayName("devuelve los envíos EN_TRANSITO que aún no están asignados a ningún conductor")
        void listarEnviosPendientes_feliz() {
            // Arrange
            PedidoResponseDTO pedidoNoAsignado  = pedidoDTOEnTransito(PEDIDO_ID);
            PedidoResponseDTO pedidoYaAsignado  = pedidoDTOEnTransito(20L);

            // Solo el pedido 20 tiene parada PENDIENTE (ya asignado)
            when(paradaRutaRepository.findPedidoIdsByEstado(EstadoParada.PENDIENTE))
                    .thenReturn(List.of(20L));
            when(pedidoServiceI.listarEnTransito())
                    .thenReturn(List.of(pedidoNoAsignado, pedidoYaAsignado));

            // Act
            List<PedidoResponseDTO> resultado = rutaService.listarEnviosPendientesDeAsignacion();

            // Assert — solo el que NO está asignado (PEDIDO_ID=10) debe aparecer
            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).id()).isEqualTo(PEDIDO_ID);
        }

        /** CP-HU09-01: si todos están asignados, devuelve lista vacía. */
        @Test
        @DisplayName("devuelve lista vacía si todos los envíos en tránsito ya están asignados")
        void listarEnviosPendientes_todosAsignados_listaVacia() {
            // Arrange
            when(paradaRutaRepository.findPedidoIdsByEstado(EstadoParada.PENDIENTE))
                    .thenReturn(List.of(PEDIDO_ID));
            when(pedidoServiceI.listarEnTransito())
                    .thenReturn(List.of(pedidoDTOEnTransito(PEDIDO_ID)));

            // Act
            List<PedidoResponseDTO> resultado = rutaService.listarEnviosPendientesDeAsignacion();

            // Assert
            assertThat(resultado).isEmpty();
        }

        /** CP-HU09-01: si no hay envíos en tránsito, devuelve lista vacía. */
        @Test
        @DisplayName("devuelve lista vacía si no hay envíos EN_TRANSITO")
        void listarEnviosPendientes_sinEnTransito_listaVacia() {
            // Arrange
            when(paradaRutaRepository.findPedidoIdsByEstado(EstadoParada.PENDIENTE))
                    .thenReturn(List.of());
            when(pedidoServiceI.listarEnTransito()).thenReturn(List.of());

            // Act
            List<PedidoResponseDTO> resultado = rutaService.listarEnviosPendientesDeAsignacion();

            // Assert
            assertThat(resultado).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CP-HU09-02: Asignación de envíos a un conductor
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("CP-HU09-02: asignarEnvio (asignación al conductor)")
    class AsignarEnvio {

        /** CP-HU09-02: camino feliz — el envío se asocia al conductor y la ruta se guarda. */
        @Test
        @DisplayName("asignarEnvio() asocia el pedido a la ruta del conductor y devuelve RutaResponseDTO")
        void asignarEnvio_feliz() {
            // Arrange
            AsignarEnvioRequestDTO dto = new AsignarEnvioRequestDTO(PEDIDO_ID, USUARIO_ID_CONDUCTOR);
            Conductor conductor = conductorFake(CONDUCTOR_INTERNO_ID, USUARIO_ID_CONDUCTOR);
            Ruta ruta = Ruta.crear(CONDUCTOR_INTERNO_ID, LocalDate.now());

            when(paradaRutaRepository.findByPedidoIdAndEstado(PEDIDO_ID, EstadoParada.PENDIENTE))
                    .thenReturn(Optional.empty()); // no está asignado aún
            when(conductorRepository.findByUsuarioId(USUARIO_ID_CONDUCTOR))
                    .thenReturn(Optional.of(conductor));
            when(gestorRutaActiva.obtenerOCrear(CONDUCTOR_INTERNO_ID)).thenReturn(ruta);
            when(rutaRepository.save(any(Ruta.class))).thenAnswer(inv -> inv.getArgument(0));
            when(conductorRepository.findById(CONDUCTOR_INTERNO_ID))
                    .thenReturn(Optional.of(conductor));

            // Act
            RutaResponseDTO resultado = rutaService.asignarEnvio(dto);

            // Assert
            assertThat(resultado).isNotNull();
            // El conductorId del DTO debe corresponder al usuarioId del conductor
            assertThat(resultado.conductorId()).isEqualTo(USUARIO_ID_CONDUCTOR);
            // La ruta debe contener la parada del pedido asignado
            assertThat(resultado.paradas()).hasSize(1);
            assertThat(resultado.paradas().get(0).pedidoId()).isEqualTo(PEDIDO_ID);
            verify(rutaRepository).save(any(Ruta.class));
        }

        /**
         * CP-HU09-02b: camino de error — el envío ya está asignado (PENDIENTE
         * en alguna ruta) → lanza EnvioYaAsignadoException.
         */
        @Test
        @DisplayName("asignarEnvio() lanza EnvioYaAsignadoException si el envío ya tiene parada PENDIENTE")
        void asignarEnvio_envioYaAsignado_lanzaExcepcion() {
            // Arrange
            AsignarEnvioRequestDTO dto = new AsignarEnvioRequestDTO(PEDIDO_ID, USUARIO_ID_CONDUCTOR);
            Ruta rutaExistente = Ruta.crear(CONDUCTOR_INTERNO_ID, LocalDate.now());
            ParadaRuta paradaExistente = rutaExistente.agregarParada(PEDIDO_ID);

            when(paradaRutaRepository.findByPedidoIdAndEstado(PEDIDO_ID, EstadoParada.PENDIENTE))
                    .thenReturn(Optional.of(paradaExistente));

            // Act & Assert
            assertThatThrownBy(() -> rutaService.asignarEnvio(dto))
                    .isInstanceOf(EnvioYaAsignadoException.class);
        }

        /**
         * CP-HU09-02c: camino de error — el usuarioId no corresponde a ningún conductor
         * → lanza IllegalArgumentException.
         */
        @Test
        @DisplayName("asignarEnvio() lanza IllegalArgumentException si el usuario no es conductor")
        void asignarEnvio_usuarioNoConductor_lanzaExcepcion() {
            // Arrange
            AsignarEnvioRequestDTO dto = new AsignarEnvioRequestDTO(PEDIDO_ID, USUARIO_ID_CONDUCTOR);

            when(paradaRutaRepository.findByPedidoIdAndEstado(PEDIDO_ID, EstadoParada.PENDIENTE))
                    .thenReturn(Optional.empty());
            when(conductorRepository.findByUsuarioId(USUARIO_ID_CONDUCTOR))
                    .thenReturn(Optional.empty()); // no existe conductor para este usuario

            // Act & Assert
            assertThatThrownBy(() -> rutaService.asignarEnvio(dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CP-HU09-03: Organización de la ruta
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("CP-HU09-03: reordenarRuta (organización de la ruta)")
    class ReordenarRuta {

        private static final Long PEDIDO_B = 20L;
        private static final Long PEDIDO_C = 30L;

        /** CP-HU09-03: camino feliz — el operador aplica un nuevo orden. */
        @Test
        @DisplayName("reordenarRuta() aplica el orden indicado y devuelve RutaResponseDTO actualizado")
        void reordenarRuta_feliz() {
            // Arrange
            Ruta ruta = rutaConParadas(CONDUCTOR_INTERNO_ID, PEDIDO_ID, PEDIDO_B, PEDIDO_C);
            ReordenarRutaRequestDTO dto = new ReordenarRutaRequestDTO(List.of(PEDIDO_C, PEDIDO_B, PEDIDO_ID));
            Conductor conductor = conductorFake(CONDUCTOR_INTERNO_ID, USUARIO_ID_CONDUCTOR);

            when(rutaRepository.findById(RUTA_ID)).thenReturn(Optional.of(ruta));
            when(rutaRepository.save(any(Ruta.class))).thenAnswer(inv -> inv.getArgument(0));
            when(conductorRepository.findById(CONDUCTOR_INTERNO_ID))
                    .thenReturn(Optional.of(conductor));

            // Act
            RutaResponseDTO resultado = rutaService.reordenarRuta(RUTA_ID, dto);

            // Assert — la primera parada del resultado debe ser PEDIDO_C (orden 1)
            assertThat(resultado.paradas()).isNotEmpty();
            assertThat(resultado.paradas().get(0).pedidoId()).isEqualTo(PEDIDO_C);
            assertThat(resultado.paradas().get(0).orden()).isEqualTo(1);
            verify(rutaRepository).save(any(Ruta.class));
        }

        /** CP-HU09-03b: camino de error — la ruta no existe. */
        @Test
        @DisplayName("reordenarRuta() lanza RutaNoEncontradaException si la ruta no existe")
        void reordenarRuta_rutaNoExiste_lanzaExcepcion() {
            // Arrange
            ReordenarRutaRequestDTO dto = new ReordenarRutaRequestDTO(List.of(PEDIDO_ID));
            when(rutaRepository.findById(RUTA_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> rutaService.reordenarRuta(RUTA_ID, dto))
                    .isInstanceOf(RutaNoEncontradaException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CP-HU09-04: Reasignación de envíos
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("CP-HU09-04: reasignarEnvio (reasignación a otro conductor)")
    class ReasignarEnvio {

        /**
         * CP-HU09-04: camino feliz — el envío se remueve del conductor original
         * y se agrega al nuevo conductor.
         */
        @Test
        @DisplayName("reasignarEnvio() transfiere el envío al nuevo conductor y devuelve la ruta destino")
        void reasignarEnvio_feliz() {
            // Arrange
            ReasignarEnvioRequestDTO dto = new ReasignarEnvioRequestDTO(PEDIDO_ID, USUARIO_ID_NUEVO);
            Conductor conductorOrigen = conductorFake(CONDUCTOR_INTERNO_ID, USUARIO_ID_CONDUCTOR);
            Conductor conductorDestino = conductorFake(CONDUCTOR_INTERNO_NUEVO, USUARIO_ID_NUEVO);

            Ruta rutaOrigen = rutaConParadas(CONDUCTOR_INTERNO_ID, PEDIDO_ID);
            // obtener la parada recién creada para poder retornarla desde el mock
            ParadaRuta paradaActual = rutaOrigen.getParadas().get(0);

            Ruta rutaDestino = Ruta.crear(CONDUCTOR_INTERNO_NUEVO, LocalDate.now());

            when(paradaRutaRepository.findByPedidoIdAndEstado(PEDIDO_ID, EstadoParada.PENDIENTE))
                    .thenReturn(Optional.of(paradaActual));
            when(rutaRepository.save(rutaOrigen)).thenReturn(rutaOrigen);
            when(conductorRepository.findByUsuarioId(USUARIO_ID_NUEVO))
                    .thenReturn(Optional.of(conductorDestino));
            when(gestorRutaActiva.obtenerOCrear(CONDUCTOR_INTERNO_NUEVO)).thenReturn(rutaDestino);
            when(rutaRepository.save(rutaDestino)).thenAnswer(inv -> inv.getArgument(0));
            when(conductorRepository.findById(CONDUCTOR_INTERNO_NUEVO))
                    .thenReturn(Optional.of(conductorDestino));

            // Act
            RutaResponseDTO resultado = rutaService.reasignarEnvio(dto);

            // Assert — la ruta destino recibe el pedido
            assertThat(resultado).isNotNull();
            assertThat(resultado.conductorId()).isEqualTo(USUARIO_ID_NUEVO);
            assertThat(resultado.paradas()).hasSize(1);
            assertThat(resultado.paradas().get(0).pedidoId()).isEqualTo(PEDIDO_ID);

            // La parada original debe haber sido cancelada (removida del panel del conductor original)
            assertThat(paradaActual.getEstado()).isEqualTo(EstadoParada.CANCELADA);

            // Se persiste la ruta origen con la parada cancelada (historial)
            verify(rutaRepository).save(rutaOrigen);
        }

        /**
         * CP-HU09-04b: camino de error — el envío no está asignado a ningún conductor
         * → lanza EnvioNoAsignadoException.
         */
        @Test
        @DisplayName("reasignarEnvio() lanza EnvioNoAsignadoException si el envío no tiene parada PENDIENTE")
        void reasignarEnvio_envioNoAsignado_lanzaExcepcion() {
            // Arrange
            ReasignarEnvioRequestDTO dto = new ReasignarEnvioRequestDTO(PEDIDO_ID, USUARIO_ID_NUEVO);
            when(paradaRutaRepository.findByPedidoIdAndEstado(PEDIDO_ID, EstadoParada.PENDIENTE))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> rutaService.reasignarEnvio(dto))
                    .isInstanceOf(EnvioNoAsignadoException.class);
        }

        /**
         * CP-HU09-04c: camino de error — el nuevo conductor no existe
         * → lanza IllegalArgumentException.
         */
        @Test
        @DisplayName("reasignarEnvio() lanza IllegalArgumentException si el nuevo usuario no es conductor")
        void reasignarEnvio_nuevoConductorInexistente_lanzaExcepcion() {
            // Arrange
            ReasignarEnvioRequestDTO dto = new ReasignarEnvioRequestDTO(PEDIDO_ID, USUARIO_ID_NUEVO);
            Ruta rutaOrigen = rutaConParadas(CONDUCTOR_INTERNO_ID, PEDIDO_ID);
            ParadaRuta paradaActual = rutaOrigen.getParadas().get(0);

            when(paradaRutaRepository.findByPedidoIdAndEstado(PEDIDO_ID, EstadoParada.PENDIENTE))
                    .thenReturn(Optional.of(paradaActual));
            when(rutaRepository.save(rutaOrigen)).thenReturn(rutaOrigen);
            when(conductorRepository.findByUsuarioId(USUARIO_ID_NUEVO))
                    .thenReturn(Optional.empty()); // el nuevo usuario no es conductor

            // Act & Assert
            assertThatThrownBy(() -> rutaService.reasignarEnvio(dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CP-HU09-05: Obtener ruta activa del conductor
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("CP-HU09-05: obtenerRutaActivaDeConductor")
    class ObtenerRutaActiva {

        /** CP-HU09-05: camino feliz — devuelve la ruta activa del conductor. */
        @Test
        @DisplayName("obtenerRutaActivaDeConductor() devuelve la ruta activa con sus paradas")
        void obtenerRutaActiva_feliz() {
            // Arrange
            Conductor conductor = conductorFake(CONDUCTOR_INTERNO_ID, USUARIO_ID_CONDUCTOR);
            Ruta ruta = rutaConParadas(CONDUCTOR_INTERNO_ID, PEDIDO_ID);

            when(conductorRepository.findByUsuarioId(USUARIO_ID_CONDUCTOR))
                    .thenReturn(Optional.of(conductor));
            when(rutaRepository.findByConductorIdAndFecha(CONDUCTOR_INTERNO_ID, LocalDate.now()))
                    .thenReturn(Optional.of(ruta));
            when(conductorRepository.findById(CONDUCTOR_INTERNO_ID))
                    .thenReturn(Optional.of(conductor));

            // Act
            RutaResponseDTO resultado = rutaService.obtenerRutaActivaDeConductor(USUARIO_ID_CONDUCTOR);

            // Assert — la ruta devuelta corresponde al conductor correcto con las paradas esperadas
            assertThat(resultado).isNotNull();
            assertThat(resultado.conductorId()).isEqualTo(USUARIO_ID_CONDUCTOR);
            assertThat(resultado.paradas()).hasSize(1);
            assertThat(resultado.paradas().get(0).pedidoId()).isEqualTo(PEDIDO_ID);
        }

        /** CP-HU09-05: camino de error — no existe ruta activa. */
        @Test
        @DisplayName("obtenerRutaActivaDeConductor() lanza RutaNoEncontradaException si no hay ruta activa")
        void obtenerRutaActiva_noExiste_lanzaExcepcion() {
            // Arrange
            Conductor conductor = conductorFake(CONDUCTOR_INTERNO_ID, USUARIO_ID_CONDUCTOR);

            when(conductorRepository.findByUsuarioId(USUARIO_ID_CONDUCTOR))
                    .thenReturn(Optional.of(conductor));
            when(rutaRepository.findByConductorIdAndFecha(CONDUCTOR_INTERNO_ID, LocalDate.now()))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> rutaService.obtenerRutaActivaDeConductor(USUARIO_ID_CONDUCTOR))
                    .isInstanceOf(RutaNoEncontradaException.class);
        }
    }
}
