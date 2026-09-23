package com.udea.demo.pedidos.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_pedidos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HistorialPedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial") private Long id;
    @Column(name = "id_pedido", nullable = false) private Long pedidoId;
    @Column(name = "id_usuario") private Long usuarioId;
    @Column(name = "tipo_evento", nullable = false, length = 60) private String tipoEvento;
    @Column(name = "campo_observado", length = 120) private String campoObservado;
    @Column(name = "detalle", length = 500) private String detalle;
    @Column(name = "fecha", nullable = false) private LocalDateTime fecha;
}
