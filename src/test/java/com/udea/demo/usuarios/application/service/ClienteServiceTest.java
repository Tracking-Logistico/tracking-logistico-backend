package com.udea.demo.usuarios.application.service;

import com.udea.demo.usuarios.application.dto.ActualizarPerfilRequestDTO;
import com.udea.demo.usuarios.application.dto.ClienteResponseDTO;
import com.udea.demo.usuarios.application.dto.RegistroClienteRequestDTO;
import com.udea.demo.usuarios.application.dto.UsuarioResponseDTO;
import com.udea.demo.usuarios.domain.model.Cliente;
import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.TokenVerificacion;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.interfaces.persistence.ClienteRepository;
import com.udea.demo.usuarios.interfaces.persistence.TokenVerificacionRepository;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService - HU-01A: Registro y Gestión de Clientes")
class ClienteServiceTest {

    private static final String PASSWORD = "Password123!";
    private static final String HASH = "$2a$10$hashedCliente";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private TokenVerificacionRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private com.udea.demo.usuarios.interfaces.services.EmailServiceI emailService;
    @Mock private ActorAuthorizationService actorAuthorizationService;
    @Mock private PasswordPolicyService passwordPolicyService;

    @org.junit.jupiter.api.BeforeEach
    void configuration() {
        org.springframework.test.util.ReflectionTestUtils.setField(clienteService,
                "verificationUrl", "http://localhost:5173/verificar");
    }

    @InjectMocks private ClienteService clienteService;

    private RegistroClienteRequestDTO registroValido() {
        return new RegistroClienteRequestDTO(
                "Ana Pérez",
                "ana@tracking.com",
                PASSWORD,
                PASSWORD,
                "3001234567",
                "Calle 10 #5-20",
                "Medellín",
                true,
                "T&C-v1.0"
        );
    }

