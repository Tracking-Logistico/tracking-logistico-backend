package com.udea.demo.pedidos.application.dto;

import com.udea.demo.pedidos.domain.model.ResultadoEntrega;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.OffsetDateTime;

public record ResultadoEntregaRequestDTO(
        @NotNull ResultadoEntrega resultado,
        @Size(max = 50) String codigoNovedad,
        @Size(max = 500) String motivo,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitud,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitud,
        @NotNull OffsetDateTime fechaEvento,
        @NotBlank @Pattern(regexp = "^[0-9a-fA-F-]{36}$") String idEventoCliente
) {
    @AssertTrue(message = "La fecha del evento debe estar entre las últimas 24 horas y 5 minutos en el futuro")
    @JsonIgnore
    public boolean isFechaEventoValida() {
        if (fechaEvento == null) return true;
        OffsetDateTime ahora = OffsetDateTime.now();
        return !fechaEvento.isAfter(ahora.plusMinutes(5))
                && !fechaEvento.isBefore(ahora.minusHours(24));
    }
}
