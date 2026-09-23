package com.udea.demo.usuarios.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tokens_verificacion")
public class TokenVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime fechaExpiracion;

    public boolean estaExpirado() {
        return LocalDateTime.now().isAfter(fechaExpiracion);
    }


    public TokenVerificacion() {}
    public TokenVerificacion(Long id, String token, Usuario usuario, LocalDateTime fechaExpiracion) {
        this.id = id;
        this.token = token;
        this.usuario = usuario;
        this.fechaExpiracion = fechaExpiracion;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDateTime getFechaExpiracion() { return fechaExpiracion; }
    public void setFechaExpiracion(LocalDateTime fechaExpiracion) { this.fechaExpiracion = fechaExpiracion; }

    public static TokenVerificacionBuilder builder() { return new TokenVerificacionBuilder(); }
    public static class TokenVerificacionBuilder {
        private Long id;
        private String token;
        private Usuario usuario;
        private LocalDateTime fechaExpiracion;

        public TokenVerificacionBuilder id(Long id) { this.id = id; return this; }
        public TokenVerificacionBuilder token(String token) { this.token = token; return this; }
        public TokenVerificacionBuilder usuario(Usuario usuario) { this.usuario = usuario; return this; }
        public TokenVerificacionBuilder fechaExpiracion(LocalDateTime fechaExpiracion) { this.fechaExpiracion = fechaExpiracion; return this; }
        public TokenVerificacion build() {
            return new TokenVerificacion(id, token, usuario, fechaExpiracion);
        }
    }
}
