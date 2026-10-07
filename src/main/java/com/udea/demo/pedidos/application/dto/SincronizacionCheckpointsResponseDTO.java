package com.udea.demo.pedidos.application.dto;

import java.util.List;

/**
 * Resumen de una sincronización offline. {@code pendientesRevision} informa al conductor cuántos
 * eventos quedaron retenidos por conflicto con el estado actual del envío.
 */
public record SincronizacionCheckpointsResponseDTO(
        int aplicados,
        int duplicados,
        int pendientesRevision,
        int rechazados,
        List<ResultadoItem> resultados
) {
    public enum EstadoSincronizacion { APLICADO, DUPLICADO, PENDIENTE_REVISION, RECHAZADO }

    public record ResultadoItem(String idEventoCliente, Long pedidoId, EstadoSincronizacion estado,
                                Long checkpointId, String motivo) {}

    public static SincronizacionCheckpointsResponseDTO de(List<ResultadoItem> resultados) {
        return new SincronizacionCheckpointsResponseDTO(
                contar(resultados, EstadoSincronizacion.APLICADO),
                contar(resultados, EstadoSincronizacion.DUPLICADO),
                contar(resultados, EstadoSincronizacion.PENDIENTE_REVISION),
                contar(resultados, EstadoSincronizacion.RECHAZADO),
                List.copyOf(resultados));
    }

    private static int contar(List<ResultadoItem> resultados, EstadoSincronizacion estado) {
        return (int) resultados.stream().filter(r -> r.estado() == estado).count();
    }
}
