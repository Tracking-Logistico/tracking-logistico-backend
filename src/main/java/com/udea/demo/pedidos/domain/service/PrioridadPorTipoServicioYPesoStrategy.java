package com.udea.demo.pedidos.domain.service;

import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import org.springframework.stereotype.Component;

// Implementación por defecto: EXPRESS siempre es urgente, la carga pesada sube de prioridad
@Component
public class PrioridadPorTipoServicioYPesoStrategy implements PrioridadStrategy {

    private static final double PESO_UMBRAL_ALTA_KG = 20.0;

    @Override
    public Prioridad sugerir(TipoServicio tipoServicio, Double pesoKg) {
        if (tipoServicio == TipoServicio.EXPRESS) {
            return Prioridad.URGENTE;
        }
        if (pesoKg != null && pesoKg > PESO_UMBRAL_ALTA_KG) {
            return Prioridad.ALTA;
        }
        if (tipoServicio == TipoServicio.PROGRAMADO) {
            return Prioridad.BAJA;
        }
        return Prioridad.MEDIA;
    }
}
