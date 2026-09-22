package com.udea.demo.usuarios.interfaces.persistence;

import com.udea.demo.usuarios.domain.model.Operador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OperadorRepository extends JpaRepository<Operador, Long> {
    Optional<Operador> findByUsuarioId(Long usuarioId);
    void deleteByUsuarioId(Long usuarioId);
    boolean existsByCodigoEmpleado(String codigoEmpleado);
}
