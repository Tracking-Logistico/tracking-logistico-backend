package com.udea.demo.pedidos.infrastructure.controller;

import com.udea.demo.pedidos.domain.event.DevolucionIniciadaEvent;
import com.udea.demo.pedidos.domain.event.EntregaReprogramadaEvent;
import com.udea.demo.pedidos.domain.event.IncidenciaRegistradaEvent;
import com.udea.demo.pedidos.domain.event.VerificacionDireccionSolicitadaEvent;
import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.rutas.domain.model.EstadoParada;
import com.udea.demo.rutas.domain.model.Ruta;
import com.udea.demo.rutas.interfaces.persistence.ParadaRutaRepository;
import com.udea.demo.rutas.interfaces.persistence.RutaRepository;
import com.udea.demo.usuarios.application.service.AutenticacionService;
import com.udea.demo.usuarios.domain.model.*;
import com.udea.demo.usuarios.interfaces.persistence.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RecordApplicationEvents
@DisplayName("HU-07 Incidencias - integración HTTP, seguridad, seguimiento del cliente y rutas")
class IncidenciaIntegracionTest {
    @Autowired private MockMvc mvc;
    @Autowired private ApplicationEvents eventos;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private SesionUsuarioRepository sesiones;
    @Autowired private OperadorRepository operadores;
    @Autowired private ClienteRepository clientes;
    @Autowired private ConductorRepository conductores;
    @Autowired private RutaRepository rutas;
    @Autowired private ParadaRutaRepository paradas;
    @Autowired private PedidoRepository pedidos;

    private String tokenOperador;
    private String tokenCliente;
    private Long clienteId;
    private Long conductorId;

