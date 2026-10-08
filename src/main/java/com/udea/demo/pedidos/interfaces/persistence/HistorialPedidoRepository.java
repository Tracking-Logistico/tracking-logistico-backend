package com.udea.demo.pedidos.interfaces.persistence;

import com.udea.demo.pedidos.domain.model.HistorialPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialPedidoRepository extends JpaRepository<HistorialPedido, Long> {
    List<HistorialPedido> findByPedidoIdOrderByFechaAsc(Long pedidoId);

    Page<HistorialPedido> findByTipoEventoInOrderByFechaDescIdDesc(List<String> tiposEvento, Pageable pageable);
}
