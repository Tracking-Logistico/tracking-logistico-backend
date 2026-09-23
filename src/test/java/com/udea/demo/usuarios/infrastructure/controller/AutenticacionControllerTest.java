package com.udea.demo.usuarios.infrastructure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.interfaces.services.AutenticacionServiceI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutenticacionController - endpoint web REST")
class AutenticacionControllerTest {

    @Mock
    private AutenticacionServiceI autenticacionService;

    @InjectMocks
    private AutenticacionController autenticacionController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(autenticacionController)
                .setControllerAdvice(new UsuarioControllerAdvice())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginEndpoint {

        @Test
        @DisplayName("200 OK cuando las credenciales son válidas y envía IP y User-Agent")
        void login_exitoso() throws Exception {

            LoginRequestDTO request = new LoginRequestDTO("operador@tracking.com", "Secret123!");
            LocalDateTime now = LocalDateTime.now();
            LoginResponseDTO responseDTO = new LoginResponseDTO(
                    "token-access-123",
                    "token-refresh-456",
                    now.plusMinutes(15),
                    now.plusDays(7),
                    Rol.OPERADOR,
                    "/panel/operador"
            );

            when(autenticacionService.iniciarSesion(eq(request), any(), any()))
                    .thenReturn(responseDTO);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("User-Agent", "JUnit-Client"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("token-access-123"))
                    .andExpect(jsonPath("$.refreshToken").value("token-refresh-456"))
                    .andExpect(jsonPath("$.rol").value("OPERADOR"))
                    .andExpect(jsonPath("$.panel").value("/panel/operador"));

            verify(autenticacionService).iniciarSesion(eq(request), any(), eq("JUnit-Client"));
        }

        @Test
        @DisplayName("401 UNAUTHORIZED cuando las credenciales son incorrectas (mensaje genérico)")
        void login_credencialesInvalidas() throws Exception {

            LoginRequestDTO request = new LoginRequestDTO("usuario@tracking.com", "BadPass123!");
            when(autenticacionService.iniciarSesion(eq(request), any(), any()))
                    .thenThrow(new CredencialesInvalidasException());

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Usuario o contraseña incorrectos"));
        }

        @Test
        @DisplayName("423 LOCKED cuando la cuenta está bloqueada temporalmente")
        void login_cuentaBloqueada() throws Exception {

            LoginRequestDTO request = new LoginRequestDTO("bloqueado@tracking.com", "Password123!");
            LocalDateTime bloqueadoHasta = LocalDateTime.now().plusMinutes(15);
            when(autenticacionService.iniciarSesion(eq(request), any(), any()))
                    .thenThrow(new CuentaBloqueadaLoginException(bloqueadoHasta));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isLocked())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("400 BAD REQUEST cuando los campos son inválidos")
        void login_camposInvalidos() throws Exception {
            LoginRequestDTO requestInvalido = new LoginRequestDTO("correo-invalido", "");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestInvalido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.email").exists())
                    .andExpect(jsonPath("$.password").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshEndpoint {

        @Test
        @DisplayName("200 OK con nuevo token de acceso cuando el refresh token es válido")
        void refresh_exitoso() throws Exception {

            RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("valid-refresh-token");
            LocalDateTime now = LocalDateTime.now();
            LoginResponseDTO responseDTO = new LoginResponseDTO(
                    "new-access-token",
                    "new-refresh-token",
                    now.plusMinutes(30),
                    now.plusDays(7),
                    Rol.CLIENTE,
                    "/panel/cliente"
            );

            when(autenticacionService.renovarSesion(request)).thenReturn(responseDTO);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                    .andExpect(jsonPath("$.rol").value("CLIENTE"))
                    .andExpect(jsonPath("$.panel").value("/panel/cliente"));
        }

        @Test
        @DisplayName("401 UNAUTHORIZED cuando el refresh token está revocado o vencido")
        void refresh_invalido() throws Exception {
            RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("revoked-token");
            when(autenticacionService.renovarSesion(request)).thenThrow(new SesionInvalidaException());

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("400 BAD REQUEST cuando el refresh token está vacío")
        void refresh_vacio() throws Exception {
            RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("   ");

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.refreshToken").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutEndpoint {

        @Test
        @DisplayName("204 NO CONTENT cuando se cierra sesión enviando Bearer token en Authorization")
        void logout_exitoso() throws Exception {

            String token = "access-token-to-invalidate";

            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            verify(autenticacionService).cerrarSesion(token);
        }

        @Test
        @DisplayName("204 NO CONTENT cuando no se envía header Authorization (cierra con null)")
        void logout_sinHeader() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isNoContent());

            verify(autenticacionService).cerrarSesion(null);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/password/forgot")
    class ForgotPasswordEndpoint {

        @Test
        @DisplayName("200 OK con mensaje genérico de restablecimiento")
        void forgot_exitoso() throws Exception {
            SolicitarRestablecimientoPasswordDTO request =
                    new SolicitarRestablecimientoPasswordDTO("usuario@correo.com");

            mockMvc.perform(post("/api/v1/auth/password/forgot")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Si el correo existe, intentaremos enviar instrucciones para restablecer tu contraseña"));

            verify(autenticacionService).solicitarRestablecimiento(request);
        }

        @Test
        @DisplayName("400 BAD REQUEST cuando el correo tiene formato inválido")
        void forgot_emailInvalido() throws Exception {
            SolicitarRestablecimientoPasswordDTO request =
                    new SolicitarRestablecimientoPasswordDTO("email-sin-formato");

            mockMvc.perform(post("/api/v1/auth/password/forgot")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.email").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/password/reset")
    class ResetPasswordEndpoint {

        @Test
        @DisplayName("204 NO CONTENT cuando el restablecimiento es exitoso")
        void reset_exitoso() throws Exception {
            RestablecerPasswordDTO request = new RestablecerPasswordDTO(
                    "token-xyz", "NuevaClave123!", "NuevaClave123!"
            );

            mockMvc.perform(post("/api/v1/auth/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(autenticacionService).restablecerPassword(request);
        }

        @Test
        @DisplayName("400 BAD REQUEST cuando las contraseñas no coinciden")
        void reset_noCoinciden() throws Exception {
            RestablecerPasswordDTO request = new RestablecerPasswordDTO(
                    "token-xyz", "NuevaClave123!", "OtraClaveDiferente123!"
            );

            doThrow(new PasswordNoCoincideException())
                    .when(autenticacionService).restablecerPassword(request);

            mockMvc.perform(post("/api/v1/auth/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.confirmarPassword").exists());
        }

        @Test
        @DisplayName("400 BAD REQUEST cuando la contraseña no cumple la política de complejidad")
        void reset_passwordDebil() throws Exception {
            RestablecerPasswordDTO request = new RestablecerPasswordDTO(
                    "token-xyz", "debil", "debil"
            );

            doThrow(new PasswordDebilException("La contraseña debe tener al menos 8 caracteres"))
                    .when(autenticacionService).restablecerPassword(request);

            mockMvc.perform(post("/api/v1/auth/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.password").exists());
        }

        @Test
        @DisplayName("401 UNAUTHORIZED cuando el token es inválido o expirado")
        void reset_tokenInvalido() throws Exception {
            RestablecerPasswordDTO request = new RestablecerPasswordDTO(
                    "token-invalido", "NuevaClave123!", "NuevaClave123!"
            );

            doThrow(new TokenRestablecimientoInvalidoException())
                    .when(autenticacionService).restablecerPassword(request);

            mockMvc.perform(post("/api/v1/auth/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").exists());
        }
    }
}