    private void stubPersistenciaRegistro() {
        when(usuarioRepository.existsByEmail("ana@tracking.com")).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });
    }

    @Nested
    @DisplayName("registrarCliente")
    class RegistrarCliente {

        @Test
        @DisplayName("Registro exitoso asigna Rol.CLIENTE, PENDIENTE_VERIFICACION y token de 2h")
        void registroExitoso_asignaRolEstadoYTokenDeDosHoras() {
            // Arrange
            stubPersistenciaRegistro();
            LocalDateTime antes = LocalDateTime.now();

            // Act
            ClienteResponseDTO respuesta = clienteService.registrarCliente(registroValido());

            // Assert
            assertThat(respuesta.idCliente()).isEqualTo(10L);
            assertThat(respuesta.email()).isEqualTo("ana@tracking.com");
            assertThat(respuesta.rol()).isEqualTo(Rol.CLIENTE);
            assertThat(respuesta.estado()).isEqualTo(EstadoUsuario.PENDIENTE_VERIFICACION);
            assertThat(respuesta.ciudad()).isEqualTo("Medellín");

            ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(usuarioCaptor.capture());
            Usuario guardado = usuarioCaptor.getValue();
            assertThat(guardado.getRol()).isEqualTo(Rol.CLIENTE);
            assertThat(guardado.getEstado()).isEqualTo(EstadoUsuario.PENDIENTE_VERIFICACION);
            assertThat(guardado.getActivo()).isTrue();
            assertThat(guardado.getPassword()).isEqualTo(HASH);
            assertThat(guardado.getAceptoTerminos()).isTrue();
            assertThat(guardado.getVersionTerminos()).isEqualTo("T&C-v1.0");
            assertThat(guardado.getFechaAceptacionTerminos()).isNotNull();

            ArgumentCaptor<TokenVerificacion> tokenCaptor = ArgumentCaptor.forClass(TokenVerificacion.class);
            verify(tokenRepository).save(tokenCaptor.capture());
            TokenVerificacion token = tokenCaptor.getValue();
            assertThat(token.getToken()).isNotBlank();
            assertThat(UUID.fromString(token.getToken())).isNotNull();
            assertThat(token.getFechaExpiracion()).isAfter(antes.plusHours(2).minusMinutes(1));
            assertThat(token.getFechaExpiracion()).isBefore(LocalDateTime.now().plusHours(2).plusMinutes(1));
            assertThat(token.getUsuario().getEmail()).isEqualTo("ana@tracking.com");
        }

        @Test
        @DisplayName("Registro fallido por correo duplicado lanza IllegalArgumentException")
        void registroFallido_correoDuplicado() {
            // Arrange
            when(usuarioRepository.existsByEmail("ana@tracking.com")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> clienteService.registrarCliente(registroValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("El correo ya se encuentra registrado");

            verify(usuarioRepository, never()).save(any());
            verify(tokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("Registro fallido cuando las contraseñas no coinciden")
        void registroFallido_passwordsNoCoinciden() {
            // Arrange
            RegistroClienteRequestDTO dto = new RegistroClienteRequestDTO(
                    "Ana Pérez", "ana@tracking.com", PASSWORD, "OtraClave123!",
                    "3001234567", "Calle 10", "Medellín", true, "T&C-v1.0");

            // Act & Assert
            assertThatThrownBy(() -> clienteService.registrarCliente(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Las contraseñas no coinciden");

            verify(usuarioRepository, never()).existsByEmail(any());
            verify(usuarioRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("verificarCuenta")
    class VerificarCuenta {

        @Test
        @DisplayName("Token válido pasa la cuenta a ACTIVO y elimina el token")
        void tokenValido_activaCuentaYEliminaToken() {
            // Arrange
            Usuario usuario = Usuario.builder()
                    .id(1L)
                    .email("ana@tracking.com")
                    .estado(EstadoUsuario.PENDIENTE_VERIFICACION)
                    .build();
            TokenVerificacion token = TokenVerificacion.builder()
                    .id(5L)
                    .token("token-valido")
                    .usuario(usuario)
                    .fechaExpiracion(LocalDateTime.now().plusHours(1))
                    .build();
            when(tokenRepository.findByToken("token-valido")).thenReturn(Optional.of(token));

            // Act
            clienteService.verificarCuenta("token-valido");

            // Assert
            assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
            verify(usuarioRepository).save(usuario);
            verify(tokenRepository).delete(token);
        }

        @Test
        @DisplayName("Token expirado lanza IllegalArgumentException y no activa la cuenta")
        void tokenExpirado_lanzaExcepcion() {
            // Arrange — el código de producción lanza IllegalArgumentException, no IllegalStateException
            Usuario usuario = Usuario.builder()
                    .id(1L)
                    .estado(EstadoUsuario.PENDIENTE_VERIFICACION)
                    .build();
            TokenVerificacion token = TokenVerificacion.builder()
                    .token("token-expirado")
                    .usuario(usuario)
                    .fechaExpiracion(LocalDateTime.now().minusMinutes(1))
                    .build();
            when(tokenRepository.findByToken("token-expirado")).thenReturn(Optional.of(token));

            // Act & Assert
            assertThatThrownBy(() -> clienteService.verificarCuenta("token-expirado"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("El token de verificación ha expirado");

            assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.PENDIENTE_VERIFICACION);
            verify(usuarioRepository, never()).save(any());
            verify(tokenRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Token inexistente lanza IllegalArgumentException")
        void tokenInvalido_lanzaExcepcion() {
            // Arrange
            when(tokenRepository.findByToken("no-existe")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> clienteService.verificarCuenta("no-existe"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Token de verificación inválido");
        }
    }

    @Nested
    @DisplayName("actualizarPerfil")
    class ActualizarPerfil {

        @Test
        @DisplayName("Actualiza nombre, teléfono y dirección cuando vienen en el DTO")
        void actualizaCamposEditables() {
            // Arrange
            Usuario usuario = Usuario.builder()
                    .id(1L)
                    .nombre("Ana")
                    .email("ana@tracking.com")
                    .telefono("3001111111")
                    .direccion("Antigua")
                    .rol(Rol.CLIENTE)
                    .estado(EstadoUsuario.ACTIVO)
                    .fechaCreacion(LocalDateTime.now())
                    .build();
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
            ActualizarPerfilRequestDTO dto = new ActualizarPerfilRequestDTO("Ana María", "3009999999", "Nueva 12");

            // Act
            UsuarioResponseDTO respuesta = clienteService.actualizarPerfil(1L, dto);

            // Assert
            assertThat(respuesta.nombre()).isEqualTo("Ana María");
            assertThat(respuesta.telefono()).isEqualTo("3009999999");
            assertThat(respuesta.direccion()).isEqualTo("Nueva 12");
            assertThat(respuesta.email()).isEqualTo("ana@tracking.com");
            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("No sobrescribe teléfono ni dirección cuando vienen nulos")
        void camposOpcionalesNulos_conservaValores() {
            // Arrange
            Usuario usuario = Usuario.builder()
                    .id(1L)
                    .nombre("Ana")
                    .email("ana@tracking.com")
                    .telefono("3001111111")
                    .direccion("Antigua")
                    .rol(Rol.CLIENTE)
                    .estado(EstadoUsuario.ACTIVO)
                    .build();
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            UsuarioResponseDTO respuesta = clienteService.actualizarPerfil(
                    1L, new ActualizarPerfilRequestDTO("Ana Pérez", null, null));

            // Assert
            assertThat(respuesta.nombre()).isEqualTo("Ana Pérez");
            assertThat(respuesta.telefono()).isEqualTo("3001111111");
            assertThat(respuesta.direccion()).isEqualTo("Antigua");
        }

        @Test
        @DisplayName("Usuario inexistente lanza IllegalArgumentException")
        void usuarioNoEncontrado() {
            // Arrange
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> clienteService.actualizarPerfil(
                    99L, new ActualizarPerfilRequestDTO("Nombre", "300", "Dir")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Usuario no encontrado con ID: 99");
        }
    }

    @Nested
    @DisplayName("desactivarCuentaCliente")
    class DesactivarCuenta {

        @Test
        @DisplayName("Baja lógica: activo=false y estado=INACTIVO")
        void bajaLogica_inactivaUsuario() {
            // Arrange
            Usuario usuario = Usuario.builder()
                    .id(1L)
                    .activo(true)
                    .estado(EstadoUsuario.ACTIVO)
                    .build();
            Cliente cliente = Cliente.builder().id(10L).usuario(usuario).build();
            when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));

            // Act
            clienteService.desactivarCuentaCliente(10L);

            // Assert
            assertThat(usuario.getActivo()).isFalse();
            assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.INACTIVO);
            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("Cliente inexistente lanza IllegalArgumentException")
        void clienteNoEncontrado() {
            // Arrange
            when(clienteRepository.findById(77L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> clienteService.desactivarCuentaCliente(77L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cliente no encontrado con ID: 77");
            verify(usuarioRepository, never()).save(any());
        }
    }
}
