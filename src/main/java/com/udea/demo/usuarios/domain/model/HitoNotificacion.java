package com.udea.demo.usuarios.domain.model;

import com.udea.demo.pedidos.domain.model.EstadoPedido;

/**
 * Datos mínimos que necesita el módulo de notificaciones para enviar un mensaje
 * cuando un evento representa un hito clave. Es un value object: inmutable.
 */
public record HitoNotificacion(
        Long pedidoId,
        String numeroTracking,
        Long clienteId,
        EstadoPedido estadoParaNotificar,
        String mensajeCliente,
        String tipoEvento
) {}