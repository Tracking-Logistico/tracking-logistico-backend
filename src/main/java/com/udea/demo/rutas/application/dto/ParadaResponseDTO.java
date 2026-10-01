package com.udea.demo.rutas.application.dto;
import java.time.LocalDateTime;
public record ParadaResponseDTO(
    Long id, Long pedidoId, Integer orden, String estado, LocalDateTime fechaAsignacion,
    String numeroPedido, String numeroTracking, String direccionDestino, String ciudadDestino,
    String destinatarioNombre, String destinatarioTelefono, String prioridad,
    Double pesoKg
) {
    public ParadaResponseDTO(Long id, Long pedidoId, Integer orden, String estado, LocalDateTime fechaAsignacion) {
        this(id, pedidoId, orden, estado, fechaAsignacion, null, null, null, null, null, null, null, null);
    }
}
