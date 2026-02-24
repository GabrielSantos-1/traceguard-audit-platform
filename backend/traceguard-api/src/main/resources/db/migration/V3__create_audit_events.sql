CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    entity VARCHAR(100),
    entity_id VARCHAR(100),
    severity VARCHAR(50) NOT NULL,
    ip VARCHAR(100),
    user_agent VARCHAR(255),
    occurred_at TIMESTAMP NOT NULL DEFAULT NOW(),
    metadata JSONB,
    before_data JSONB,
    after_data JSONB,

    CONSTRAINT fk_audit_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_audit_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE SET NULL
);
