-- =============================================================================
-- V6: Soporte para cifrado AES-256-GCM en campos sensibles
--
-- Los campos de texto sensible (email, nombre, descripción, notas) pasan a TEXT
-- para almacenar el payload cifrado: enc:v1:<Base64(IV||ciphertext||tag)>
--
-- email_hmac almacena HMAC-SHA256(email_normalizado, clave_cifrado).
-- Permite búsquedas deterministas sobre el email cifrado y reemplaza la
-- restricción UNIQUE original sobre la columna email.
--
-- Para registros existentes en texto plano, EncryptionDataMigrationRunner
-- recalcula email_hmac con la clave real al arrancar la aplicación.
-- El placeholder generado aquí es único (basado en el UUID del usuario) y
-- se sobreescribe en el primer arranque.
-- =============================================================================

-- ── users ────────────────────────────────────────────────────────────────────

ALTER TABLE users
    ALTER COLUMN email TYPE TEXT,
    ALTER COLUMN name  TYPE TEXT;

ALTER TABLE users DROP CONSTRAINT IF EXISTS uq_users_email;

ALTER TABLE users
    ADD COLUMN email_hmac VARCHAR(64);

-- Placeholder único hasta que EncryptionDataMigrationRunner lo reemplace
UPDATE users
SET email_hmac = 'migrate_' || id::text
WHERE email_hmac IS NULL;

ALTER TABLE users
    ALTER COLUMN email_hmac SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uq_users_email_hmac UNIQUE (email_hmac);

-- ── transactions ─────────────────────────────────────────────────────────────

ALTER TABLE transactions
    ALTER COLUMN description TYPE TEXT,
    ALTER COLUMN notes       TYPE TEXT;

-- ── savings_goals ────────────────────────────────────────────────────────────

ALTER TABLE savings_goals
    ALTER COLUMN name        TYPE TEXT,
    ALTER COLUMN description TYPE TEXT;
