-- Runtime feature-flag overrides written from the operations console.
create table if not exists feature_flag (
    name varchar(80) primary key,
    enabled boolean not null,
    updated_at timestamptz not null default now()
);
