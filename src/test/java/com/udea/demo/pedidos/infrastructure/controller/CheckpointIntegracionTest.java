package com.udea.demo.pedidos.infrastructure.controller;

import com.udea.demo.pedidos.domain.event.CheckpointPendienteRevisionEvent;
import com.udea.demo.pedidos.domain.event.CheckpointRegistradoEvent;
import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.interfaces.persistence.CheckpointPedidoRepository;
import com.udea.demo.pedidos.interfaces.persistence.HistorialPedidoRepository;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.rutas.domain.model.Ruta;
import com.udea.demo.rutas.interfaces.persistence.RutaRepository;
import com.udea.demo.usuarios.application.service.AutenticacionService;
import com.udea.demo.usuarios.domain.model.*;
import com.udea.demo.usuarios.interfaces.persistence.ConductorRepository;
import com.udea.demo.usuarios.interfaces.persistence.SesionUsuarioRepository;
import com.udea.demo.usuarios.interfaces.persistence.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RecordApplicationEvents
@DisplayName("HU-06 Checkpoints - integración HTTP, seguridad y persistencia")
class CheckpointIntegracionTest {
    @Autowired private MockMvc mvc;
    @Autowired private ApplicationEvents eventos;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private SesionUsuarioRepository sesiones;
    @Autowired private ConductorRepository conductores;
    @Autowired private RutaRepository rutas;
    @Autowired private PedidoRepository pedidos;
    @Autowired private CheckpointPedidoRepository checkpoints;
    @Autowired private HistorialPedidoRepository historial;

    private Pedido pedido;
    private String tokenConductor;

    @BeforeEach
    void datos() {
        Usuario conductor = usuario("conductor-hu06@tracking.com", Rol.CONDUCTOR);
        tokenConductor = sesion(conductor, "token-conductor-hu06");
        Conductor perfil = conductores.saveAndFlush(Conductor.builder().usuario(conductor).licencia("LIC-HU06")
                .estado("ACTIVO").build());
        pedido = pedidos.saveAndFlush(Pedido.builder().numeroPedido("PED-HU06-1").clienteId(10L)
                .direccionOrigen("Carrera 7 #71-21").direccionDestino("Calle 45 #12-30").descripcionPaquete("Caja")
                .pesoKg(2.0).largoCm(10.0).anchoCm(10.0).altoCm(10.0).tipoServicio(TipoServicio.ESTANDAR)
                .prioridadSugerida(Prioridad.MEDIA).estado(EstadoPedido.EN_REPARTO).fechaCreacion(LocalDateTime.now())
                .numeroTracking("LTHU06000001").build());
        Ruta ruta = Ruta.crear(perfil.getId(), LocalDate.now());
        ruta.agregarParada(pedido.getId());
        rutas.saveAndFlush(ruta);
    }

    private Usuario usuario(String email, Rol rol) {
        return usuarios.saveAndFlush(Usuario.builder().nombre("Usuario " + rol).email(email).password("hash").rol(rol)
                .estado(EstadoUsuario.ACTIVO).activo(true).aceptoTerminos(false).build());
    }

    private String sesion(Usuario usuario, String token) {
        LocalDateTime ahora = LocalDateTime.now();
        sesiones.saveAndFlush(new SesionUsuario(usuario, AutenticacionService.hash(token),
                AutenticacionService.hash(token + "-refresh"), ahora.plusMinutes(15), ahora.plusDays(7), ahora));
        return token;
    }

    private String checkpointJson(String idEvento, String qr) {
        return """
                {"codigoQr":"%s","etapa":"EN_REPARTO","idEventoCliente":"%s",
                 "latitud":6.2442,"longitud":-75.5812,"precisionMetros":8,"fechaDispositivo":"%s"}
                """.formatted(qr, idEvento, OffsetDateTime.now().minusMinutes(1));
    }

    private String qrValido() { return CodigoQrEnvio.para(pedido.getNumeroTracking()).contenido(); }

