package com.udea.demo.usuarios.infrastructure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.demo.usuarios.application.dto.CambiarPasswordRequestDTO;
import com.udea.demo.usuarios.domain.exception.PasswordNoCoincideException;
import com.udea.demo.usuarios.domain.exception.UsuarioNoEncontradoException;
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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioControler - HU-01B cambio de password")
class UsuarioControlerTest {

    @Mock private UsuarioInternoServiceI usuarioInternoService;
    @InjectMocks private UsuarioControler usuarioControler;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(usuarioControler)
                .setControllerAdvice(new UsuarioControllerAdvice())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("PUT /api/v1/usuarios/{id}/password")
    class CambiarPasswordEndpoint {

        @Test
        @DisplayName("200 OK cuando el cambio de clave es exitoso")
        void cambiarPassword_devuelve200() throws Exception {
            // Arrange
            CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO(
                    "Temporal1!", "NuevaClave1!", "NuevaClave1!");

            // Act & Assert
            mockMvc.perform(put("/api/v1/usuarios/20/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Contraseña actualizada correctamente."));

            verify(usuarioInternoService).cambiarPassword(20L, "Temporal1!", "NuevaClave1!", "NuevaClave1!");
        }

        @Test
        @DisplayName("400 Bad Request cuando las confirmaciones no coinciden")
        void cambiarPassword_noCoincide_devuelve400() throws Exception {
            // Arrange
            CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO(
                    "Temporal1!", "NuevaClave1!", "NuevaClave1!");
            doThrow(new PasswordNoCoincideException())
                    .when(usuarioInternoService)
                    .cambiarPassword(20L, "Temporal1!", "NuevaClave1!", "NuevaClave1!");

            // Act & Assert
            mockMvc.perform(put("/api/v1/usuarios/20/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.confirmarPassword").value("Las contraseñas no coinciden"));
        }

        @Test
        @DisplayName("404 Not Found cuando el usuario no existe")
        void cambiarPassword_usuarioNoExiste_devuelve404() throws Exception {
            CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO(
                    "Temporal1!", "NuevaClave1!", "NuevaClave1!");
            doThrow(new UsuarioNoEncontradoException(99L))
                    .when(usuarioInternoService)
                    .cambiarPassword(99L, "Temporal1!", "NuevaClave1!", "NuevaClave1!");

            mockMvc.perform(put("/api/v1/usuarios/99/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Usuario no encontrado con ID: 99"));
        }

        @Test
        @DisplayName("400 Bad Request cuando el DTO viola la política de complejidad")
        void cambiarPassword_dtoInvalido_devuelve400() throws Exception {
            String cuerpo = """
                    {"passwordActual":"","nuevaPassword":"123","confirmarPassword":""}
                    """;

            mockMvc.perform(put("/api/v1/usuarios/20/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.passwordActual").exists())
                    .andExpect(jsonPath("$.nuevaPassword").exists());
        }
    }
}
