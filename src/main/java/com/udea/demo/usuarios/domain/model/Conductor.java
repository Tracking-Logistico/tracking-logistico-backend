package com.udea.demo.usuarios.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "conductores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Conductor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conductor") private Long id;
    @OneToOne @JoinColumn(name = "id_usuario", nullable = false, unique = true) private Usuario usuario;
    @Column(nullable = false) private String licencia;
    private String estado;
    @Builder.Default @Column(name = "capacidad_max_kg", nullable = false) private Double capacidadMaxKg = 200.0;
    @Builder.Default @Column(name = "capacidad_max_volumen_cm3", nullable = false) private Double capacidadMaxVolumenCm3 = 2_000_000.0;
    @Builder.Default @Column(name = "max_entregas_dia", nullable = false) private Integer maxEntregasDia = 20;
}
