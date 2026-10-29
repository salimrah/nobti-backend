-- TOTP (RFC 6238) second factor for operations accounts.
create table if not exists user_totp_secret (
    user_id uuid primary key references users(id) on delete cascade,
    secret varchar(255) not null,
    enabled boolean not null default false,
    confirmed_at timestamptz,
    last_used_step bigint,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

comment on table user_totp_secret is 'TOTP second factor; secret is encrypted at rest by the application';
