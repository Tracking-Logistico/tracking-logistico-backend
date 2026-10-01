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
import org.junit.jupiter.api.BeforeEach;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("RutaService - aplicación (HU-09)")
class RutaServiceTest {

    @Mock private RutaRepository rutaRepository;
    @Mock private ParadaRutaRepository paradaRutaRepository;
    @Mock private GestorRutaActiva gestorRutaActiva;
    @Mock private PedidoServiceI pedidoServiceI;
    @Mock private ConductorRepository conductorRepository;
    @Mock private RegistroAsignacionService registroAsignacionService;

    @InjectMocks private RutaService rutaService;

    @BeforeEach
    void pedidosParaPruebas() {
        org.mockito.Mockito.lenient().when(pedidoServiceI.obtener(any(Long.class)))
            .thenAnswer(inv -> pedidoDTOEnTransito(inv.getArgument(0)));
    }

    private static final Long USUARIO_ID_CONDUCTOR = 5L;
    private static final Long CONDUCTOR_INTERNO_ID  = 1L;
    private static final Long USUARIO_ID_NUEVO      = 6L;
    private static final Long CONDUCTOR_INTERNO_NUEVO = 2L;
    private static final Long PEDIDO_ID             = 10L;
    private static final Long RUTA_ID               = 100L;

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

    private PedidoResponseDTO pedidoDTOReparto(Long id) {
        PedidoResponseDTO p = pedidoDTOEnTransito(id);
        return new PedidoResponseDTO(p.id(), p.numeroPedido(), p.clienteId(), p.direccionOrigen(),
            p.direccionDestino(), p.descripcionPaquete(), p.pesoKg(), p.largoCm(), p.anchoCm(), p.altoCm(),
            p.tipoServicio(), p.prioridadSugerida(), p.prioridadConfirmada(), EstadoPedido.EN_REPARTO,
            p.observacionesValidacion(), p.operadorValidadorId(), p.fechaCreacion(), p.fechaValidacion(),
            p.numeroTracking(), p.fechaActivacionTracking(), p.etiquetaImpresa(), p.fechaImpresionEtiqueta());
    }

    @Nested
    @DisplayName("CP-HU09-01: listarEnviosPendientesDeAsignacion")
    class ListarEnviosPendientes {

