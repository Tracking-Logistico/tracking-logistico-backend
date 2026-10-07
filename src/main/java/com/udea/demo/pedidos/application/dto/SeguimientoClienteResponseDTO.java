package com.udea.demo.pedidos.application.dto;

import com.udea.demo.pedidos.domain.model.EstadoPedido;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SeguimientoClienteResponseDTO(
        Long id,
        String numeroPedido,
        String numeroTracking,
        EstadoPedido estado,
        LocalDateTime fechaEstimadaEntrega,
        List<MovimientoSeguimientoResponseDTO> movimientos,
        String descripcionEstado,
        NovedadClienteDTO novedad,
        LocalDate fechaEntregaReprogramada,
        LocalDateTime fechaLimiteVerificacionDireccion
) {}
