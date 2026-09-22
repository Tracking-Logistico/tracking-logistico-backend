package com.udea.demo.usuarios.infrastructure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.udea.demo.usuarios.application.dto.ActualizarPerfilRequestDTO;
import com.udea.demo.usuarios.application.dto.ClienteResponseDTO;
import com.udea.demo.usuarios.application.dto.RegistroClienteRequestDTO;
import com.udea.demo.usuarios.application.dto.UsuarioResponseDTO;
import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.interfaces.services.ClienteServiceI;
import com.udea.demo.usuarios.interfaces.services.UsuarioInternoServiceI;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteController - HU-01A endpoints REST")
class ClienteControllerTest {

    @Mock private ClienteServiceI clienteService;
    @Mock private UsuarioInternoServiceI usuarioInternoService;

    @InjectMocks private ClienteController clienteController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clienteController)
                .setControllerAdvice(new UsuarioControllerAdvice())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private RegistroClienteRequestDTO registroValido() {
        return new RegistroClienteRequestDTO(
                "Ana Pérez", "ana@tracking.com", "Password123!", "Password123!",
                "3001234567", "Calle 10", "Medellín", true, "T&C-v1.0");
    }

    @Nested
    @DisplayName("POST /api/v1/clientes/registro")
    class Registro {

        @Test
        @DisplayName("201 Created cuando el registro es exitoso")
        void registro_devuelve201() throws Exception {
            // Arrange
            ClienteResponseDTO respuesta = new ClienteResponseDTO(
                    10L, "Ana Pérez", "ana@tracking.com", "3001234567", "Calle 10",
                    "Medellín", Rol.CLIENTE, EstadoUsuario.PENDIENTE_VERIFICACION, LocalDateTime.now());
            when(clienteService.registrarCliente(any(RegistroClienteRequestDTO.class))).thenReturn(respuesta);

            // Act & Assert
            mockMvc.perform(post("/api/v1/clientes/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registroValido())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.idCliente").value(10))
                    .andExpect(jsonPath("$.rol").value("CLIENTE"))
                    .andExpect(jsonPath("$.estado").value("PENDIENTE_VERIFICACION"))
                    .andExpect(jsonPath("$.email").value("ana@tracking.com"));

            verify(clienteService).registrarCliente(any(RegistroClienteRequestDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request cuando el correo ya está registrado")
        void registro_correoDuplicado_devuelve400() throws Exception {
            // Arrange
            when(clienteService.registrarCliente(any(RegistroClienteRequestDTO.class)))
                    .thenThrow(new IllegalArgumentException("El correo ya se encuentra registrado"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/clientes/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registroValido())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("El correo ya se encuentra registrado"));
        }

        @Test
        @DisplayName("400 Bad Request con errores por campo cuando el DTO es inválido")
        void registro_dtoInvalido_devuelve400PorCampo() throws Exception {
            // Arrange
            String cuerpo = """
                    {"nombre":"","email":"no-es-email","password":"123","confirmarPassword":"",
                     "aceptoTerminos":false,"versionTerminos":""}
                    """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/clientes/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.nombre").exists())
                    .andExpect(jsonPath("$.email").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/clientes/verificar")
    class Verificar {

        @Test
        @DisplayName("200 OK cuando el token es válido")
        void verificar_devuelve200() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/clientes/verificar").param("token", "uuid-token"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Cuenta verificada con éxito. Ya puedes iniciar sesión."));

            verify(clienteService).verificarCuenta("uuid-token");
        }

        @Test
        @DisplayName("400 Bad Request cuando el token expiró")
        void verificar_tokenExpirado_devuelve400() throws Exception {
            // Arrange
            doThrow(new IllegalArgumentException("El token de verificación ha expirado"))
                    .when(clienteService).verificarCuenta("expirado");

            // Act & Assert
            mockMvc.perform(get("/api/v1/clientes/verificar").param("token", "expirado"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("El token de verificación ha expirado"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/clientes/{id}/perfil")
    class Perfil {

        @Test
        @DisplayName("200 OK al actualizar el perfil")
        void actualizarPerfil_devuelve200() throws Exception {
            // Arrange
            UsuarioResponseDTO respuesta = new UsuarioResponseDTO(
                    1L, "Ana María", "ana@tracking.com", "300999", "Nueva",
                    Rol.CLIENTE, EstadoUsuario.ACTIVO, LocalDateTime.now());
            when(clienteService.actualizarPerfil(eq(1L), any(ActualizarPerfilRequestDTO.class)))
                    .thenReturn(respuesta);
            ActualizarPerfilRequestDTO dto = new ActualizarPerfilRequestDTO("Ana María", "300999", "Nueva");

            // Act & Assert
            mockMvc.perform(put("/api/v1/clientes/1/perfil")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Ana María"))
                    .andExpect(jsonPath("$.id").value(1));

            verify(clienteService).actualizarPerfil(eq(1L), any(ActualizarPerfilRequestDTO.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/clientes/{id}")
    class Baja {

        @Test
        @DisplayName("204 No Content en baja lógica")
        void desactivar_devuelve204() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/api/v1/clientes/10"))
                    .andExpect(status().isNoContent());

            verify(clienteService).desactivarCuentaCliente(10L);
        }
    }
}
