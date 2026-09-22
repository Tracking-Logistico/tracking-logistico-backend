package com.udea.demo.usuarios.interfaces.persistence;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.udea.demo.usuarios.domain.model.IntentoInicioSesion;

public interface IntentoInicioSesionRepository extends JpaRepository<IntentoInicioSesion, Long> {
    long countByEmailAndIntentadoEnAfter(String email, LocalDateTime desde);
    IntentoInicioSesion findFirstByEmailAndBloqueadoHastaAfterOrderByBloqueadoHastaDesc(
            String email, LocalDateTime ahora);
    void deleteByEmail(String email);
}