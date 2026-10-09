-- HU-08: resultados de entrega, motivos y coordenadas opcionales.
ALTER TABLE pedidos ADD COLUMN latitud_destino DOUBLE PRECISION;
ALTER TABLE pedidos ADD COLUMN longitud_destino DOUBLE PRECISION;
ALTER TABLE pedidos ADD CONSTRAINT chk_pedidos_latitud_destino
    CHECK (latitud_destino IS NULL OR latitud_destino BETWEEN -90 AND 90);
ALTER TABLE pedidos ADD CONSTRAINT chk_pedidos_longitud_destino
    CHECK (longitud_destino IS NULL OR longitud_destino BETWEEN -180 AND 180);

CREATE TABLE catalogo_novedades_entrega (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    resultado VARCHAR(35) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_catalogo_novedad_resultado CHECK
        (resultado IN ('ENTREGA_FALLIDA', 'DEVOLUCION_AL_REMITENTE'))
);

INSERT INTO catalogo_novedades_entrega (codigo, resultado, descripcion) VALUES
    ('CLIENTE_AUSENTE', 'ENTREGA_FALLIDA', 'Cliente ausente'),
    ('DIRECCION_INCORRECTA', 'ENTREGA_FALLIDA', 'Dirección incorrecta'),
    ('PAQUETE_RECHAZADO', 'DEVOLUCION_AL_REMITENTE', 'Paquete rechazado por el cliente');

CREATE TABLE eventos_entrega (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_evento_cliente VARCHAR(36) NOT NULL UNIQUE,
    id_pedido BIGINT NOT NULL REFERENCES pedidos(id_pedido) ON DELETE RESTRICT,
    id_usuario BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    resultado VARCHAR(35) NOT NULL,
    codigo_novedad VARCHAR(50),
    motivo VARCHAR(500),
    latitud DOUBLE PRECISION,
    longitud DOUBLE PRECISION,
    fecha_evento TIMESTAMP NOT NULL,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_eventos_entrega_resultado CHECK
        (resultado IN ('ENTREGADO', 'ENTREGA_FALLIDA', 'DEVOLUCION_AL_REMITENTE')),
    CONSTRAINT chk_eventos_entrega_latitud CHECK (latitud IS NULL OR latitud BETWEEN -90 AND 90),
    CONSTRAINT chk_eventos_entrega_longitud CHECK (longitud IS NULL OR longitud BETWEEN -180 AND 180)
);

CREATE INDEX idx_eventos_entrega_pedido_fecha ON eventos_entrega(id_pedido, fecha_evento);
