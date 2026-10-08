package com.udea.demo.pedidos.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "checkpoints_pedido", indexes = {
        @Index(name = "idx_checkpoints_pedido_fecha", columnList = "id_pedido, fecha_evento"),
        @Index(name = "idx_checkpoints_estado_registro", columnList = "estado_registro, fecha_registro")})
public class CheckpointPedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_checkpoint") private Long id;
    @Column(name = "id_pedido", nullable = false) private Long pedidoId;
    @Column(name = "id_usuario", nullable = false) private Long usuarioId;
    @Column(name = "id_evento_cliente", nullable = false, unique = true, length = 36) private String idEventoCliente;
    @Enumerated(EnumType.STRING) @Column(name = "etapa", nullable = false, length = 30) private EtapaCheckpoint etapa;
    @Enumerated(EnumType.STRING) @Column(name = "estado_anterior", nullable = false, length = 30) private EstadoPedido estadoAnterior;
    @Enumerated(EnumType.STRING) @Column(name = "estado_resultante", nullable = false, length = 30) private EstadoPedido estadoResultante;
    @Column(name = "latitud") private Double latitud;
    @Column(name = "longitud") private Double longitud;
    @Column(name = "precision_metros") private Double precisionMetros;
    @Column(name = "ubicacion_confiable", nullable = false) private boolean ubicacionConfiable;
    @Enumerated(EnumType.STRING) @Column(name = "origen", nullable = false, length = 20) private OrigenCheckpoint origen;
    @Enumerated(EnumType.STRING) @Column(name = "estado_registro", nullable = false, length = 30) private EstadoRegistroCheckpoint estadoRegistro;
    @Column(name = "motivo_revision", length = 255) private String motivoRevision;
    @Column(name = "fecha_evento", nullable = false) private LocalDateTime fechaEvento;
    @Column(name = "fecha_registro", nullable = false) private LocalDateTime fechaRegistro;

    protected CheckpointPedido() {}

    private CheckpointPedido(Long pedidoId, Long usuarioId, String idEventoCliente, EtapaCheckpoint etapa,
                             EstadoPedido estadoAnterior, EstadoPedido estadoResultante, UbicacionReportada ubicacion,
                             OrigenCheckpoint origen, EstadoRegistroCheckpoint estadoRegistro, String motivoRevision,
                             LocalDateTime fechaEvento) {
        this.pedidoId = pedidoId;
        this.usuarioId = usuarioId;
        this.idEventoCliente = idEventoCliente;
        this.etapa = etapa;
        this.estadoAnterior = estadoAnterior;
        this.estadoResultante = estadoResultante;
        this.latitud = ubicacion.latitud();
        this.longitud = ubicacion.longitud();
        this.precisionMetros = ubicacion.precisionMetros();
        this.ubicacionConfiable = ubicacion.confiable();
        this.origen = origen;
        this.estadoRegistro = estadoRegistro;
        this.motivoRevision = motivoRevision;
        this.fechaEvento = fechaEvento;
        this.fechaRegistro = LocalDateTime.now();
    }

    public static CheckpointPedido aplicado(Long pedidoId, Long usuarioId, String idEventoCliente, EtapaCheckpoint etapa,
                                            EstadoPedido estadoAnterior, EstadoPedido estadoResultante,
                                            UbicacionReportada ubicacion, OrigenCheckpoint origen, LocalDateTime fechaEvento) {
        return new CheckpointPedido(pedidoId, usuarioId, idEventoCliente, etapa, estadoAnterior, estadoResultante,
                ubicacion, origen, EstadoRegistroCheckpoint.APLICADO, null, fechaEvento);
    }

    /** Evento sincronizado que entra en conflicto con el estado actual: se conserva sin aplicarse. */
    public static CheckpointPedido pendienteRevision(Long pedidoId, Long usuarioId, String idEventoCliente,
                                                     EtapaCheckpoint etapa, EstadoPedido estadoActual,
                                                     UbicacionReportada ubicacion, String motivo,
                                                     LocalDateTime fechaEvento) {
        return new CheckpointPedido(pedidoId, usuarioId, idEventoCliente, etapa, estadoActual, estadoActual,
                ubicacion, OrigenCheckpoint.OFFLINE, EstadoRegistroCheckpoint.PENDIENTE_REVISION, motivo, fechaEvento);
    }

    public boolean cambioEstado() { return estadoAnterior != estadoResultante; }

    public Long getId() { return id; }
    public Long getPedidoId() { return pedidoId; }
    public Long getUsuarioId() { return usuarioId; }
    public String getIdEventoCliente() { return idEventoCliente; }
    public EtapaCheckpoint getEtapa() { return etapa; }
    public EstadoPedido getEstadoAnterior() { return estadoAnterior; }
    public EstadoPedido getEstadoResultante() { return estadoResultante; }
    public Double getLatitud() { return latitud; }
    public Double getLongitud() { return longitud; }
    public Double getPrecisionMetros() { return precisionMetros; }
    public boolean isUbicacionConfiable() { return ubicacionConfiable; }
    public OrigenCheckpoint getOrigen() { return origen; }
    public EstadoRegistroCheckpoint getEstadoRegistro() { return estadoRegistro; }
    public String getMotivoRevision() { return motivoRevision; }
    public LocalDateTime getFechaEvento() { return fechaEvento; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
}
