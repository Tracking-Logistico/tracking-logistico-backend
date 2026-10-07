package com.udea.demo.pedidos.interfaces.persistence;

import com.udea.demo.pedidos.domain.model.IncidenciaPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncidenciaPedidoRepository extends JpaRepository<IncidenciaPedido, Long> {
    List<IncidenciaPedido> findByPedidoIdOrderByFechaAscIdAsc(Long pedidoId);

    Optional<IncidenciaPedido> findFirstByPedidoIdOrderByFechaDescIdDesc(Long pedidoId);
}
