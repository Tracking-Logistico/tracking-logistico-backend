package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.EtiquetaEnvioResponseDTO;
import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.application.dto.RecibirPedidoRequestDTO;
import com.udea.demo.pedidos.application.dto.ValidarPedidoRequestDTO;
import com.udea.demo.pedidos.domain.exception.PedidoNoEncontradoException;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.Pedido;
import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.service.GeneradorEtiqueta;
import com.udea.demo.pedidos.domain.service.PrioridadStrategy;
import com.udea.demo.pedidos.interfaces.persistence.PedidoRepository;
import com.udea.demo.pedidos.interfaces.services.PedidoServiceI;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Facade/Controller (GRASP) hacia el módulo pedidos: orquesta repositorio + estrategia de prioridad
@Service
public class PedidoService implements PedidoServiceI {

    private final PedidoRepository pedidoRepository;
    private final PrioridadStrategy prioridadStrategy;
    private final GeneradorNumeroPedido generadorNumeroPedido;
    private final GeneradorNumeroTracking generadorNumeroTracking;
    private final GeneradorEtiqueta generadorEtiqueta;

    public PedidoService(PedidoRepository pedidoRepository,
                         PrioridadStrategy prioridadStrategy,
                         GeneradorNumeroPedido generadorNumeroPedido,
                         GeneradorNumeroTracking generadorNumeroTracking,
                         GeneradorEtiqueta generadorEtiqueta) {
        this.pedidoRepository = pedidoRepository;
        this.prioridadStrategy = prioridadStrategy;
        this.generadorNumeroPedido = generadorNumeroPedido;
        this.generadorNumeroTracking = generadorNumeroTracking;
        this.generadorEtiqueta = generadorEtiqueta;
    }

    @Override
    @Transactional
    public PedidoResponseDTO recibir(RecibirPedidoRequestDTO dto) {
        Prioridad prioridadSugerida = prioridadStrategy.sugerir(dto.tipoServicio(), dto.pesoKg());
        String numeroPedido = generadorNumeroPedido.generar();

        Pedido pedido = Pedido.recibir(
                dto.clienteId(),
                dto.direccionOrigen(),
                dto.direccionDestino(),
                dto.descripcionPaquete(),
                dto.pesoKg(),
                dto.largoCm(),
                dto.anchoCm(),
                dto.altoCm(),
                dto.tipoServicio(),
                numeroPedido,
                prioridadSugerida
        );

        return mapToDTO(pedidoRepository.save(pedido));
    }

    @Override
    public List<PedidoResponseDTO> listarPendientes() {
        List<EstadoPedido> pendientes = List.of(EstadoPedido.RECIBIDO, EstadoPedido.EN_VALIDACION);
        return pedidoRepository.findByEstadoInOrderByFechaCreacionAsc(pendientes)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public List<PedidoResponseDTO> listarEnTransito() {
        return pedidoRepository.findByEstadoInOrderByFechaCreacionAsc(List.of(EstadoPedido.EN_TRANSITO))
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public PedidoResponseDTO obtener(Long id) {
        return mapToDTO(buscarOLanzar(id));
    }

    @Override
    @Transactional
    public PedidoResponseDTO validar(Long id, ValidarPedidoRequestDTO dto) {
        Pedido pedido = buscarOLanzar(id);

        pedido.validar(dto.aprobar(), dto.prioridadConfirmada(), dto.observaciones(), dto.operadorId());

        return mapToDTO(pedidoRepository.save(pedido));
    }

    private Pedido buscarOLanzar(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new PedidoNoEncontradoException(id));
    }

    @Override
    @Transactional
    public PedidoResponseDTO activarTracking(Long id) {
        Pedido pedido = buscarOLanzar(id);

        pedido.activarTracking(generadorNumeroTracking.generar());

        return mapToDTO(pedidoRepository.save(pedido));
    }

    @Override
    @Transactional
    public EtiquetaEnvioResponseDTO generarEtiqueta(Long id) {
        Pedido pedido = buscarOLanzar(id);

        pedido.confirmarImpresionEtiqueta();
        pedidoRepository.save(pedido);

        String contenido = generadorEtiqueta.generar(pedido);

        return new EtiquetaEnvioResponseDTO(
                pedido.getNumeroPedido(),
                pedido.getNumeroTracking(),
                contenido,
                pedido.getFechaImpresionEtiqueta()
        );
    }

    private PedidoResponseDTO mapToDTO(Pedido p) {
        return new PedidoResponseDTO(
                p.getId(),
                p.getNumeroPedido(),
                p.getClienteId(),
                p.getDireccionOrigen(),
                p.getDireccionDestino(),
                p.getDescripcionPaquete(),
                p.getPesoKg(),
                p.getLargoCm(),
                p.getAnchoCm(),
                p.getAltoCm(),
                p.getTipoServicio(),
                p.getPrioridadSugerida(),
                p.getPrioridadConfirmada(),
                p.getEstado(),
                p.getObservacionesValidacion(),
                p.getOperadorValidadorId(),
                p.getFechaCreacion(),
                p.getFechaValidacion(),
                p.getNumeroTracking(),
                p.getFechaActivacionTracking(),
                p.getEtiquetaImpresa(),
                p.getFechaImpresionEtiqueta()
        );
    }
}
