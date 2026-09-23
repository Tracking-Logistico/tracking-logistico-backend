package com.udea.demo.config;

import com.udea.demo.pedidos.interfaces.services.PedidoServiceI;
import com.udea.demo.rutas.application.service.RegistroAsignacionService;
import com.udea.demo.usuarios.application.dto.UsuarioResponseDTO;
import com.udea.demo.usuarios.application.service.AutenticacionService;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.domain.model.SesionUsuario;
import com.udea.demo.usuarios.interfaces.persistence.UsuarioRepository;
import com.udea.demo.usuarios.interfaces.persistence.SesionUsuarioRepository;
import com.udea.demo.usuarios.domain.model.EstadoUsuario;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.interfaces.services.UsuarioInternoServiceI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AutorizacionFlujosOperativosTest {
    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private SesionUsuarioRepository sesiones;
    @MockitoBean private PedidoServiceI pedidos;
    @MockitoBean private RegistroAsignacionService notificaciones;
    @MockitoBean private UsuarioInternoServiceI usuariosInternos;

    @Test
    @Transactional
    void bearerRealDeOperadorPermiteValidarPedidos() throws Exception {
        Usuario usuario = usuarios.saveAndFlush(Usuario.builder()
                .nombre("Operador JWT").email("operador-bearer-test@tracking.com")
                .password("hash").rol(Rol.OPERADOR).estado(EstadoUsuario.ACTIVO)
                .activo(true).aceptoTerminos(false).build());
        String token = "test-operador-bearer";
        LocalDateTime ahora = LocalDateTime.now();
        sesiones.saveAndFlush(new SesionUsuario(usuario, AutenticacionService.hash(token),
                AutenticacionService.hash("test-operador-refresh"), ahora.plusMinutes(15),
                ahora.plusDays(7), ahora));

        mvc.perform(put("/api/v1/pedidos/42/validar")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprobar\":true,\"prioridadConfirmada\":\"ALTA\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void bearerRealDeConductorPermiteVerNotificaciones() throws Exception {
        Usuario usuario = usuarios.saveAndFlush(Usuario.builder()
                .nombre("Conductor JWT").email("conductor-bearer-test@tracking.com")
                .password("hash").rol(Rol.CONDUCTOR).estado(EstadoUsuario.ACTIVO)
                .activo(true).aceptoTerminos(false).build());
        String token = "test-conductor-bearer";
        LocalDateTime ahora = LocalDateTime.now();
        sesiones.saveAndFlush(new SesionUsuario(usuario, AutenticacionService.hash(token),
                AutenticacionService.hash("test-conductor-refresh"), ahora.plusMinutes(15),
                ahora.plusDays(7), ahora));
        when(notificaciones.misNotificaciones()).thenReturn(List.of());

        mvc.perform(get("/api/v1/rutas/mis-notificaciones")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void operadorPuedeConfirmarPrioridadYAprobar() throws Exception {
        mvc.perform(put("/api/v1/pedidos/42/validar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprobar\":true,\"prioridadConfirmada\":\"ALTA\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CONDUCTOR")
    void conductorPuedeLeerYMarcarSusNotificaciones() throws Exception {
        when(notificaciones.misNotificaciones()).thenReturn(List.of());
        mvc.perform(get("/api/v1/rutas/mis-notificaciones"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/rutas/mis-notificaciones/10/leer"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNoPuedeAprobarNiVerNotificacionesDeConductor() throws Exception {
        mvc.perform(put("/api/v1/pedidos/42/validar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprobar\":true}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/rutas/mis-notificaciones"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void apiKeyDbaPrevaleceSobreUnBearerAdjunto() throws Exception {
        Usuario usuario = usuarios.saveAndFlush(Usuario.builder()
                .nombre("Cliente JWT").email("cliente-bearer-y-dba@tracking.com")
                .password("hash").rol(Rol.CLIENTE).estado(EstadoUsuario.ACTIVO)
                .activo(true).aceptoTerminos(true).fechaAceptacionTerminos(LocalDateTime.now()).build());
        String token = "test-cliente-y-dba";
        LocalDateTime ahora = LocalDateTime.now();
        sesiones.saveAndFlush(new SesionUsuario(usuario, AutenticacionService.hash(token),
                AutenticacionService.hash("test-cliente-refresh-y-dba"), ahora.plusMinutes(20),
                ahora.plusDays(7), ahora));
        when(usuariosInternos.editar(eq(22L), any())).thenReturn(new UsuarioResponseDTO(
                22L, "Cliente", "cliente@test.com", null, null, Rol.OPERADOR,
                EstadoUsuario.ACTIVO, LocalDateTime.now()));

        mvc.perform(patch("/api/v1/admin/usuarios/22/rol")
                .header("X-DBA-Key", "test-dba-key")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rol\":\"OPERADOR\",\"codigoEmpleado\":\"OP-22\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void dbaPuedeConvertirUnClienteEnOperador() throws Exception {
        when(usuariosInternos.editar(eq(22L), any())).thenReturn(new UsuarioResponseDTO(
                22L, "Cliente", "cliente@test.com", null, null, Rol.OPERADOR,
                EstadoUsuario.ACTIVO, LocalDateTime.now()));
        mvc.perform(patch("/api/v1/admin/usuarios/22/rol")
                .header("X-DBA-Key", "test-dba-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rol\":\"OPERADOR\",\"codigoEmpleado\":\"OP-22\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("OPERADOR"));
    }
}
