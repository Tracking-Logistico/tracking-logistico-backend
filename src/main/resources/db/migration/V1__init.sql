-- ============================================================
-- SISTEMA DE LOGÍSTICA Y ENVÍOS
-- POSTGRESQL
-- ============================================================


-- ============================================================
-- USUARIOS
-- ============================================================

CREATE TABLE usuarios (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,

    telefono VARCHAR(20),
    direccion VARCHAR(200),

    rol VARCHAR(20) NOT NULL,
    estado VARCHAR(30) NOT NULL,

    acepto_terminos BOOLEAN NOT NULL,
    fecha_aceptacion_terminos TIMESTAMP,
    version_terminos VARCHAR(30),

    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_usuarios_rol
        CHECK (
            rol IN (
                'CLIENTE',
                'OPERADOR',
                'CONDUCTOR'
            )
        ),

    CONSTRAINT chk_usuarios_estado
        CHECK (
            estado IN (
                'PENDIENTE_VERIFICACION',
                'PENDIENTE_ACTIVACION',
                'ACTIVO',
                'INACTIVO',
                'BLOQUEADO'
            )
        ),

    CONSTRAINT chk_usuarios_terminos
        CHECK (
            acepto_terminos = FALSE
            OR fecha_aceptacion_terminos IS NOT NULL
        )
);


-- ============================================================
-- CLIENTES
-- ============================================================

