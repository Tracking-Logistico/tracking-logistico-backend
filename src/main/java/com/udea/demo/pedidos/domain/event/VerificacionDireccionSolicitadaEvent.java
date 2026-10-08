package com.udea.demo.pedidos.domain.event;

import com.udea.demo.compartido.eventos.EventoIntegracion;
import com.udea.demo.pedidos.domain.model.Pedido;

import java.time.LocalDateTime;
import java.util.UUID;

/** El cliente debe confirmar o actualizar la dirección antes de {@code fechaLimite}. */
public record VerificacionDireccionSolicitadaEvent(
        UUID eventId,
        String tipo,
        int version,
        LocalDateTime ocurridoEn,
        Long pedidoId,
        String numeroTracking,
        Long clienteId,
        LocalDateTime fechaLimite,
        String mensajeCliente
) implements EventoIntegracion {
    public static final String TIPO = "VERIFICACION_DIRECCION_SOLICITADA";
    public static final String ROUTING_KEY = "pedido.direccion.verificacion-solicitada";

    public static VerificacionDireccionSolicitadaEvent de(Pedido pedido) {
        return new VerificacionDireccionSolicitadaEvent(UUID.randomUUID(), TIPO, 1, LocalDateTime.now(), pedido.getId(),
                pedido.getNumeroTracking(), pedido.getClienteId(), pedido.getFechaLimiteVerificacionDireccion(),
                "Confirma o actualiza la dirección de entrega antes del "
                        + pedido.getFechaLimiteVerificacionDireccion().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        + " para evitar la devolución del paquete al remitente.");
    }

    @Override public String routingKey() { return ROUTING_KEY; }
}