    @Test
    @DisplayName("El conductor asignado registra el checkpoint: 201, línea de tiempo y evento publicado")
    void registraCheckpoint() throws Exception {
        String idEvento = UUID.randomUUID().toString();

        mvc.perform(post("/api/v1/pedidos/{id}/checkpoints", pedido.getId())
                        .header("Authorization", "Bearer " + tokenConductor)
                        .contentType(MediaType.APPLICATION_JSON).content(checkpointJson(idEvento, qrValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoRegistro").value("APLICADO"))
                .andExpect(jsonPath("$.ubicacionConfiable").value(true));

        assertThat(checkpoints.findByIdEventoCliente(idEvento)).isPresent();
        assertThat(historial.findByPedidoIdOrderByFechaAsc(pedido.getId()))
                .anyMatch(h -> "CHECKPOINT".equals(h.getTipoEvento()));
        assertThat(eventos.stream(CheckpointRegistradoEvent.class)).hasSize(1);

        mvc.perform(post("/api/v1/pedidos/{id}/checkpoints", pedido.getId())
                        .header("Authorization", "Bearer " + tokenConductor)
                        .contentType(MediaType.APPLICATION_JSON).content(checkpointJson(idEvento, qrValido())))
                .andExpect(status().isOk());
        assertThat(checkpoints.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("QR alterado responde 400 QR_INVALIDO")
    void qrInvalido() throws Exception {
        mvc.perform(post("/api/v1/pedidos/{id}/checkpoints", pedido.getId())
                        .header("Authorization", "Bearer " + tokenConductor).contentType(MediaType.APPLICATION_JSON)
                        .content(checkpointJson(UUID.randomUUID().toString(), "LTHU06000001|ZZZZZZZZ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QR_INVALIDO"));
    }

    @Test
    @DisplayName("Un conductor sin asignación recibe 403 ENVIO_NO_ASIGNADO")
    void conductorNoAsignado() throws Exception {
        String otroToken = sesion(usuario("otro-conductor-hu06@tracking.com", Rol.CONDUCTOR), "token-otro-hu06");

        mvc.perform(post("/api/v1/pedidos/{id}/checkpoints", pedido.getId())
                        .header("Authorization", "Bearer " + otroToken).contentType(MediaType.APPLICATION_JSON)
                        .content(checkpointJson(UUID.randomUUID().toString(), qrValido())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ENVIO_NO_ASIGNADO"));
    }

    @Test
    @DisplayName("Un cliente no puede registrar checkpoints")
    void clienteNoAutorizado() throws Exception {
        String tokenCliente = sesion(usuario("cliente-hu06@tracking.com", Rol.CLIENTE), "token-cliente-hu06");

        mvc.perform(post("/api/v1/pedidos/{id}/checkpoints", pedido.getId())
                        .header("Authorization", "Bearer " + tokenCliente).contentType(MediaType.APPLICATION_JSON)
                        .content(checkpointJson(UUID.randomUUID().toString(), qrValido())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Envío entregado: en línea 409 ENVIO_FINALIZADO; sincronizado queda pendiente de revisión")
    void envioEntregado() throws Exception {
        Pedido entregado = pedidos.findById(pedido.getId()).orElseThrow();
        entregado.cambiarEstadoLogistico(EstadoPedido.ENTREGADO);
        pedidos.saveAndFlush(entregado);

        mvc.perform(post("/api/v1/pedidos/{id}/checkpoints", pedido.getId())
                        .header("Authorization", "Bearer " + tokenConductor).contentType(MediaType.APPLICATION_JSON)
                        .content(checkpointJson(UUID.randomUUID().toString(), qrValido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENVIO_FINALIZADO"));

        String lote = """
                {"eventos":[{"pedidoId":%d,"checkpoint":%s}]}
                """.formatted(pedido.getId(), checkpointJson(UUID.randomUUID().toString(), qrValido()));
        mvc.perform(post("/api/v1/pedidos/checkpoints/sincronizacion")
                        .header("Authorization", "Bearer " + tokenConductor)
                        .contentType(MediaType.APPLICATION_JSON).content(lote))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendientesRevision").value(1))
                .andExpect(jsonPath("$.aplicados").value(0));
        assertThat(eventos.stream(CheckpointPendienteRevisionEvent.class)).hasSize(1);

        mvc.perform(get("/api/v1/pedidos/checkpoints/mios/pendientes-revision")
                        .header("Authorization", "Bearer " + tokenConductor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @DisplayName("El operador ve el checkpoint en su panel de novedades")
    void novedadesOperador() throws Exception {
        mvc.perform(post("/api/v1/pedidos/{id}/checkpoints", pedido.getId())
                        .header("Authorization", "Bearer " + tokenConductor).contentType(MediaType.APPLICATION_JSON)
                        .content(checkpointJson(UUID.randomUUID().toString(), qrValido())))
                .andExpect(status().isCreated());
        Usuario operador = usuario("operador-hu06@tracking.com", Rol.OPERADOR);
        String tokenOperador = sesion(operador, "token-operador-hu06");
        com.udea.demo.usuarios.domain.model.Operador perfil = new com.udea.demo.usuarios.domain.model.Operador();
        perfil.setUsuario(operador);
        perfil.setCodigoEmpleado("OP-HU06");
        operadores.saveAndFlush(perfil);

        mvc.perform(get("/api/v1/pedidos/novedades").header("Authorization", "Bearer " + tokenOperador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.pedidoId == %d && @.tipoEvento == 'CHECKPOINT')]",
                        pedido.getId()).exists());
    }

    @Autowired private com.udea.demo.usuarios.interfaces.persistence.OperadorRepository operadores;
}
