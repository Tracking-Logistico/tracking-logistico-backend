package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.*;
import com.udea.demo.pedidos.domain.exception.PedidoNoEncontradoException;
import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.interfaces.persistence.*;
import com.udea.demo.rutas.interfaces.persistence.RutaRepository;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import com.udea.demo.pedidos.interfaces.services.AccesoPedidoConductorI;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class ResultadoEntregaService implements ResultadoEntregaServiceI {
    private final PedidoRepository pedidos;
    private final EventoEntregaRepository eventos;
    private final CatalogoNovedadEntregaRepository catalogo;
    private final HistorialPedidoRepository historial;
    private final ActorAuthorizationService actores;
    private final AccesoPedidoConductorI acceso;
    private final RutaRepository rutas;
    private final com.udea.demo.usuarios.interfaces.persistence.ConductorRepository conductores;

    public ResultadoEntregaService(PedidoRepository pedidos, EventoEntregaRepository eventos,
                                   CatalogoNovedadEntregaRepository catalogo,
                                   HistorialPedidoRepository historial, ActorAuthorizationService actores,
                                   AccesoPedidoConductorI acceso, RutaRepository rutas,
                                   com.udea.demo.usuarios.interfaces.persistence.ConductorRepository conductores) {
        this.pedidos = pedidos; this.eventos = eventos; this.catalogo = catalogo;
        this.historial = historial; this.actores = actores; this.acceso = acceso; this.rutas = rutas;
        this.conductores = conductores;
    }

    @Override
    @Transactional
    public ResultadoEntregaResponseDTO registrar(Long pedidoId, ResultadoEntregaRequestDTO dto) {
        validarFechaEvento(dto);
        Long usuarioId = actores.conductorActualUsuarioId();
        var existente = eventos.findByIdEventoCliente(dto.idEventoCliente());
        if (existente.isPresent()) {
            if (!existente.get().getPedidoId().equals(pedidoId)
                    || !existente.get().getUsuarioId().equals(usuarioId))
                throw new IllegalArgumentException("El identificador del evento ya fue utilizado");
            return new ResultadoEntregaResponseDTO(dto.idEventoCliente(), pedidoId, existente.get().getResultado(),
                    "DUPLICADO", dto.fechaEvento().toLocalDateTime(), true);
        }

        if (!acceso.tieneAsignacionActiva(pedidoId, usuarioId))
            throw new AccessDeniedException("El envío no está asignado al conductor autenticado");
        Pedido pedido = pedidos.findByIdForUpdate(pedidoId)
                .orElseThrow(() -> new PedidoNoEncontradoException(pedidoId));
        if (pedido.getEstado() != EstadoPedido.EN_REPARTO)
            throw new IllegalStateException("Solo se pueden registrar resultados sobre envíos en reparto");
        validarNovedad(dto);
        LocalDateTime fecha = dto.fechaEvento().toLocalDateTime();
        historial.save(HistorialPedido.builder().pedidoId(pedidoId).usuarioId(usuarioId)
                .tipoEvento(RegistroHistorialPedido.ESTADO_LOGISTICO).campoObservado("resultado_entrega")
                .detalle(dto.resultado().name() + (dto.motivo() == null ? "" : ": " + dto.motivo()))
                .fecha(fecha).build());
        pedido.cambiarEstadoLogistico(estadoFinal(dto.resultado()));
        pedidos.save(pedido);
        eventos.saveAndFlush(new EventoEntrega(dto.idEventoCliente(), pedidoId, usuarioId, dto.resultado(),
                dto.codigoNovedad(), dto.motivo(), dto.latitud(), dto.longitud(), fecha));
        conductores.findByUsuarioId(usuarioId).ifPresent(conductor ->
                rutas.findByConductorIdAndFecha(conductor.getId(), LocalDate.now(ZoneId.of("America/Bogota")))
                        .ifPresent(ruta -> {
                            ruta.marcarParadaEntregada(pedidoId);
                            rutas.save(ruta);
                        }));
        return new ResultadoEntregaResponseDTO(dto.idEventoCliente(), pedidoId, dto.resultado(),
                "APLICADO", fecha, false);
    }

    private void validarFechaEvento(ResultadoEntregaRequestDTO dto) {
        if (!dto.isFechaEventoValida()) {
            throw new IllegalArgumentException(
                    "La fecha del evento debe estar entre las últimas 24 horas y 5 minutos en el futuro");
        }
    }

    private void validarNovedad(ResultadoEntregaRequestDTO dto) {
        if (dto.resultado() == ResultadoEntrega.ENTREGADO) return;
        if (dto.codigoNovedad() == null || dto.codigoNovedad().isBlank()
                || dto.motivo() == null || dto.motivo().isBlank())
            throw new IllegalArgumentException("El motivo y el código de novedad son obligatorios");
        CatalogoNovedadEntrega novedad = catalogo.findByCodigoAndActivoTrue(dto.codigoNovedad().trim())
                .orElseThrow(() -> new IllegalArgumentException("El motivo de novedad no existe en el catálogo"));
        if (novedad.getResultado() != dto.resultado())
            throw new IllegalArgumentException("El motivo no corresponde al resultado seleccionado");
    }

    private EstadoPedido estadoFinal(ResultadoEntrega resultado) {
        return switch (resultado) {
            case ENTREGADO -> EstadoPedido.ENTREGADO;
            case ENTREGA_FALLIDA -> EstadoPedido.ENTREGA_FALLIDA_CERRADA;
            case DEVOLUCION_AL_REMITENTE -> EstadoPedido.DEVOLUCION_AL_REMITENTE;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<NovedadEntregaResponseDTO> catalogo() {
        actores.conductorActualUsuarioId();
        return catalogo.findByActivoTrueOrderByResultadoAscCodigoAsc().stream()
                .map(n -> new NovedadEntregaResponseDTO(n.getCodigo(), n.getResultado(), n.getDescripcion()))
                .toList();
    }
}
