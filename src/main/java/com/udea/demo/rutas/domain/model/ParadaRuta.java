package com.udea.demo.rutas.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Solo la raíz del agregado (Ruta) puede crear o mutar una parada: fábrica y mutadores package-private
@Entity
@Table(name = "paradas_ruta")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParadaRuta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parada")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_ruta", nullable = false)
    private Ruta ruta;

    @Column(name = "id_pedido", nullable = false)
    private Long pedidoId;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoParada estado;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion;

    static ParadaRuta asignar(Ruta ruta, Long pedidoId, int orden) {
        return ParadaRuta.builder()
                .ruta(ruta)
                .pedidoId(pedidoId)
                .orden(orden)
                .estado(EstadoParada.PENDIENTE)
                .fechaAsignacion(LocalDateTime.now())
                .build();
    }

    void cancelar() {
        this.estado = EstadoParada.CANCELADA;
    }

    void actualizarOrden(int nuevoOrden) {
        this.orden = nuevoOrden;
    }
}
