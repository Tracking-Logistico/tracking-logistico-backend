package com.udea.demo.usuarios.interfaces.persistence;

import com.udea.demo.usuarios.domain.model.CanalNotificacion;
import com.udea.demo.usuarios.domain.model.NotificacionEnviada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificacionEnviadaRepository extends JpaRepository<NotificacionEnviada, Long> {

    Optional<NotificacionEnviada> findByEventIdAndCanal(String eventId, CanalNotificacion canal);

    Page<NotificacionEnviada> findByClienteIdOrderByFechaCreacionDesc(Long clienteId, Pageable pageable);
}