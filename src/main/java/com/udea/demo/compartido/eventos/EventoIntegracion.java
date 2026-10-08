package com.udea.demo.compartido.eventos;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento publicado por un módulo para que otros módulos (p. ej. notificaciones) reaccionen
 * sin acoplarse al emisor. El transporte (RabbitMQ u otro) se resuelve en infraestructura.
 */
public interface EventoIntegracion {
    /** Identificador único; los consumidores lo usan para descartar entregas duplicadas. */
    UUID eventId();

    String tipo();

    /** Clave de enrutamiento en el exchange de eventos, p. ej. {@code pedido.incidencia.registrada}. */
    String routingKey();

    LocalDateTime ocurridoEn();
}
