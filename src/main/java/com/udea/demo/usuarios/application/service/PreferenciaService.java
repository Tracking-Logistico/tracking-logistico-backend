package com.udea.demo.usuarios.application.service;

import com.udea.demo.usuarios.application.dto.ActualizarPreferenciaRequestDTO;
import com.udea.demo.usuarios.application.dto.PreferenciaResponseDTO;
import com.udea.demo.usuarios.domain.model.CanalNotificacion;
import com.udea.demo.usuarios.domain.model.PreferenciaNotificacion;
import com.udea.demo.usuarios.interfaces.persistence.PreferenciaNotificacionRepository;
import com.udea.demo.usuarios.interfaces.services.PreferenciaServiceI;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreferenciaService implements PreferenciaServiceI {

    private final PreferenciaNotificacionRepository repository;
    private final ActorAuthorizationService actores;

    public PreferenciaService(PreferenciaNotificacionRepository repository,
                              ActorAuthorizationService actores) {
        this.repository = repository;
        this.actores = actores;
    }

    @Override
    @Transactional(readOnly = true)
    public PreferenciaResponseDTO obtener() {
        Long clienteId = actores.clienteActualId();
        return repository.findById(clienteId)
                .map(this::aDTO)
                .orElseGet(() -> new PreferenciaResponseDTO(CanalNotificacion.EMAIL, null, null));
    }

    @Override
    @Transactional
    public PreferenciaResponseDTO actualizar(ActualizarPreferenciaRequestDTO dto) {
        Long clienteId = actores.clienteActualId();
        PreferenciaNotificacion pref = repository.findById(clienteId)
                .orElseGet(() -> new PreferenciaNotificacion(clienteId));
        pref.actualizar(dto.canal(), dto.telefonoSms());
        return aDTO(repository.save(pref));
    }

    private PreferenciaResponseDTO aDTO(PreferenciaNotificacion p) {
        return new PreferenciaResponseDTO(p.getCanal(), p.getTelefonoSms(), p.getFechaActualizacion());
    }
}