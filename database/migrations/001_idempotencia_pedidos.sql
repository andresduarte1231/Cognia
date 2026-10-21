-- 001: soporte de idempotencia de pedidos (incremental, re-ejecutable, no destructivo)
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS clave_idempotencia VARCHAR(80) NULL AFTER numero_pedido;
CREATE UNIQUE INDEX IF NOT EXISTS uk_pedidos_idempotencia ON pedidos (cliente_id, clave_idempotencia);
