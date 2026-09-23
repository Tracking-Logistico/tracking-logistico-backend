package com.udea.demo.pedidos.domain.service;

import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import org.springframework.stereotype.Component;

@Component
public class PrioridadPorTipoServicioYPesoStrategy implements PrioridadStrategy {
    @Override
    public Prioridad sugerir(TipoServicio tipoServicio, Double pesoKg) {
        if (tipoServicio == TipoServicio.EXPRESS) return Prioridad.ALTA;
        if (tipoServicio == TipoServicio.PROGRAMADO) return Prioridad.BAJA;
        return Prioridad.MEDIA;
    }
}
