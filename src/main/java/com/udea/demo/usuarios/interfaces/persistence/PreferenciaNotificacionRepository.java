package com.udea.demo.usuarios.interfaces.persistence;

import com.udea.demo.usuarios.domain.model.PreferenciaNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreferenciaNotificacionRepository extends JpaRepository<PreferenciaNotificacion, Long> {
}