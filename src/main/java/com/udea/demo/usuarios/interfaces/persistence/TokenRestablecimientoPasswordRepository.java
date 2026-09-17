package com.udea.demo.usuarios.interfaces.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.udea.demo.usuarios.domain.model.TokenRestablecimientoPassword;

public interface TokenRestablecimientoPasswordRepository
        extends JpaRepository<TokenRestablecimientoPassword, Long> {
    Optional<TokenRestablecimientoPassword> findByTokenHash(String tokenHash);
    void deleteByUsuarioId(Long usuarioId);
}