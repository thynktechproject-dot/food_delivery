alter table restaurants add column if not exists active boolean not null default true;
alter table restaurants add column if not exists deleted_at timestamp;
