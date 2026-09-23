package com.udea.demo.rutas.application.service;

import com.udea.demo.pedidos.application.dto.CambiarEstadoLogisticoRequestDTO;
import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.interfaces.services.PedidoServiceI;
import com.udea.demo.rutas.application.dto.*;
import com.udea.demo.rutas.domain.exception.*;
import com.udea.demo.rutas.domain.model.*;
import com.udea.demo.rutas.interfaces.persistence.*;
import com.udea.demo.rutas.interfaces.services.RutaServiceI;
import com.udea.demo.usuarios.domain.model.Conductor;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.interfaces.persistence.ConductorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Service
public class RutaService implements RutaServiceI {
    private final RutaRepository rutas;
    private final ParadaRutaRepository paradas;
    private final GestorRutaActiva gestor;
    private final PedidoServiceI pedidos;
    private final ConductorRepository conductores;
    private final RegistroAsignacionService registro;
    @Value("${app.operations.time-zone:America/Bogota}")
    private String zonaHoraria = "UTC";

    public RutaService(RutaRepository rutas, ParadaRutaRepository paradas, GestorRutaActiva gestor,
                       PedidoServiceI pedidos, ConductorRepository conductores, RegistroAsignacionService registro) {
        this.rutas = rutas; this.paradas = paradas; this.gestor = gestor; this.pedidos = pedidos; this.conductores = conductores; this.registro = registro;
    }

