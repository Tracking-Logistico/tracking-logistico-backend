package com.udea.demo.usuarios.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Historial y cola de una notificación (HU-11). Se crea en PENDIENTE, pasa a ENVIADA
 * o FALLIDA según el resultado. El par (event_id, canal) es UNIQUE para idempotencia.
 */
@Entity
@Table(name = "notificaciones_enviadas")
public class NotificacionEnviada {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long id;

    @Column(name = "id_cliente", nullable = false) private Long clienteId;
    @Column(name = "id_pedido") private Long pedidoId;
    @Column(name = "numero_tracking", length = 40) private String numeroTracking;
    @Column(name = "event_id", nullable = false, length = 36) private String eventId;
    @Column(name = "tipo_evento", nullable = false, length = 60) private String tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 20)
    private CanalNotificacion canal;

    @Column(name = "destinatario", nullable = false, length = 200) private String destinatario;
    @Column(name = "asunto", length = 200) private String asunto;
    @Column(name = "contenido", columnDefinition = "TEXT") private String contenido;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoNotificacion estado = EstadoNotificacion.PENDIENTE;

    @Column(name = "intentos", nullable = false) private int intentos = 0;
    @Column(name = "motivo_fallo", length = 500) private String motivoFallo;
    @Column(name = "fecha_creacion", nullable = false) private LocalDateTime fechaCreacion = LocalDateTime.now();
    @Column(name = "fecha_ultimo_intento") private LocalDateTime fechaUltimoIntento;

    protected NotificacionEnviada() {}

    public static NotificacionEnviada pendiente(String eventId, String tipoEvento,
                                                Long clienteId, Long pedidoId, String numeroTracking,
                                                CanalNotificacion canal, String destinatario,
                                                String asunto, String contenido) {
        NotificacionEnviada n = new NotificacionEnviada();
        n.eventId = eventId; n.tipoEvento = tipoEvento;
        n.clienteId = clienteId; n.pedidoId = pedidoId; n.numeroTracking = numeroTracking;
        n.canal = canal; n.destinatario = destinatario;
        n.asunto = asunto; n.contenido = contenido;
        return n;
    }

    public void marcarEnviada() {
        this.estado = EstadoNotificacion.ENVIADA;
        this.intentos++;
        this.fechaUltimoIntento = LocalDateTime.now();
        this.motivoFallo = null;
    }

    public void registrarFallo(String motivo) {
        this.estado = EstadoNotificacion.FALLIDA;
        this.intentos++;
        this.fechaUltimoIntento = LocalDateTime.now();
        this.motivoFallo = motivo != null && motivo.length() > 500 ? motivo.substring(0, 500) : motivo;
    }

    public Long getId() { return id; }
    public Long getClienteId() { return clienteId; }
    public Long getPedidoId() { return pedidoId; }
    public String getNumeroTracking() { return numeroTracking; }
    public String getEventId() { return eventId; }
    public String getTipoEvento() { return tipoEvento; }
    public CanalNotificacion getCanal() { return canal; }
    public String getDestinatario() { return destinatario; }
    public String getAsunto() { return asunto; }
    public String getContenido() { return contenido; }
    public EstadoNotificacion getEstado() { return estado; }
    public int getIntentos() { return intentos; }
    public String getMotivoFallo() { return motivoFallo; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaUltimoIntento() { return fechaUltimoIntento; }
}
