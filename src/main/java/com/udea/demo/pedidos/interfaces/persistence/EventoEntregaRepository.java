package com.udea.demo.pedidos.interfaces.persistence;

import com.udea.demo.pedidos.domain.model.EventoEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EventoEntregaRepository extends JpaRepository<EventoEntrega, Long> {
    Optional<EventoEntrega> findByIdEventoCliente(String idEventoCliente);
}
