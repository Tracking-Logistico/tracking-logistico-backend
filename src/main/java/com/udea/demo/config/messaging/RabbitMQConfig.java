package com.udea.demo.config.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.messaging.rabbitmq.enabled", havingValue = "true")
public class RabbitMQConfig {

    /** Exchange de eventos del dominio logístico; las colas las declara cada consumidor. */
    @Bean
    public TopicExchange eventosLogisticaExchange(@Value("${app.messaging.rabbitmq.exchange:logistica.eventos}") String nombre) {
        return new TopicExchange(nombre, true, false);
    }

    @Bean
    public MessageConverter eventosMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
