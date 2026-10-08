package com.udea.demo.rutas.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DetalleEntregaDTO(
        Long idParada,
        Long pedidoId,
        Integer orden,
        String numeroPedido,
        String numeroTracking,
        String direccionDestino,
        String ciudadDestino,
        String codigoPostalDestino,
        String destinatarioNombre,
        String destinatarioTelefono,
        String indicacionesAcceso,
        String descripcionPaquete,
        Double pesoKg,
        Double largoCm,
        Double anchoCm,
        Double altoCm,
        String prioridad,
        String estadoParada,
        String estadoPedido,
        String observacionesValidacion,
        LocalDate fechaEntregaReprogramada,
        List<EventoHistorialDTO> ultimosEventos
) {
    public record EventoHistorialDTO(String tipoEvento, String detalle, LocalDateTime fecha) {}
}