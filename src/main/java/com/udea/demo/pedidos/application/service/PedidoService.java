package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.*;
import com.udea.demo.pedidos.domain.exception.PedidoNoEncontradoException;
import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.domain.service.GeneradorEtiqueta;
import com.udea.demo.pedidos.domain.service.PrioridadStrategy;
import com.udea.demo.pedidos.interfaces.persistence.HistorialPedidoRepository;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.pedidos.interfaces.services.PedidoServiceI;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import com.udea.demo.usuarios.domain.model.Rol;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PedidoService implements PedidoServiceI {
    private final PedidoRepository pedidos;
    private final HistorialPedidoRepository historial;
    private final PrioridadStrategy prioridadStrategy;
    private final GeneradorNumeroPedido numerosPedido;
    private final GeneradorNumeroTracking numerosTracking;
    private final GeneradorEtiqueta etiquetas;
    private final ActorAuthorizationService actores;

    private final LimitesServicioService limites;
    private final com.udea.demo.pedidos.interfaces.services.AccesoPedidoConductorI accesoConductor;

    public PedidoService(PedidoRepository pedidos, PrioridadStrategy prioridadStrategy,
                         GeneradorNumeroPedido numerosPedido, GeneradorNumeroTracking numerosTracking,
                         GeneradorEtiqueta etiquetas, ActorAuthorizationService actores,
                         HistorialPedidoRepository historial, LimitesServicioService limites,
                         com.udea.demo.pedidos.interfaces.services.AccesoPedidoConductorI accesoConductor) {
        this.pedidos = pedidos;
        this.prioridadStrategy = prioridadStrategy;
        this.numerosPedido = numerosPedido;
        this.numerosTracking = numerosTracking;
        this.etiquetas = etiquetas;
        this.actores = actores;
        this.historial = historial;
        this.limites = limites;
        this.accesoConductor = accesoConductor;
    }

    @Override @Transactional
    public PedidoResponseDTO recibir(RecibirPedidoRequestDTO dto) {
        limites.validar(dto);
        Long clienteId = actores.clienteActualId();
        var remitente = actores.actorActual();
        Prioridad sugerida = prioridadStrategy.sugerir(dto.tipoServicio(), dto.pesoKg());
        Pedido pedido = Pedido.recibir(clienteId, dto.direccionOrigen(), dto.ciudadOrigen(), dto.codigoPostalOrigen(),
                dto.direccionDestino(), dto.ciudadDestino(), dto.codigoPostalDestino(),
                dto.descripcionPaquete(), dto.pesoKg(), dto.largoCm(), dto.anchoCm(), dto.altoCm(),
                dto.tipoServicio(), generarNumeroPedidoUnico(), sugerida, dto.destinatarioNombre(), dto.destinatarioTelefono(),
                remitente.getNombre(), remitente.getEmail(), dto.remitenteTelefono());
        Pedido guardado = pedidos.save(pedido);
        evento(guardado.getId(), actores.actorActual().getId(), "PEDIDO_SOLICITADO", null,
                "Pedido creado por el cliente");
        return map(guardado);
    }

    @Transactional
    public PedidoResponseDTO corregir(Long id, RecibirPedidoRequestDTO dto) {
        limites.validar(dto);
        Pedido p = pedidos.findByIdForUpdate(id).orElseThrow(() -> new PedidoNoEncontradoException(id));
        if (!p.getClienteId().equals(actores.clienteActualId())) throw new AccessDeniedException("El pedido no pertenece al cliente autenticado");
        Prioridad sugerida = prioridadStrategy.sugerir(dto.tipoServicio(), dto.pesoKg());
        p.corregir(dto.direccionOrigen(), dto.ciudadOrigen(), dto.codigoPostalOrigen(), dto.direccionDestino(),
                dto.ciudadDestino(), dto.codigoPostalDestino(), dto.descripcionPaquete(), dto.pesoKg(), dto.largoCm(),
                dto.anchoCm(), dto.altoCm(), dto.tipoServicio(), dto.destinatarioNombre(), dto.destinatarioTelefono(),
                dto.remitenteTelefono(), sugerida);
        evento(id, actores.actorActual().getId(), "PEDIDO_CORREGIDO", null, "El cliente actualizó la información observada");
        return map(pedidos.save(p));
    }

    @Override @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarMios() {
        return pedidos.findByClienteIdOrderByFechaCreacionDesc(actores.clienteActualId()).stream().map(this::map).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarPendientes() {
        return pedidos.findByEstadoInOrderByFechaCreacionAsc(List.of(EstadoPedido.SOLICITADO, EstadoPedido.CORRECCION_SOLICITADA))
                .stream().filter(p -> p.getEstado() == EstadoPedido.CORRECCION_SOLICITADA || p.getFechaValidacion() == null)
                .map(this::map).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarValidados() {
        return pedidos.findByEstadoInOrderByFechaCreacionAsc(List.of(EstadoPedido.SOLICITADO)).stream()
                .filter(p -> p.getFechaValidacion() != null).map(this::map).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarActivables() {
        return pedidos.findByEstadoInOrderByFechaCreacionAsc(List.of(EstadoPedido.SOLICITADO, EstadoPedido.CREADO))
                .stream().filter(p -> p.getEstado() == EstadoPedido.CREADO || p.getFechaValidacion() != null)
                .map(this::map).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarDespachos() {
        return pedidos.findByEstadoInOrderByFechaCreacionAsc(List.of(
                EstadoPedido.SOLICITADO, EstadoPedido.CREADO,
                EstadoPedido.RECIBIDO_EN_ORIGEN, EstadoPedido.EN_TRANSITO)).stream()
                .filter(p -> p.getEstado() != EstadoPedido.SOLICITADO || p.getFechaValidacion() != null)
                .map(this::map).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarEnTransito() {
        return pedidos.findByEstadoInOrderByFechaCreacionAsc(List.of(EstadoPedido.RECIBIDO_EN_ORIGEN, EstadoPedido.EN_TRANSITO))
                .stream().map(this::map).toList();
    }

    @Override @Transactional(readOnly = true)
    public PedidoResponseDTO obtener(Long id) {
        Pedido p = buscar(id); autorizarLectura(p); return map(p);
    }

    @Override @Transactional(readOnly = true)
    public PedidoResponseDTO obtenerPorTracking(String numeroTracking) {
        Pedido p = pedidos.findByNumeroTracking(numeroTracking).orElseThrow(() -> new PedidoNoEncontradoException(numeroTracking));
        autorizarLectura(p); return map(p);
    }

    @Override @Transactional
    public PedidoResponseDTO validar(Long id, ValidarPedidoRequestDTO dto) {
        Long operadorId = actores.operadorActualId();
        Pedido p = pedidos.findByIdForUpdate(id).orElseThrow(() -> new PedidoNoEncontradoException(id));
        if (Boolean.TRUE.equals(dto.solicitarCorreccion())) {
            if (dto.campoObservado() == null || dto.campoObservado().isBlank())
                throw new IllegalArgumentException("Selecciona el campo que debe corregirse");
            p.solicitarCorreccion(dto.observaciones(), operadorId);
            evento(id, actores.actorActual().getId(), "CORRECCION_SOLICITADA", dto.campoObservado(), dto.observaciones());
        } else {
            if (dto.aprobar() == null) throw new IllegalArgumentException("Debes aprobar, rechazar o solicitar una corrección");
            boolean aprobar = dto.aprobar();
            if (!aprobar && (dto.observaciones() == null || dto.observaciones().isBlank()))
                throw new IllegalArgumentException("Indica el motivo del rechazo");
            p.validar(aprobar, dto.prioridadConfirmada(), dto.observaciones(), dto.justificacionPrioridad(), operadorId);
            evento(id, actores.actorActual().getId(), aprobar ? "PEDIDO_VALIDADO" : "PEDIDO_RECHAZADO", null,
                    dto.justificacionPrioridad() != null ? dto.justificacionPrioridad() : dto.observaciones());
        }
        return map(pedidos.save(p));
    }

    @Override @Transactional
    public PedidoResponseDTO activarTracking(Long id) {
        actores.operadorActualId();
        Pedido p = pedidos.findByIdForUpdate(id).orElseThrow(() -> new PedidoNoEncontradoException(id));
        if (p.getNumeroTracking() != null) return map(p);
        p.activarTracking(generarTrackingUnico());
        Pedido guardado = pedidos.save(p);
        evento(id, actores.actorActual().getId(), "TRACKING_ACTIVADO", null, guardado.getNumeroTracking());
        return map(guardado);
    }

    @Override @Transactional
    public PedidoResponseDTO cambiarEstadoLogistico(Long id, CambiarEstadoLogisticoRequestDTO dto) {
        actores.operadorActualId();
        Pedido p = buscar(id);
        p.cambiarEstadoLogistico(dto.estado());
        evento(id, actores.actorActual().getId(), "ESTADO_LOGISTICO", null, dto.estado().name());
        return map(pedidos.save(p));
    }

    @Override @Transactional
    public EtiquetaEnvioResponseDTO generarEtiqueta(Long id) {
        actores.operadorActualId();
        Pedido p = pedidos.findByIdForUpdate(id).orElseThrow(() -> new PedidoNoEncontradoException(id));
        if (p.getNumeroTracking() == null) throw new com.udea.demo.pedidos.domain.exception.TrackingNoActivoException(id);

        String contenido = etiquetas.generar(p);
        p.confirmarImpresionEtiqueta();
        pedidos.save(p);
        evento(id, actores.actorActual().getId(), "ETIQUETA_GENERADA", null, null);
        return new EtiquetaEnvioResponseDTO(p.getNumeroPedido(), p.getNumeroTracking(), contenido, p.getFechaImpresionEtiqueta());
    }

    @Override @Transactional(readOnly = true)
    public List<HistorialPedidoResponseDTO> historial(Long id) {
        Pedido p = buscar(id); autorizarLectura(p);
        return historial.findByPedidoIdOrderByFechaAsc(id).stream()
                .map(h -> new HistorialPedidoResponseDTO(h.getId(), h.getUsuarioId(), h.getTipoEvento(),
                        h.getCampoObservado(), h.getDetalle(), h.getFecha())).toList();
    }

    private String generarNumeroPedidoUnico() {
        for (int i = 0; i < 10; i++) {
            String numero = numerosPedido.generar();
            if (!pedidos.existsByNumeroPedido(numero)) return numero;
        }
        throw new IllegalStateException("No fue posible generar un identificador único de pedido");
    }

    private String generarTrackingUnico() {
        for (int i = 0; i < 10; i++) {
            String tracking = numerosTracking.generar();
            if (!pedidos.existsByNumeroTracking(tracking)) return tracking;
        }
        throw new IllegalStateException("No fue posible generar un código de tracking único");
    }

    private void autorizarLectura(Pedido p) {
        var actor = actores.actorActual();
        if (actor.getRol() == Rol.CLIENTE && !p.getClienteId().equals(actores.clienteActualId()))
            throw new AccessDeniedException("El pedido no pertenece al cliente autenticado");
        if (actor.getRol() == Rol.CONDUCTOR && !accesoConductor.tieneAsignacionActiva(p.getId(), actor.getId()))
            throw new AccessDeniedException("El envío no está asignado al conductor autenticado");
    }

    private void evento(Long pedidoId, Long usuarioId, String tipo, String campo, String detalle) {
        historial.save(HistorialPedido.builder().pedidoId(pedidoId).usuarioId(usuarioId).tipoEvento(tipo)
                .campoObservado(campo).detalle(detalle).fecha(LocalDateTime.now()).build());
    }

    private Pedido buscar(Long id) { return pedidos.findById(id).orElseThrow(() -> new PedidoNoEncontradoException(id)); }

    private PedidoResponseDTO map(Pedido p) {
        return new PedidoResponseDTO(p.getId(), p.getNumeroPedido(), p.getClienteId(), p.getDireccionOrigen(),
                p.getDireccionDestino(), p.getDescripcionPaquete(), p.getPesoKg(), p.getLargoCm(), p.getAnchoCm(),
                p.getAltoCm(), p.getTipoServicio(), p.getPrioridadSugerida(), p.getPrioridadConfirmada(), p.getEstado(),
                p.getObservacionesValidacion(), p.getOperadorValidadorId(), p.getFechaCreacion(), p.getFechaValidacion(),
                p.getNumeroTracking(), p.getFechaActivacionTracking(), p.getEtiquetaImpresa(), p.getFechaImpresionEtiqueta(),
                p.getDestinatarioNombre(), p.getDestinatarioTelefono(), p.getJustificacionPrioridad(),
                p.getCiudadOrigen(), p.getCiudadDestino(), p.getCodigoPostalOrigen(), p.getCodigoPostalDestino(),
                p.getRemitenteNombre(), p.getRemitenteEmail(), p.getRemitenteTelefono());
    }
}
