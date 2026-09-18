package com.udea.demo.usuarios.domain.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "intentos_inicio_sesion")
public class IntentoInicioSesion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "intentado_en", nullable = false)
    private LocalDateTime intentadoEn;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    protected IntentoInicioSesion() {
    }

    public IntentoInicioSesion(String email, Usuario usuario, LocalDateTime intentadoEn,
                               LocalDateTime bloqueadoHasta) {
        this.email = email;
        this.usuario = usuario;
        this.intentadoEn = intentadoEn;
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public LocalDateTime getIntentadoEn() { return intentadoEn; }
    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
}