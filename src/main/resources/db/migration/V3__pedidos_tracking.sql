-- ============================================================
-- PEDIDOS: activación de tracking y etiqueta de envío
-- ============================================================

ALTER TABLE pedidos ADD COLUMN numero_tracking VARCHAR(50) UNIQUE;

ALTER TABLE pedidos ADD COLUMN fecha_activacion_tracking TIMESTAMP;

ALTER TABLE pedidos ADD COLUMN etiqueta_impresa BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE pedidos ADD COLUMN fecha_impresion_etiqueta TIMESTAMP;

ALTER TABLE pedidos DROP CONSTRAINT chk_pedidos_estado;

ALTER TABLE pedidos ADD CONSTRAINT chk_pedidos_estado
    CHECK (estado IN ('RECIBIDO', 'EN_VALIDACION', 'VALIDADO', 'RECHAZADO', 'EN_TRANSITO'));
