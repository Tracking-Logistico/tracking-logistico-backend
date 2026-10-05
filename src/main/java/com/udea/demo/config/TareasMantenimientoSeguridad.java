package com.udea.demo.config;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.udea.demo.usuarios.interfaces.persistence.IntentoInicioSesionRepository;
import com.udea.demo.usuarios.interfaces.persistence.SesionUsuarioRepository;

@Component
public class TareasMantenimientoSeguridad {
    private final IntentoInicioSesionRepository intentoRepository;
    private final SesionUsuarioRepository sesionRepository;

    public TareasMantenimientoSeguridad(IntentoInicioSesionRepository intentoRepository,
                                        SesionUsuarioRepository sesionRepository) {
        this.intentoRepository = intentoRepository;
        this.sesionRepository = sesionRepository;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void purgarDatosCaducados() {
        LocalDateTime ahora = LocalDateTime.now();
        intentoRepository.deleteByIntentadoEnBefore(ahora.minusDays(15));
        sesionRepository.deleteByRefreshTokenExpiresAtBefore(ahora);
    }
}