-- Scheduled and cancellable broadcast campaigns.
alter table notification_campaign add column if not exists scheduled_at timestamptz;
alter table notification_campaign add column if not exists cancelled_at timestamptz;
alter table notification_campaign add column if not exists cancelled_by uuid;

create index if not exists idx_notification_campaign_due
    on notification_campaign (scheduled_at)
    where status = 'SCHEDULED';

create index if not exists idx_notification_dedupe_key on notification (dedupe_key);
