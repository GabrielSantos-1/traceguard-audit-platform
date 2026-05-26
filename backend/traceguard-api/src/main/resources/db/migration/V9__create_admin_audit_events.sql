CREATE TABLE IF NOT EXISTS admin_audit_events (
    id UUID PRIMARY KEY,
    admin_id UUID NOT NULL,
    tenant_id UUID,
    action VARCHAR(120) NOT NULL,
    resource_type VARCHAR(80) NOT NULL,
    resource_id VARCHAR(120),
    correlation_id VARCHAR(128) NOT NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT NOW(),
    details_json JSONB
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_occurred_at
    ON admin_audit_events(occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_admin_audit_admin_id
    ON admin_audit_events(admin_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_admin_audit_tenant_id
    ON admin_audit_events(tenant_id, occurred_at DESC);
