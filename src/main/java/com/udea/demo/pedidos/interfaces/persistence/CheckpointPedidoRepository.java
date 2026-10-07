package com.udea.demo.pedidos.interfaces.persistence;

import com.udea.demo.pedidos.domain.model.CheckpointPedido;
import com.udea.demo.pedidos.domain.model.EstadoRegistroCheckpoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CheckpointPedidoRepository extends JpaRepository<CheckpointPedido, Long> {
    Optional<CheckpointPedido> findByIdEventoCliente(String idEventoCliente);

    List<CheckpointPedido> findByUsuarioIdAndEstadoRegistroOrderByFechaEventoAsc(Long usuarioId,
                                                                                 EstadoRegistroCheckpoint estado);

    Page<CheckpointPedido> findByEstadoRegistroOrderByFechaRegistroDesc(EstadoRegistroCheckpoint estado,
                                                                       Pageable pageable);
}
