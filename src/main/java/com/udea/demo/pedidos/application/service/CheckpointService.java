package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.CheckpointResponseDTO;
import com.udea.demo.pedidos.application.dto.CheckpointsPendientesRevisionDTO;
import com.udea.demo.pedidos.application.dto.RegistrarCheckpointRequestDTO;
import com.udea.demo.pedidos.application.dto.RegistroCheckpointResultadoDTO;
import com.udea.demo.pedidos.domain.event.CheckpointPendienteRevisionEvent;
import com.udea.demo.pedidos.domain.event.CheckpointRegistradoEvent;
import com.udea.demo.pedidos.domain.exception.*;
import com.udea.demo.pedidos.domain.model.*;
import com.udea.demo.pedidos.domain.service.ValidadorUbicacion;
import com.udea.demo.pedidos.interfaces.persistence.CheckpointPedidoRepository;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.pedidos.interfaces.services.AccesoPedidoConductorI;
import com.udea.demo.pedidos.interfaces.services.CheckpointServiceI;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class CheckpointService implements CheckpointServiceI {
    private final PedidoRepository pedidos;
    private final CheckpointPedidoRepository checkpoints;
    private final RegistroHistorialPedido historial;
    private final ActorAuthorizationService actores;
    private final AccesoPedidoConductorI accesoConductor;
    private final ValidadorUbicacion validadorUbicacion;
    private final ApplicationEventPublisher eventos;
    private final long toleranciaFuturoMinutos;

    public CheckpointService(PedidoRepository pedidos, CheckpointPedidoRepository checkpoints,
                             RegistroHistorialPedido historial, ActorAuthorizationService actores,
                             AccesoPedidoConductorI accesoConductor, ValidadorUbicacion validadorUbicacion,
                             ApplicationEventPublisher eventos,
                             @Value("${app.checkpoints.tolerancia-futuro-minutos:5}") long toleranciaFuturoMinutos) {
        this.pedidos = pedidos;
        this.checkpoints = checkpoints;
        this.historial = historial;
        this.actores = actores;
        this.accesoConductor = accesoConductor;
        this.validadorUbicacion = validadorUbicacion;
        this.eventos = eventos;
        this.toleranciaFuturoMinutos = toleranciaFuturoMinutos;
    }

    @Override @Transactional
    public RegistroCheckpointResultadoDTO registrar(Long pedidoId, RegistrarCheckpointRequestDTO dto) {
        return registrar(pedidoId, dto, OrigenCheckpoint.EN_LINEA);
    }

    @Override @Transactional
    public RegistroCheckpointResultadoDTO registrarSincronizado(Long pedidoId, RegistrarCheckpointRequestDTO dto) {
        return registrar(pedidoId, dto, OrigenCheckpoint.OFFLINE);
    }

    @Override @Transactional(readOnly = true)
    public CheckpointsPendientesRevisionDTO misPendientesRevision() {
        Long conductor = actores.conductorActualUsuarioId();
        var pendientes = checkpoints.findByUsuarioIdAndEstadoRegistroOrderByFechaEventoAsc(
                conductor, EstadoRegistroCheckpoint.PENDIENTE_REVISION).stream().map(CheckpointResponseDTO::de).toList();
        return new CheckpointsPendientesRevisionDTO(pendientes.size(), pendientes);
    }

    @Override @Transactional(readOnly = true)
    public Page<CheckpointResponseDTO> pendientesRevision(Pageable pageable) {
        actores.operadorActualId();
        return checkpoints.findByEstadoRegistroOrderByFechaRegistroDesc(EstadoRegistroCheckpoint.PENDIENTE_REVISION,
                pageable).map(CheckpointResponseDTO::de);
    }

    private RegistroCheckpointResultadoDTO registrar(Long pedidoId, RegistrarCheckpointRequestDTO dto,
                                                     OrigenCheckpoint origen) {
        Long conductor = actores.conductorActualUsuarioId();
        Optional<CheckpointPedido> previo = checkpoints.findByIdEventoCliente(dto.idEventoCliente());
        if (previo.isPresent()) return duplicado(previo.get(), pedidoId, conductor);

        Pedido escaneado = validarEnvioEscaneado(pedidoId, dto.codigoQr());
        exigirAsignacion(escaneado, conductor, origen);
        Pedido pedido = pedidos.findByIdForUpdate(pedidoId).orElseThrow(() -> new PedidoNoEncontradoException(pedidoId));
        UbicacionReportada ubicacion = validadorUbicacion.evaluar(dto.latitud(), dto.longitud(), dto.precisionMetros());
        LocalDateTime fechaEvento = fechaEvento(dto);

        Optional<String> conflicto = conflicto(pedido, dto.etapa(), origen);
        if (conflicto.isPresent()) {
            return retenerParaRevision(pedido, conductor, dto, ubicacion, conflicto.get(), fechaEvento);
        }
        return aplicar(pedido, conductor, dto, ubicacion, origen, fechaEvento);
    }

    private RegistroCheckpointResultadoDTO duplicado(CheckpointPedido previo, Long pedidoId, Long conductor) {
        if (!previo.getPedidoId().equals(pedidoId) || !previo.getUsuarioId().equals(conductor))
            throw new IllegalArgumentException("El identificador del evento ya fue utilizado por otro registro");
        return new RegistroCheckpointResultadoDTO(CheckpointResponseDTO.de(previo), true);
    }

    /** El QR debe ser íntegro, pertenecer a un envío existente y coincidir con el envío esperado. */
    private Pedido validarEnvioEscaneado(Long pedidoIdEsperado, String codigoQr) {
        CodigoQrEnvio qr = CodigoQrEnvio.parsear(codigoQr);
        Pedido escaneado = pedidos.findByNumeroTracking(qr.numeroTracking())
                .orElseThrow(CodigoQrInvalidoException::new);
        if (!escaneado.getId().equals(pedidoIdEsperado)) throw new CodigoQrOtroEnvioException(pedidoIdEsperado);
        return escaneado;
    }

    /**
     * En línea se exige la asignación vigente; offline basta con que el envío haya estado asignado al conductor.
     * Si el envío ya finalizó (su parada se cerró), el conductor que lo tuvo asignado recibe ese motivo.
     */
    private void exigirAsignacion(Pedido pedido, Long conductor, OrigenCheckpoint origen) {
        if (origen == OrigenCheckpoint.EN_LINEA && accesoConductor.tieneAsignacionActiva(pedido.getId(), conductor)) return;
        if (!accesoConductor.fueAsignado(pedido.getId(), conductor))
            throw new EnvioNoAsignadoAlConductorException(pedido.getId());
        if (origen == OrigenCheckpoint.EN_LINEA) {
            pedido.exigirNoFinalizado();
            throw new EnvioNoAsignadoAlConductorException(pedido.getId());
        }
    }

    private LocalDateTime fechaEvento(RegistrarCheckpointRequestDTO dto) {
        LocalDateTime fecha = dto.fechaDispositivo().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        if (fecha.isAfter(LocalDateTime.now().plusMinutes(toleranciaFuturoMinutos)))
            throw new FechaDispositivoInvalidaException();
        return fecha;
    }

    /**
     * En línea los conflictos se rechazan; los eventos offline en conflicto se conservan como
     * pendientes de revisión en lugar de aplicarse silenciosamente o perderse.
     */
    private Optional<String> conflicto(Pedido pedido, EtapaCheckpoint etapa, OrigenCheckpoint origen) {
        EstadoPedido actual = pedido.getEstado();
        if (actual.esFinal()) {
            if (origen == OrigenCheckpoint.EN_LINEA) throw new EnvioFinalizadoException(actual);
            return Optional.of("El envío ya se encuentra finalizado en estado " + actual);
        }
        if (etapa.estado() != actual && !pedido.puedeCambiarEstadoLogisticoA(etapa.estado())) {
            if (origen == OrigenCheckpoint.EN_LINEA) throw new TransicionEstadoInvalidaException(actual, etapa.estado());
            return Optional.of("La etapa " + etapa + " no es válida desde el estado actual " + actual);
        }
        return Optional.empty();
    }

    private RegistroCheckpointResultadoDTO aplicar(Pedido pedido, Long conductor, RegistrarCheckpointRequestDTO dto,
                                                   UbicacionReportada ubicacion, OrigenCheckpoint origen,
                                                   LocalDateTime fechaEvento) {
        EstadoPedido anterior = pedido.getEstado();
        if (dto.etapa().estado() != anterior) {
            pedido.cambiarEstadoLogistico(dto.etapa().estado());
            pedidos.save(pedido);
        }
        CheckpointPedido checkpoint = checkpoints.save(CheckpointPedido.aplicado(pedido.getId(), conductor,
                dto.idEventoCliente(), dto.etapa(), anterior, pedido.getEstado(), ubicacion, origen, fechaEvento));
        if (checkpoint.cambioEstado()) {
            historial.registrar(pedido.getId(), conductor, RegistroHistorialPedido.ESTADO_LOGISTICO,
                    RegistroHistorialPedido.CHECKPOINT, pedido.getEstado().name(), fechaEvento);
        } else {
            historial.registrar(pedido.getId(), conductor, RegistroHistorialPedido.CHECKPOINT, null,
                    "Punto de control registrado en etapa " + dto.etapa(), fechaEvento);
        }
        eventos.publishEvent(CheckpointRegistradoEvent.de(pedido, checkpoint));
        return new RegistroCheckpointResultadoDTO(CheckpointResponseDTO.de(checkpoint), false);
    }

    private RegistroCheckpointResultadoDTO retenerParaRevision(Pedido pedido, Long conductor,
                                                               RegistrarCheckpointRequestDTO dto,
                                                               UbicacionReportada ubicacion, String motivo,
                                                               LocalDateTime fechaEvento) {
        CheckpointPedido checkpoint = checkpoints.save(CheckpointPedido.pendienteRevision(pedido.getId(), conductor,
                dto.idEventoCliente(), dto.etapa(), pedido.getEstado(), ubicacion, motivo, fechaEvento));
        historial.registrar(pedido.getId(), conductor, RegistroHistorialPedido.CHECKPOINT_EN_REVISION, null,
                motivo, fechaEvento);
        eventos.publishEvent(CheckpointPendienteRevisionEvent.de(pedido, checkpoint));
        return new RegistroCheckpointResultadoDTO(CheckpointResponseDTO.de(checkpoint), false);
    }
}
