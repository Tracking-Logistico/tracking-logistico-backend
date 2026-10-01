-- La migracion V11 agrego capacidades NUMERIC, pero Conductor las mapea como Double.
-- En PostgreSQL Hibernate espera DOUBLE PRECISION para Double (float(53)).
-- Conservar los valores existentes, DEFAULT y NOT NULL; no modificar V11 ya aplicada.
ALTER TABLE conductores
    ALTER COLUMN capacidad_max_kg TYPE DOUBLE PRECISION
    USING capacidad_max_kg::DOUBLE PRECISION;

ALTER TABLE conductores
    ALTER COLUMN capacidad_max_volumen_cm3 TYPE DOUBLE PRECISION
    USING capacidad_max_volumen_cm3::DOUBLE PRECISION;
