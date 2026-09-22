package com.udea.demo.rutas.interfaces.services;

import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.rutas.application.dto.AsignarEnvioRequestDTO;
import com.udea.demo.rutas.application.dto.ReasignarEnvioRequestDTO;
import com.udea.demo.rutas.application.dto.ReordenarRutaRequestDTO;
import com.udea.demo.rutas.application.dto.RutaResponseDTO;

import java.util.List;

public interface RutaServiceI {

    List<PedidoResponseDTO> listarEnviosPendientesDeAsignacion();

    RutaResponseDTO asignarEnvio(AsignarEnvioRequestDTO dto);

    RutaResponseDTO reordenarRuta(Long rutaId, ReordenarRutaRequestDTO dto);

    RutaResponseDTO reasignarEnvio(ReasignarEnvioRequestDTO dto);

    RutaResponseDTO obtenerRutaActivaDeConductor(Long conductorId);
}
