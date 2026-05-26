ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS slug VARCHAR(80),
    ADD COLUMN IF NOT EXISTS status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE tenants
SET slug = COALESCE(slug, regexp_replace(lower(name), '[^a-z0-9]+', '-', 'g')),
    status = COALESCE(status, 'ACTIVE'),
    updated_at = COALESCE(updated_at, created_at)
WHERE slug IS NULL OR status IS NULL OR updated_at IS NULL;

ALTER TABLE tenants
    ALTER COLUMN slug SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_tenants_status'
    ) THEN
        ALTER TABLE tenants
            ADD CONSTRAINT ck_tenants_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DISABLED'));
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_tenants_slug
    ON tenants(slug);
