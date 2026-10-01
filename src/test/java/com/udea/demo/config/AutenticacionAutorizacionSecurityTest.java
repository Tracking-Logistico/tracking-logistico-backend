package com.udea.demo.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SecurityConfig - Autorización por roles y rechazo HTTP 403")
class AutenticacionAutorizacionSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Endpoints públicos sin autenticación")
    class EndpointsPublicos {

        @Test
        @DisplayName("POST /api/v1/auth/login es público (no devuelve 401 ni 403)")
        void login_publico() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/v1/auth/password/forgot es público (no devuelve 401 ni 403)")
        void forgot_publico() throws Exception {
            mockMvc.perform(post("/api/v1/auth/password/forgot")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Rol CLIENTE - restricciones y rechazo HTTP 403")
    class RolClienteRestricciones {

        @Test
        @WithMockUser(username = "cliente@correo.com", roles = {"CLIENTE"})
        @DisplayName("CLIENTE recibe HTTP 403 al intentar validar pedidos (solo OPERADOR)")
        void clienteNoPuedeValidarPedidos() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/pedidos/1/validar")
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "cliente@correo.com", roles = {"CLIENTE"})
        @DisplayName("CLIENTE recibe HTTP 403 al intentar acceder a /api/v1/panel/operador/**")
        void clienteAccedeAPanelOperador_rechaza403() throws Exception {
            mockMvc.perform(get("/api/v1/panel/operador/dashboard"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "cliente@correo.com", roles = {"CLIENTE"})
        @DisplayName("CLIENTE recibe HTTP 403 al intentar acceder a /api/v1/panel/conductor/**")
        void clienteAccedeAPanelConductor_rechaza403() throws Exception {
            mockMvc.perform(get("/api/v1/panel/conductor/rutas"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "cliente@correo.com", roles = {"CLIENTE"})
        @DisplayName("CLIENTE recibe HTTP 403 al intentar acceder a gestión de rutas de operador")
        void clienteAccedeARutasOperador_rechaza403() throws Exception {
            mockMvc.perform(get("/api/v1/rutas/envios-pendientes"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Rol OPERADOR - restricciones y rechazo HTTP 403")
    class RolOperadorRestricciones {

        @Test
        @WithMockUser(username = "operador@tracking.com", roles = {"OPERADOR"})
        @DisplayName("OPERADOR recibe HTTP 403 al intentar acceder a /api/v1/panel/cliente/**")
        void operadorAccedeAPanelCliente_rechaza403() throws Exception {
            mockMvc.perform(get("/api/v1/panel/cliente/mis-envios"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "operador@tracking.com", roles = {"OPERADOR"})
        @DisplayName("OPERADOR recibe HTTP 403 al intentar acceder a /api/v1/panel/conductor/**")
        void operadorAccedeAPanelConductor_rechaza403() throws Exception {
            mockMvc.perform(get("/api/v1/panel/conductor/rutas"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Rol CONDUCTOR - restricciones y rechazo HTTP 403")
    class RolConductorRestricciones {
        @Test
        @WithMockUser(username = "conductor@tracking.com", roles = {"CONDUCTOR"})
        @DisplayName("CONDUCTOR no puede consultar por URL la ruta de otro conductor")
        void conductorNoConsultaRutaPorUsuario() throws Exception {
            mockMvc.perform(get("/api/v1/rutas/conductores/999"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "conductor@tracking.com", roles = {"CONDUCTOR"})
        @DisplayName("CONDUCTOR no puede asignarse envíos")
        void conductorNoAsignaEnvios() throws Exception {
            mockMvc.perform(post("/api/v1/rutas/asignaciones/lote")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"conductorId\":1,\"pedidoIds\":[2]}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "conductor@tracking.com", roles = {"CONDUCTOR"})
        @DisplayName("CONDUCTOR recibe HTTP 403 al intentar acceder a /api/v1/panel/cliente/**")
        void conductorAccedeAPanelCliente_rechaza403() throws Exception {
            mockMvc.perform(get("/api/v1/panel/cliente/cuenta"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "conductor@tracking.com", roles = {"CONDUCTOR"})
        @DisplayName("CONDUCTOR recibe HTTP 403 al intentar consultar bandeja de validación")
        void conductorAccedeAPedidos_rechaza403() throws Exception {
            mockMvc.perform(get("/api/v1/pedidos/pendientes"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "conductor@tracking.com", roles = {"CONDUCTOR"})
        @DisplayName("CONDUCTOR recibe HTTP 403 al intentar acceder a /api/v1/panel/operador/**")
        void conductorAccedeAPanelOperador_rechaza403() throws Exception {
            mockMvc.perform(get("/api/v1/panel/operador/gestion"))
                    .andExpect(status().isForbidden());
        }
    }
}