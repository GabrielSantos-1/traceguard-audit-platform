CREATE TABLE IF NOT EXISTS notification_configs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('SLACK', 'EMAIL')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    config JSONB,
    min_severity VARCHAR(20) NOT NULL DEFAULT 'HIGH' CHECK (min_severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    rule_filter VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notification_configs_tenant ON notification_configs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_notification_configs_channel ON notification_configs(channel);
CREATE INDEX IF NOT EXISTS idx_notification_configs_enabled ON notification_configs(enabled);
