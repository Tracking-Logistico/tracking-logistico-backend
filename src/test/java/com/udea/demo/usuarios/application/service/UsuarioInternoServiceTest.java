package com.udea.demo.usuarios.application.service;

import com.udea.demo.usuarios.application.dto.CrearUsuarioInternoCommand;
import com.udea.demo.usuarios.application.dto.EditarUsuarioInternoCommand;
import com.udea.demo.usuarios.application.dto.ResultadoCreacionUsuarioInterno;
import com.udea.demo.usuarios.application.dto.UsuarioResponseDTO;
import com.udea.demo.usuarios.domain.exception.CodigoEmpleadoRequeridoException;
import com.udea.demo.usuarios.domain.exception.CuentaInactivaException;
import com.udea.demo.usuarios.domain.exception.EmailYaRegistradoException;
import com.udea.demo.usuarios.domain.exception.LicenciaRequeridaException;
import com.udea.demo.usuarios.domain.exception.PasswordDebilException;
import com.udea.demo.usuarios.domain.exception.PasswordNoCoincideException;
import com.udea.demo.usuarios.domain.exception.RolInternoInvalidoException;
import com.udea.demo.usuarios.domain.exception.UsuarioNoEncontradoException;
import com.udea.demo.usuarios.domain.model.Cliente;
import com.udea.demo.usuarios.domain.model.Conductor;
import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.Operador;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.interfaces.persistence.ClienteRepository;
import com.udea.demo.usuarios.interfaces.persistence.ConductorRepository;
import com.udea.demo.usuarios.interfaces.persistence.OperadorRepository;
import com.udea.demo.usuarios.interfaces.persistence.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioInternoService - HU-01B: Creación y Gestión de Usuarios Internos")
class UsuarioInternoServiceTest {

    private static final String TEMPORAL = "Aa1@tempora16chr";
    private static final String HASH = "$2a$10$internoHash";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private ConductorRepository conductorRepository;
    @Mock private OperadorRepository operadorRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PasswordTemporalService passwordTemporalService;
    @Mock private ActorAuthorizationService actorAuthorizationService;
    @Mock private PasswordPolicyService passwordPolicyService;
    @Mock private com.udea.demo.usuarios.interfaces.persistence.SesionUsuarioRepository sesionRepository;

    @InjectMocks private UsuarioInternoService usuarioInternoService;