CREATE TABLE clientes (
    id_cliente BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    id_usuario BIGINT NOT NULL UNIQUE,

    ciudad VARCHAR(100),

    CONSTRAINT fk_clientes_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuarios(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);


-- ============================================================
-- CONDUCTORES
-- ============================================================

CREATE TABLE conductores (
    id_conductor BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    id_usuario BIGINT NOT NULL UNIQUE,

    licencia VARCHAR(50) NOT NULL,
    estado VARCHAR(30),

    CONSTRAINT fk_conductores_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuarios(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);


-- ============================================================
-- OPERADORES
-- ============================================================

CREATE TABLE operadores (
    id_operador BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    id_usuario BIGINT NOT NULL UNIQUE,

    codigo_empleado VARCHAR(50) UNIQUE,

    CONSTRAINT fk_operadores_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuarios(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);


-- ============================================================
-- TOKENS DE VERIFICACIÓN
-- ============================================================

CREATE TABLE tokens_verificacion (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    token VARCHAR(255) NOT NULL UNIQUE,

    usuario_id BIGINT NOT NULL UNIQUE,

    fecha_expiracion TIMESTAMP NOT NULL,

    CONSTRAINT fk_tokens_verificacion_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);


-- ============================================================
-- TIPOS DE SERVICIO
-- ============================================================

CREATE TABLE tipos_servicio (
    id_tipo_servicio BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(200),

    peso_maximo_kg NUMERIC(10,2),
    largo_maximo_cm NUMERIC(10,2),
    ancho_maximo_cm NUMERIC(10,2),
    alto_maximo_cm NUMERIC(10,2),

    prioridad VARCHAR(30),

    CONSTRAINT chk_tipos_servicio_peso
        CHECK (
            peso_maximo_kg IS NULL
            OR peso_maximo_kg > 0
        ),

    CONSTRAINT chk_tipos_servicio_largo
        CHECK (
            largo_maximo_cm IS NULL
            OR largo_maximo_cm > 0
        ),

    CONSTRAINT chk_tipos_servicio_ancho
        CHECK (
            ancho_maximo_cm IS NULL
            OR ancho_maximo_cm > 0
        ),

    CONSTRAINT chk_tipos_servicio_alto
        CHECK (
            alto_maximo_cm IS NULL
            OR alto_maximo_cm > 0
        )
);


-- ============================================================
-- ESTADOS DE ENVÍO
-- ============================================================

CREATE TABLE estados_envio (
    id_estado BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    nombre_estado VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(200)
);


-- ============================================================
-- PAQUETES
-- ============================================================

CREATE TABLE paquetes (
    id_paquete BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    peso_kg NUMERIC(10,2) NOT NULL,
    largo_cm NUMERIC(10,2) NOT NULL,
    ancho_cm NUMERIC(10,2) NOT NULL,
    alto_cm NUMERIC(10,2) NOT NULL,

    descripcion VARCHAR(255),

    requiere_edad BOOLEAN NOT NULL DEFAULT FALSE,
    edad_minima INTEGER,

    CONSTRAINT chk_paquetes_peso
        CHECK (peso_kg > 0),

    CONSTRAINT chk_paquetes_largo
        CHECK (largo_cm > 0),

    CONSTRAINT chk_paquetes_ancho
        CHECK (ancho_cm > 0),

    CONSTRAINT chk_paquetes_alto
        CHECK (alto_cm > 0),

    CONSTRAINT chk_paquetes_edad
        CHECK (
            requiere_edad = FALSE
            OR (
                requiere_edad = TRUE
                AND edad_minima IS NOT NULL
                AND edad_minima >= 0
            )
        )
);


-- ============================================================
-- ENVÍOS
-- ============================================================

CREATE TABLE envios (
    id_envio BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    numero_pedido VARCHAR(50) NOT NULL UNIQUE,
    numero_tracking VARCHAR(100) NOT NULL UNIQUE,

    id_remitente BIGINT NOT NULL,
    id_destinatario BIGINT NOT NULL,

    id_paquete BIGINT NOT NULL UNIQUE,
    id_tipo_servicio BIGINT NOT NULL,
    id_estado BIGINT NOT NULL,

    direccion_origen VARCHAR(250) NOT NULL,
    direccion_destino VARCHAR(250) NOT NULL,

    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_estimada_entrega TIMESTAMP,
    fecha_entrega TIMESTAMP,

    prioridad VARCHAR(30),
    observaciones VARCHAR(500),

    CONSTRAINT fk_envios_remitente
        FOREIGN KEY (id_remitente)
        REFERENCES clientes(id_cliente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_envios_destinatario
        FOREIGN KEY (id_destinatario)
        REFERENCES clientes(id_cliente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_envios_paquete
        FOREIGN KEY (id_paquete)
        REFERENCES paquetes(id_paquete)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_envios_tipo_servicio
        FOREIGN KEY (id_tipo_servicio)
        REFERENCES tipos_servicio(id_tipo_servicio)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_envios_estado
        FOREIGN KEY (id_estado)
        REFERENCES estados_envio(id_estado)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);


-- ============================================================
-- EVENTOS LOGÍSTICOS
-- ============================================================

CREATE TABLE eventos_logisticos (
    id_evento BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    id_envio BIGINT NOT NULL,
    id_usuario BIGINT,
    id_estado BIGINT NOT NULL,

    fecha_hora TIMESTAMP NOT NULL,

    latitud NUMERIC(10,7),
    longitud NUMERIC(10,7),
    precision_gps NUMERIC(10,2),

    origen VARCHAR(30),

    sincronizado BOOLEAN NOT NULL DEFAULT FALSE,
    estado_sincronizacion VARCHAR(30),

    observacion VARCHAR(500),

    CONSTRAINT fk_eventos_logisticos_envio
        FOREIGN KEY (id_envio)
        REFERENCES envios(id_envio)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_eventos_logisticos_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuarios(id)
        ON UPDATE CASCADE
        ON DELETE SET NULL,

    CONSTRAINT fk_eventos_logisticos_estado
        FOREIGN KEY (id_estado)
        REFERENCES estados_envio(id_estado)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT chk_eventos_logisticos_latitud
        CHECK (
            latitud IS NULL
            OR latitud BETWEEN -90 AND 90
        ),

    CONSTRAINT chk_eventos_logisticos_longitud
        CHECK (
            longitud IS NULL
            OR longitud BETWEEN -180 AND 180
        ),

    CONSTRAINT chk_eventos_logisticos_precision
        CHECK (
            precision_gps IS NULL
            OR precision_gps >= 0
        )
);


-- ============================================================
-- TIPOS DE INCIDENCIA
-- ============================================================

CREATE TABLE tipos_incidencia (
    id_tipo_incidencia BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(250)
);


-- ============================================================
-- INCIDENCIAS
-- ============================================================

CREATE TABLE incidencias (
    id_incidencia BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    id_envio BIGINT NOT NULL,
    id_tipo_incidencia BIGINT NOT NULL,
    id_usuario BIGINT,

    descripcion VARCHAR(500),
    fecha_reporte TIMESTAMP,
    estado VARCHAR(30),
    prioridad VARCHAR(30),
    fecha_resolucion TIMESTAMP,

    CONSTRAINT fk_incidencias_envio
        FOREIGN KEY (id_envio)
        REFERENCES envios(id_envio)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_incidencias_tipo
        FOREIGN KEY (id_tipo_incidencia)
        REFERENCES tipos_incidencia(id_tipo_incidencia)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_incidencias_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuarios(id)
        ON UPDATE CASCADE
        ON DELETE SET NULL,

    CONSTRAINT chk_incidencias_fechas
        CHECK (
            fecha_resolucion IS NULL
            OR fecha_reporte IS NULL
            OR fecha_resolucion >= fecha_reporte
        )
);


-- ============================================================
-- CENTROS DE DISTRIBUCIÓN
-- ============================================================

CREATE TABLE centros_distribucion (
    id_centro BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    nombre VARCHAR(100) NOT NULL,
    direccion VARCHAR(250),
    ciudad VARCHAR(100),

    latitud NUMERIC(10,7),
    longitud NUMERIC(10,7),

    estado VARCHAR(30),

    CONSTRAINT chk_centros_distribucion_latitud
        CHECK (
            latitud IS NULL
            OR latitud BETWEEN -90 AND 90
        ),

    CONSTRAINT chk_centros_distribucion_longitud
        CHECK (
            longitud IS NULL
            OR longitud BETWEEN -180 AND 180
        )
);


-- ============================================================
-- VEHÍCULOS
-- ============================================================

CREATE TABLE vehiculos (
    id_vehiculo BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    placa VARCHAR(20) NOT NULL UNIQUE,
    tipo_vehiculo VARCHAR(50),

    capacidad_peso_kg NUMERIC(10,2),
    capacidad_volumen_m3 NUMERIC(10,2),

    estado VARCHAR(30),

    CONSTRAINT chk_vehiculos_peso
        CHECK (
            capacidad_peso_kg IS NULL
            OR capacidad_peso_kg > 0
        ),

    CONSTRAINT chk_vehiculos_volumen
        CHECK (
            capacidad_volumen_m3 IS NULL
            OR capacidad_volumen_m3 > 0
        )
);


-- ============================================================
-- RUTAS
-- ============================================================

CREATE TABLE rutas (
    id_ruta BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    id_conductor BIGINT NOT NULL,
    id_vehiculo BIGINT,

    id_centro_origen BIGINT,
    id_centro_destino BIGINT,

    fecha DATE,
    hora_inicio TIMESTAMP,
    hora_fin TIMESTAMP,

    estado VARCHAR(30),

    distancia_km NUMERIC(10,2),

    CONSTRAINT fk_rutas_conductor
        FOREIGN KEY (id_conductor)
        REFERENCES conductores(id_conductor)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_rutas_vehiculo
        FOREIGN KEY (id_vehiculo)
        REFERENCES vehiculos(id_vehiculo)
        ON UPDATE CASCADE
        ON DELETE SET NULL,

    CONSTRAINT fk_rutas_centro_origen
        FOREIGN KEY (id_centro_origen)
        REFERENCES centros_distribucion(id_centro)
        ON UPDATE CASCADE
        ON DELETE SET NULL,

    CONSTRAINT fk_rutas_centro_destino
        FOREIGN KEY (id_centro_destino)
        REFERENCES centros_distribucion(id_centro)
        ON UPDATE CASCADE
        ON DELETE SET NULL,

    CONSTRAINT chk_rutas_distancia
        CHECK (
            distancia_km IS NULL
            OR distancia_km >= 0
        ),

    CONSTRAINT chk_rutas_horas
        CHECK (
            hora_fin IS NULL
            OR hora_inicio IS NULL
            OR hora_fin >= hora_inicio
        )
);


-- ============================================================
-- ASIGNACIONES DE ENVÍOS
-- ============================================================

CREATE TABLE asignaciones_envio (
    id_asignacion BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    id_ruta BIGINT NOT NULL,
    id_envio BIGINT NOT NULL,

    orden_entrega INTEGER,

    fecha_asignacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_inicio TIMESTAMP,
    fecha_fin TIMESTAMP,

    estado VARCHAR(30),

    CONSTRAINT fk_asignaciones_envio_ruta
        FOREIGN KEY (id_ruta)
        REFERENCES rutas(id_ruta)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_asignaciones_envio_envio
        FOREIGN KEY (id_envio)
        REFERENCES envios(id_envio)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT chk_asignaciones_envio_orden
        CHECK (
            orden_entrega IS NULL
            OR orden_entrega > 0
        ),

    CONSTRAINT chk_asignaciones_envio_fechas
        CHECK (
            fecha_fin IS NULL
            OR fecha_inicio IS NULL
            OR fecha_fin >= fecha_inicio
        ),

    CONSTRAINT uq_asignaciones_envio_ruta_envio
        UNIQUE (id_ruta, id_envio)
);


-- ============================================================
-- EVIDENCIAS DE ENTREGA
-- ============================================================

CREATE TABLE evidencias_entrega (
    id_evidencia BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    id_envio BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,

    tipo_evidencia VARCHAR(30),
    ruta_archivo VARCHAR(500),
    hash_archivo VARCHAR(255),

    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    observacion VARCHAR(500),

    CONSTRAINT fk_evidencias_entrega_envio
        FOREIGN KEY (id_envio)
        REFERENCES envios(id_envio)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_evidencias_entrega_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuarios(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);


-- ============================================================
-- VERIFICACIONES DE EDAD
-- ============================================================

CREATE TABLE verificaciones_edad (
    id_verificacion BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    id_envio BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,

    tipo_documento VARCHAR(50),
    fecha_nacimiento DATE,
    edad_calculada INTEGER,
    cumple_edad BOOLEAN,

    fecha_verificacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_verificaciones_edad_envio
        FOREIGN KEY (id_envio)
        REFERENCES envios(id_envio)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_verificaciones_edad_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuarios(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT chk_verificaciones_edad_calculada
        CHECK (
            edad_calculada IS NULL
            OR edad_calculada >= 0
        )
);


-- ============================================================
-- NOTIFICACIONES
-- ============================================================

CREATE TABLE notificaciones (
    id_notificacion BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    id_envio BIGINT NOT NULL,
    id_cliente BIGINT NOT NULL,

    canal VARCHAR(20),
    mensaje VARCHAR(500),

    fecha_envio TIMESTAMP,
    estado VARCHAR(30),

    intentos INTEGER NOT NULL DEFAULT 0,
    fecha_ultimo_intento TIMESTAMP,

    CONSTRAINT fk_notificaciones_envio
        FOREIGN KEY (id_envio)
        REFERENCES envios(id_envio)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_notificaciones_cliente
        FOREIGN KEY (id_cliente)
        REFERENCES clientes(id_cliente)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT chk_notificaciones_intentos
        CHECK (intentos >= 0)
);


-- ============================================================
-- REPROGRAMACIONES
-- ============================================================

CREATE TABLE reprogramaciones (
    id_reprogramacion BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    id_envio BIGINT NOT NULL,
    id_cliente BIGINT NOT NULL,

    fecha_anterior TIMESTAMP,
    nueva_fecha TIMESTAMP,
    motivo VARCHAR(500),

    fecha_solicitud TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    estado VARCHAR(30),

    CONSTRAINT fk_reprogramaciones_envio
        FOREIGN KEY (id_envio)
        REFERENCES envios(id_envio)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_reprogramaciones_cliente
        FOREIGN KEY (id_cliente)
        REFERENCES clientes(id_cliente)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);


-- ============================================================
-- ANOMALÍAS
-- ============================================================

CREATE TABLE anomalias (
    id_anomalia BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    id_envio BIGINT NOT NULL,
    id_ruta BIGINT,
    id_evento BIGINT,
    id_usuario_resolucion BIGINT,

    tipo_anomalia VARCHAR(100),
    descripcion VARCHAR(500),
    nivel VARCHAR(30),

    fecha_deteccion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    estado VARCHAR(30),
    fecha_resolucion TIMESTAMP,

    CONSTRAINT fk_anomalias_envio
        FOREIGN KEY (id_envio)
        REFERENCES envios(id_envio)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_anomalias_ruta
        FOREIGN KEY (id_ruta)
        REFERENCES rutas(id_ruta)
        ON UPDATE CASCADE
        ON DELETE SET NULL,

    CONSTRAINT fk_anomalias_evento
        FOREIGN KEY (id_evento)
        REFERENCES eventos_logisticos(id_evento)
        ON UPDATE CASCADE
        ON DELETE SET NULL,

    CONSTRAINT fk_anomalias_usuario_resolucion
        FOREIGN KEY (id_usuario_resolucion)
        REFERENCES usuarios(id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);


-- ============================================================
-- ESTIMACIONES DE ENTREGA
-- ============================================================

CREATE TABLE estimaciones_entrega (
    id_estimacion BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    id_envio BIGINT NOT NULL,

    fecha_estimacion TIMESTAMP,
    fecha_calculo TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    distancia_restante_km NUMERIC(10,2),
    tiempo_estimado_min INTEGER,

    metodo VARCHAR(50),

    CONSTRAINT fk_estimaciones_entrega_envio
        FOREIGN KEY (id_envio)
        REFERENCES envios(id_envio)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT chk_estimaciones_entrega_distancia
        CHECK (
            distancia_restante_km IS NULL
            OR distancia_restante_km >= 0
        ),

    CONSTRAINT chk_estimaciones_entrega_tiempo
        CHECK (
            tiempo_estimado_min IS NULL
            OR tiempo_estimado_min >= 0
        )
);


-- ============================================================
-- CONSENTIMIENTOS
-- ============================================================

CREATE TABLE consentimientos (
    id_consentimiento BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    id_cliente BIGINT NOT NULL,

    tipo_documento VARCHAR(100),
    version_documento VARCHAR(30),

    aceptado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_aceptacion TIMESTAMP,

    CONSTRAINT fk_consentimientos_cliente
        FOREIGN KEY (id_cliente)
        REFERENCES clientes(id_cliente)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT chk_consentimientos_fecha
        CHECK (
            aceptado = FALSE
            OR fecha_aceptacion IS NOT NULL
        )
);


-- ============================================================
-- AUDITORÍAS
-- ============================================================

CREATE TABLE auditorias (
    id_auditoria BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    id_usuario BIGINT,

    accion VARCHAR(100),
    entidad VARCHAR(100),
    id_entidad BIGINT,

    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    ip VARCHAR(50),
    origen VARCHAR(50),

    detalle VARCHAR(1000),

    CONSTRAINT fk_auditorias_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuarios(id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);


-- ============================================================
-- ÍNDICES
-- ============================================================

CREATE INDEX idx_usuarios_rol
    ON usuarios(rol);

CREATE INDEX idx_usuarios_estado
    ON usuarios(estado);

CREATE INDEX idx_eventos_logisticos_envio
    ON eventos_logisticos(id_envio);

CREATE INDEX idx_eventos_logisticos_fecha
    ON eventos_logisticos(fecha_hora);

CREATE INDEX idx_incidencias_envio
    ON incidencias(id_envio);

CREATE INDEX idx_incidencias_tipo
    ON incidencias(id_tipo_incidencia);

CREATE INDEX idx_rutas_conductor
    ON rutas(id_conductor);

CREATE INDEX idx_rutas_vehiculo
    ON rutas(id_vehiculo);

CREATE INDEX idx_rutas_fecha
    ON rutas(fecha);

CREATE INDEX idx_asignaciones_envio_ruta
    ON asignaciones_envio(id_ruta);

CREATE INDEX idx_asignaciones_envio_envio
    ON asignaciones_envio(id_envio);

CREATE INDEX idx_evidencias_entrega_envio
    ON evidencias_entrega(id_envio);

CREATE INDEX idx_verificaciones_edad_envio
    ON verificaciones_edad(id_envio);

CREATE INDEX idx_notificaciones_envio
    ON notificaciones(id_envio);

CREATE INDEX idx_notificaciones_cliente
    ON notificaciones(id_cliente);

CREATE INDEX idx_reprogramaciones_envio
    ON reprogramaciones(id_envio);

CREATE INDEX idx_anomalias_envio
    ON anomalias(id_envio);

CREATE INDEX idx_anomalias_ruta
    ON anomalias(id_ruta);

CREATE INDEX idx_anomalias_evento
    ON anomalias(id_evento);

CREATE INDEX idx_estimaciones_entrega_envio
    ON estimaciones_entrega(id_envio);

CREATE INDEX idx_consentimientos_cliente
    ON consentimientos(id_cliente);

CREATE INDEX idx_auditorias_usuario
    ON auditorias(id_usuario);

CREATE INDEX idx_auditorias_entidad
    ON auditorias(entidad, id_entidad);

CREATE INDEX idx_envios_estado
    ON envios(id_estado);

CREATE INDEX idx_envios_remitente
    ON envios(id_remitente);

CREATE INDEX idx_envios_destinatario
    ON envios(id_destinatario);
