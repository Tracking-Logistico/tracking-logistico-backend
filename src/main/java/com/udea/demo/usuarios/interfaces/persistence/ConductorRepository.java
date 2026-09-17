package com.udea.demo.usuarios.interfaces.persistence;

import com.udea.demo.usuarios.domain.model.Conductor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConductorRepository extends JpaRepository<Conductor, Long> {
    Optional<Conductor> findByUsuarioId(Long usuarioId);
    void deleteByUsuarioId(Long usuarioId);
}