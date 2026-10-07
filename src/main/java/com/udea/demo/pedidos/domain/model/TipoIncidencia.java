package com.udea.demo.pedidos.domain.model;

import com.udea.demo.pedidos.domain.exception.TipoIncidenciaInvalidoException;

import java.util.Locale;

/**
 * Catálogo cerrado de novedades que el operador puede registrar sobre un envío.
 * Cada tipo define su efecto sobre el estado y el texto que verá el cliente.
 */
public enum TipoIncidencia {
    DIRECCION_INCORRECTA("Dirección incorrecta", EstadoPedido.DIRECCION_POR_VERIFICAR, false, false,
            "No pudimos ubicar la dirección de entrega. Confirma o actualiza tu dirección para continuar con la entrega."),
    CLIENTE_AUSENTE("Cliente ausente", EstadoPedido.ENTREGA_FALLIDA, false, true,
            "Intentamos entregar tu paquete, pero no encontramos a nadie para recibirlo. Puedes elegir una nueva fecha de entrega."),
    PAQUETE_DANADO("Paquete dañado", null, true, false,
            "Detectamos un daño en tu paquete y lo estamos revisando. Te informaremos cómo continuará la entrega."),
    PAQUETE_RECHAZADO("Paquete rechazado", EstadoPedido.DEVOLUCION_AL_REMITENTE, false, false,
            "El paquete fue rechazado al momento de la entrega y será devuelto al remitente."),
    RETRASO_OPERATIVO("Retraso operativo", null, false, false,
            "Tu envío presenta un retraso en la operación logística. Estamos trabajando para entregarlo lo antes posible."),
    OTRO("Otro", null, true, false,
            "Se registró una novedad en tu envío y nuestro equipo la está gestionando.");

    private final String descripcion;
    private final EstadoPedido estadoResultante;
    private final boolean requiereComentario;
    private final boolean cuentaComoIntento;
    private final String mensajeCliente;

    TipoIncidencia(String descripcion, EstadoPedido estadoResultante, boolean requiereComentario,
                   boolean cuentaComoIntento, String mensajeCliente) {
        this.descripcion = descripcion;
        this.estadoResultante = estadoResultante;
        this.requiereComentario = requiereComentario;
        this.cuentaComoIntento = cuentaComoIntento;
        this.mensajeCliente = mensajeCliente;
    }

    public static TipoIncidencia desde(String codigo) {
        if (codigo == null || codigo.isBlank()) throw new TipoIncidenciaInvalidoException(codigo);
        try {
            return valueOf(codigo.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new TipoIncidenciaInvalidoException(codigo);
        }
    }

    public String descripcion() { return descripcion; }

    /** Estado al que lleva la incidencia; {@code null} si solo informa sin cambiar el estado. */
    public EstadoPedido estadoResultante() { return estadoResultante; }

    public boolean cambiaEstado() { return estadoResultante != null; }

    /** Tipos de naturaleza no estándar: el operador debe describir la situación. */
    public boolean requiereComentario() { return requiereComentario; }

    /** Cuenta como intento de entrega fallido para el límite de devolución automática. */
    public boolean cuentaComoIntento() { return cuentaComoIntento; }

    public String mensajeCliente() { return mensajeCliente; }
}
