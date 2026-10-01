-- Datos de despacho completos; nulos únicamente para pedidos históricos anteriores a esta HU.
ALTER TABLE pedidos ADD COLUMN ciudad_origen VARCHAR(100);
ALTER TABLE pedidos ADD COLUMN codigo_postal_origen VARCHAR(12);
ALTER TABLE pedidos ADD COLUMN ciudad_destino VARCHAR(100);
ALTER TABLE pedidos ADD COLUMN codigo_postal_destino VARCHAR(12);
ALTER TABLE pedidos ADD COLUMN remitente_nombre VARCHAR(120);
ALTER TABLE pedidos ADD COLUMN remitente_email VARCHAR(255);
ALTER TABLE pedidos ADD COLUMN remitente_telefono VARCHAR(20);
ALTER TABLE pedidos ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE INDEX idx_pedidos_cliente_fecha ON pedidos(id_cliente, fecha_creacion DESC);
