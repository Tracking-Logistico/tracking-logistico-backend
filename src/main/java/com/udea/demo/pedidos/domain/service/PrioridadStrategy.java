package com.udea.demo.pedidos.domain.service;

import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;

public interface PrioridadStrategy {
    Prioridad sugerir(TipoServicio tipoServicio, Double pesoKg);
}
