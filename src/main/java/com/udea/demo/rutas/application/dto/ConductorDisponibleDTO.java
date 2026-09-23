package com.udea.demo.rutas.application.dto;
public record ConductorDisponibleDTO(
    Long usuarioId, String nombre, String estado,
    Double capacidadMaxKg, Double capacidadMaxVolumenCm3, Integer maxEntregasDia,
    int entregasAsignadas, double pesoAsignadoKg, double volumenAsignadoCm3
) {
    public ConductorDisponibleDTO(Long usuarioId, String nombre, String estado,
            Double capacidadMaxKg, Double capacidadMaxVolumenCm3, Integer maxEntregasDia) {
        this(usuarioId, nombre, estado, capacidadMaxKg, capacidadMaxVolumenCm3, maxEntregasDia, 0, 0, 0);
    }
}