        @Test
        @DisplayName("devuelve los envíos EN_TRANSITO que aún no están asignados a ningún conductor")
        void listarEnviosPendientes_feliz() {

            PedidoResponseDTO pedidoNoAsignado  = pedidoDTOEnTransito(PEDIDO_ID);
            PedidoResponseDTO pedidoYaAsignado  = pedidoDTOEnTransito(20L);

            when(paradaRutaRepository.findPedidoIdsByEstado(EstadoParada.PENDIENTE))
                    .thenReturn(List.of(20L));
            when(pedidoServiceI.listarEnTransito())
                    .thenReturn(List.of(pedidoNoAsignado, pedidoYaAsignado));

            List<PedidoResponseDTO> resultado = rutaService.listarEnviosPendientesDeAsignacion();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).id()).isEqualTo(PEDIDO_ID);
        }

        @Test
        @DisplayName("devuelve lista vacía si todos los envíos en tránsito ya están asignados")
        void listarEnviosPendientes_todosAsignados_listaVacia() {

            when(paradaRutaRepository.findPedidoIdsByEstado(EstadoParada.PENDIENTE))
                    .thenReturn(List.of(PEDIDO_ID));
            when(pedidoServiceI.listarEnTransito())
                    .thenReturn(List.of(pedidoDTOEnTransito(PEDIDO_ID)));

            List<PedidoResponseDTO> resultado = rutaService.listarEnviosPendientesDeAsignacion();

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("devuelve lista vacía si no hay envíos EN_TRANSITO")
        void listarEnviosPendientes_sinEnTransito_listaVacia() {

            when(paradaRutaRepository.findPedidoIdsByEstado(EstadoParada.PENDIENTE))
                    .thenReturn(List.of());
            when(pedidoServiceI.listarEnTransito()).thenReturn(List.of());

            List<PedidoResponseDTO> resultado = rutaService.listarEnviosPendientesDeAsignacion();

            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("CP-HU09-02: asignarEnvio (asignación al conductor)")
    class AsignarEnvio {

        @Test
        @DisplayName("asignarEnvio() asocia el pedido a la ruta del conductor y devuelve RutaResponseDTO")
        void asignarEnvio_feliz() {

            AsignarEnvioRequestDTO dto = new AsignarEnvioRequestDTO(PEDIDO_ID, USUARIO_ID_CONDUCTOR);
            Conductor conductor = conductorFake(CONDUCTOR_INTERNO_ID, USUARIO_ID_CONDUCTOR);
            Ruta ruta = Ruta.crear(CONDUCTOR_INTERNO_ID, LocalDate.now());

            when(paradaRutaRepository.findByPedidoIdAndEstado(PEDIDO_ID, EstadoParada.PENDIENTE))
                    .thenReturn(Optional.empty());
            when(conductorRepository.findByUsuarioIdForUpdate(USUARIO_ID_CONDUCTOR))
                    .thenReturn(Optional.of(conductor));
            when(gestorRutaActiva.obtenerOCrear(CONDUCTOR_INTERNO_ID)).thenReturn(ruta);
            when(rutaRepository.saveAndFlush(any(Ruta.class))).thenAnswer(inv -> inv.getArgument(0));
            when(conductorRepository.findById(CONDUCTOR_INTERNO_ID))
                    .thenReturn(Optional.of(conductor));

            RutaResponseDTO resultado = rutaService.asignarEnvio(dto);

            assertThat(resultado).isNotNull();

            assertThat(resultado.conductorId()).isEqualTo(USUARIO_ID_CONDUCTOR);

            assertThat(resultado.paradas()).hasSize(1);
            assertThat(resultado.paradas().get(0).pedidoId()).isEqualTo(PEDIDO_ID);
            verify(rutaRepository).saveAndFlush(any(Ruta.class));
        }

        @Test
        @DisplayName("asignarEnvio() lanza EnvioYaAsignadoException si el envío ya tiene parada PENDIENTE")
        void asignarEnvio_envioYaAsignado_lanzaExcepcion() {

            AsignarEnvioRequestDTO dto = new AsignarEnvioRequestDTO(PEDIDO_ID, USUARIO_ID_CONDUCTOR);
            Ruta rutaExistente = Ruta.crear(CONDUCTOR_INTERNO_ID, LocalDate.now());
            ParadaRuta paradaExistente = rutaExistente.agregarParada(PEDIDO_ID);

            when(paradaRutaRepository.findByPedidoIdAndEstado(PEDIDO_ID, EstadoParada.PENDIENTE))
                    .thenReturn(Optional.of(paradaExistente));

            assertThatThrownBy(() -> rutaService.asignarEnvio(dto))
                    .isInstanceOf(EnvioYaAsignadoException.class);
        }

        @Test
        @DisplayName("asignarEnvio() lanza IllegalArgumentException si el usuario no es conductor")
        void asignarEnvio_usuarioNoConductor_lanzaExcepcion() {

            AsignarEnvioRequestDTO dto = new AsignarEnvioRequestDTO(PEDIDO_ID, USUARIO_ID_CONDUCTOR);

            when(paradaRutaRepository.findByPedidoIdAndEstado(PEDIDO_ID, EstadoParada.PENDIENTE))
                    .thenReturn(Optional.empty());
            when(conductorRepository.findByUsuarioIdForUpdate(USUARIO_ID_CONDUCTOR))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> rutaService.asignarEnvio(dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("CP-HU09-03: reordenarRuta (organización de la ruta)")
    class ReordenarRuta {

        private static final Long PEDIDO_B = 20L;
        private static final Long PEDIDO_C = 30L;

        @Test
        @DisplayName("reordenarRuta() aplica el orden indicado y devuelve RutaResponseDTO actualizado")
        void reordenarRuta_feliz() {

            Ruta ruta = rutaConParadas(CONDUCTOR_INTERNO_ID, PEDIDO_ID, PEDIDO_B, PEDIDO_C);
            ReordenarRutaRequestDTO dto = new ReordenarRutaRequestDTO(List.of(PEDIDO_C, PEDIDO_B, PEDIDO_ID));
            Conductor conductor = conductorFake(CONDUCTOR_INTERNO_ID, USUARIO_ID_CONDUCTOR);

            when(rutaRepository.findById(RUTA_ID)).thenReturn(Optional.of(ruta));
            when(rutaRepository.save(any(Ruta.class))).thenAnswer(inv -> inv.getArgument(0));
            when(conductorRepository.findById(CONDUCTOR_INTERNO_ID))
                    .thenReturn(Optional.of(conductor));

            RutaResponseDTO resultado = rutaService.reordenarRuta(RUTA_ID, dto);

            assertThat(resultado.paradas()).isNotEmpty();
            assertThat(resultado.paradas().get(0).pedidoId()).isEqualTo(PEDIDO_C);
            assertThat(resultado.paradas().get(0).orden()).isEqualTo(1);
            verify(rutaRepository).save(any(Ruta.class));
        }

        @Test
        @DisplayName("reordenarRuta() lanza RutaNoEncontradaException si la ruta no existe")
        void reordenarRuta_rutaNoExiste_lanzaExcepcion() {

            ReordenarRutaRequestDTO dto = new ReordenarRutaRequestDTO(List.of(PEDIDO_ID));
            when(rutaRepository.findById(RUTA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> rutaService.reordenarRuta(RUTA_ID, dto))
                    .isInstanceOf(RutaNoEncontradaException.class);
        }
    }

    @Nested
    @DisplayName("CP-HU09-04: reasignarEnvio (reasignación a otro conductor)")
    class ReasignarEnvio {

        @Test
        @DisplayName("reasignarEnvio() transfiere el envío al nuevo conductor y devuelve la ruta destino")
        void reasignarEnvio_feliz() {

            ReasignarEnvioRequestDTO dto = new ReasignarEnvioRequestDTO(PEDIDO_ID, USUARIO_ID_NUEVO);
            Conductor conductorOrigen = conductorFake(CONDUCTOR_INTERNO_ID, USUARIO_ID_CONDUCTOR);
            Conductor conductorDestino = conductorFake(CONDUCTOR_INTERNO_NUEVO, USUARIO_ID_NUEVO);

            Ruta rutaOrigen = rutaConParadas(CONDUCTOR_INTERNO_ID, PEDIDO_ID);

            ParadaRuta paradaActual = rutaOrigen.getParadas().get(0);

            Ruta rutaDestino = Ruta.crear(CONDUCTOR_INTERNO_NUEVO, LocalDate.now());

            when(paradaRutaRepository.findByPedidoIdAndEstado(PEDIDO_ID, EstadoParada.PENDIENTE))
                    .thenReturn(Optional.of(paradaActual));
            when(rutaRepository.saveAndFlush(rutaOrigen)).thenReturn(rutaOrigen);
            when(conductorRepository.findByUsuarioIdForUpdate(USUARIO_ID_NUEVO))
                    .thenReturn(Optional.of(conductorDestino));
            when(gestorRutaActiva.obtenerOCrear(CONDUCTOR_INTERNO_NUEVO)).thenReturn(rutaDestino);
            when(rutaRepository.saveAndFlush(rutaDestino)).thenAnswer(inv -> inv.getArgument(0));
            when(conductorRepository.findById(CONDUCTOR_INTERNO_NUEVO))
                    .thenReturn(Optional.of(conductorDestino));

            when(pedidoServiceI.obtener(PEDIDO_ID)).thenReturn(pedidoDTOReparto(PEDIDO_ID));

            RutaResponseDTO resultado = rutaService.reasignarEnvio(dto);

            assertThat(resultado).isNotNull();
            assertThat(resultado.conductorId()).isEqualTo(USUARIO_ID_NUEVO);
            assertThat(resultado.paradas()).hasSize(1);
            assertThat(resultado.paradas().get(0).pedidoId()).isEqualTo(PEDIDO_ID);

            assertThat(paradaActual.getEstado()).isEqualTo(EstadoParada.CANCELADA);

            verify(rutaRepository).saveAndFlush(rutaOrigen);
        }

        @Test
        @DisplayName("reasignarEnvio() lanza EnvioNoAsignadoException si el envío no tiene parada PENDIENTE")
        void reasignarEnvio_envioNoAsignado_lanzaExcepcion() {

            ReasignarEnvioRequestDTO dto = new ReasignarEnvioRequestDTO(PEDIDO_ID, USUARIO_ID_NUEVO);
            when(paradaRutaRepository.findByPedidoIdAndEstado(PEDIDO_ID, EstadoParada.PENDIENTE))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> rutaService.reasignarEnvio(dto))
                    .isInstanceOf(EnvioNoAsignadoException.class);
        }

        @Test
        @DisplayName("reasignarEnvio() lanza IllegalArgumentException si el nuevo usuario no es conductor")
        void reasignarEnvio_nuevoConductorInexistente_lanzaExcepcion() {

            ReasignarEnvioRequestDTO dto = new ReasignarEnvioRequestDTO(PEDIDO_ID, USUARIO_ID_NUEVO);
            Ruta rutaOrigen = rutaConParadas(CONDUCTOR_INTERNO_ID, PEDIDO_ID);
            ParadaRuta paradaActual = rutaOrigen.getParadas().get(0);

            when(paradaRutaRepository.findByPedidoIdAndEstado(PEDIDO_ID, EstadoParada.PENDIENTE))
                    .thenReturn(Optional.of(paradaActual));
            when(conductorRepository.findByUsuarioIdForUpdate(USUARIO_ID_NUEVO))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> rutaService.reasignarEnvio(dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("CP-HU09-05: obtenerRutaActivaDeConductor")
    class ObtenerRutaActiva {

        @Test
        @DisplayName("obtenerRutaActivaDeConductor() devuelve la ruta activa con sus paradas")
        void obtenerRutaActiva_feliz() {

            Conductor conductor = conductorFake(CONDUCTOR_INTERNO_ID, USUARIO_ID_CONDUCTOR);
            Ruta ruta = rutaConParadas(CONDUCTOR_INTERNO_ID, PEDIDO_ID);

            when(conductorRepository.findByUsuarioId(USUARIO_ID_CONDUCTOR))
                    .thenReturn(Optional.of(conductor));
            when(rutaRepository.findByConductorIdAndFecha(CONDUCTOR_INTERNO_ID, LocalDate.now()))
                    .thenReturn(Optional.of(ruta));
            when(conductorRepository.findById(CONDUCTOR_INTERNO_ID))
                    .thenReturn(Optional.of(conductor));

            RutaResponseDTO resultado = rutaService.obtenerRutaActivaDeConductor(USUARIO_ID_CONDUCTOR);

            assertThat(resultado).isNotNull();
            assertThat(resultado.conductorId()).isEqualTo(USUARIO_ID_CONDUCTOR);
            assertThat(resultado.paradas()).hasSize(1);
            assertThat(resultado.paradas().get(0).pedidoId()).isEqualTo(PEDIDO_ID);
        }

        @Test
        @DisplayName("obtenerRutaActivaDeConductor() lanza RutaNoEncontradaException si no hay ruta activa")
        void obtenerRutaActiva_noExiste_lanzaExcepcion() {

            Conductor conductor = conductorFake(CONDUCTOR_INTERNO_ID, USUARIO_ID_CONDUCTOR);

            when(conductorRepository.findByUsuarioId(USUARIO_ID_CONDUCTOR))
                    .thenReturn(Optional.of(conductor));
            when(rutaRepository.findByConductorIdAndFecha(CONDUCTOR_INTERNO_ID, LocalDate.now()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> rutaService.obtenerRutaActivaDeConductor(USUARIO_ID_CONDUCTOR))
                    .isInstanceOf(RutaNoEncontradaException.class);
        }
    }

    @Test
    @DisplayName("Asignación masiva exige identificadores distintos")
    void loteNoAdmiteDuplicados() {
        var dto = new com.udea.demo.rutas.application.dto.AsignacionMasivaRequestDTO(
            USUARIO_ID_CONDUCTOR, java.util.List.of(PEDIDO_ID, PEDIDO_ID));
        assertThatThrownBy(() -> rutaService.asignarVarios(dto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("distintos");
        org.mockito.Mockito.verifyNoInteractions(gestorRutaActiva);
    }
}
