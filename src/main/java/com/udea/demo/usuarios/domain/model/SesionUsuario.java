package com.udea.demo.usuarios.domain.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "sesiones_usuario")
public class SesionUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "access_token_hash", nullable = false, unique = true, length = 64)
    private String accessTokenHash;

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 64)
    private String refreshTokenHash;

    @Column(name = "access_token_expires_at", nullable = false)
    private LocalDateTime accessTokenExpiresAt;

    @Column(name = "refresh_token_expires_at", nullable = false)
    private LocalDateTime refreshTokenExpiresAt;

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;

    private LocalDateTime revokedAt;

    protected SesionUsuario() {
    }

    public SesionUsuario(Usuario usuario, String accessTokenHash, String refreshTokenHash,
                         LocalDateTime accessTokenExpiresAt, LocalDateTime refreshTokenExpiresAt,
                         LocalDateTime lastActivityAt) {
        this.usuario = usuario;
        this.accessTokenHash = accessTokenHash;
        this.refreshTokenHash = refreshTokenHash;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        this.lastActivityAt = lastActivityAt;
    }

    public Usuario getUsuario() { return usuario; }
    public String getAccessTokenHash() { return accessTokenHash; }
    public String getRefreshTokenHash() { return refreshTokenHash; }
    public LocalDateTime getAccessTokenExpiresAt() { return accessTokenExpiresAt; }
    public LocalDateTime getRefreshTokenExpiresAt() { return refreshTokenExpiresAt; }
    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
}