package com.udea.demo.pedidos.interfaces.services;

import com.udea.demo.pedidos.application.dto.IncidenciasPedidoResponseDTO;
import com.udea.demo.pedidos.application.dto.RegistrarIncidenciaRequestDTO;
import com.udea.demo.pedidos.application.dto.RegistroIncidenciaResponseDTO;
import com.udea.demo.pedidos.application.dto.TipoIncidenciaResponseDTO;

import java.util.List;

public interface IncidenciaServiceI {
    List<TipoIncidenciaResponseDTO> catalogo();

    RegistroIncidenciaResponseDTO registrar(Long pedidoId, RegistrarIncidenciaRequestDTO dto);

    IncidenciasPedidoResponseDTO listar(Long pedidoId);
}
