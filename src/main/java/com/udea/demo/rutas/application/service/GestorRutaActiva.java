package com.udea.demo.rutas.application.service;

import com.udea.demo.rutas.domain.model.Ruta;
import com.udea.demo.rutas.interfaces.persistence.RutaRepository;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

// Pure Fabrication (GRASP): encapsula la búsqueda o apertura de la ruta del día de un conductor
@Component
public class GestorRutaActiva {

    private final RutaRepository rutaRepository;

    public GestorRutaActiva(RutaRepository rutaRepository) {
        this.rutaRepository = rutaRepository;
    }

    public Ruta obtenerOCrear(Long conductorId) {
        LocalDate hoy = LocalDate.now();

        return rutaRepository.findByConductorIdAndFecha(conductorId, hoy)
                .orElseGet(() -> rutaRepository.save(Ruta.crear(conductorId, hoy)));
    }
}
