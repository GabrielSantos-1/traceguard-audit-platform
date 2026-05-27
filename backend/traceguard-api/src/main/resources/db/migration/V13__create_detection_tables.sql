CREATE TABLE IF NOT EXISTS detection_alerts (
    id UUID PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    tenant_id UUID NOT NULL,
    user_id UUID,
    source_ip VARCHAR(45),
    evidence JSONB,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_by UUID,
    acknowledged_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_detection_alerts_tenant ON detection_alerts(tenant_id);
CREATE INDEX IF NOT EXISTS idx_detection_alerts_rule ON detection_alerts(rule_name);
CREATE INDEX IF NOT EXISTS idx_detection_alerts_severity ON detection_alerts(severity);
CREATE INDEX IF NOT EXISTS idx_detection_alerts_acknowledged ON detection_alerts(acknowledged);
CREATE INDEX IF NOT EXISTS idx_detection_alerts_created ON detection_alerts(created_at);

CREATE TABLE IF NOT EXISTS blocked_ips (
    id UUID PRIMARY KEY,
    ip VARCHAR(45) NOT NULL,
    tenant_id UUID NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    reason VARCHAR(255),
    blocked_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_blocked_ips_ip ON blocked_ips(ip);
CREATE INDEX IF NOT EXISTS idx_blocked_ips_tenant ON blocked_ips(tenant_id);
CREATE INDEX IF NOT EXISTS idx_blocked_ips_active ON blocked_ips(active);
