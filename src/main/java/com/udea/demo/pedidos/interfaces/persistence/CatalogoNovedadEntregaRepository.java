package com.udea.demo.pedidos.interfaces.persistence;

import com.udea.demo.pedidos.domain.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CatalogoNovedadEntregaRepository extends JpaRepository<CatalogoNovedadEntrega, Long> {
    List<CatalogoNovedadEntrega> findByActivoTrueOrderByResultadoAscCodigoAsc();
    Optional<CatalogoNovedadEntrega> findByCodigoAndActivoTrue(String codigo);
}
