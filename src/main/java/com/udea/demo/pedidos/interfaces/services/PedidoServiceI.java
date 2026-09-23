package com.udea.demo.pedidos.interfaces.services;

import com.udea.demo.pedidos.application.dto.*;
import java.util.List;

public interface PedidoServiceI {
    PedidoResponseDTO recibir(RecibirPedidoRequestDTO dto);
    PedidoResponseDTO corregir(Long id, RecibirPedidoRequestDTO dto);
    List<PedidoResponseDTO> listarMios();
    List<PedidoResponseDTO> listarPendientes();
    List<PedidoResponseDTO> listarValidados();
    List<PedidoResponseDTO> listarActivables();
    List<PedidoResponseDTO> listarDespachos();
    List<PedidoResponseDTO> listarEnTransito();
    PedidoResponseDTO obtener(Long id);
    PedidoResponseDTO obtenerPorTracking(String numeroTracking);
    PedidoResponseDTO validar(Long id, ValidarPedidoRequestDTO dto);
    PedidoResponseDTO activarTracking(Long id);
    PedidoResponseDTO cambiarEstadoLogistico(Long id, CambiarEstadoLogisticoRequestDTO dto);
    EtiquetaEnvioResponseDTO generarEtiqueta(Long id);
    List<HistorialPedidoResponseDTO> historial(Long id);
}
