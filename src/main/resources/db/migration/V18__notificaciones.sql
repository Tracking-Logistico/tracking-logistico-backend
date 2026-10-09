-- HU-11: notificaciones automáticas de cambio de estado.

-- 1. Habilitar la etapa ENTREGADO para que el conductor pueda escanear la entrega final.
--    El CHECK actual solo permite 3 etapas; sin ENTREGADO no hay evento del hito "Entregado".
ALTER TABLE checkpoints_pedido DROP CONSTRAINT IF EXISTS chk_checkpoints_etapa;
ALTER TABLE checkpoints_pedido ADD CONSTRAINT chk_checkpoints_etapa
    CHECK (etapa IN ('RECIBIDO_EN_ORIGEN','EN_TRANSITO','EN_REPARTO','ENTREGADO'));

-- 2. Preferencias de notificación: una fila por cliente. Ausencia de fila = EMAIL por defecto.
CREATE TABLE preferencias_notificacion (
    id_cliente BIGINT PRIMARY KEY REFERENCES clientes(id_cliente) ON DELETE CASCADE,
    canal VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    telefono_sms VARCHAR(20),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pref_canal CHECK (canal IN ('EMAIL','SMS','AMBOS'))
);

-- 3. Historial de notificaciones. event_id + canal es UNIQUE para idempotencia:
--    un mismo evento RabbitMQ no se procesa dos veces por el mismo canal.
CREATE TABLE notificaciones_enviadas (
    id_notificacion BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente BIGINT NOT NULL,
    id_pedido BIGINT,
    numero_tracking VARCHAR(40),
    event_id VARCHAR(36) NOT NULL,
    tipo_evento VARCHAR(60) NOT NULL,
    canal VARCHAR(20) NOT NULL,
    destinatario VARCHAR(200) NOT NULL,
    asunto VARCHAR(200),
    contenido TEXT,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos INTEGER NOT NULL DEFAULT 0,
    motivo_fallo VARCHAR(500),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_ultimo_intento TIMESTAMP,
    CONSTRAINT uk_notif_event_canal UNIQUE (event_id, canal),
    CONSTRAINT chk_notif_canal CHECK (canal IN ('EMAIL','SMS')),
    CONSTRAINT chk_notif_estado CHECK (estado IN ('PENDIENTE','ENVIADA','FALLIDA','DESCARTADA'))
);

CREATE INDEX idx_notif_cliente ON notificaciones_enviadas(id_cliente, fecha_creacion DESC);
CREATE INDEX idx_notif_pedido  ON notificaciones_enviadas(id_pedido, fecha_creacion DESC);
CREATE INDEX idx_notif_estado  ON notificaciones_enviadas(estado, fecha_creacion DESC);