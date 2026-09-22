package com.udea.demo.rutas.interfaces.persistence;

import com.udea.demo.rutas.domain.model.EstadoParada;
import com.udea.demo.rutas.domain.model.ParadaRuta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParadaRutaRepository extends JpaRepository<ParadaRuta, Long> {

    Optional<ParadaRuta> findByPedidoIdAndEstado(Long pedidoId, EstadoParada estado);

    @Query("select p.pedidoId from ParadaRuta p where p.estado = :estado")
    List<Long> findPedidoIdsByEstado(EstadoParada estado);
}
