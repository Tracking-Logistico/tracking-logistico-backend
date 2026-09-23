package com.udea.demo.config;

import com.udea.demo.usuarios.application.service.AutenticacionService;
import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.SesionUsuario;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.interfaces.persistence.SesionUsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SesionAuthenticationFilter - gestión de sesión, inactividad y expiración")
class SesionAuthenticationFilterTest {

    @Mock
    private SesionUsuarioRepository sesionRepository;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private SesionAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();

        ReflectionTestUtils.setField(filter, "minutosInactividadCliente", 30L);
        ReflectionTestUtils.setField(filter, "minutosInactividadOperativo", 15L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Usuario crearUsuario(String email, Rol rol, boolean activo) {
        return Usuario.builder()
                .id(1L)
                .nombre("Usuario Test")
                .email(email)
                .password("hash")
                .rol(rol)
                .estado(EstadoUsuario.ACTIVO)
                .aceptoTerminos(true)
                .activo(activo)
                .build();
    }

    @Test
    @DisplayName("Token de cliente pendiente de correo tiene ROLE_CLIENTE y permite consultar endpoints")
    void clientePendienteVerificacion_recibeRolCliente() throws ServletException, IOException {
        String token = "token-cliente-pendiente";
        Usuario cliente = crearUsuario("pendiente@tracking.com", Rol.CLIENTE, true);
        cliente.setEstado(EstadoUsuario.PENDIENTE_VERIFICACION);
        LocalDateTime ahora = LocalDateTime.now();
        SesionUsuario sesion = new SesionUsuario(cliente, AutenticacionService.hash(token), "refreshHash",
                ahora.plusMinutes(10), ahora.plusDays(1), ahora.minusMinutes(1));
        when(sesionRepository.findByAccessTokenHash(AutenticacionService.hash(token)))
                .thenReturn(Optional.of(sesion));
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));
        verify(sesionRepository).save(sesion);
    }

    @Test
    @DisplayName("No se acepta token de una cuenta desactivada aunque tenga estado pendiente")
    void clienteDesactivadoPendiente_noSeAutentica() throws ServletException, IOException {
        String token = "token-cliente-desactivado";
        Usuario cliente = crearUsuario("desactivado@tracking.com", Rol.CLIENTE, false);
        cliente.setEstado(EstadoUsuario.PENDIENTE_VERIFICACION);
        LocalDateTime ahora = LocalDateTime.now();
        SesionUsuario sesion = new SesionUsuario(cliente, AutenticacionService.hash(token), "refreshHash",
                ahora.plusMinutes(10), ahora.plusDays(1), ahora.minusMinutes(1));
        when(sesionRepository.findByAccessTokenHash(AutenticacionService.hash(token)))
                .thenReturn(Optional.of(sesion));
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(sesionRepository, never()).save(any());
    }

    @Nested
    @DisplayName("Criterio: Inactividad y expiración por rol")
    class InactividadYExpiracion {

        @Test
        @DisplayName("Cliente activo dentro de la ventana de 30 minutos se autentica y actualiza lastActivityAt")
        void clienteActivo_seAutentica() throws ServletException, IOException {
            // Arrange
            String token = "client-token";
            String tokenHash = AutenticacionService.hash(token);
            Usuario cliente = crearUsuario("cliente@correo.com", Rol.CLIENTE, true);
            LocalDateTime ahora = LocalDateTime.now();

            SesionUsuario sesion = new SesionUsuario(
                    cliente, tokenHash, "refreshHash",
                    ahora.plusMinutes(25), ahora.plusDays(7), ahora.minusMinutes(10) // última actividad hace 10 min (<30)
            );

            when(sesionRepository.findByAccessTokenHash(tokenHash)).thenReturn(Optional.of(sesion));
            request.addHeader("Authorization", "Bearer " + token);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getName()).isEqualTo("cliente@correo.com");
            assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));

            assertThat(sesion.getLastActivityAt()).isAfterOrEqualTo(ahora);
            verify(sesionRepository).save(sesion);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Cliente inactivo más de 30 minutos NO se autentica y requiere reautenticación")
        void clienteInactivoMasDe30Minutos_noSeAutentica() throws ServletException, IOException {
            // Arrange
            String token = "client-token-inactivo";
            String tokenHash = AutenticacionService.hash(token);
            Usuario cliente = crearUsuario("cliente@correo.com", Rol.CLIENTE, true);
            LocalDateTime ahora = LocalDateTime.now();

            SesionUsuario sesion = new SesionUsuario(
                    cliente, tokenHash, "refreshHash",
                    ahora.plusMinutes(25), ahora.plusDays(7), ahora.minusMinutes(35) // inactivo hace 35 min (>30)
            );

            when(sesionRepository.findByAccessTokenHash(tokenHash)).thenReturn(Optional.of(sesion));
            request.addHeader("Authorization", "Bearer " + token);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNull();
            verify(sesionRepository, never()).save(any());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Operador inactivo más de 15 minutos NO se autentica por sensibilidad de datos")
        void operadorInactivoMasDe15Minutos_noSeAutentica() throws ServletException, IOException {
            // Arrange
            String token = "operador-token-inactivo";
            String tokenHash = AutenticacionService.hash(token);
            Usuario operador = crearUsuario("operador@tracking.com", Rol.OPERADOR, true);
            LocalDateTime ahora = LocalDateTime.now();

            SesionUsuario sesion = new SesionUsuario(
                    operador, tokenHash, "refreshHash",
                    ahora.plusMinutes(10), ahora.plusDays(7), ahora.minusMinutes(16) // inactivo hace 16 min (>15)
            );

            when(sesionRepository.findByAccessTokenHash(tokenHash)).thenReturn(Optional.of(sesion));
            request.addHeader("Authorization", "Bearer " + token);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNull();
            verify(sesionRepository, never()).save(any());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Operador activo dentro de los 15 minutos se autentica con ROLE_OPERADOR")
        void operadorActivoDentroDe15Minutos_seAutentica() throws ServletException, IOException {
            // Arrange
            String token = "operador-token-activo";
            String tokenHash = AutenticacionService.hash(token);
            Usuario operador = crearUsuario("operador@tracking.com", Rol.OPERADOR, true);
            LocalDateTime ahora = LocalDateTime.now();

            SesionUsuario sesion = new SesionUsuario(
                    operador, tokenHash, "refreshHash",
                    ahora.plusMinutes(10), ahora.plusDays(7), ahora.minusMinutes(5) // inactivo hace 5 min (<15)
            );

            when(sesionRepository.findByAccessTokenHash(tokenHash)).thenReturn(Optional.of(sesion));
            request.addHeader("Authorization", "Bearer " + token);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_OPERADOR"));
            verify(sesionRepository).save(sesion);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Conductor inactivo más de 15 minutos NO se autentica")
        void conductorInactivoMasDe15Minutos_noSeAutentica() throws ServletException, IOException {
            // Arrange
            String token = "conductor-token-inactivo";
            String tokenHash = AutenticacionService.hash(token);
            Usuario conductor = crearUsuario("conductor@tracking.com", Rol.CONDUCTOR, true);
            LocalDateTime ahora = LocalDateTime.now();

            SesionUsuario sesion = new SesionUsuario(
                    conductor, tokenHash, "refreshHash",
                    ahora.plusMinutes(10), ahora.plusDays(7), ahora.minusMinutes(20) // inactivo hace 20 min (>15)
            );

            when(sesionRepository.findByAccessTokenHash(tokenHash)).thenReturn(Optional.of(sesion));
            request.addHeader("Authorization", "Bearer " + token);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Sesión revocada NO se autentica aunque esté dentro del tiempo")
        void sesionRevocada_noSeAutentica() throws ServletException, IOException {
            String token = "token-revocado";
            String tokenHash = AutenticacionService.hash(token);
            Usuario cliente = crearUsuario("cliente@correo.com", Rol.CLIENTE, true);
            LocalDateTime ahora = LocalDateTime.now();

            SesionUsuario sesion = new SesionUsuario(
                    cliente, tokenHash, "refreshHash",
                    ahora.plusMinutes(25), ahora.plusDays(7), ahora.minusMinutes(2)
            );
            sesion.setRevokedAt(ahora.minusMinutes(1)); // Sesión cerrada previamente

            when(sesionRepository.findByAccessTokenHash(tokenHash)).thenReturn(Optional.of(sesion));
            request.addHeader("Authorization", "Bearer " + token);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(sesionRepository, never()).save(any());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("AccessToken expirado NO se autentica")
        void accessTokenExpirado_noSeAutentica() throws ServletException, IOException {
            String token = "token-expirado";
            String tokenHash = AutenticacionService.hash(token);
            Usuario cliente = crearUsuario("cliente@correo.com", Rol.CLIENTE, true);
            LocalDateTime ahora = LocalDateTime.now();

            SesionUsuario sesion = new SesionUsuario(
                    cliente, tokenHash, "refreshHash",
                    ahora.minusSeconds(1), // ya venció
                    ahora.plusDays(7), ahora.minusMinutes(2)
            );

            when(sesionRepository.findByAccessTokenHash(tokenHash)).thenReturn(Optional.of(sesion));
            request.addHeader("Authorization", "Bearer " + token);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Usuario inactivo en el sistema NO se autentica")
        void usuarioInactivo_noSeAutentica() throws ServletException, IOException {
            String token = "token-user-inactivo";
            String tokenHash = AutenticacionService.hash(token);
            Usuario inactivo = crearUsuario("bloqueado@correo.com", Rol.CLIENTE, false);
            LocalDateTime ahora = LocalDateTime.now();

            SesionUsuario sesion = new SesionUsuario(
                    inactivo, tokenHash, "refreshHash",
                    ahora.plusMinutes(20), ahora.plusDays(7), ahora.minusMinutes(2)
            );

            when(sesionRepository.findByAccessTokenHash(tokenHash)).thenReturn(Optional.of(sesion));
            request.addHeader("Authorization", "Bearer " + token);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Petición sin encabezado Authorization continúa sin autenticar")
        void peticionSinAuthorization_continua() throws ServletException, IOException {
            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(sesionRepository, never()).findByAccessTokenHash(any());
            verify(filterChain).doFilter(request, response);
        }
    }
}
