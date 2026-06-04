CREATE TABLE otp_templates (
    id          UUID PRIMARY KEY,
    locale      VARCHAR(10)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    content     TEXT         NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (locale, name)
);

COMMENT ON TABLE otp_templates IS 'Overridable OTP message templates per locale. Falls back to messages.properties when no row matches.';
COMMENT ON COLUMN otp_templates.locale IS 'BCP-47 language tag (e.g. en, fr, ar-MA)';
COMMENT ON COLUMN otp_templates.name IS 'Template name (e.g. otp, otp-email)';
COMMENT ON COLUMN otp_templates.content IS 'Template body with {variable} placeholders';
