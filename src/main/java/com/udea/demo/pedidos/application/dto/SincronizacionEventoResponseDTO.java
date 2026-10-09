package com.udea.demo.pedidos.application.dto;

public record SincronizacionEventoResponseDTO(
        String idEventoCliente,
        String estado,
        String motivoRechazo
) {}
