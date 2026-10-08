package com.udea.demo.config.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.messaging.rabbitmq.enabled", havingValue = "true")
public class RabbitMQConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RabbitMQConfig.class);

    public RabbitMQConfig() {
        log.info("RabbitMQConfig instanciado");
    }
    /** Exchange de eventos del dominio logístico; las colas las declara cada consumidor. */
    @Bean
    public TopicExchange eventosLogisticaExchange(@Value("${app.messaging.rabbitmq.exchange:logistica.eventos}") String nombre) {
        return new TopicExchange(nombre, true, false);
    }

    @Bean
    public MessageConverter eventosMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();

        DefaultJacksonJavaTypeMapper mapper = new DefaultJacksonJavaTypeMapper();
        mapper.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.TYPE_ID);
        mapper.setTrustedPackages(
                "com.udea.demo.pedidos.domain.event",
                "com.udea.demo.compartido.eventos"
        );

        converter.setJavaTypeMapper(mapper);
        return converter;
    }

    @Bean
    public org.springframework.amqp.rabbit.core.RabbitAdmin rabbitAdmin(
            org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) {
        var admin = new org.springframework.amqp.rabbit.core.RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    } 
}
