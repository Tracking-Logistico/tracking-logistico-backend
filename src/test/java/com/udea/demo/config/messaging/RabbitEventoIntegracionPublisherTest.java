package com.udea.demo.config.messaging;

import com.udea.demo.compartido.eventos.EventoIntegracion;
import com.udea.demo.pedidos.domain.event.CheckpointRegistradoEvent;
import com.udea.demo.pedidos.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Publicación de eventos de integración en RabbitMQ")
class RabbitEventoIntegracionPublisherTest {
    @Mock private RabbitTemplate rabbit;

    private EventoIntegracion evento() {
        Pedido pedido = Pedido.builder().id(1L).clienteId(10L).numeroTracking("LT1").estado(EstadoPedido.EN_REPARTO).build();
        CheckpointPedido checkpoint = CheckpointPedido.aplicado(1L, 7L, "e-1", EtapaCheckpoint.EN_REPARTO,
                EstadoPedido.EN_TRANSITO, EstadoPedido.EN_REPARTO, new UbicacionReportada(6.0, -75.0, 5.0, true),
                OrigenCheckpoint.EN_LINEA, LocalDateTime.now());
        return CheckpointRegistradoEvent.de(pedido, checkpoint);
    }

    @Test
    @DisplayName("Envía al exchange con la routing key del evento, message-id = eventId y entrega persistente")
    void publica() {
        var publisher = new RabbitEventoIntegracionPublisher(rabbit, "logistica.eventos");
        EventoIntegracion evento = evento();

        publisher.publicar(evento);

        ArgumentCaptor<MessagePostProcessor> procesador = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbit).convertAndSend(eq("logistica.eventos"), eq("pedido.checkpoint.registrado"), eq(evento),
                procesador.capture());
        Message mensaje = procesador.getValue().postProcessMessage(new Message(new byte[0], new MessageProperties()));
        assertThat(mensaje.getMessageProperties().getMessageId()).isEqualTo(evento.eventId().toString());
        assertThat(mensaje.getMessageProperties().getType()).isEqualTo("CHECKPOINT_REGISTRADO");
        assertThat(mensaje.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
    }

    @Test
    @DisplayName("Una caída del broker no propaga el error a la operación ya confirmada")
    void brokerCaido() {
        var publisher = new RabbitEventoIntegracionPublisher(rabbit, "logistica.eventos");
        doThrow(new AmqpConnectException(new java.net.ConnectException("down")))
                .when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));

        assertThatCode(() -> publisher.publicar(evento())).doesNotThrowAnyException();
    }
}
