package com.udea.demo.usuarios.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "conductores")
public class Conductor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conductor") private Long id;
    @OneToOne @JoinColumn(name = "id_usuario", nullable = false, unique = true) private Usuario usuario;
    @Column(nullable = false) private String licencia;
    private String estado;
    @Column(name = "capacidad_max_kg", nullable = false) private Double capacidadMaxKg = 200.0;
    @Column(name = "capacidad_max_volumen_cm3", nullable = false) private Double capacidadMaxVolumenCm3 = 2_000_000.0;
    @Column(name = "max_entregas_dia", nullable = false) private Integer maxEntregasDia = 20;

    public Conductor() {}
    public Conductor(Long id, Usuario usuario, String licencia, String estado, Double capacidadMaxKg, Double capacidadMaxVolumenCm3, Integer maxEntregasDia) {
        this.id = id;
        this.usuario = usuario;
        this.licencia = licencia;
        this.estado = estado;
        this.capacidadMaxKg = capacidadMaxKg;
        this.capacidadMaxVolumenCm3 = capacidadMaxVolumenCm3;
        this.maxEntregasDia = maxEntregasDia;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getLicencia() { return licencia; }
    public void setLicencia(String licencia) { this.licencia = licencia; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Double getCapacidadMaxKg() { return capacidadMaxKg; }
    public void setCapacidadMaxKg(Double capacidadMaxKg) { this.capacidadMaxKg = capacidadMaxKg; }
    public Double getCapacidadMaxVolumenCm3() { return capacidadMaxVolumenCm3; }
    public void setCapacidadMaxVolumenCm3(Double capacidadMaxVolumenCm3) { this.capacidadMaxVolumenCm3 = capacidadMaxVolumenCm3; }
    public Integer getMaxEntregasDia() { return maxEntregasDia; }
    public void setMaxEntregasDia(Integer maxEntregasDia) { this.maxEntregasDia = maxEntregasDia; }

    public static ConductorBuilder builder() { return new ConductorBuilder(); }
    public static class ConductorBuilder {
        private Long id;
        private Usuario usuario;
        private String licencia;
        private String estado;
        private Double capacidadMaxKg;
        private boolean capacidadMaxKgSet;
        private Double capacidadMaxVolumenCm3;
        private boolean capacidadMaxVolumenCm3Set;
        private Integer maxEntregasDia;
        private boolean maxEntregasDiaSet;

        public ConductorBuilder id(Long id) { this.id = id; return this; }
        public ConductorBuilder usuario(Usuario usuario) { this.usuario = usuario; return this; }
        public ConductorBuilder licencia(String licencia) { this.licencia = licencia; return this; }
        public ConductorBuilder estado(String estado) { this.estado = estado; return this; }
        public ConductorBuilder capacidadMaxKg(Double capacidadMaxKg) { this.capacidadMaxKg = capacidadMaxKg; this.capacidadMaxKgSet = true; return this; }
        public ConductorBuilder capacidadMaxVolumenCm3(Double capacidadMaxVolumenCm3) { this.capacidadMaxVolumenCm3 = capacidadMaxVolumenCm3; this.capacidadMaxVolumenCm3Set = true; return this; }
        public ConductorBuilder maxEntregasDia(Integer maxEntregasDia) { this.maxEntregasDia = maxEntregasDia; this.maxEntregasDiaSet = true; return this; }
        public Conductor build() {
            Double capacidadMaxKgValue = capacidadMaxKgSet ? capacidadMaxKg : 200.0;
            Double capacidadMaxVolumenCm3Value = capacidadMaxVolumenCm3Set ? capacidadMaxVolumenCm3 : 2_000_000.0;
            Integer maxEntregasDiaValue = maxEntregasDiaSet ? maxEntregasDia : 20;
            return new Conductor(id, usuario, licencia, estado, capacidadMaxKgValue, capacidadMaxVolumenCm3Value, maxEntregasDiaValue);
        }
    }
}
