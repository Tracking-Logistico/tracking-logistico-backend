-- ============================================================
-- PEDIDOS (bandeja de entrada de operador logístico)
-- ============================================================

CREATE TABLE pedidos (
    id_pedido BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    numero_pedido VARCHAR(50) NOT NULL UNIQUE,

    id_cliente BIGINT NOT NULL,

    direccion_origen VARCHAR(250) NOT NULL,
    direccion_destino VARCHAR(250) NOT NULL,

    descripcion_paquete VARCHAR(255) NOT NULL,

    peso_kg NUMERIC(10,2) NOT NULL,
    largo_cm NUMERIC(10,2) NOT NULL,
    ancho_cm NUMERIC(10,2) NOT NULL,
    alto_cm NUMERIC(10,2) NOT NULL,

    tipo_servicio VARCHAR(30) NOT NULL,
    prioridad_sugerida VARCHAR(30) NOT NULL,
    prioridad_confirmada VARCHAR(30),
    estado VARCHAR(30) NOT NULL,

    observaciones_validacion VARCHAR(500),
    id_operador_validador BIGINT,

    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_validacion TIMESTAMP,

    CONSTRAINT fk_pedidos_cliente
        FOREIGN KEY (id_cliente)
        REFERENCES clientes(id_cliente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_pedidos_operador_validador
        FOREIGN KEY (id_operador_validador)
        REFERENCES operadores(id_operador)
        ON UPDATE CASCADE
        ON DELETE SET NULL,

    CONSTRAINT chk_pedidos_tipo_servicio
        CHECK (tipo_servicio IN ('ESTANDAR', 'EXPRESS', 'PROGRAMADO')),

    CONSTRAINT chk_pedidos_prioridad_sugerida
        CHECK (prioridad_sugerida IN ('BAJA', 'MEDIA', 'ALTA', 'URGENTE')),

    CONSTRAINT chk_pedidos_prioridad_confirmada
        CHECK (prioridad_confirmada IS NULL OR prioridad_confirmada IN ('BAJA', 'MEDIA', 'ALTA', 'URGENTE')),

    CONSTRAINT chk_pedidos_estado
        CHECK (estado IN ('RECIBIDO', 'EN_VALIDACION', 'VALIDADO', 'RECHAZADO')),

    CONSTRAINT chk_pedidos_peso
        CHECK (peso_kg > 0),

    CONSTRAINT chk_pedidos_largo
        CHECK (largo_cm > 0),

    CONSTRAINT chk_pedidos_ancho
        CHECK (ancho_cm > 0),

    CONSTRAINT chk_pedidos_alto
        CHECK (alto_cm > 0)
);

CREATE INDEX idx_pedidos_estado ON pedidos(estado);
