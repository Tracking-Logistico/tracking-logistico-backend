package com.udea.demo.config.messaging;

import com.udea.demo.compartido.eventos.EventoIntegracion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Sustituto local (H2/pruebas) cuando RabbitMQ está deshabilitado: solo deja traza del evento. */
@Component
@ConditionalOnProperty(name = "app.messaging.rabbitmq.enabled", havingValue = "false", matchIfMissing = true)
public class LogEventoIntegracionPublisher {
    private static final Logger log = LoggerFactory.getLogger(LogEventoIntegracionPublisher.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publicar(EventoIntegracion evento) {
        log.info("Evento de integración {} eventId={} routingKey={} (RabbitMQ deshabilitado)",
                evento.tipo(), evento.eventId(), evento.routingKey());
    }
}
