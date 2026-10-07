package com.udea.demo.pedidos.domain.model;

/** Coordenadas GPS declaradas por el dispositivo y su evaluación de confiabilidad. */
public record UbicacionReportada(Double latitud, Double longitud, Double precisionMetros, boolean confiable) {}
