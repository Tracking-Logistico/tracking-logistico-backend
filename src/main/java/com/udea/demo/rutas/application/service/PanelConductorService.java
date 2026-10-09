package com.udea.demo.rutas.application.service;

import com.udea.demo.pedidos.application.dto.HistorialPedidoResponseDTO;
import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.interfaces.services.PedidoServiceI;
import com.udea.demo.rutas.application.dto.*;
import com.udea.demo.rutas.domain.model.EstadoParada;
import com.udea.demo.rutas.domain.model.ParadaRuta;
import com.udea.demo.rutas.domain.model.Ruta;
import com.udea.demo.rutas.interfaces.persistence.RutaRepository;
import com.udea.demo.rutas.interfaces.services.PanelConductorServiceI;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import com.udea.demo.usuarios.domain.model.Conductor;
import com.udea.demo.usuarios.interfaces.persistence.ConductorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PanelConductorService implements PanelConductorServiceI {

    private static final Set<EstadoPedido> ESTADOS_FALLIDOS = EnumSet.of(
            EstadoPedido.ENTREGA_FALLIDA, EstadoPedido.DEVOLUCION_AL_REMITENTE, EstadoPedido.ENTREGA_FALLIDA_CERRADA);


    private final RutaRepository rutas;
    private final ConductorRepository conductores;
    private final PedidoServiceI pedidos;
    private final ActorAuthorizationService actores;

    @Value("${app.operations.time-zone:America/Bogota}")
    private String zonaHoraria = "UTC";

    public PanelConductorService(RutaRepository rutas, ConductorRepository conductores,
                                 PedidoServiceI pedidos, ActorAuthorizationService actores) {
        this.rutas = rutas;
        this.conductores = conductores;
        this.pedidos = pedidos;
        this.actores = actores;
    }

    @Override @Transactional(readOnly = true)
    public ProgresoPanelDTO progreso() {
        Ruta ruta = rutaDeHoyDelConductorAutenticado();
        if (ruta == null) return new ProgresoPanelDTO(hoy(), 0, 0, 0, 0, 0);

        List<ParadaRuta> activas = ruta.getParadas();
        Map<Long, PedidoResponseDTO> pedidosPorId = pedidos.obtenerPorIds(
                activas.stream().map(ParadaRuta::getPedidoId).collect(Collectors.toSet()));

        int total = 0, entregadas = 0, pendientes = 0, fallidas = 0, canceladas = 0;
        for (ParadaRuta parada : activas) {
            if (parada.getEstado() == EstadoParada.CANCELADA) {
                canceladas++;
                continue;
            }
            total++;
            if (parada.getEstado() == EstadoParada.ENTREGADO) {
                entregadas++;
                continue;
            }
            PedidoResponseDTO pedido = pedidosPorId.get(parada.getPedidoId());
            if (pedido == null) { pendientes++; continue; }
            if (ESTADOS_FALLIDOS.contains(pedido.estado())) fallidas++;
            else pendientes++;
        }
        return new ProgresoPanelDTO(hoy(), total, entregadas, pendientes, fallidas, canceladas);
    }

    @Override @Transactional(readOnly = true)
    public List<ParadaPanelDTO> entregasAsignadas() {
        Ruta ruta = rutaDeHoyDelConductorAutenticado();
        if (ruta == null) return List.of();

        Map<Long, PedidoResponseDTO> pedidosPorId = pedidos.obtenerPorIds(
                ruta.getParadas().stream().map(ParadaRuta::getPedidoId).collect(Collectors.toSet()));

        return ruta.getParadas().stream()
                .filter(p -> p.getEstado() != EstadoParada.CANCELADA)
                .sorted(Comparator.comparing(ParadaRuta::getOrden))
                .map(p -> mapParadaPanel(p, pedidosPorId.get(p.getPedidoId())))
                .toList();
    }

    @Override @Transactional(readOnly = true)
    public DetalleEntregaDTO detalleEntrega(Long pedidoId) {
        Ruta ruta = rutaDeHoyDelConductorAutenticado();
        if (ruta == null) throw new AccessDeniedException("El envío no está asignado al conductor autenticado");

        ParadaRuta parada = ruta.getParadas().stream()
                .filter(p -> p.getPedidoId().equals(pedidoId) && p.getEstado() != EstadoParada.CANCELADA)
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("El envío no está asignado al conductor autenticado"));

        PedidoResponseDTO pedido = pedidos.obtenerParaConductor(pedidoId);
        List<HistorialPedidoResponseDTO> historial = pedidos.historialParaConductor(pedidoId);
        List<DetalleEntregaDTO.EventoHistorialDTO> ultimos = historial.stream()
                .sorted(Comparator.comparing(HistorialPedidoResponseDTO::fecha).reversed())
                .limit(5)
                .map(h -> new DetalleEntregaDTO.EventoHistorialDTO(h.tipoEvento(), h.detalle(), h.fecha()))
                .toList();

        return new DetalleEntregaDTO(
                parada.getId(), pedido.id(), parada.getOrden(),
                pedido.numeroPedido(), pedido.numeroTracking(),
                pedido.direccionDestino(), pedido.ciudadDestino(), pedido.codigoPostalDestino(),
                pedido.destinatarioNombre(), pedido.destinatarioTelefono(),
                pedido.indicacionesAcceso(),
                pedido.descripcionPaquete(),
                pedido.pesoKg(), pedido.largoCm(), pedido.anchoCm(), pedido.altoCm(),
                prioridadDe(pedido).name(),
                parada.getEstado().name(),
                pedido.estado().name(),
                pedido.observacionesValidacion(),
                pedido.fechaEntregaReprogramada(),
                ultimos);
    }

    @Override @Transactional(readOnly = true)
    public SiguienteParadaDTO siguienteParada() {
        Ruta ruta = rutaDeHoyDelConductorAutenticado();
        if (ruta == null) return null;

        Optional<ParadaRuta> siguiente = ruta.getParadas().stream()
                .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                .sorted(Comparator.comparing(ParadaRuta::getOrden))
                .findFirst();
        if (siguiente.isEmpty()) return null;

        ParadaRuta parada = siguiente.get();
        PedidoResponseDTO pedido = pedidos.obtenerParaConductor(parada.getPedidoId());
        return new SiguienteParadaDTO(
                parada.getId(), parada.getPedidoId(), parada.getOrden(),
                pedido.numeroTracking(), pedido.direccionDestino(), pedido.ciudadDestino(),
                pedido.destinatarioNombre(), pedido.destinatarioTelefono(),
                pedido.indicacionesAcceso(), pedido.pesoKg());
    }

    // ----- helpers -----

    private Ruta rutaDeHoyDelConductorAutenticado() {
        Long usuarioId = actores.conductorActualUsuarioId();
        Conductor conductor = conductores.findByUsuarioId(usuarioId).orElse(null);
        if (conductor == null) return null;
        return rutas.findByConductorIdAndFecha(conductor.getId(), hoy()).orElse(null);
    }

    private LocalDate hoy() { return LocalDate.now(ZoneId.of(zonaHoraria)); }

    private ParadaPanelDTO mapParadaPanel(ParadaRuta p, PedidoResponseDTO pedido) {
        if (pedido == null) {
            return new ParadaPanelDTO(p.getId(), p.getPedidoId(), p.getOrden(),
                    null, null, null, null, null, null, null, null,
                    p.getEstado().name(), null);
        }
        return new ParadaPanelDTO(
                p.getId(), p.getPedidoId(), p.getOrden(),
                pedido.numeroPedido(), pedido.numeroTracking(),
                pedido.direccionDestino(), pedido.ciudadDestino(),
                pedido.destinatarioNombre(), pedido.destinatarioTelefono(),
                prioridadDe(pedido).name(), pedido.pesoKg(),
                p.getEstado().name(), pedido.estado().name());
    }

    private com.udea.demo.pedidos.domain.model.Prioridad prioridadDe(PedidoResponseDTO p) {
        return p.prioridadConfirmada() != null ? p.prioridadConfirmada() : p.prioridadSugerida();
    }
}