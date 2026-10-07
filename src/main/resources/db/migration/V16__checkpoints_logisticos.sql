-- HU-06: checkpoints logísticos registrados por el conductor (en línea y sincronizados offline).
-- historial_pedidos sigue siendo la línea de tiempo; esta tabla guarda lo que aquella no modela:
-- coordenadas GPS, confiabilidad, clave de idempotencia del dispositivo y estado de revisión.

-- Estados de excepción y cierre del flujo logístico (HU-06 / HU-07).
ALTER TABLE pedidos DROP CONSTRAINT IF EXISTS chk_pedidos_estado;
ALTER TABLE pedidos ADD CONSTRAINT chk_pedidos_estado CHECK (estado IN (
    'SOLICITADO','CORRECCION_SOLICITADA','CREADO','RECIBIDO_EN_ORIGEN','EN_TRANSITO','EN_REPARTO','ENTREGADO',
    'RECHAZADO','ENTREGA_FALLIDA','ENTREGA_REPROGRAMADA','DIRECCION_POR_VERIFICAR','DEVOLUCION_AL_REMITENTE',
    'ENTREGA_FALLIDA_CERRADA'
));

CREATE TABLE checkpoints_pedido (
    id_checkpoint BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pedido BIGINT NOT NULL REFERENCES pedidos(id_pedido) ON DELETE RESTRICT,
    id_usuario BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    id_evento_cliente VARCHAR(36) NOT NULL,
    etapa VARCHAR(30) NOT NULL,
    estado_anterior VARCHAR(30) NOT NULL,
    estado_resultante VARCHAR(30) NOT NULL,
    latitud DOUBLE PRECISION,
    longitud DOUBLE PRECISION,
    precision_metros DOUBLE PRECISION,
    ubicacion_confiable BOOLEAN NOT NULL,
    origen VARCHAR(20) NOT NULL,
    estado_registro VARCHAR(30) NOT NULL,
    motivo_revision VARCHAR(255),
    fecha_evento TIMESTAMP NOT NULL,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_checkpoints_evento_cliente UNIQUE (id_evento_cliente),
    CONSTRAINT chk_checkpoints_etapa CHECK (etapa IN ('RECIBIDO_EN_ORIGEN','EN_TRANSITO','EN_REPARTO')),
    CONSTRAINT chk_checkpoints_origen CHECK (origen IN ('EN_LINEA','OFFLINE')),
    CONSTRAINT chk_checkpoints_estado_registro CHECK (estado_registro IN ('APLICADO','PENDIENTE_REVISION')),
    CONSTRAINT chk_checkpoints_latitud CHECK (latitud IS NULL OR latitud BETWEEN -90 AND 90),
    CONSTRAINT chk_checkpoints_longitud CHECK (longitud IS NULL OR longitud BETWEEN -180 AND 180),
    CONSTRAINT chk_checkpoints_precision CHECK (precision_metros IS NULL OR precision_metros >= 0)
);

CREATE INDEX idx_checkpoints_pedido_fecha ON checkpoints_pedido(id_pedido, fecha_evento);
CREATE INDEX idx_checkpoints_estado_registro ON checkpoints_pedido(estado_registro, fecha_registro);
CREATE INDEX idx_historial_tipo_fecha ON historial_pedidos(tipo_evento, fecha DESC);
