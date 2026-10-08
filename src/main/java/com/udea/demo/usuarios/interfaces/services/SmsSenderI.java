package com.udea.demo.usuarios.interfaces.services;

/**
 * Envío de SMS. La implementación actual es un mock que solo loguea.
 * Cuando se integre un proveedor real (Twilio, etc.), se añade otra implementación
 * y se selecciona por configuración sin tocar el servicio.
 */
public interface SmsSenderI {
    /**
     * @return true si el envío fue aceptado por el proveedor (o simulado), false si no.
     * @throws RuntimeException si el proveedor falla y el llamador debe reintentar.
     */
    boolean enviar(String telefono, String mensaje);
}