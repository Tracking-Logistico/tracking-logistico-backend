package com.udea.demo.rutas.application.service;

import com.udea.demo.rutas.domain.model.Ruta;
import com.udea.demo.rutas.interfaces.persistence.RutaRepository;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;

@Component
public class GestorRutaActiva {

    private final RutaRepository rutaRepository;
    @Value("${app.operations.time-zone:America/Bogota}")
    private String zonaHoraria = "UTC";

    public GestorRutaActiva(RutaRepository rutaRepository) {
        this.rutaRepository = rutaRepository;
    }

    public Ruta obtenerOCrear(Long conductorId) {
        LocalDate hoy = LocalDate.now(ZoneId.of(zonaHoraria));

        return rutaRepository.findByConductorIdAndFecha(conductorId, hoy)
                .orElseGet(() -> rutaRepository.save(Ruta.crear(conductorId, hoy)));
    }
}
