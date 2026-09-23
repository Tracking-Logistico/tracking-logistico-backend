package com.udea.demo.usuarios.application.service;

import com.udea.demo.usuarios.application.dto.LoginRequestDTO;
import com.udea.demo.usuarios.application.dto.LoginResponseDTO;
import com.udea.demo.usuarios.application.dto.RefreshTokenRequestDTO;
import com.udea.demo.usuarios.application.dto.RestablecerPasswordDTO;
import com.udea.demo.usuarios.application.dto.SolicitarRestablecimientoPasswordDTO;
import com.udea.demo.usuarios.domain.exception.CredencialesInvalidasException;
import com.udea.demo.usuarios.domain.exception.CuentaBloqueadaLoginException;
import com.udea.demo.usuarios.domain.exception.PasswordDebilException;
import com.udea.demo.usuarios.domain.exception.PasswordNoCoincideException;
import com.udea.demo.usuarios.domain.exception.SesionInvalidaException;
import com.udea.demo.usuarios.domain.exception.TokenRestablecimientoInvalidoException;
import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.IntentoInicioSesion;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.SesionUsuario;
import com.udea.demo.usuarios.domain.model.TokenRestablecimientoPassword;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.interfaces.persistence.IntentoInicioSesionRepository;
import com.udea.demo.usuarios.interfaces.persistence.SesionUsuarioRepository;
import com.udea.demo.usuarios.interfaces.persistence.TokenRestablecimientoPasswordRepository;
import com.udea.demo.usuarios.interfaces.persistence.UsuarioRepository;
import com.udea.demo.usuarios.interfaces.services.EmailServiceI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutenticacionService - HU-02: Autenticación e Inicio de Sesión")
class AutenticacionServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private IntentoInicioSesionRepository intentoRepository;
    @Mock private SesionUsuarioRepository sesionRepository;
    @Mock private TokenRestablecimientoPasswordRepository resetRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailServiceI emailService;
    @Mock private PasswordPolicyService passwordPolicyService;

    @InjectMocks private AutenticacionService autenticacionService;

    private static final String IP_ORIGEN = "192.168.1.100";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";
    private static final String RAW_PASSWORD = "PasswordSegura123!";
    private static final String ENCODED_PASSWORD = "$2a$10$hashedPasswordValue...";

    @BeforeEach
    void setUpConfig() {
        ReflectionTestUtils.setField(autenticacionService, "ventanaIntentosMinutos", 10L);
        ReflectionTestUtils.setField(autenticacionService, "minutosBloqueo", 15L);
        ReflectionTestUtils.setField(autenticacionService, "diasRefresh", 7L);
        ReflectionTestUtils.setField(autenticacionService, "minutosReset", 45L);
        ReflectionTestUtils.setField(autenticacionService, "passwordResetUrl", "http://localhost:8080/restablecer-password");
        ReflectionTestUtils.setField(autenticacionService, "minutosInactividadCliente", 30L);
        ReflectionTestUtils.setField(autenticacionService, "minutosInactividadOperativo", 15L);
    }

    private Usuario crearUsuario(Long id, String email, Rol rol, EstadoUsuario estado, boolean activo) {
        return Usuario.builder()
                .id(id)
                .nombre("Usuario Prueba")
                .email(email)
                .password(ENCODED_PASSWORD)
                .rol(rol)
                .estado(estado)
                .aceptoTerminos(true)
                .activo(activo)
                .build();
    }

    @Nested
    @DisplayName("Criterio: Inicio de sesión exitoso y redirección según rol")
    class InicioSesionExitoso {

        @Test
        @DisplayName("Autentica a un CLIENTE en tiempo < 2s, emite tokens, expiración 30 min y redirige a /panel/cliente")
        void loginExitoso_cliente() {

            String email = "cliente@correo.com";
            Usuario cliente = crearUsuario(1L, email, Rol.CLIENTE, EstadoUsuario.ACTIVO, true);
            LoginRequestDTO request = new LoginRequestDTO(email, RAW_PASSWORD);

            when(intentoRepository.findFirstByEmailAndBloqueadoHastaAfterOrderByBloqueadoHastaDesc(eq(email), any(LocalDateTime.class)))
                    .thenReturn(null);
            when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
            when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

            LoginResponseDTO response = assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                    autenticacionService.iniciarSesion(request, IP_ORIGEN, USER_AGENT)
            );

            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.refreshToken()).isNotBlank();
            assertThat(response.rol()).isEqualTo(Rol.CLIENTE);
            assertThat(response.panel()).isEqualTo("/panel/cliente");
            assertThat(response.accessTokenExpiresAt())
                    .isAfter(LocalDateTime.now().plusMinutes(29))
                    .isBefore(LocalDateTime.now().plusMinutes(31));
            assertThat(response.refreshTokenExpiresAt())
                    .isAfter(LocalDateTime.now().plusDays(6));

            ArgumentCaptor<SesionUsuario> sesionCaptor = ArgumentCaptor.forClass(SesionUsuario.class);
            verify(sesionRepository).save(sesionCaptor.capture());
            SesionUsuario sesionGuardada = sesionCaptor.getValue();
            assertThat(sesionGuardada.getUsuario()).isEqualTo(cliente);
            assertThat(sesionGuardada.getAccessTokenHash()).isNotBlank();
            assertThat(sesionGuardada.getRefreshTokenHash()).isNotBlank();
            assertThat(sesionGuardada.getRevokedAt()).isNull();

            verify(intentoRepository).deleteByEmail(email);
        }

        @Test
        @DisplayName("Cliente pendiente de correo puede usar la aplicación sin verificarse")
        void loginClientePendienteCorreo() {
            String email = "pendiente@correo.com";
            Usuario cliente = crearUsuario(34L, email, Rol.CLIENTE, EstadoUsuario.PENDIENTE_VERIFICACION, true);
            when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
            when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

            LoginResponseDTO resultado = autenticacionService.iniciarSesion(
                    new LoginRequestDTO(email, RAW_PASSWORD), IP_ORIGEN, USER_AGENT);

            assertThat(resultado.rol()).isEqualTo(Rol.CLIENTE);
            assertThat(resultado.panel()).isEqualTo("/panel/cliente");
            assertThat(resultado.requiereCambioPassword()).isFalse();
            verify(sesionRepository).save(any(SesionUsuario.class));
        }

        @Test
        @DisplayName("Autentica a un OPERADOR con expiración de 15 min y redirige a /panel/operador")
        void loginExitoso_operador() {

            String email = "operador@tracking.com";
            Usuario operador = crearUsuario(2L, email, Rol.OPERADOR, EstadoUsuario.ACTIVO, true);
            LoginRequestDTO request = new LoginRequestDTO(email, RAW_PASSWORD);

            when(intentoRepository.findFirstByEmailAndBloqueadoHastaAfterOrderByBloqueadoHastaDesc(eq(email), any(LocalDateTime.class)))
                    .thenReturn(null);
            when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(operador));
            when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

            LoginResponseDTO response = autenticacionService.iniciarSesion(request, IP_ORIGEN, USER_AGENT);

            assertThat(response.rol()).isEqualTo(Rol.OPERADOR);
            assertThat(response.panel()).isEqualTo("/panel/operador");
            assertThat(response.accessTokenExpiresAt())
                    .isAfter(LocalDateTime.now().plusMinutes(14))
                    .isBefore(LocalDateTime.now().plusMinutes(16));
            verify(intentoRepository).deleteByEmail(email);
        }

        @Test
        @DisplayName("Autentica a un CONDUCTOR con expiración de 15 min y redirige a /panel/conductor")
        void loginExitoso_conductor() {

            String email = "conductor@tracking.com";
            Usuario conductor = crearUsuario(3L, email, Rol.CONDUCTOR, EstadoUsuario.ACTIVO, true);
            LoginRequestDTO request = new LoginRequestDTO(email, RAW_PASSWORD);

            when(intentoRepository.findFirstByEmailAndBloqueadoHastaAfterOrderByBloqueadoHastaDesc(eq(email), any(LocalDateTime.class)))
                    .thenReturn(null);
            when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(conductor));
            when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

            LoginResponseDTO response = autenticacionService.iniciarSesion(request, IP_ORIGEN, USER_AGENT);

            assertThat(response.rol()).isEqualTo(Rol.CONDUCTOR);
            assertThat(response.panel()).isEqualTo("/panel/conductor");
            verify(intentoRepository).deleteByEmail(email);
        }
    }

    @Nested
    @DisplayName("Criterio: Credenciales inválidas y prevención de enumeración")
    class CredencialesInvalidas {

        @Test
        @DisplayName("Contraseña incorrecta lanza CredencialesInvalidasException con mensaje genérico")
        void passwordIncorrecta_lanzaExcepcionGenerica() {

            String email = "cliente@correo.com";
            Usuario cliente = crearUsuario(1L, email, Rol.CLIENTE, EstadoUsuario.ACTIVO, true);
            LoginRequestDTO request = new LoginRequestDTO(email, "WrongPassword!");

            when(intentoRepository.findFirstByEmailAndBloqueadoHastaAfterOrderByBloqueadoHastaDesc(eq(email), any(LocalDateTime.class)))
                    .thenReturn(null);
            when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
            when(passwordEncoder.matches("WrongPassword!", ENCODED_PASSWORD)).thenReturn(false);
            when(intentoRepository.countByEmailAndIntentadoEnAfter(eq(email), any(LocalDateTime.class))).thenReturn(0L);

            assertThatThrownBy(() -> autenticacionService.iniciarSesion(request, IP_ORIGEN, USER_AGENT))
                    .isInstanceOf(CredencialesInvalidasException.class)
                    .hasMessage("Usuario o contraseña incorrectos");

            verify(intentoRepository).save(any(IntentoInicioSesion.class));
            verify(sesionRepository, never()).save(any(SesionUsuario.class));
        }

        @Test
        @DisplayName("Usuario no existente lanza CredencialesInvalidasException con mensaje genérico idéntico")
        void usuarioNoExiste_lanzaExcepcionGenerica() {

            String email = "noexiste@correo.com";
            LoginRequestDTO request = new LoginRequestDTO(email, RAW_PASSWORD);

            when(intentoRepository.findFirstByEmailAndBloqueadoHastaAfterOrderByBloqueadoHastaDesc(eq(email), any(LocalDateTime.class)))
                    .thenReturn(null);
            when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(intentoRepository.countByEmailAndIntentadoEnAfter(eq(email), any(LocalDateTime.class))).thenReturn(1L);

            assertThatThrownBy(() -> autenticacionService.iniciarSesion(request, IP_ORIGEN, USER_AGENT))
                    .isInstanceOf(CredencialesInvalidasException.class)
                    .hasMessage("Usuario o contraseña incorrectos");

            verify(intentoRepository).save(any(IntentoInicioSesion.class));
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Usuario inactivo o no verificado lanza CredencialesInvalidasException")
        void usuarioInactivo_lanzaCredencialesInvalidas() {

            String email = "inactivo@correo.com";
            Usuario usuarioInactivo = crearUsuario(4L, email, Rol.CLIENTE, EstadoUsuario.INACTIVO, false);
            LoginRequestDTO request = new LoginRequestDTO(email, RAW_PASSWORD);

            when(intentoRepository.findFirstByEmailAndBloqueadoHastaAfterOrderByBloqueadoHastaDesc(eq(email), any(LocalDateTime.class)))
                    .thenReturn(null);
            when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuarioInactivo));
            when(intentoRepository.countByEmailAndIntentadoEnAfter(eq(email), any(LocalDateTime.class))).thenReturn(0L);

            assertThatThrownBy(() -> autenticacionService.iniciarSesion(request, IP_ORIGEN, USER_AGENT))
                    .isInstanceOf(CredencialesInvalidasException.class)
                    .hasMessage("Usuario o contraseña incorrectos");

            verify(intentoRepository).save(any(IntentoInicioSesion.class));
        }
    }

    @Nested
    @DisplayName("Criterio: Bloqueo por intentos fallidos")
    class BloqueoPorIntentosFallidos {

        @Test
        @DisplayName("Acumular 5 intentos fallidos consecutivos en 10 min bloquea por 15 min")
        void acumula5IntentosFallidos_bloqueaCuenta() {

            String email = "atacante@correo.com";
            Usuario usuario = crearUsuario(5L, email, Rol.CLIENTE, EstadoUsuario.ACTIVO, true);
            LoginRequestDTO request = new LoginRequestDTO(email, "BadPass!");

            when(intentoRepository.findFirstByEmailAndBloqueadoHastaAfterOrderByBloqueadoHastaDesc(eq(email), any(LocalDateTime.class)))
                    .thenReturn(null);
            when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("BadPass!", ENCODED_PASSWORD)).thenReturn(false);

            when(intentoRepository.countByEmailAndIntentadoEnAfter(eq(email), any(LocalDateTime.class))).thenReturn(4L);

            assertThatThrownBy(() -> autenticacionService.iniciarSesion(request, IP_ORIGEN, USER_AGENT))
                    .isInstanceOf(CuentaBloqueadaLoginException.class)
                    .hasMessageMatching("Cuenta bloqueada temporalmente\\. Intenta nuevamente en (14|15) minutos");

            ArgumentCaptor<IntentoInicioSesion> captor = ArgumentCaptor.forClass(IntentoInicioSesion.class);
            verify(intentoRepository).save(captor.capture());
            IntentoInicioSesion intentoGuardado = captor.getValue();
            assertThat(intentoGuardado.getBloqueadoHasta()).isNotNull();
            assertThat(intentoGuardado.getBloqueadoHasta())
                    .isAfter(LocalDateTime.now().plusMinutes(14))
                    .isBefore(LocalDateTime.now().plusMinutes(16));
        }

        @Test
        @DisplayName("Si la cuenta ya está bloqueada actualmente, se rechaza de inmediato sin evaluar password")
        void cuentaYaBloqueada_rechazoInmediato() {

            String email = "bloqueado@correo.com";
            LocalDateTime bloqueadoHasta = LocalDateTime.now().plusMinutes(10);
            IntentoInicioSesion bloqueoPrevio = new IntentoInicioSesion(email, null, LocalDateTime.now(), bloqueadoHasta);
            LoginRequestDTO request = new LoginRequestDTO(email, RAW_PASSWORD);

            when(intentoRepository.findFirstByEmailAndBloqueadoHastaAfterOrderByBloqueadoHastaDesc(eq(email), any(LocalDateTime.class)))
                    .thenReturn(bloqueoPrevio);

            assertThatThrownBy(() -> autenticacionService.iniciarSesion(request, IP_ORIGEN, USER_AGENT))
                    .isInstanceOf(CuentaBloqueadaLoginException.class)
                    .hasMessageContaining("Cuenta bloqueada temporalmente");

            verify(usuarioRepository, never()).findByEmail(anyString());
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Criterio: Gestión de sesión y expiración (Refresh Token y Logout)")
    class GestionSesionExpiracion {

        @Test
        @DisplayName("renovarSesion() con refresh token válido crea nueva sesión y revoca la anterior")
        void renovarSesion_exitoso() {

            String refreshToken = "valid-refresh-token-123";
            String tokenHash = AutenticacionService.hash(refreshToken);
            Usuario usuario = crearUsuario(6L, "renovador@mail.com", Rol.OPERADOR, EstadoUsuario.ACTIVO, true);
            LocalDateTime ahora = LocalDateTime.now();

            SesionUsuario sesionExistente = new SesionUsuario(
                    usuario,
                    "old-access-hash",
                    tokenHash,
                    ahora.minusMinutes(5),
                    ahora.plusDays(3),
                    ahora.minusMinutes(10)
            );

            when(sesionRepository.findByRefreshTokenHash(tokenHash)).thenReturn(Optional.of(sesionExistente));

            LoginResponseDTO nuevaRespuesta = autenticacionService.renovarSesion(new RefreshTokenRequestDTO(refreshToken));

            assertThat(nuevaRespuesta).isNotNull();
            assertThat(nuevaRespuesta.accessToken()).isNotBlank();
            assertThat(nuevaRespuesta.refreshToken()).isNotBlank();
            assertThat(nuevaRespuesta.rol()).isEqualTo(Rol.OPERADOR);
            assertThat(nuevaRespuesta.panel()).isEqualTo("/panel/operador");

            assertThat(sesionExistente.getRevokedAt()).isNotNull();

            verify(sesionRepository).save(any(SesionUsuario.class));
        }

        @Test
        @DisplayName("renovarSesion() con token no existente lanza SesionInvalidaException")
        void renovarSesion_tokenNoExiste() {

            String token = "no-existe";
            when(sesionRepository.findByRefreshTokenHash(AutenticacionService.hash(token)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> autenticacionService.renovarSesion(new RefreshTokenRequestDTO(token)))
                    .isInstanceOf(SesionInvalidaException.class);
        }

        @Test
        @DisplayName("renovarSesion() con sesión ya revocada o expirada lanza SesionInvalidaException")
        void renovarSesion_revocadaOExpirada() {

            String token = "token-revocado";
            Usuario usuario = crearUsuario(7L, "revocado@mail.com", Rol.CLIENTE, EstadoUsuario.ACTIVO, true);
            SesionUsuario sesionRevocada = new SesionUsuario(
                    usuario, "acc", AutenticacionService.hash(token),
                    LocalDateTime.now(), LocalDateTime.now().plusDays(2), LocalDateTime.now()
            );
            sesionRevocada.setRevokedAt(LocalDateTime.now().minusHours(1));

            when(sesionRepository.findByRefreshTokenHash(AutenticacionService.hash(token)))
                    .thenReturn(Optional.of(sesionRevocada));

            assertThatThrownBy(() -> autenticacionService.renovarSesion(new RefreshTokenRequestDTO(token)))
                    .isInstanceOf(SesionInvalidaException.class);
        }

        @Test
        @DisplayName("cerrarSesion() invalida la sesión en el servidor asignando revokedAt")
        void cerrarSesion_exitoso() {

            String accessToken = "active-access-token";
            String tokenHash = AutenticacionService.hash(accessToken);
            Usuario usuario = crearUsuario(8L, "saliente@mail.com", Rol.CLIENTE, EstadoUsuario.ACTIVO, true);
            SesionUsuario sesion = new SesionUsuario(
                    usuario, tokenHash, "refHash",
                    LocalDateTime.now().plusMinutes(20), LocalDateTime.now().plusDays(5), LocalDateTime.now()
            );

            when(sesionRepository.findByAccessTokenHash(tokenHash)).thenReturn(Optional.of(sesion));

            autenticacionService.cerrarSesion(accessToken);

            assertThat(sesion.getRevokedAt()).isNotNull();
            verify(sesionRepository).save(sesion);
        }

        @Test
        @DisplayName("cerrarSesion() con token nulo o en blanco no ejecuta operaciones en bd")
        void cerrarSesion_tokenVacio() {
            autenticacionService.cerrarSesion(null);
            autenticacionService.cerrarSesion("   ");

            verify(sesionRepository, never()).findByAccessTokenHash(anyString());
            verify(sesionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Criterio: Recuperación de contraseña")
    class RecuperacionPassword {

        @Test
        @DisplayName("solicitarRestablecimiento() con usuario existente genera token de 45 min y envía correo")
        void solicitarRestablecimiento_usuarioExiste() {

            String email = "olvido@mail.com";
            Usuario usuario = crearUsuario(9L, email, Rol.CLIENTE, EstadoUsuario.ACTIVO, true);
            SolicitarRestablecimientoPasswordDTO dto = new SolicitarRestablecimientoPasswordDTO(email);

            when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));

            autenticacionService.solicitarRestablecimiento(dto);

            verify(resetRepository).deleteByUsuarioId(usuario.getId());

            ArgumentCaptor<TokenRestablecimientoPassword> tokenCaptor =
                    ArgumentCaptor.forClass(TokenRestablecimientoPassword.class);
            verify(resetRepository).save(tokenCaptor.capture());
            TokenRestablecimientoPassword tokenGuardado = tokenCaptor.getValue();

            assertThat(tokenGuardado.getUsuario()).isEqualTo(usuario);
            assertThat(tokenGuardado.getExpiraEn())
                    .isAfter(LocalDateTime.now().plusMinutes(44))
                    .isBefore(LocalDateTime.now().plusMinutes(46));

            verify(emailService).enviarEnlaceRestablecimiento(eq(email), anyString());
        }

        @Test
        @DisplayName("solicitarRestablecimiento() con usuario inexistente no envía correo ni expone error")
        void solicitarRestablecimiento_usuarioNoExiste() {

            String email = "fantasma@mail.com";
            SolicitarRestablecimientoPasswordDTO dto = new SolicitarRestablecimientoPasswordDTO(email);
            when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

            autenticacionService.solicitarRestablecimiento(dto);

            verify(resetRepository, never()).deleteByUsuarioId(any());
            verify(resetRepository, never()).save(any());
            verify(emailService, never()).enviarEnlaceRestablecimiento(anyString(), anyString());
        }

        @Test
        @DisplayName("restablecerPassword() falla si las contraseñas no coinciden")
        void restablecerPassword_noCoinciden() {
            RestablecerPasswordDTO dto = new RestablecerPasswordDTO("token123", "Pass1234!", "PassDiferente123!");

            assertThatThrownBy(() -> autenticacionService.restablecerPassword(dto))
                    .isInstanceOf(PasswordNoCoincideException.class);

            verify(resetRepository, never()).findByTokenHash(anyString());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "corta1!",
                "solominusculas1!",
                "SOLOMAYUSCULAS1!",
                "SinNumerosEspecial!",
                "SinEspecial123"
        })
        @DisplayName("restablecerPassword() falla si la nueva contraseña es débil")
        void restablecerPassword_passwordDebil(String passwordDebil) {
            RestablecerPasswordDTO dto = new RestablecerPasswordDTO("token123", passwordDebil, passwordDebil);

            assertThatThrownBy(() -> autenticacionService.restablecerPassword(dto))
                    .isInstanceOf(PasswordDebilException.class)
                    .hasMessageContaining("La contraseña debe tener al menos 8 caracteres");

            verify(resetRepository, never()).findByTokenHash(anyString());
        }

        @Test
        @DisplayName("restablecerPassword() falla si el token es inexistente o expirado")
        void restablecerPassword_tokenExpirado() {

            String rawToken = "token-expirado";
            String tokenHash = AutenticacionService.hash(rawToken);
            Usuario usuario = crearUsuario(10L, "expirado@mail.com", Rol.CLIENTE, EstadoUsuario.ACTIVO, true);
            TokenRestablecimientoPassword token = new TokenRestablecimientoPassword(
                    tokenHash, usuario, LocalDateTime.now().minusMinutes(5)
            );

            when(resetRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

            RestablecerPasswordDTO dto = new RestablecerPasswordDTO(rawToken, "NuevaPass123!", "NuevaPass123!");

            assertThatThrownBy(() -> autenticacionService.restablecerPassword(dto))
                    .isInstanceOf(TokenRestablecimientoInvalidoException.class);

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("restablecerPassword() exitoso actualiza contraseña, invalida sesiones previas y marca token usado")
        void restablecerPassword_exitoso() {

            String rawToken = "token-valido";
            String tokenHash = AutenticacionService.hash(rawToken);
            Usuario usuario = crearUsuario(11L, "cambio@mail.com", Rol.CLIENTE, EstadoUsuario.ACTIVO, true);
            TokenRestablecimientoPassword token = new TokenRestablecimientoPassword(
                    tokenHash, usuario, LocalDateTime.now().plusMinutes(30)
            );

            when(resetRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("NuevaPass123!")).thenReturn("$2a$10$encodedNewPassword");

            RestablecerPasswordDTO dto = new RestablecerPasswordDTO(rawToken, "NuevaPass123!", "NuevaPass123!");

            autenticacionService.restablecerPassword(dto);

            assertThat(usuario.getPassword()).isEqualTo("$2a$10$encodedNewPassword");
            verify(usuarioRepository).save(usuario);

            assertThat(token.getUsadoEn()).isNotNull();
            verify(resetRepository).save(token);

            verify(sesionRepository).deleteByUsuarioId(usuario.getId());

            verify(intentoRepository).deleteByEmail(usuario.getEmail());
        }
    }
}
