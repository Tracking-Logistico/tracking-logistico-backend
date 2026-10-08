package com.udea.demo.usuarios.interfaces.services;

public interface EmailServiceI {
    void enviarEnlaceRestablecimiento(String destinatario, String enlace);
    void enviarVerificacion(String destinatario, String nombreUsuario, String enlace);

        /**
     * Envía una notificación de cambio de estado (HU-11).
     * A diferencia de los métodos anteriores, ESTE lanza excepción si el proveedor falla,
     * para que el listener de RabbitMQ pueda reintentar.
     */
    void enviarNotificacionCambioEstado(String destinatario, String asunto, String contenido);
}
