-- Provider verification documents reviewed from the admin console.
create table if not exists provider_document (
    id uuid primary key,
    provider_id uuid not null,
    type varchar(40) not null,
    file_name varchar(255) not null,
    object_key varchar(512) not null,
    content_type varchar(120) not null,
    size_bytes bigint not null,
    status varchar(16) not null default 'PENDING',
    reviewer_note varchar(500),
    reviewed_by uuid,
    reviewed_at timestamptz,
    created_at timestamptz not null default now()
);

create index if not exists idx_provider_document_provider on provider_document (provider_id, created_at desc);
create index if not exists idx_provider_document_status on provider_document (status, created_at);
