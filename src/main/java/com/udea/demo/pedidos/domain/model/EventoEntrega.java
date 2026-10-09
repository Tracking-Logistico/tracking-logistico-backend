package com.udea.demo.pedidos.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_entrega")
public class EventoEntrega {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "id_evento_cliente", nullable = false, unique = true, length = 36) private String idEventoCliente;
    @Column(name = "id_pedido", nullable = false) private Long pedidoId;
    @Column(name = "id_usuario", nullable = false) private Long usuarioId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 35) private ResultadoEntrega resultado;
    @Column(name = "codigo_novedad", length = 50) private String codigoNovedad;
    @Column(length = 500) private String motivo;
    private Double latitud;
    private Double longitud;
    @Column(name = "fecha_evento", nullable = false) private LocalDateTime fechaEvento;
    @Column(name = "fecha_registro", nullable = false) private LocalDateTime fechaRegistro;

    protected EventoEntrega() {}
    public EventoEntrega(String idEventoCliente, Long pedidoId, Long usuarioId, ResultadoEntrega resultado,
                         String codigoNovedad, String motivo, Double latitud, Double longitud,
                         LocalDateTime fechaEvento) {
        this.idEventoCliente = idEventoCliente; this.pedidoId = pedidoId; this.usuarioId = usuarioId;
        this.resultado = resultado; this.codigoNovedad = codigoNovedad; this.motivo = motivo;
        this.latitud = latitud; this.longitud = longitud; this.fechaEvento = fechaEvento;
        this.fechaRegistro = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public String getIdEventoCliente() { return idEventoCliente; }
    public Long getPedidoId() { return pedidoId; }
    public Long getUsuarioId() { return usuarioId; }
    public ResultadoEntrega getResultado() { return resultado; }
}
