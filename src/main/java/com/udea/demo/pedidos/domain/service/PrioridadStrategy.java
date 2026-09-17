package com.udea.demo.pedidos.domain.service;

import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;

// Strategy: permite intercambiar el algoritmo de sugerencia de prioridad sin tocar el servicio que lo usa
public interface PrioridadStrategy {
    Prioridad sugerir(TipoServicio tipoServicio, Double pesoKg);
}
