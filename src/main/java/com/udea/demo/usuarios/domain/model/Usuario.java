package com.udea.demo.usuarios.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String telefono;
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoUsuario estado;

    @Column(nullable = false)
    private Boolean aceptoTerminos;

    private LocalDateTime fechaAceptacionTerminos;
    private String versionTerminos;

    @Builder.Default
    @Column(nullable = false)
    private Boolean aceptoPoliticaDatos = false;
    private LocalDateTime fechaAceptacionPoliticaDatos;
    private String versionPoliticaDatos;

    @Builder.Default
    private Boolean activo = true;

    private LocalDateTime fechaCreacion;

    @PrePersist
    public void prePersist() {
        this.fechaCreacion = LocalDateTime.now();
    }
}