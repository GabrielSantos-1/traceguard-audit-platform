ALTER TABLE tenant_api_keys
    ADD COLUMN IF NOT EXISTS last_used_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_tenant_api_keys_last_used_at
    ON tenant_api_keys(last_used_at);
