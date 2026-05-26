ALTER TABLE tenant_api_keys
    ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE tenant_api_keys
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;

ALTER TABLE tenant_api_keys
    ADD COLUMN IF NOT EXISTS key_prefix VARCHAR(16);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_tenant_api_keys_key_prefix_len'
    ) THEN
        ALTER TABLE tenant_api_keys
            ADD CONSTRAINT ck_tenant_api_keys_key_prefix_len
                CHECK (key_prefix IS NULL OR char_length(key_prefix) BETWEEN 6 AND 16);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS tenant_api_key_scopes (
    api_key_id UUID NOT NULL REFERENCES tenant_api_keys(id) ON DELETE CASCADE,
    scope VARCHAR(50) NOT NULL,
    PRIMARY KEY (api_key_id, scope),
    CONSTRAINT ck_tenant_api_key_scopes_scope
        CHECK (scope IN ('AUDIT_READ', 'AUDIT_WRITE'))
);

CREATE INDEX IF NOT EXISTS idx_tenant_api_keys_active_lookup
    ON tenant_api_keys (tenant_id, enabled, revoked_at, expires_at);

CREATE INDEX IF NOT EXISTS idx_tenant_api_key_scopes_scope
    ON tenant_api_key_scopes (scope);
