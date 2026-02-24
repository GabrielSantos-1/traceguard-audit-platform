CREATE INDEX idx_audit_tenant_time
ON audit_events (tenant_id, occurred_at DESC);

CREATE INDEX idx_audit_user_time
ON audit_events (tenant_id, user_id, occurred_at DESC);

CREATE INDEX idx_audit_entity_time
ON audit_events (tenant_id, entity, occurred_at DESC);
