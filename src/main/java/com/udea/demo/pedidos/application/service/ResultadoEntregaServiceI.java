package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.*;
import java.util.List;

public interface ResultadoEntregaServiceI {
    ResultadoEntregaResponseDTO registrar(Long pedidoId, ResultadoEntregaRequestDTO dto);
    List<NovedadEntregaResponseDTO> catalogo();
}
