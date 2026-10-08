package com.udea.demo.usuarios.infrastructure.rabbit;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 * Topología de colas para HU-11.
 *
 *  logistica.eventos  ──(pedido.#)──▶  notification.queue
 *                                             │
 *                                    fallo → dead-letter ──▶ notification.retry.queue (TTL)
 *                                                                     │
 *                                                              expira TTL → vuelve a notification.queue
 *
 *  Tras N intentos fallidos, el listener publica manualmente en notification.dlq.
 */
@Configuration
@ConditionalOnProperty(name = "app.messaging.rabbitmq.enabled", havingValue = "true")
public class NotificacionRabbitConfig {

    public static final String QUEUE       = "notification.queue";
    public static final String RETRY_QUEUE = "notification.retry.queue";
    public static final String DLQ         = "notification.dlq";

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange("")               // exchange default
                .deadLetterRoutingKey(RETRY_QUEUE)
                .build();
    }

    @Bean
    public Queue notificationRetryQueue(@Value("${app.notifications.retry-ttl-ms:5000}") int ttlMs) {
        return QueueBuilder.durable(RETRY_QUEUE)
                .ttl(ttlMs)
                .deadLetterExchange("")
                .deadLetterRoutingKey(QUEUE)
                .build();
    }

    @Bean
    public Queue notificationDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    /** Captura todas las routing keys del productor (pedido.*). */
    @Bean
    public Binding notificationBinding(Queue notificationQueue,
                                       TopicExchange eventosLogisticaExchange) {
        return BindingBuilder.bind(notificationQueue).to(eventosLogisticaExchange).with("pedido.#");
    }

    /** Contenedor con concurrencia moderada. Requeue desactivado para que el DLX funcione. */
    @Bean
    public SimpleRabbitListenerContainerFactory notificacionListenerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter eventosMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(eventosMessageConverter); 
        factory.setDefaultRequeueRejected(false);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(3);
        factory.setPrefetchCount(10);
        return factory;
    }
}