-- HU-07: incidencias y retrasos de envíos.
-- historial_pedidos registra la novedad en la línea de tiempo; incidencias_pedido conserva el detalle
-- operativo (tipo del catálogo, comentario, ubicación, intento) que el historial no modela.

ALTER TABLE pedidos ADD COLUMN intentos_entrega_fallidos INTEGER NOT NULL DEFAULT 0;
ALTER TABLE pedidos ADD COLUMN fecha_entrega_fallida TIMESTAMP;
ALTER TABLE pedidos ADD COLUMN fecha_entrega_reprogramada DATE;
ALTER TABLE pedidos ADD COLUMN fecha_limite_verificacion_direccion TIMESTAMP;
ALTER TABLE pedidos ADD COLUMN fecha_ultima_incidencia TIMESTAMP;
ALTER TABLE pedidos ADD CONSTRAINT chk_pedidos_intentos_entrega CHECK (intentos_entrega_fallidos >= 0);

-- Tarea programada: envíos con dirección por verificar cuyo plazo venció.
CREATE INDEX idx_pedidos_verificacion_direccion ON pedidos(fecha_limite_verificacion_direccion)
    WHERE estado = 'DIRECCION_POR_VERIFICAR';

CREATE TABLE incidencias_pedido (
    id_incidencia BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pedido BIGINT NOT NULL REFERENCES pedidos(id_pedido) ON DELETE RESTRICT,
    id_usuario BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    tipo VARCHAR(40) NOT NULL,
    comentario VARCHAR(500),
    latitud DOUBLE PRECISION,
    longitud DOUBLE PRECISION,
    estado_anterior VARCHAR(30) NOT NULL,
    estado_resultante VARCHAR(30) NOT NULL,
    numero_intento INTEGER,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_incidencias_tipo CHECK (tipo IN ('DIRECCION_INCORRECTA','CLIENTE_AUSENTE','PAQUETE_DANADO',
        'PAQUETE_RECHAZADO','RETRASO_OPERATIVO','OTRO')),
    CONSTRAINT chk_incidencias_comentario CHECK (tipo NOT IN ('OTRO','PAQUETE_DANADO')
        OR (comentario IS NOT NULL AND length(trim(comentario)) > 0)),
    CONSTRAINT chk_incidencias_latitud CHECK (latitud IS NULL OR latitud BETWEEN -90 AND 90),
    CONSTRAINT chk_incidencias_longitud CHECK (longitud IS NULL OR longitud BETWEEN -180 AND 180),
    CONSTRAINT chk_incidencias_intento CHECK (numero_intento IS NULL OR numero_intento > 0)
);

CREATE INDEX idx_incidencias_pedido_fecha ON incidencias_pedido(id_pedido, fecha);
