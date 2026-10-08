package com.udea.demo.usuarios.interfaces.services;

import com.udea.demo.compartido.eventos.EventoIntegracion;

/**
 * Puerto de entrada para procesar eventos que puedan derivar en notificaciones.
 * Consumidores (RabbitMQ listener, jobs, etc.) dependen de esta abstracción.
 */
public interface NotificacionServiceI {

    /**
     * Procesa un evento de dominio. Si el evento es un hito clave y el cliente
     * tiene el canal habilitado, envía la notificación y registra el resultado.
     *
     * Si el envío falla y aún quedan intentos, lanza excepción para que el
     * contenedor de RabbitMQ mueva el mensaje a la cola de reintentos.
     */
    void procesar(EventoIntegracion evento);
}
