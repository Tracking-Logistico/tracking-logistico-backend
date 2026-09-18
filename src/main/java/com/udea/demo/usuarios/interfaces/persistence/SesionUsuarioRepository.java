package com.udea.demo.usuarios.interfaces.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.udea.demo.usuarios.domain.model.SesionUsuario;

public interface SesionUsuarioRepository extends JpaRepository<SesionUsuario, Long> {
    @EntityGraph(attributePaths = "usuario")
    Optional<SesionUsuario> findByAccessTokenHash(String accessTokenHash);

    @EntityGraph(attributePaths = "usuario")
    Optional<SesionUsuario> findByRefreshTokenHash(String refreshTokenHash);
    void deleteByUsuarioId(Long usuarioId);
}