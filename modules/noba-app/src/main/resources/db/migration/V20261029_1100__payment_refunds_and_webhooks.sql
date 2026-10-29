-- Partial refunds, payout retry support and a replayable webhook log.
alter table payment_transaction add column if not exists refunded_amount numeric(10,2) not null default 0;
alter table payment_transaction add column if not exists refund_reason varchar(500);
alter table payout add column if not exists source_charge_id varchar(100);

create table if not exists payment_webhook_event (
    id uuid primary key,
    provider varchar(32) not null,
    event_id varchar(120),
    event_type varchar(80),
    payment_intent_id varchar(100),
    charge_id varchar(100),
    failure_message varchar(500),
    payload text,
    status varchar(16) not null,
    error_message varchar(500),
    replay_count integer not null default 0,
    received_at timestamptz not null default now(),
    processed_at timestamptz
);

create index if not exists idx_payment_webhook_event_received on payment_webhook_event (received_at desc);
create unique index if not exists uq_payment_webhook_event_provider_event
    on payment_webhook_event (provider, event_id) where event_id is not null;
