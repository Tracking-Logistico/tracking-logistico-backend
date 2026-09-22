package com.udea.demo.rutas.interfaces.persistence;

import com.udea.demo.rutas.domain.model.Ruta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface RutaRepository extends JpaRepository<Ruta, Long> {
    Optional<Ruta> findByConductorIdAndFecha(Long conductorId, LocalDate fecha);
}