    @Override @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarEnviosPendientesDeAsignacion() {
        List<Long> asignados = paradas.findPedidoIdsByEstado(EstadoParada.PENDIENTE);
        return pedidos.listarEnTransito().stream().filter(p -> !asignados.contains(p.id())).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<ConductorDisponibleDTO> listarConductoresDisponibles() {
        return conductores.findAll().stream()
                .filter(c -> c.getUsuario() != null && Boolean.TRUE.equals(c.getUsuario().getActivo())
                        && "ACTIVO".equalsIgnoreCase(c.getEstado())
                        && c.getUsuario().getRol() == Rol.CONDUCTOR)
                .map(c -> {
                    List<PedidoResponseDTO> asignados = rutas.findByConductorIdAndFecha(c.getId(), LocalDate.now(ZoneId.of(zonaHoraria)))
                            .map(r -> r.getParadas().stream().filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                                .map(p -> pedidos.obtener(p.getPedidoId())).toList())
                            .orElseGet(List::of);
                    return new ConductorDisponibleDTO(c.getUsuario().getId(), c.getUsuario().getNombre(), c.getEstado(),
                        c.getCapacidadMaxKg(), c.getCapacidadMaxVolumenCm3(), c.getMaxEntregasDia(),
                        asignados.size(), asignados.stream().mapToDouble(PedidoResponseDTO::pesoKg).sum(),
                        asignados.stream().mapToDouble(this::volumen).sum());
                })
                .sorted(Comparator.comparing(ConductorDisponibleDTO::nombre))
                .toList();
    }

    @Override @Transactional
    public RutaResponseDTO asignarEnvio(AsignarEnvioRequestDTO dto) {
        paradas.findByPedidoIdAndEstado(dto.pedidoId(), EstadoParada.PENDIENTE)
            .ifPresent(p -> { throw new EnvioYaAsignadoException(dto.pedidoId()); });
        return asignarVarios(new AsignacionMasivaRequestDTO(dto.conductorId(), List.of(dto.pedidoId())));
    }

    @Override @Transactional
    public RutaResponseDTO asignarVarios(AsignacionMasivaRequestDTO dto) {
        if (dto.pedidoIds() == null || dto.pedidoIds().isEmpty() || dto.pedidoIds().size() > 100 ||
            dto.pedidoIds().contains(null) || dto.pedidoIds().stream().anyMatch(id -> id <= 0) ||
            new HashSet<>(dto.pedidoIds()).size() != dto.pedidoIds().size())
            throw new IllegalArgumentException("Selecciona entre 1 y 100 pedidos distintos");
        Conductor conductor = conductorPorUsuario(dto.conductorId(), true);
        Ruta ruta = gestor.obtenerOCrear(conductor.getId());
        for (Long pedidoId : dto.pedidoIds()) {
            paradas.findByPedidoIdAndEstado(pedidoId, EstadoParada.PENDIENTE)
                .ifPresent(p -> { throw new EnvioYaAsignadoException(pedidoId); });
            PedidoResponseDTO pedido = pedidos.obtener(pedidoId);
            if (pedido.estado() != EstadoPedido.RECIBIDO_EN_ORIGEN && pedido.estado() != EstadoPedido.EN_TRANSITO)
                throw new IllegalStateException("Solo pueden asignarse envíos recibidos en origen o en tránsito");
            validarCapacidad(ruta, conductor, pedido);
            ruta.agregarParada(pedidoId);
            pedidos.cambiarEstadoLogistico(pedidoId, new CambiarEstadoLogisticoRequestDTO(EstadoPedido.EN_REPARTO));
            registro.registrar(pedidoId, null, dto.conductorId(), "ASIGNACION", null);
        }
        priorizarAltas(ruta);
        return map(rutas.saveAndFlush(ruta));
    }

    @Override @Transactional
    public RutaResponseDTO reordenarRuta(Long rutaId, ReordenarRutaRequestDTO dto) {
        Ruta ruta = rutas.findById(rutaId).orElseThrow(() -> RutaNoEncontradaException.porId(rutaId));
        ruta.reordenar(dto.pedidoIdsEnOrden());
        return map(rutas.save(ruta));
    }

    @Override @Transactional
    public RutaResponseDTO reasignarEnvio(ReasignarEnvioRequestDTO dto) {
        ParadaRuta actual = paradas.findByPedidoIdAndEstado(dto.pedidoId(), EstadoParada.PENDIENTE)
                .orElseThrow(() -> new EnvioNoAsignadoException(dto.pedidoId()));
        Long anteriorId = conductores.findById(actual.getRuta().getConductorId())
                .map(c -> c.getUsuario().getId()).orElse(actual.getRuta().getConductorId());
        if (anteriorId.equals(dto.nuevoConductorId()))
            throw new IllegalArgumentException("El nuevo conductor debe ser diferente del actual");
        Conductor nuevo = conductorPorUsuario(dto.nuevoConductorId(), true);
        Ruta destino = gestor.obtenerOCrear(nuevo.getId());
        PedidoResponseDTO pedido = pedidos.obtener(dto.pedidoId());
        if (pedido.estado() != EstadoPedido.EN_REPARTO)
            throw new IllegalStateException("Solo se pueden reasignar envíos en reparto");
        validarCapacidad(destino, nuevo, pedido);
        actual.getRuta().cancelarParada(dto.pedidoId());

        rutas.saveAndFlush(actual.getRuta());
        destino.agregarParada(dto.pedidoId());
        priorizarAltas(destino);
        registro.registrar(dto.pedidoId(), anteriorId, dto.nuevoConductorId(), "REASIGNACION", dto.motivo());
        return map(rutas.saveAndFlush(destino));
    }

    @Override @Transactional(readOnly = true)
    public RutaResponseDTO obtenerRutaActivaDeConductor(Long conductorUsuarioId) {
        Conductor c = conductorPorUsuario(conductorUsuarioId);
        Ruta ruta = rutas.findByConductorIdAndFecha(c.getId(), LocalDate.now(ZoneId.of(zonaHoraria)))
                .orElseThrow(() -> RutaNoEncontradaException.paraConductor(conductorUsuarioId));
        return map(ruta);
    }

    private void priorizarAltas(Ruta ruta) {
        List<Long> orden = ruta.getParadas().stream()
                .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                .sorted(Comparator.comparingInt(p -> prioridadOrden(pedidos.obtener(p.getPedidoId()))))
                .map(ParadaRuta::getPedidoId).toList();
        if (!orden.isEmpty() && !ruta.isOrdenManual()) ruta.reordenarAutomatico(orden);
    }

    private int prioridadOrden(PedidoResponseDTO p) {
        var prioridad = p.prioridadConfirmada() != null ? p.prioridadConfirmada() : p.prioridadSugerida();
        return switch (prioridad) {
            case ALTA -> 0;
            case MEDIA -> 1;
            case BAJA -> 2;
        };
    }

    private void validarCapacidad(Ruta ruta, Conductor conductor, PedidoResponseDTO nuevo) {
        List<PedidoResponseDTO> actuales = ruta.getParadas().stream()
                .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                .map(p -> pedidos.obtener(p.getPedidoId())).toList();
        int entregas = actuales.size() + 1;
        double peso = actuales.stream().mapToDouble(PedidoResponseDTO::pesoKg).sum() + nuevo.pesoKg();
        double volumen = actuales.stream().mapToDouble(this::volumen).sum() + volumen(nuevo);
        if (conductor.getMaxEntregasDia() == null || conductor.getMaxEntregasDia() < 2)
            throw new IllegalStateException("La capacidad del conductor debe admitir mínimo 2 entregas");
        if (entregas > conductor.getMaxEntregasDia())
            throw new IllegalArgumentException("El conductor supera el máximo de entregas de la jornada");
        if (peso > conductor.getCapacidadMaxKg())
            throw new IllegalArgumentException("La asignación supera la capacidad máxima de peso del conductor");
        if (volumen > conductor.getCapacidadMaxVolumenCm3())
            throw new IllegalArgumentException("La asignación supera la capacidad máxima de volumen del conductor");
    }

    private double volumen(PedidoResponseDTO p) { return p.largoCm() * p.anchoCm() * p.altoCm(); }

    private Conductor conductorPorUsuario(Long usuarioId) { return conductorPorUsuario(usuarioId, false); }

    private Conductor conductorPorUsuario(Long usuarioId, boolean bloquear) {
        return (bloquear ? conductores.findByUsuarioIdForUpdate(usuarioId) : conductores.findByUsuarioId(usuarioId))
                .filter(c -> c.getUsuario() != null && Boolean.TRUE.equals(c.getUsuario().getActivo())
                        && c.getUsuario().getRol() == Rol.CONDUCTOR
                        && "ACTIVO".equalsIgnoreCase(c.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("No existe un conductor activo para el usuario indicado"));
    }

    private RutaResponseDTO map(Ruta ruta) {
        List<ParadaResponseDTO> items = ruta.getParadas().stream()
                .filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                .sorted(Comparator.comparing(ParadaRuta::getOrden))
                .map(p -> {
                    PedidoResponseDTO pedido = pedidos.obtener(p.getPedidoId());
                    return new ParadaResponseDTO(p.getId(), p.getPedidoId(), p.getOrden(), p.getEstado().name(),
                        p.getFechaAsignacion(), pedido.numeroPedido(), pedido.numeroTracking(), pedido.direccionDestino(),
                        pedido.ciudadDestino(), pedido.destinatarioNombre(), pedido.destinatarioTelefono(),
                        (pedido.prioridadConfirmada() != null ? pedido.prioridadConfirmada() : pedido.prioridadSugerida()).name(),
                        pedido.pesoKg());
                }).toList();
        Long usuarioId = conductores.findById(ruta.getConductorId()).map(c -> c.getUsuario().getId()).orElse(ruta.getConductorId());
        return new RutaResponseDTO(ruta.getId(), usuarioId, ruta.getFecha(), items);
    }
}
