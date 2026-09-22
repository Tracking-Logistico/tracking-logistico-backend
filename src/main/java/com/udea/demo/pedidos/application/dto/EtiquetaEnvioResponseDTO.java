package com.udea.demo.pedidos.application.dto;

import java.time.LocalDateTime;

public record EtiquetaEnvioResponseDTO(
    String numeroPedido,
    String numeroTracking,
    String contenido,
    LocalDateTime fechaImpresion
) {}
