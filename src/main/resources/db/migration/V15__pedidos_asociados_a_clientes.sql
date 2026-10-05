ALTER TABLE pedidos ADD COLUMN destinatario_email VARCHAR(255);
ALTER TABLE pedidos ADD COLUMN fecha_estimada_entrega TIMESTAMP;

UPDATE pedidos
SET fecha_estimada_entrega = fecha_creacion
    + CASE WHEN tipo_servicio = 'EXPRESS' THEN INTERVAL '1 day' ELSE INTERVAL '3 days' END
WHERE fecha_estimada_entrega IS NULL;

CREATE INDEX idx_pedidos_destinatario_email ON pedidos(destinatario_email);
