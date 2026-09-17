package com.udea.demo.pedidos.interfaces.services;

import com.udea.demo.pedidos.application.dto.EtiquetaEnvioResponseDTO;
import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.pedidos.application.dto.RecibirPedidoRequestDTO;
import com.udea.demo.pedidos.application.dto.ValidarPedidoRequestDTO;

import java.util.List;

public interface PedidoServiceI {

    PedidoResponseDTO recibir(RecibirPedidoRequestDTO dto);

    List<PedidoResponseDTO> listarPendientes();

    PedidoResponseDTO obtener(Long id);

    PedidoResponseDTO validar(Long id, ValidarPedidoRequestDTO dto);

    PedidoResponseDTO activarTracking(Long id);

    EtiquetaEnvioResponseDTO generarEtiqueta(Long id);

    List<PedidoResponseDTO> listarEnTransito();
}
