package com.udea.demo.rutas.interfaces.services;

import com.udea.demo.pedidos.application.dto.PedidoResponseDTO;
import com.udea.demo.rutas.application.dto.*;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RutaServiceI {
    Page<PedidoResponseDTO> listarEnviosPendientesDeAsignacion(Pageable pageable);
    List<ConductorDisponibleDTO> listarConductoresDisponibles();
    RutaResponseDTO asignarEnvio(AsignarEnvioRequestDTO dto);
    RutaResponseDTO asignarVarios(AsignacionMasivaRequestDTO dto);
    RutaResponseDTO reordenarRuta(Long rutaId, ReordenarRutaRequestDTO dto);
    RutaResponseDTO reasignarEnvio(ReasignarEnvioRequestDTO dto);
    RutaResponseDTO obtenerRutaActivaDeConductor(Long conductorId);
}
