package com.udea.demo.rutas.interfaces.services;

import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.rutas.application.dto.*;
import java.util.List;

public interface RutaServiceI {
    List<PedidoResponseDTO> listarEnviosPendientesDeAsignacion();
    List<ConductorDisponibleDTO> listarConductoresDisponibles();
    RutaResponseDTO asignarEnvio(AsignarEnvioRequestDTO dto);
    RutaResponseDTO asignarVarios(AsignacionMasivaRequestDTO dto);
    RutaResponseDTO reordenarRuta(Long rutaId, ReordenarRutaRequestDTO dto);
    RutaResponseDTO reasignarEnvio(ReasignarEnvioRequestDTO dto);
    RutaResponseDTO obtenerRutaActivaDeConductor(Long conductorId);
}
