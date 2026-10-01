ALTER TABLE rutas ADD COLUMN orden_manual BOOLEAN NOT NULL DEFAULT FALSE;
-- Auditar cada reasignación sin eliminar paradas anteriores ni historial del pedido.
CREATE TABLE historial_asignaciones (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pedido BIGINT NOT NULL REFERENCES pedidos(id_pedido) ON DELETE RESTRICT,
    conductor_anterior_id BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    conductor_nuevo_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    operador_id BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    accion VARCHAR(20) NOT NULL CHECK (accion IN ('ASIGNACION','REASIGNACION')),
    motivo VARCHAR(500),
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_historial_asignaciones_pedido ON historial_asignaciones(id_pedido, fecha);
CREATE TABLE notificaciones_conductor (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario_conductor BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    id_pedido BIGINT NOT NULL REFERENCES pedidos(id_pedido) ON DELETE RESTRICT,
    mensaje VARCHAR(255) NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    leida BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_notificaciones_conductor ON notificaciones_conductor(id_usuario_conductor, fecha DESC);
