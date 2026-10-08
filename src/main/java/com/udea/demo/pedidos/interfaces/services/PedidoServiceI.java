package com.udea.demo.pedidos.interfaces.services;

import com.udea.demo.pedidos.application.dto.*;
import java.util.List;
import java.time.LocalDateTime;
import com.udea.demo.pedidos.domain.model.EstadoPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PedidoServiceI {
    PedidoResponseDTO recibir(RecibirPedidoRequestDTO dto);
    PedidoResponseDTO corregir(Long id, RecibirPedidoRequestDTO dto);
    Page<PedidoClienteResponseDTO> listarMios(EstadoPedido estado, LocalDateTime fechaDesde,
                                               LocalDateTime fechaHasta, Pageable pageable);
    Page<PedidoResponseDTO> listarPendientes(Pageable pageable);
    Page<PedidoResponseDTO> listarValidados(Pageable pageable);
    Page<PedidoResponseDTO> listarActivables(Pageable pageable);
    List<PedidoResponseDTO> listarDespachos();
    Page<PedidoResponseDTO> listarEnTransito(Pageable pageable);
    PedidoResponseDTO obtener(Long id);
    PedidoResponseDTO obtenerPorTracking(String numeroTracking);
    SeguimientoClienteResponseDTO obtenerSeguimientoCliente(String numeroTracking);
    PedidoResponseDTO validar(Long id, ValidarPedidoRequestDTO dto);
    PedidoResponseDTO activarTracking(Long id);
    PedidoResponseDTO cambiarEstadoLogistico(Long id, CambiarEstadoLogisticoRequestDTO dto);
    EtiquetaEnvioResponseDTO generarEtiqueta(Long id);
    List<HistorialPedidoResponseDTO> historial(Long id);
    List<HistorialPedidoResponseDTO> historialParaConductor(Long id);
    java.util.Map<Long, PedidoResponseDTO> obtenerPorIds(java.util.Collection<Long> ids);
    PedidoResponseDTO obtenerParaConductor(Long pedidoId);
}
