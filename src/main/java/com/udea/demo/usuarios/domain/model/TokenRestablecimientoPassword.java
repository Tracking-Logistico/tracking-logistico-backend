package com.udea.demo.usuarios.domain.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "tokens_restablecimiento_password")
public class TokenRestablecimientoPassword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime expiraEn;

    private LocalDateTime usadoEn;

    protected TokenRestablecimientoPassword() {
    }

    public TokenRestablecimientoPassword(String tokenHash, Usuario usuario, LocalDateTime expiraEn) {
        this.tokenHash = tokenHash;
        this.usuario = usuario;
        this.expiraEn = expiraEn;
    }

    public Usuario getUsuario() { return usuario; }
    public LocalDateTime getExpiraEn() { return expiraEn; }
    public LocalDateTime getUsadoEn() { return usadoEn; }
    public void setUsadoEn(LocalDateTime usadoEn) { this.usadoEn = usadoEn; }
}