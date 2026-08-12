-- ============================================================
-- Budgetin - Migration: Hirarki Dompet (Main & Branch) + Status Transaksi
-- Jalankan di Supabase SQL Editor untuk project yang sudah berjalan.
-- Semua statement idempotent (aman dijalankan berulang).
-- ============================================================

-- 1) Accounts: dukung relasi induk-anak (null = Dompet Utama/Main).
alter table public.accounts add column if not exists parent_id uuid
  references public.accounts (id) on delete set null;

-- 1b) Kolom lain yang harus ada agar push akun dari aplikasi tidak gagal.
alter table public.accounts add column if not exists count_in_total boolean not null default true;
alter table public.accounts add column if not exists sync_state text not null default 'synced';

-- 2) Transactions: status transaksi Sudah Terjadi / Belum Terjadi.
alter table public.transactions add column if not exists is_completed boolean not null default true;

-- 2b) Kolom debts yang harus ada agar push hutang-piutang tidak gagal.
alter table public.debts add column if not exists account_id uuid;
alter table public.debts add column if not exists settled_account_id uuid;

-- 2c) Kolom categories yang harus ada agar konsisten dengan aplikasi.
alter table public.categories add column if not exists sync_state text not null default 'synced';

-- 3) Indeks performa untuk filter & agregasi.
create index if not exists accounts_user_parent_idx on public.accounts (user_id, parent_id);
create index if not exists transactions_user_date_idx on public.transactions (user_id, transaction_date desc);
create index if not exists transactions_account_idx on public.transactions (account_id);
create index if not exists transactions_completed_idx on public.transactions (user_id, is_completed);

-- 4) Fungsi saldo akurat per branch & gabungan dompet induk (rekursif).
create or replace function public.account_balance(account uuid)
returns double precision
language sql stable
as $$
  select (
    a.balance
    + coalesce((
        select sum(t.amount - t.admin_fee) from public.transactions t
        where t.account_id = account and t.type = 'income' and t.is_completed
      ), 0)
    - coalesce((
        select sum(t.amount + t.admin_fee) from public.transactions t
        where t.account_id = account and t.type in ('expense', 'transfer') and t.is_completed
      ), 0)
    + coalesce((
        select sum(t.amount) from public.transactions t
        where t.transfer_to_account_id = account and t.type = 'transfer' and t.is_completed
      ), 0)
  )
  from public.accounts a where a.id = account;
$$;

create or replace function public.account_balance_total(account uuid)
returns double precision
language sql stable
as $$
  with recursive tree as (
    select id from public.accounts where id = account
    union all
    select c.id from public.accounts c join tree t on c.parent_id = t.id
  )
  select coalesce(sum(public.account_balance(id)), 0) from tree;
$$;
