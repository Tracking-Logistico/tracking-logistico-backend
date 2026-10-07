package com.udea.demo.pedidos.infrastructure.controller;

import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.interfaces.persistence.HistorialPedidoRepository;
import com.udea.demo.pedidos.interfaces.persistence.IncidenciaPedidoRepository;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.usuarios.application.service.AutenticacionService;
import com.udea.demo.usuarios.domain.model.*;
import com.udea.demo.usuarios.interfaces.persistence.OperadorRepository;
import com.udea.demo.usuarios.interfaces.persistence.SesionUsuarioRepository;
import com.udea.demo.usuarios.interfaces.persistence.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Criterio 7: dos operadores registran una incidencia sobre el mismo envío al mismo tiempo.
 * Sin transacción de prueba: cada solicitud confirma su propia transacción contra H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("HU-07 Concurrencia en la gestión de incidencias")
class IncidenciaConcurrenciaIntegracionTest {
    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private SesionUsuarioRepository sesiones;
    @Autowired private OperadorRepository operadores;
    @Autowired private PedidoRepository pedidos;
    @Autowired private IncidenciaPedidoRepository incidencias;
    @Autowired private HistorialPedidoRepository historial;

    private final List<Usuario> creados = new ArrayList<>();
    private final List<Operador> perfiles = new ArrayList<>();
    private Pedido pedido;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void datos() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        tokenA = operador("op-a-" + sufijo);
        tokenB = operador("op-b-" + sufijo);
        pedido = pedidos.saveAndFlush(Pedido.builder().numeroPedido("PED-CC-" + sufijo).clienteId(10L)
                .direccionOrigen("Carrera 7 #71-21").direccionDestino("Calle 45 #12-30").descripcionPaquete("Caja")
                .pesoKg(2.0).largoCm(10.0).anchoCm(10.0).altoCm(10.0).tipoServicio(TipoServicio.ESTANDAR)
                .prioridadSugerida(Prioridad.MEDIA).estado(EstadoPedido.EN_REPARTO).fechaCreacion(LocalDateTime.now())
                .numeroTracking("LTCC" + sufijo).build());
    }

    @AfterEach
    void limpiar() {
        incidencias.deleteAll(incidencias.findByPedidoIdOrderByFechaAscIdAsc(pedido.getId()));
        historial.deleteAll(historial.findByPedidoIdOrderByFechaAsc(pedido.getId()));
        pedidos.deleteById(pedido.getId());
        sesiones.deleteAll(sesiones.findAll().stream().filter(s -> creados.stream()
                .anyMatch(u -> u.getId().equals(s.getUsuario().getId()))).toList());
        operadores.deleteAll(perfiles);
        usuarios.deleteAll(creados);
    }

    private String operador(String nombre) {
        Usuario u = usuarios.saveAndFlush(Usuario.builder().nombre(nombre).email(nombre + "@tracking.com").password("hash")
                .rol(Rol.OPERADOR).estado(EstadoUsuario.ACTIVO).activo(true).aceptoTerminos(false).build());
        creados.add(u);
        perfiles.add(operadores.saveAndFlush(new Operador(null, u, "EMP-" + nombre)));
        String token = "token-" + nombre;
        LocalDateTime ahora = LocalDateTime.now();
        sesiones.saveAndFlush(new SesionUsuario(u, AutenticacionService.hash(token),
                AutenticacionService.hash(token + "-refresh"), ahora.plusMinutes(15), ahora.plusDays(7), ahora));
        return token;
    }

    /** Lanza ambas solicitudes a la vez y devuelve los códigos HTTP obtenidos. */
    private List<Integer> simultaneas(String jsonA, String jsonB) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            Future<Integer> a = pool.submit(solicitud(salida, tokenA, jsonA));
            Future<Integer> b = pool.submit(solicitud(salida, tokenB, jsonB));
            salida.countDown();
            return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    private Callable<Integer> solicitud(CountDownLatch salida, String token, String json) {
        return () -> {
            salida.await();
            return mvc.perform(post("/api/v1/pedidos/{id}/incidencias", pedido.getId())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andReturn().getResponse().getStatus();
        };
    }

    @Test
    @DisplayName("Con la misma versión esperada se confirma la primera y se rechaza la segunda (409)")
    void mismaVersionEsperada() throws Exception {
        long version = pedidos.findById(pedido.getId()).orElseThrow().getVersion();
        String json = "{\"tipo\":\"RETRASO_OPERATIVO\",\"versionEsperada\":" + version + "}";

        List<Integer> codigos = simultaneas(json, json);

        assertThat(codigos).containsExactlyInAnyOrder(201, 409);
        assertThat(incidencias.findByPedidoIdOrderByFechaAscIdAsc(pedido.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Dos 'Cliente ausente' simultáneos no duplican el intento: uno se aplica y el otro se rechaza")
    void dosIntentosSimultaneos() throws Exception {
        String json = "{\"tipo\":\"CLIENTE_AUSENTE\"}";

        List<Integer> codigos = simultaneas(json, json);

        assertThat(codigos).containsExactlyInAnyOrder(201, 409);
        Pedido actualizado = pedidos.findById(pedido.getId()).orElseThrow();
        assertThat(actualizado.getEstado()).isEqualTo(EstadoPedido.ENTREGA_FALLIDA);
        assertThat(actualizado.getIntentosEntregaFallidos()).isEqualTo(1);
    }
}