    @BeforeEach
    void datos() {
        Usuario operador = usuario("operador-hu07@tracking.com", Rol.OPERADOR);
        operadores.saveAndFlush(new Operador(null, operador, "OP-HU07"));
        tokenOperador = sesion(operador, "token-operador-hu07");
        Usuario cliente = usuario("cliente-hu07@tracking.com", Rol.CLIENTE);
        clienteId = clientes.saveAndFlush(new Cliente(null, cliente, "Medellín")).getId();
        tokenCliente = sesion(cliente, "token-cliente-hu07");
        Usuario conductor = usuario("conductor-hu07@tracking.com", Rol.CONDUCTOR);
        conductorId = conductores.saveAndFlush(Conductor.builder().usuario(conductor).licencia("LIC-HU07")
                .estado("ACTIVO").build()).getId();
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

    private Pedido envioEnReparto(String tracking) {
        Pedido pedido = pedidos.saveAndFlush(Pedido.builder().numeroPedido("PED-" + tracking).clienteId(clienteId)
                .direccionOrigen("Carrera 7 #71-21").direccionDestino("Calle 45 #12-30").ciudadDestino("Medellín")
                .descripcionPaquete("Caja").pesoKg(2.0).largoCm(10.0).anchoCm(10.0).altoCm(10.0)
                .tipoServicio(TipoServicio.ESTANDAR).prioridadSugerida(Prioridad.MEDIA).estado(EstadoPedido.EN_REPARTO)
                .fechaCreacion(LocalDateTime.now()).numeroTracking(tracking).build());
        Ruta ruta = rutas.findByConductorIdAndFecha(conductorId, LocalDate.now())
                .orElseGet(() -> Ruta.crear(conductorId, LocalDate.now()));
        ruta.agregarParada(pedido.getId());
        rutas.saveAndFlush(ruta);
        return pedido;
    }

    private org.springframework.test.web.servlet.ResultActions incidencia(Long pedidoId, String json) throws Exception {
        return mvc.perform(post("/api/v1/pedidos/{id}/incidencias", pedidoId)
                .header("Authorization", "Bearer " + tokenOperador)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    @DisplayName("Cliente ausente → el cliente ve la novedad en su seguimiento y reprograma dentro del rango")
    void flujoEntregaFallidaYReprogramacion() throws Exception {
        Pedido pedido = envioEnReparto("LTHU07000001");

        incidencia(pedido.getId(), "{\"tipo\":\"CLIENTE_AUSENTE\",\"latitud\":6.24,\"longitud\":-75.58,\"precisionMetros\":10}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPedido").value("ENTREGA_FALLIDA"))
                .andExpect(jsonPath("$.intentosEntregaFallidos").value(1))
                .andExpect(jsonPath("$.incidencia.descripcionTipo").value("Cliente ausente"));
        assertThat(eventos.stream(IncidenciaRegistradaEvent.class)).hasSize(1);

        mvc.perform(get("/api/v1/pedidos/mios/tracking/{t}", pedido.getNumeroTracking())
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGA_FALLIDA"))
                .andExpect(jsonPath("$.novedad.titulo").value("Cliente ausente"))
                .andExpect(jsonPath("$.novedad.mensaje").value(TipoIncidencia.CLIENTE_AUSENTE.mensajeCliente()))
                .andExpect(jsonPath("$.movimientos[?(@.tipo == 'NOVEDAD')]").exists());

        mvc.perform(get("/api/v1/pedidos/mios/tracking/{t}/reprogramacion/rango", pedido.getNumeroTracking())
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.hasta").value(LocalDate.now().plusDays(7).toString()));

        mvc.perform(post("/api/v1/pedidos/mios/tracking/{t}/reprogramacion", pedido.getNumeroTracking())
                        .header("Authorization", "Bearer " + tokenCliente).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"" + LocalDate.now().minusDays(1) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FECHA_REPROGRAMACION_INVALIDA"));

        mvc.perform(post("/api/v1/pedidos/mios/tracking/{t}/reprogramacion", pedido.getNumeroTracking())
                        .header("Authorization", "Bearer " + tokenCliente).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"" + LocalDate.now().plusDays(2) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGA_REPROGRAMADA"))
                .andExpect(jsonPath("$.fechaEntregaReprogramada").value(LocalDate.now().plusDays(2).toString()));
        assertThat(eventos.stream(EntregaReprogramadaEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("Validaciones: tipo inválido 400, 'Otro' sin comentario 400, operador requerido")
    void validaciones() throws Exception {
        Pedido pedido = envioEnReparto("LTHU07000002");

        incidencia(pedido.getId(), "{\"tipo\":\"PERDIDO\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INCIDENCIA_INVALIDA"));
        incidencia(pedido.getId(), "{\"tipo\":\"OTRO\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMENTARIO_OBLIGATORIO"));
        incidencia(pedido.getId(), "{\"tipo\":\"OTRO\",\"comentario\":\"Portería cerrada por evento\"}")
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/pedidos/{id}/incidencias", pedido.getId())
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"OTRO\",\"comentario\":\"x\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/pedidos/incidencias/tipos").header("Authorization", "Bearer " + tokenOperador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(TipoIncidencia.values().length));
        mvc.perform(get("/api/v1/pedidos/{id}/incidencias", pedido.getId()).header("Authorization", "Bearer " + tokenOperador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidencias.length()").value(1));
    }

    @Test
    @DisplayName("Dirección incorrecta → Dirección por verificar con plazo; el cliente confirma y vuelve a reparto")
    void direccionPorVerificar() throws Exception {
        Pedido pedido = envioEnReparto("LTHU07000003");

        incidencia(pedido.getId(), "{\"tipo\":\"DIRECCION_INCORRECTA\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPedido").value("DIRECCION_POR_VERIFICAR"));
        assertThat(eventos.stream(VerificacionDireccionSolicitadaEvent.class)).hasSize(1);

        mvc.perform(put("/api/v1/pedidos/mios/tracking/{t}/direccion", pedido.getNumeroTracking())
                        .header("Authorization", "Bearer " + tokenCliente).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direccionDestino\":\"Carrera 80 #45-12 Apto 301\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_REPARTO"));
        assertThat(pedidos.findById(pedido.getId()).orElseThrow().getDireccionDestino())
                .isEqualTo("Carrera 80 #45-12 Apto 301");
    }

    @Test
    @DisplayName("Paquete rechazado → Devolución al remitente, evento y cierre de la parada del conductor")
    void paqueteRechazado() throws Exception {
        Pedido pedido = envioEnReparto("LTHU07000004");

        incidencia(pedido.getId(), "{\"tipo\":\"PAQUETE_RECHAZADO\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPedido").value("DEVOLUCION_AL_REMITENTE"));

        assertThat(eventos.stream(DevolucionIniciadaEvent.class))
                .singleElement().extracting(DevolucionIniciadaEvent::motivo).isEqualTo(MotivoDevolucion.PAQUETE_RECHAZADO);
        assertThat(paradas.findByPedidoIdAndEstado(pedido.getId(), EstadoParada.PENDIENTE)).isEmpty();

        incidencia(pedido.getId(), "{\"tipo\":\"RETRASO_OPERATIVO\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENVIO_FINALIZADO"));
    }

    @Test
    @DisplayName("Tres intentos fallidos devuelven el envío y se rechaza un cuarto intento")
    void tresIntentos() throws Exception {
        Pedido pedido = envioEnReparto("LTHU07000005");
        for (int intento = 1; intento <= 2; intento++) {
            incidencia(pedido.getId(), "{\"tipo\":\"CLIENTE_AUSENTE\"}").andExpect(status().isCreated());
            mvc.perform(post("/api/v1/pedidos/mios/tracking/{t}/reprogramacion", pedido.getNumeroTracking())
                            .header("Authorization", "Bearer " + tokenCliente).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fecha\":\"" + LocalDate.now().plusDays(1) + "\"}"))
                    .andExpect(status().isOk());
        }

        incidencia(pedido.getId(), "{\"tipo\":\"CLIENTE_AUSENTE\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPedido").value("DEVOLUCION_AL_REMITENTE"))
                .andExpect(jsonPath("$.intentosEntregaFallidos").value(3));
        assertThat(eventos.stream(DevolucionIniciadaEvent.class))
                .singleElement().extracting(DevolucionIniciadaEvent::motivo).isEqualTo(MotivoDevolucion.MAXIMO_INTENTOS);

        incidencia(pedido.getId(), "{\"tipo\":\"CLIENTE_AUSENTE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENVIO_FINALIZADO"));
    }
}
