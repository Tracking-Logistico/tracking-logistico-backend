package com.udea.demo.rutas.application.service;
import com.udea.demo.rutas.application.dto.*;
import com.udea.demo.usuarios.application.service.ActorAuthorizationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class RegistroAsignacionService {
    private final JdbcTemplate jdbc;
    private final ActorAuthorizationService actores;
    public RegistroAsignacionService(JdbcTemplate jdbc, ActorAuthorizationService actores) {
        this.jdbc = jdbc; this.actores = actores;
    }
    public void registrar(Long pedido, Long anteriorUsuario, Long nuevoUsuario, String accion, String motivo) {
        Long operador = actores.actorActual().getId();
        jdbc.update("INSERT INTO historial_asignaciones (id_pedido, conductor_anterior_id, conductor_nuevo_id, operador_id, accion, motivo) VALUES (?,?,?,?,?,?)",
                pedido, anteriorUsuario, nuevoUsuario, operador, accion, motivo);
        jdbc.update("INSERT INTO notificaciones_conductor (id_usuario_conductor, id_pedido, mensaje) VALUES (?,?,?)",
                nuevoUsuario, pedido, "ASIGNACION".equals(accion) ? "Tienes un nuevo envío asignado" : "Un envío ha sido reasignado a tu ruta");
    }
    @Transactional(readOnly = true)
    public List<NotificacionRutaDTO> misNotificaciones() {
        Long usuario = actores.conductorActualUsuarioId();
        return jdbc.query("SELECT id, id_pedido, mensaje, fecha, leida FROM notificaciones_conductor WHERE id_usuario_conductor = ? ORDER BY fecha DESC, id DESC LIMIT 100",
                (rs, row) -> new NotificacionRutaDTO(rs.getLong(1), rs.getLong(2), rs.getString(3),
                        rs.getTimestamp(4).toLocalDateTime(), rs.getBoolean(5)), usuario);
    }
    @Transactional
    public void marcarLeida(Long id) {
        Long usuario = actores.conductorActualUsuarioId();
        int filas = jdbc.update("UPDATE notificaciones_conductor SET leida=true WHERE id=? AND id_usuario_conductor=?",
                id, usuario);
        if (filas == 0) throw new AccessDeniedException("No existe una notificación propia con ese identificador");
    }
    @Transactional(readOnly = true)
    public List<HistorialAsignacionDTO> historial(Long pedidoId) {
        actores.operadorActualId();
        return jdbc.query("SELECT id, id_pedido, conductor_anterior_id, conductor_nuevo_id, operador_id, accion, motivo, fecha FROM historial_asignaciones WHERE id_pedido=? ORDER BY fecha, id",
                (rs, row) -> new HistorialAsignacionDTO(rs.getLong(1), rs.getLong(2),
                        (Long) rs.getObject(3), rs.getLong(4), rs.getLong(5), rs.getString(6), rs.getString(7),
                        rs.getTimestamp(8).toLocalDateTime()), pedidoId);
    }
}
