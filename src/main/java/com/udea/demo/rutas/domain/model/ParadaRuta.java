package com.udea.demo.rutas.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Solo la raíz del agregado (Ruta) puede crear o mutar una parada: fábrica y mutadores package-private
@Entity
@Table(name = "paradas_ruta")
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


    public ParadaRuta() {}
    public ParadaRuta(Long id, Ruta ruta, Long pedidoId, Integer orden, EstadoParada estado, LocalDateTime fechaAsignacion) {
        this.id = id;
        this.ruta = ruta;
        this.pedidoId = pedidoId;
        this.orden = orden;
        this.estado = estado;
        this.fechaAsignacion = fechaAsignacion;
    }

    public Long getId() { return id; }
    public Ruta getRuta() { return ruta; }
    public Long getPedidoId() { return pedidoId; }
    public Integer getOrden() { return orden; }
    public EstadoParada getEstado() { return estado; }
    public LocalDateTime getFechaAsignacion() { return fechaAsignacion; }

    public static ParadaRutaBuilder builder() { return new ParadaRutaBuilder(); }
    public static class ParadaRutaBuilder {
        private Long id;
        private Ruta ruta;
        private Long pedidoId;
        private Integer orden;
        private EstadoParada estado;
        private LocalDateTime fechaAsignacion;

        public ParadaRutaBuilder id(Long id) { this.id = id; return this; }
        public ParadaRutaBuilder ruta(Ruta ruta) { this.ruta = ruta; return this; }
        public ParadaRutaBuilder pedidoId(Long pedidoId) { this.pedidoId = pedidoId; return this; }
        public ParadaRutaBuilder orden(Integer orden) { this.orden = orden; return this; }
        public ParadaRutaBuilder estado(EstadoParada estado) { this.estado = estado; return this; }
        public ParadaRutaBuilder fechaAsignacion(LocalDateTime fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; return this; }
        public ParadaRuta build() {
            return new ParadaRuta(id, ruta, pedidoId, orden, estado, fechaAsignacion);
        }
    }
}
