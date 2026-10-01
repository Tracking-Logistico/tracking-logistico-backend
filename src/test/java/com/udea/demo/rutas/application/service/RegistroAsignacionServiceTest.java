package com.udea.demo.rutas.application.service;

import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import com.udea.demo.usuarios.domain.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistroAsignacionServiceTest {
    @Mock JdbcTemplate jdbc;
    @Mock ActorAuthorizationService actores;
    @InjectMocks RegistroAsignacionService registro;

    @Test void asignacionGuardaAuditoriaYAvisoEnMismaOperacion() {
        when(actores.actorActual()).thenReturn(Usuario.builder().id(44L).build());
        registro.registrar(7L, null, 9L, "ASIGNACION", null);
        verify(jdbc).update(startsWith("INSERT INTO historial_asignaciones"),
                eq(7L), isNull(), eq(9L), eq(44L), eq("ASIGNACION"), isNull());
        verify(jdbc).update(startsWith("INSERT INTO notificaciones_conductor"),
                eq(9L), eq(7L), eq("Tienes un nuevo envío asignado"));
    }

    @Test void notificacionesUsanIdUsuarioSinExigirFilaConductor() {
        when(actores.conductorActualUsuarioId()).thenReturn(9L);
        registro.misNotificaciones();
        verify(jdbc).query(startsWith("SELECT id, id_pedido, mensaje"),
                any(org.springframework.jdbc.core.RowMapper.class), eq(9L));
        verify(actores, never()).conductorActualId();
    }

    @Test void marcarLeidaUsaIdUsuarioYNoPerfilConductor() {
        when(actores.conductorActualUsuarioId()).thenReturn(9L);
        when(jdbc.update(anyString(), eq(14L), eq(9L))).thenReturn(1);
        registro.marcarLeida(14L);
        verify(actores, never()).conductorActualId();
    }

    @Test void reasignacionGuardaConductorAnteriorYMotivo() {
        when(actores.actorActual()).thenReturn(Usuario.builder().id(44L).build());
        registro.registrar(7L, 8L, 9L, "REASIGNACION", "Avería");
        verify(jdbc).update(startsWith("INSERT INTO historial_asignaciones"),
                eq(7L), eq(8L), eq(9L), eq(44L), eq("REASIGNACION"), eq("Avería"));
    }
}
