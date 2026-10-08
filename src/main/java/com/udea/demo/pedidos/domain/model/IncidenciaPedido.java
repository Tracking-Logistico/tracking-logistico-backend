package com.udea.demo.pedidos.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidencias_pedido", indexes = @Index(name = "idx_incidencias_pedido_fecha", columnList = "id_pedido, fecha"))
public class IncidenciaPedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_incidencia") private Long id;
    @Column(name = "id_pedido", nullable = false) private Long pedidoId;
    @Column(name = "id_usuario", nullable = false) private Long usuarioId;
    @Enumerated(EnumType.STRING) @Column(name = "tipo", nullable = false, length = 40) private TipoIncidencia tipo;
    @Column(name = "comentario", length = 500) private String comentario;
    @Column(name = "latitud") private Double latitud;
    @Column(name = "longitud") private Double longitud;
    @Enumerated(EnumType.STRING) @Column(name = "estado_anterior", nullable = false, length = 30) private EstadoPedido estadoAnterior;
    @Enumerated(EnumType.STRING) @Column(name = "estado_resultante", nullable = false, length = 30) private EstadoPedido estadoResultante;
    @Column(name = "numero_intento") private Integer numeroIntento;
    @Column(name = "fecha", nullable = false) private LocalDateTime fecha;

    protected IncidenciaPedido() {}

    public IncidenciaPedido(Long pedidoId, Long usuarioId, TipoIncidencia tipo, String comentario,
                            UbicacionReportada ubicacion, EstadoPedido estadoAnterior, EstadoPedido estadoResultante,
                            Integer numeroIntento, LocalDateTime fecha) {
        this.pedidoId = pedidoId;
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.comentario = comentario;
        this.latitud = ubicacion.latitud();
        this.longitud = ubicacion.longitud();
        this.estadoAnterior = estadoAnterior;
        this.estadoResultante = estadoResultante;
        this.numeroIntento = numeroIntento;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public Long getPedidoId() { return pedidoId; }
    public Long getUsuarioId() { return usuarioId; }
    public TipoIncidencia getTipo() { return tipo; }
    public String getComentario() { return comentario; }
    public Double getLatitud() { return latitud; }
    public Double getLongitud() { return longitud; }
    public EstadoPedido getEstadoAnterior() { return estadoAnterior; }
    public EstadoPedido getEstadoResultante() { return estadoResultante; }
    public Integer getNumeroIntento() { return numeroIntento; }
    public LocalDateTime getFecha() { return fecha; }
}
