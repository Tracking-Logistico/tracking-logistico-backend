package com.udea.demo.usuarios.infrastructure.rabbit;

import com.udea.demo.compartido.eventos.EventoIntegracion;
import com.udea.demo.usuarios.interfaces.services.NotificacionServiceI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * SRP: solo escucha y delega.
 * DIP: depende de NotificacionServiceI, no de la implementación.
 * Si el servicio lanza excepción, el mensaje va a notification.retry.queue vía DLX.
 */
@Component
@ConditionalOnProperty(name = "app.messaging.rabbitmq.enabled", havingValue = "true")
public class ConsumidorEventoNotificacion {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorEventoNotificacion.class);

    private final NotificacionServiceI notificacionService;

    public ConsumidorEventoNotificacion(NotificacionServiceI notificacionService) {
        this.notificacionService = notificacionService;
    }

    @RabbitListener(queues = NotificacionRabbitConfig.QUEUE,
                    containerFactory = "notificacionListenerFactory")
    public void onEvento(EventoIntegracion evento) {
        log.debug("Evento recibido tipo={} eventId={}", evento.tipo(), evento.eventId());
        notificacionService.procesar(evento);
    }
}