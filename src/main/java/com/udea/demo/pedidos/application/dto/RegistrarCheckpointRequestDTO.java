package com.udea.demo.pedidos.application.dto;

import com.udea.demo.pedidos.domain.model.EtapaCheckpoint;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;

/**
 * Escaneo del QR de un envío en un punto de control.
 * {@code idEventoCliente} lo genera el dispositivo y hace idempotente el reenvío del mismo escaneo;
 * {@code fechaDispositivo} es la marca de tiempo real del escaneo (no la de sincronización).
 */
public record RegistrarCheckpointRequestDTO(
        @NotBlank(message = "El código QR escaneado es obligatorio") @Size(max = 120) String codigoQr,
        @NotNull(message = "La etapa del recorrido es obligatoria") EtapaCheckpoint etapa,
        @NotBlank(message = "El identificador del evento es obligatorio")
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "El identificador del evento debe ser un UUID") String idEventoCliente,
        Double latitud,
        Double longitud,
        Double precisionMetros,
        @NotNull(message = "La fecha del dispositivo es obligatoria") OffsetDateTime fechaDispositivo
) {}
