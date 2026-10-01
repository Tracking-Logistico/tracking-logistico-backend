-- Alineación de persistencia con las historias de usuario vigentes.
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS justificacion_prioridad VARCHAR(500);

UPDATE pedidos SET prioridad_sugerida = 'ALTA' WHERE prioridad_sugerida = 'URGENTE';
UPDATE pedidos SET prioridad_confirmada = 'ALTA' WHERE prioridad_confirmada = 'URGENTE';
UPDATE pedidos SET estado = 'SOLICITADO' WHERE estado IN ('RECIBIDO', 'EN_VALIDACION', 'VALIDADO');

ALTER TABLE pedidos DROP CONSTRAINT IF EXISTS chk_pedidos_prioridad_sugerida;
ALTER TABLE pedidos DROP CONSTRAINT IF EXISTS chk_pedidos_prioridad_confirmada;
ALTER TABLE pedidos DROP CONSTRAINT IF EXISTS chk_pedidos_estado;
ALTER TABLE pedidos ADD CONSTRAINT chk_pedidos_prioridad_sugerida CHECK (prioridad_sugerida IN ('BAJA','MEDIA','ALTA'));
ALTER TABLE pedidos ADD CONSTRAINT chk_pedidos_prioridad_confirmada CHECK (prioridad_confirmada IS NULL OR prioridad_confirmada IN ('BAJA','MEDIA','ALTA'));
ALTER TABLE pedidos ADD CONSTRAINT chk_pedidos_estado CHECK (estado IN (
    'SOLICITADO','CORRECCION_SOLICITADA','CREADO','RECIBIDO_EN_ORIGEN','EN_TRANSITO','EN_REPARTO','ENTREGADO','RECHAZADO'
));

CREATE TABLE IF NOT EXISTS historial_pedidos (
    id_historial BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pedido BIGINT NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    id_usuario BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    tipo_evento VARCHAR(60) NOT NULL,
    campo_observado VARCHAR(120),
    detalle VARCHAR(500),
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_historial_pedido ON historial_pedidos(id_pedido, fecha);

ALTER TABLE conductores ADD COLUMN IF NOT EXISTS capacidad_max_kg NUMERIC(10,2) NOT NULL DEFAULT 200.00;
ALTER TABLE conductores ADD COLUMN IF NOT EXISTS capacidad_max_volumen_cm3 NUMERIC(14,2) NOT NULL DEFAULT 2000000.00;
ALTER TABLE conductores ADD COLUMN IF NOT EXISTS max_entregas_dia INTEGER NOT NULL DEFAULT 20;
ALTER TABLE conductores DROP CONSTRAINT IF EXISTS chk_conductor_max_entregas;
ALTER TABLE conductores ADD CONSTRAINT chk_conductor_max_entregas CHECK (max_entregas_dia >= 2);

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS acepto_politica_datos BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS fecha_aceptacion_politica_datos TIMESTAMP;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS version_politica_datos VARCHAR(100);

ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS destinatario_nombre VARCHAR(120);
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS destinatario_telefono VARCHAR(20);
