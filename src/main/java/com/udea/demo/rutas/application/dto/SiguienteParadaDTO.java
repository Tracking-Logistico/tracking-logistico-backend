package com.udea.demo.rutas.application.dto;

public record SiguienteParadaDTO(
        Long idParada,
        Long pedidoId,
        Integer orden,
        String numeroTracking,
        String direccionDestino,
        String ciudadDestino,
        String destinatarioNombre,
        String destinatarioTelefono,
        String indicacionesAcceso,
        Double pesoKg
) {}