-- Append-only audit trail for administrative actions.

CREATE TABLE admin_audit_event (
    id          BIGSERIAL PRIMARY KEY,
    actor_id    UUID         NOT NULL,
    actor_label VARCHAR(160),
    action      VARCHAR(64)  NOT NULL,
    target_type VARCHAR(32)  NOT NULL,
    target_id   UUID,
    metadata    JSONB,
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_admin_audit_created ON admin_audit_event (created_at DESC);
CREATE INDEX idx_admin_audit_action ON admin_audit_event (action, created_at DESC);
CREATE INDEX idx_admin_audit_target ON admin_audit_event (target_type, target_id);