    private void stubCreacionExitosa() {
        when(passwordTemporalService.generar()).thenReturn(TEMPORAL);
        when(passwordEncoder.encode(TEMPORAL)).thenReturn(HASH);
        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(20L);
            return u;
        });
    }

    @Nested
    @DisplayName("crear")
    class Crear {

        @Test
        @DisplayName("Crea OPERADOR en PENDIENTE_ACTIVACION con clave temporal de 16 caracteres")
        void crearOperador_exitoso() {
            // Arrange
            stubCreacionExitosa();
            when(operadorRepository.existsByCodigoEmpleado("OP-001")).thenReturn(false);
            CrearUsuarioInternoCommand cmd = new CrearUsuarioInternoCommand(
                    "Laura Ops", "laura@tracking.com", "3001112233",
                    Rol.OPERADOR, "Calle Ops", null, "OP-001");

            // Act
            ResultadoCreacionUsuarioInterno resultado = usuarioInternoService.crear(cmd);

            // Assert
            assertThat(resultado.id()).isEqualTo(20L);
            assertThat(resultado.email()).isEqualTo("laura@tracking.com");
            assertThat(resultado.rol()).isEqualTo(Rol.OPERADOR);
            assertThat(resultado.passwordTemporal()).isEqualTo(TEMPORAL);
            assertThat(resultado.passwordTemporal()).hasSize(16);

            ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(usuarioCaptor.capture());
            assertThat(usuarioCaptor.getValue().getEstado()).isEqualTo(EstadoUsuario.PENDIENTE_ACTIVACION);
            assertThat(usuarioCaptor.getValue().getActivo()).isTrue();
            assertThat(usuarioCaptor.getValue().getPassword()).isEqualTo(HASH);

            ArgumentCaptor<Operador> operadorCaptor = ArgumentCaptor.forClass(Operador.class);
            verify(operadorRepository).save(operadorCaptor.capture());
            assertThat(operadorCaptor.getValue().getCodigoEmpleado()).isEqualTo("OP-001");
        }

        @Test
        @DisplayName("Crea CONDUCTOR en PENDIENTE_ACTIVACION")
        void crearConductor_exitoso() {
            // Arrange
            stubCreacionExitosa();
            CrearUsuarioInternoCommand cmd = new CrearUsuarioInternoCommand(
                    "Carlos Cond", "carlos@tracking.com", "3002223344",
                    Rol.CONDUCTOR, "Calle Cond", "LIC-999", null);

            // Act
            ResultadoCreacionUsuarioInterno resultado = usuarioInternoService.crear(cmd);

            // Assert
            assertThat(resultado.rol()).isEqualTo(Rol.CONDUCTOR);
            assertThat(resultado.passwordTemporal()).hasSize(16);
            ArgumentCaptor<Conductor> captor = ArgumentCaptor.forClass(Conductor.class);
            verify(conductorRepository).save(captor.capture());
            assertThat(captor.getValue().getLicencia()).isEqualTo("LIC-999");
            assertThat(captor.getValue().getEstado()).isEqualTo("ACTIVO");
        }

        @Test
        @DisplayName("Rechaza crear usuario interno con Rol.CLIENTE")
        void crearConRolCliente_lanzaRolInternoInvalido() {
            // Arrange
            CrearUsuarioInternoCommand cmd = new CrearUsuarioInternoCommand(
                    "Cliente", "c@tracking.com", "3000000000",
                    Rol.CLIENTE, "Dir", null, null);

            // Act & Assert
            assertThatThrownBy(() -> usuarioInternoService.crear(cmd))
                    .isInstanceOf(RolInternoInvalidoException.class)
                    .hasMessageContaining("OPERADOR o CONDUCTOR");
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("CONDUCTOR sin licencia lanza LicenciaRequeridaException")
        void conductorSinLicencia() {
            CrearUsuarioInternoCommand cmd = new CrearUsuarioInternoCommand(
                    "Carlos", "c@tracking.com", "3002223344",
                    Rol.CONDUCTOR, "Dir", "  ", null);

            assertThatThrownBy(() -> usuarioInternoService.crear(cmd))
                    .isInstanceOf(LicenciaRequeridaException.class);
        }

        @Test
        @DisplayName("OPERADOR sin código de empleado lanza CodigoEmpleadoRequeridoException")
        void operadorSinCodigo() {
            CrearUsuarioInternoCommand cmd = new CrearUsuarioInternoCommand(
                    "Laura", "l@tracking.com", "3001112233",
                    Rol.OPERADOR, "Dir", null, null);

            assertThatThrownBy(() -> usuarioInternoService.crear(cmd))
                    .isInstanceOf(CodigoEmpleadoRequeridoException.class);
        }

        @Test
        @DisplayName("Correo duplicado lanza EmailYaRegistradoException")
        void emailDuplicado() {
            when(usuarioRepository.existsByEmail("laura@tracking.com")).thenReturn(true);
            CrearUsuarioInternoCommand cmd = new CrearUsuarioInternoCommand(
                    "Laura", "laura@tracking.com", "3001112233",
                    Rol.OPERADOR, "Dir", null, "OP-001");

            assertThatThrownBy(() -> usuarioInternoService.crear(cmd))
                    .isInstanceOf(EmailYaRegistradoException.class)
                    .hasMessageContaining("laura@tracking.com");
        }

        @Test
        @DisplayName("Código de empleado duplicado lanza CodigoEmpleadoRequeridoException")
        void codigoEmpleadoDuplicado() {
            when(usuarioRepository.existsByEmail("laura@tracking.com")).thenReturn(false);
            when(operadorRepository.existsByCodigoEmpleado("OP-001")).thenReturn(true);
            CrearUsuarioInternoCommand cmd = new CrearUsuarioInternoCommand(
                    "Laura", "laura@tracking.com", "3001112233",
                    Rol.OPERADOR, "Dir", null, "OP-001");

            assertThatThrownBy(() -> usuarioInternoService.crear(cmd))
                    .isInstanceOf(CodigoEmpleadoRequeridoException.class);
        }
    }

    @Nested
    @DisplayName("editar")
    class Editar {

        private Usuario operadorPersistido() {
            return Usuario.builder()
                    .id(20L)
                    .nombre("Laura")
                    .email("laura@tracking.com")
                    .telefono("3001112233")
                    .direccion("Antigua")
                    .rol(Rol.OPERADOR)
                    .estado(EstadoUsuario.ACTIVO)
                    .activo(true)
                    .build();
        }

        @Test
        @DisplayName("Actualiza datos personales sin cambiar de rol")
        void editarDatos_sinCambioDeRol() {
            // Arrange
            Usuario usuario = operadorPersistido();
            when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
            when(operadorRepository.findByUsuarioId(20L)).thenReturn(Optional.of(
                    Operador.builder().id(1L).usuario(usuario).codigoEmpleado("OP-001").build()));
            EditarUsuarioInternoCommand cmd = new EditarUsuarioInternoCommand(
                    "Laura Nueva", "3009998877", "Nueva dir", null, null, "OP-002");

            // Act
            UsuarioResponseDTO dto = usuarioInternoService.editar(20L, cmd);

            // Assert
            assertThat(dto.nombre()).isEqualTo("Laura Nueva");
            assertThat(dto.telefono()).isEqualTo("3009998877");
            assertThat(dto.direccion()).isEqualTo("Nueva dir");
            assertThat(dto.rol()).isEqualTo(Rol.OPERADOR);
            verify(operadorRepository).save(any(Operador.class));
        }

        @Test
        @DisplayName("Reasigna OPERADOR a CONDUCTOR")
        void reasignaOperadorAConductor() {
            Usuario usuario = operadorPersistido();
            when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
            EditarUsuarioInternoCommand cmd = new EditarUsuarioInternoCommand(
                    null, null, null, Rol.CONDUCTOR, "LIC-100", null);

            UsuarioResponseDTO dto = usuarioInternoService.editar(20L, cmd);

            assertThat(dto.rol()).isEqualTo(Rol.CONDUCTOR);
            verify(operadorRepository, never()).deleteByUsuarioId(20L);
            verify(conductorRepository).save(any(Conductor.class));
            verify(sesionRepository).deleteByUsuarioId(20L);
        }

        @Test
        @DisplayName("Reasigna CONDUCTOR a OPERADOR")
        void reasignaConductorAOperador() {
            Usuario usuario = Usuario.builder()
                    .id(21L).nombre("Carlos").email("c@t.com")
                    .rol(Rol.CONDUCTOR).estado(EstadoUsuario.ACTIVO).activo(true).build();
            when(usuarioRepository.findById(21L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
            EditarUsuarioInternoCommand cmd = new EditarUsuarioInternoCommand(
                    null, null, null, Rol.OPERADOR, null, "OP-009");

            UsuarioResponseDTO dto = usuarioInternoService.editar(21L, cmd);

            assertThat(dto.rol()).isEqualTo(Rol.OPERADOR);
            verify(conductorRepository, never()).deleteByUsuarioId(21L);
            verify(operadorRepository).save(any(Operador.class));
            verify(sesionRepository).deleteByUsuarioId(21L);
        }

        @Test
        @DisplayName("Actualiza licencia si el rol CONDUCTOR no cambia")
        void actualizaLicenciaMismoRol() {
            Usuario usuario = Usuario.builder()
                    .id(21L).nombre("Carlos").email("c@t.com")
                    .rol(Rol.CONDUCTOR).estado(EstadoUsuario.ACTIVO).activo(true).build();
            Conductor conductor = Conductor.builder().id(2L).usuario(usuario).licencia("OLD").build();
            when(usuarioRepository.findById(21L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
            when(conductorRepository.findByUsuarioId(21L)).thenReturn(Optional.of(conductor));

            usuarioInternoService.editar(21L, new EditarUsuarioInternoCommand(
                    null, null, null, Rol.CONDUCTOR, "NEW-LIC", null));

            assertThat(conductor.getLicencia()).isEqualTo("NEW-LIC");
            verify(conductorRepository).save(conductor);
        }

        @Test
        @DisplayName("Permite pasar de OPERADOR a CLIENTE conservando el operador histórico")
        void reasignarACliente() {
            Usuario usuario = operadorPersistido();
            when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            UsuarioResponseDTO resultado = usuarioInternoService.editar(20L,
                    new EditarUsuarioInternoCommand(null, null, null, Rol.CLIENTE, null, null));

            assertThat(resultado.rol()).isEqualTo(Rol.CLIENTE);
            verify(clienteRepository).save(any(Cliente.class));
            verify(operadorRepository, never()).deleteByUsuarioId(20L);
            verify(sesionRepository).deleteByUsuarioId(20L);
        }

        @Test
        @DisplayName("Convierte CLIENTE sin verificar a OPERADOR y crea su fila operativa")
        void promoverClienteAOperador() {
            Usuario usuario = Usuario.builder().id(22L).rol(Rol.CLIENTE)
                    .estado(EstadoUsuario.PENDIENTE_VERIFICACION).activo(true).build();
            when(usuarioRepository.findById(22L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            UsuarioResponseDTO resultado = usuarioInternoService.editar(22L,
                    new EditarUsuarioInternoCommand(null, null, null, Rol.OPERADOR, null, "OP-022"));

            assertThat(resultado.rol()).isEqualTo(Rol.OPERADOR);
            assertThat(resultado.estado()).isEqualTo(EstadoUsuario.ACTIVO);
            verify(operadorRepository).save(any(Operador.class));
            verify(sesionRepository).deleteByUsuarioId(22L);
        }

        @Test
        @DisplayName("Convierte CLIENTE a CONDUCTOR sin perder historial de pedidos")
        void promoverClienteAConductor() {
            Usuario usuario = Usuario.builder().id(23L).rol(Rol.CLIENTE)
                    .estado(EstadoUsuario.ACTIVO).activo(true).build();
            when(usuarioRepository.findById(23L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            UsuarioResponseDTO resultado = usuarioInternoService.editar(23L,
                    new EditarUsuarioInternoCommand(null, null, null, Rol.CONDUCTOR, "LIC-023", null));

            assertThat(resultado.rol()).isEqualTo(Rol.CONDUCTOR);
            verify(conductorRepository).save(any(Conductor.class));
            verify(sesionRepository).deleteByUsuarioId(23L);
        }

        @Test
        @DisplayName("Repara la fila OPERADOR ausente sin modificar el rol")
        void repararOperadorSinFila() {
            Usuario usuario = operadorPersistido();
            when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            usuarioInternoService.editar(20L,
                    new EditarUsuarioInternoCommand(null, null, null, Rol.OPERADOR, null, "OP-020"));

            verify(operadorRepository).save(any(Operador.class));
            verify(sesionRepository, never()).deleteByUsuarioId(20L);
        }

        @Test
        @DisplayName("Cliente a conductor sin licencia devuelve validación y no cambia rol")
        void clienteAConductorSinLicencia() {
            Usuario usuario = Usuario.builder().id(23L).rol(Rol.CLIENTE)
                    .estado(EstadoUsuario.ACTIVO).build();
            when(usuarioRepository.findById(23L)).thenReturn(Optional.of(usuario));

            assertThatThrownBy(() -> usuarioInternoService.editar(23L,
                    new EditarUsuarioInternoCommand(null, null, null, Rol.CONDUCTOR, null, null)))
                    .isInstanceOf(LicenciaRequeridaException.class);
            assertThat(usuario.getRol()).isEqualTo(Rol.CLIENTE);
            verify(sesionRepository, never()).deleteByUsuarioId(23L);
        }

        @Test
        @DisplayName("Cambio a CONDUCTOR sin licencia lanza LicenciaRequeridaException")
        void cambioAConductorSinLicencia() {
            when(usuarioRepository.findById(20L)).thenReturn(Optional.of(operadorPersistido()));

            assertThatThrownBy(() -> usuarioInternoService.editar(20L,
                    new EditarUsuarioInternoCommand(null, null, null, Rol.CONDUCTOR, null, null)))
                    .isInstanceOf(LicenciaRequeridaException.class);
        }

        @Test
        @DisplayName("Cambio a OPERADOR sin código lanza CodigoEmpleadoRequeridoException")
        void cambioAOperadorSinCodigo() {
            Usuario conductor = Usuario.builder()
                    .id(21L).nombre("Carlos").email("c@t.com")
                    .rol(Rol.CONDUCTOR).estado(EstadoUsuario.ACTIVO).build();
            when(usuarioRepository.findById(21L)).thenReturn(Optional.of(conductor));

            assertThatThrownBy(() -> usuarioInternoService.editar(21L,
                    new EditarUsuarioInternoCommand(null, null, null, Rol.OPERADOR, null, " ")))
                    .isInstanceOf(CodigoEmpleadoRequeridoException.class);
        }

        @Test
        @DisplayName("Usuario inexistente lanza UsuarioNoEncontradoException")
        void usuarioNoEncontrado() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioInternoService.editar(99L,
                    new EditarUsuarioInternoCommand("N", null, null, null, null, null)))
                    .isInstanceOf(UsuarioNoEncontradoException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("desactivar")
    class Desactivar {

        @Test
        @DisplayName("Desactiva cuenta: activo=false y estado=INACTIVO")
        void desactivar_bajaLogica() {
            Usuario usuario = Usuario.builder()
                    .id(20L).activo(true).estado(EstadoUsuario.ACTIVO).rol(Rol.OPERADOR).build();
            when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));

            usuarioInternoService.desactivar(20L);

            assertThat(usuario.getActivo()).isFalse();
            assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.INACTIVO);
            verify(usuarioRepository).save(usuario);
            verify(conductorRepository, never()).deleteByUsuarioId(any());
            verify(operadorRepository, never()).deleteByUsuarioId(any());
        }

        @Test
        @DisplayName("Usuario inexistente lanza UsuarioNoEncontradoException")
        void desactivar_noEncontrado() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioInternoService.desactivar(99L))
                    .isInstanceOf(UsuarioNoEncontradoException.class);
        }
    }

    @Nested
    @DisplayName("cambiarPassword")
    class CambiarPassword {

        private Usuario pendienteActivacion() {
            return Usuario.builder()
                    .id(20L)
                    .password(HASH)
                    .activo(true)
                    .estado(EstadoUsuario.PENDIENTE_ACTIVACION)
                    .rol(Rol.OPERADOR)
                    .build();
        }

        @Test
        @DisplayName("Cambio inicial coincidente pasa de PENDIENTE_ACTIVACION a ACTIVO")
        void cambioInicial_activaCuenta() {
            // Arrange
            Usuario usuario = pendienteActivacion();
            when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(TEMPORAL, HASH)).thenReturn(true);
            when(passwordEncoder.matches("NuevaClave1!", HASH)).thenReturn(false);
            when(passwordEncoder.encode("NuevaClave1!")).thenReturn("$2a$10$nueva");

            // Act
            usuarioInternoService.cambiarPassword(20L, TEMPORAL, "NuevaClave1!", "NuevaClave1!");

            // Assert
            assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
            assertThat(usuario.getPassword()).isEqualTo("$2a$10$nueva");
            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("Confirmación no coincidente lanza PasswordNoCoincideException")
        void confirmacionNoCoincide() {
            assertThatThrownBy(() -> usuarioInternoService.cambiarPassword(
                    20L, TEMPORAL, "NuevaClave1!", "OtraClave1!"))
                    .isInstanceOf(PasswordNoCoincideException.class);
            verify(usuarioRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Cuenta inactiva lanza CuentaInactivaException")
        void cuentaInactiva() {
            Usuario usuario = pendienteActivacion();
            usuario.setActivo(false);
            when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));

            assertThatThrownBy(() -> usuarioInternoService.cambiarPassword(
                    20L, TEMPORAL, "NuevaClave1!", "NuevaClave1!"))
                    .isInstanceOf(CuentaInactivaException.class);
        }

        @Test
        @DisplayName("Password actual incorrecta lanza PasswordDebilException")
        void passwordActualIncorrecta() {
            when(usuarioRepository.findById(20L)).thenReturn(Optional.of(pendienteActivacion()));
            when(passwordEncoder.matches("mala", HASH)).thenReturn(false);

            assertThatThrownBy(() -> usuarioInternoService.cambiarPassword(
                    20L, "mala", "NuevaClave1!", "NuevaClave1!"))
                    .isInstanceOf(PasswordDebilException.class)
                    .hasMessage("La contraseña actual no es correcta");
        }

        @Test
        @DisplayName("Nueva password igual a la anterior lanza PasswordDebilException")
        void nuevaIgualAAnterior() {
            when(usuarioRepository.findById(20L)).thenReturn(Optional.of(pendienteActivacion()));
            when(passwordEncoder.matches(TEMPORAL, HASH)).thenReturn(true);

            assertThatThrownBy(() -> usuarioInternoService.cambiarPassword(
                    20L, TEMPORAL, TEMPORAL, TEMPORAL))
                    .isInstanceOf(PasswordDebilException.class)
                    .hasMessage("La nueva contraseña no puede ser igual a la anterior");
        }

        @Test
        @DisplayName("Usuario ya ACTIVO conserva el estado al cambiar password")
        void usuarioActivo_conservaEstado() {
            Usuario usuario = pendienteActivacion();
            usuario.setEstado(EstadoUsuario.ACTIVO);
            when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("Actual1!", HASH)).thenReturn(true);
            when(passwordEncoder.matches("NuevaClave1!", HASH)).thenReturn(false);
            when(passwordEncoder.encode("NuevaClave1!")).thenReturn("$2a$10$otra");

            usuarioInternoService.cambiarPassword(20L, "Actual1!", "NuevaClave1!", "NuevaClave1!");

            assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("Usuario inexistente lanza UsuarioNoEncontradoException")
        void usuarioNoEncontrado() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioInternoService.cambiarPassword(
                    99L, "a", "NuevaClave1!", "NuevaClave1!"))
                    .isInstanceOf(UsuarioNoEncontradoException.class);
        }
    }
}
