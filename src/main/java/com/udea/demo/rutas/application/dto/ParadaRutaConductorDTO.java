package com.udea.demo.rutas.application.dto;

public record ParadaRutaConductorDTO(Long paradaId, Long pedidoId, int orden, String direccion,
                                     String ciudad, String estado, boolean sinUbicacion) {}
