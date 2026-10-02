package com.udea.demo.pedidos.interfaces.persistence;

import com.udea.demo.pedidos.domain.model.EstadoPedido;
import com.udea.demo.pedidos.domain.model.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByEstadoInOrderByFechaCreacionAsc(List<EstadoPedido> estados);

    Page<Pedido> findByEstadoInOrderByFechaCreacionAsc(List<EstadoPedido> estados, Pageable pageable);

    @Query("""
        select p from Pedido p
        where p.estado = :correccion
           or (p.estado = :solicitado and p.fechaValidacion is null)
        order by p.fechaCreacion asc
        """)
    Page<Pedido> findPendientes(@Param("solicitado") EstadoPedido solicitado,
                                @Param("correccion") EstadoPedido correccion,
                                Pageable pageable);

    Page<Pedido> findByEstadoAndFechaValidacionIsNotNullOrderByFechaCreacionAsc(
            EstadoPedido estado, Pageable pageable);

    @Query("""
        select p from Pedido p
        where p.estado = :creado
           or (p.estado = :solicitado and p.fechaValidacion is not null)
        order by p.fechaCreacion asc
        """)
    Page<Pedido> findActivables(@Param("solicitado") EstadoPedido solicitado,
                                @Param("creado") EstadoPedido creado,
                                Pageable pageable);
    List<Pedido> findByClienteIdOrderByFechaCreacionDesc(Long clienteId);
    Optional<Pedido> findByNumeroTracking(String numeroTracking);
    boolean existsByNumeroTracking(String numeroTracking);
    boolean existsByNumeroPedido(String numeroPedido);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> findByIdForUpdate(@Param("id") Long id);
}
