-- Completa la columna requerida por la entidad Ruta en bases existentes creadas con una versión anterior.
ALTER TABLE rutas
    ADD COLUMN IF NOT EXISTS fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
