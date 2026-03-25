-- V8__razorpay_payment_columns.sql
-- Update the payments table to support Razorpay integration

-- Add Razorpay-specific columns
alter table payments
    add column if not exists razorpay_order_id varchar(255);

alter table payments
    add column if not exists razorpay_payment_id varchar(255);

alter table payments
    add column if not exists razorpay_signature varchar(512);

alter table payments
    add column if not exists updated_at timestamp;

do $$
begin
    if not exists (
        select 1 from pg_constraint where conname = 'uk_payments_razorpay_order'
    ) then
        alter table payments add constraint uk_payments_razorpay_order unique (razorpay_order_id);
    end if;
end
$$;

do $$
begin
    if not exists (
        select 1 from pg_constraint where conname = 'uk_payments_razorpay_payment'
    ) then
        alter table payments add constraint uk_payments_razorpay_payment unique (razorpay_payment_id);
    end if;
end
$$;

-- The old transactionId column is no longer used (replaced by razorpay_payment_id).
-- We keep it nullable so old data is not lost and the app still compiles.
alter table payments
    alter column transaction_id drop not null;

-- PENDING is a new status — no schema change needed (stored as varchar).
-- Add index on razorpay_order_id for webhook lookups
create index if not exists idx_payments_razorpay_order_id on payments(razorpay_order_id);