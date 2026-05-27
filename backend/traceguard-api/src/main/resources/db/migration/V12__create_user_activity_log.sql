CREATE TABLE IF NOT EXISTS user_activity_log (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID,
    user_role VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(200),
    http_method VARCHAR(10),
    http_path VARCHAR(500) NOT NULL,
    http_status INT NOT NULL,
    ip VARCHAR(64),
    user_agent VARCHAR(512),
    correlation_id VARCHAR(128) NOT NULL,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    before_snapshot JSONB,
    after_snapshot JSONB,
    diff_summary TEXT,
    duration_ms BIGINT,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_activity_tenant_time
    ON user_activity_log(tenant_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_activity_user_time
    ON user_activity_log(tenant_id, user_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_activity_action_time
    ON user_activity_log(tenant_id, action, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_activity_success
    ON user_activity_log(tenant_id, success, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_activity_correlation_id
    ON user_activity_log(correlation_id);
