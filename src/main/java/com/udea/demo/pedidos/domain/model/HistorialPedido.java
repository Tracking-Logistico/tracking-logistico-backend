package com.udea.demo.pedidos.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_pedidos")
public class HistorialPedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial") private Long id;
    @Column(name = "id_pedido", nullable = false) private Long pedidoId;
    @Column(name = "id_usuario") private Long usuarioId;
    @Column(name = "tipo_evento", nullable = false, length = 60) private String tipoEvento;
    @Column(name = "campo_observado", length = 120) private String campoObservado;
    @Column(name = "detalle", length = 500) private String detalle;
    @Column(name = "fecha", nullable = false) private LocalDateTime fecha;

    public HistorialPedido() {}
    public HistorialPedido(Long id, Long pedidoId, Long usuarioId, String tipoEvento, String campoObservado, String detalle, LocalDateTime fecha) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.usuarioId = usuarioId;
        this.tipoEvento = tipoEvento;
        this.campoObservado = campoObservado;
        this.detalle = detalle;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }
    public String getCampoObservado() { return campoObservado; }
    public void setCampoObservado(String campoObservado) { this.campoObservado = campoObservado; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public static HistorialPedidoBuilder builder() { return new HistorialPedidoBuilder(); }
    public static class HistorialPedidoBuilder {
        private Long id;
        private Long pedidoId;
        private Long usuarioId;
        private String tipoEvento;
        private String campoObservado;
        private String detalle;
        private LocalDateTime fecha;

        public HistorialPedidoBuilder id(Long id) { this.id = id; return this; }
        public HistorialPedidoBuilder pedidoId(Long pedidoId) { this.pedidoId = pedidoId; return this; }
        public HistorialPedidoBuilder usuarioId(Long usuarioId) { this.usuarioId = usuarioId; return this; }
        public HistorialPedidoBuilder tipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; return this; }
        public HistorialPedidoBuilder campoObservado(String campoObservado) { this.campoObservado = campoObservado; return this; }
        public HistorialPedidoBuilder detalle(String detalle) { this.detalle = detalle; return this; }
        public HistorialPedidoBuilder fecha(LocalDateTime fecha) { this.fecha = fecha; return this; }
        public HistorialPedido build() {
            return new HistorialPedido(id, pedidoId, usuarioId, tipoEvento, campoObservado, detalle, fecha);
        }
    }
}
