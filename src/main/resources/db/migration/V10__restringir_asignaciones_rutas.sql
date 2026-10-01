-- V1 already created rutas. V7 CREATE TABLE IF NOT EXISTS did not add its
-- uniqueness constraint on databases containing that original table.
-- Backfill historic null dates before enforcing the JPA invariant.
UPDATE rutas
SET fecha = COALESCE(fecha_creacion::date, CURRENT_DATE)
WHERE fecha IS NULL;

ALTER TABLE rutas ALTER COLUMN fecha SET NOT NULL;

-- Fail safely on existing duplicates: they need explicit business reconciliation;
-- never remove deliveries, routes or assignments automatically.
CREATE UNIQUE INDEX IF NOT EXISTS uk_rutas_conductor_fecha
    ON rutas (id_conductor, fecha);

-- Avoid assigning the same shipment to two active driver routes concurrently.
CREATE UNIQUE INDEX IF NOT EXISTS uk_paradas_pedido_pendiente
    ON paradas_ruta (id_pedido) WHERE estado = 'PENDIENTE';
