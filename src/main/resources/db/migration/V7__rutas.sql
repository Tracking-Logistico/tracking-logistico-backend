-- ============================================================
-- RUTAS: organización y asignación de envíos a conductores
-- ============================================================

CREATE TABLE IF NOT EXISTS rutas (
    id_ruta BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    id_conductor BIGINT NOT NULL,
    fecha DATE NOT NULL,

    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_rutas_conductor_fecha UNIQUE (id_conductor, fecha),

    CONSTRAINT fk_rutas_conductor
        FOREIGN KEY (id_conductor)
        REFERENCES conductores(id_conductor)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS paradas_ruta (
    id_parada BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    id_ruta BIGINT NOT NULL,
    id_pedido BIGINT NOT NULL,

    orden INT NOT NULL,
    estado VARCHAR(30) NOT NULL,

    fecha_asignacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_paradas_ruta_ruta
        FOREIGN KEY (id_ruta)
        REFERENCES rutas(id_ruta)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_paradas_ruta_pedido
        FOREIGN KEY (id_pedido)
        REFERENCES pedidos(id_pedido)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT chk_paradas_ruta_estado
        CHECK (estado IN ('PENDIENTE', 'ENTREGADO', 'CANCELADA'))
);

CREATE INDEX IF NOT EXISTS idx_paradas_ruta_pedido ON paradas_ruta(id_pedido);
CREATE INDEX IF NOT EXISTS idx_paradas_ruta_estado ON paradas_ruta(estado);
