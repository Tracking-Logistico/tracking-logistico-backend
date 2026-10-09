package com.udea.demo.config.messaging;

import com.udea.demo.compartido.eventos.EventoIntegracion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reenvía a RabbitMQ los eventos de integración una vez confirmada la transacción que los originó,
 * de modo que nunca se notifique un cambio que terminó revertido.
 */
@Component
@ConditionalOnProperty(name = "app.messaging.rabbitmq.enabled", havingValue = "true")
public class RabbitEventoIntegracionPublisher {
    private static final Logger log = LoggerFactory.getLogger(RabbitEventoIntegracionPublisher.class);
    private final RabbitTemplate rabbit;
    private final String exchange;

    public RabbitEventoIntegracionPublisher(RabbitTemplate rabbit,
                                            @Value("${app.messaging.rabbitmq.exchange:logistica.eventos}") String exchange) {
        this.rabbit = rabbit;
        this.exchange = exchange;
        log.info("RabbitEventoIntegracionPublisher listo, exchange={}", exchange);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publicar(EventoIntegracion evento) {
        try {
            rabbit.convertAndSend(exchange, evento.routingKey(), evento, mensaje -> {
                mensaje.getMessageProperties().setMessageId(evento.eventId().toString());
                mensaje.getMessageProperties().setType(evento.tipo());
                mensaje.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return mensaje;
            });
        } catch (AmqpException ex) {
            // La operación de negocio ya fue confirmada; se registra para reintento/diagnóstico.
            log.error("No fue posible publicar el evento {} eventId={} routingKey={}",
                    evento.tipo(), evento.eventId(), evento.routingKey(), ex);
        }
    }
}
