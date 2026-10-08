package com.udea.demo.rutas.application.dto;

public record ParadaPanelDTO(
        Long idParada,
        Long pedidoId,
        Integer orden,
        String numeroPedido,
        String numeroTracking,
        String direccionDestino,
        String ciudadDestino,
        String destinatarioNombre,
        String destinatarioTelefono,
        String prioridad,
        Double pesoKg,
        String estadoParada,
        String estadoPedido
) {}