package com.udea.demo.rutas.interfaces.services;

import com.udea.demo.rutas.application.dto.DetalleEntregaDTO;
import com.udea.demo.rutas.application.dto.ParadaPanelDTO;
import com.udea.demo.rutas.application.dto.ProgresoPanelDTO;
import com.udea.demo.rutas.application.dto.SiguienteParadaDTO;
import com.udea.demo.rutas.application.dto.RutaConductorResponseDTO;

import java.util.List;

public interface PanelConductorServiceI {

    /** Progreso de la jornada: total, entregadas, pendientes, fallidas, canceladas. */
    ProgresoPanelDTO progreso();

    /** Entregas asignadas al conductor autenticado para la jornada activa. */
    List<ParadaPanelDTO> entregasAsignadas();

    /** Detalle extendido de un envío asignado al conductor autenticado. */
    DetalleEntregaDTO detalleEntrega(Long pedidoId);

    /** Siguiente parada recomendada: primera pendiente por orden. */
    SiguienteParadaDTO siguienteParada();
    RutaConductorResponseDTO ruta();
}