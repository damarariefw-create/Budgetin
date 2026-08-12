-- ============================================================
-- Budgetin - Skema Supabase (jalankan di SQL Editor)
-- Tabel accounts/transactions/debts/categories + RLS agar semua
-- pemasukan, pengeluaran, transfer, hutang, piutang & edit
-- tersimpan di Supabase dan ikut akun yang sedang login.
--
-- Update terbaru:
--  * accounts.parent_id       -> hirarki Dompet Utama (Main) & Branch/Ranting
--  * transactions.is_completed-> status transaksi Sudah Terjadi / Belum Terjadi
--  * fungsi saldo akurat per branch & gabungan dompet induk
-- ============================================================

-- ---------- TABEL: accounts ----------
create table if not exists public.accounts (
  id          uuid primary key,
  user_id     uuid not null references auth.users (id) on delete cascade,
  name        text not null,
  type        text not null default 'cash',
  balance     double precision not null default 0,
  icon        text not null default '💳',
  color       text not null default '#10B981',
  count_in_total boolean not null default true,
  is_archived boolean not null default false,
  parent_id   uuid references public.accounts (id) on delete set null,
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);

-- Kolom yang mungkin belum ada di project yang sudah berjalan (semua idempotent):
alter table public.accounts add column if not exists parent_id uuid
  references public.accounts (id) on delete set null;
alter table public.accounts add column if not exists count_in_total boolean not null default true;
alter table public.accounts add column if not exists sync_state text not null default 'synced';

-- ---------- TABEL: transactions ----------
create table if not exists public.transactions (
  id          uuid primary key,
  user_id     uuid not null references auth.users (id) on delete cascade,
  type        text not null,                       -- income / expense / transfer
  amount      double precision not null,
  admin_fee   double precision not null default 0,
  category_id uuid,
  account_id  uuid not null,
  transfer_to_account_id uuid,
  note        text not null default '',
  status      text not null default 'confirmed',   -- confirmed / outstanding (legacy)
  is_completed boolean not null default true,      -- true = Sudah Terjadi, false = Belum Terjadi
  is_recurring boolean not null default false,
  transaction_date date,
  sync_state  text not null default 'synced',
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);

-- Kolom is_completed untuk project yang sudah berjalan (idempotent)
alter table public.transactions add column if not exists is_completed boolean not null default true;

-- ---------- INDEX (performa filter & agregasi) ----------
create index if not exists accounts_user_parent_idx on public.accounts (user_id, parent_id);
create index if not exists transactions_user_date_idx on public.transactions (user_id, transaction_date desc);
create index if not exists transactions_account_idx on public.transactions (account_id);
create index if not exists transactions_completed_idx on public.transactions (user_id, is_completed);

-- ---------- TABEL: debts ----------
create table if not exists public.debts (
  id          uuid primary key,
  user_id     uuid not null references auth.users (id) on delete cascade,
  counterpart_name text not null,
  type        text not null,                       -- owe / owed
  amount      double precision not null,
  note        text not null default '',
  due_date    date,
  is_settled  boolean not null default false,
  account_id  uuid,                                -- dompet pinjaman
  settled_account_id uuid,                         -- dompet pelunasan
  sync_state  text not null default 'synced',
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);

-- Kolom debts untuk project yang sudah berjalan (idempotent)
alter table public.debts add column if not exists account_id uuid;
alter table public.debts add column if not exists settled_account_id uuid;

-- ---------- TABEL: categories ----------
create table if not exists public.categories (
  id          uuid primary key,
  user_id     uuid references auth.users (id) on delete cascade, -- null = kategori default
  name        text not null,
  type        text not null default 'expense',     -- expense / income
  emoji       text not null default '📦',
  color       text not null default '#6B7280',
  is_default  boolean not null default false,
  sync_state  text not null default 'synced'
);

-- Kolom categories untuk project yang sudah berjalan (idempotent)
alter table public.categories add column if not exists sync_state text not null default 'synced';

-- ---------- TABEL: deleted_transactions / deleted_accounts (tombstone hapus) ----------
-- Mencatat ID yang dihapus di satu perangkat agar perangkat lain dengan akun
-- yang sama ikut menghapus datanya (penghapusan menyebar antar-perangkat).
create table if not exists public.deleted_transactions (
  id          uuid primary key,
  user_id     uuid not null references auth.users (id) on delete cascade,
  deleted_at  timestamptz not null default now()
);

create table if not exists public.deleted_accounts (
  id          uuid primary key,
  user_id     uuid not null references auth.users (id) on delete cascade,
  deleted_at  timestamptz not null default now()
);

create index if not exists deleted_transactions_user_idx
  on public.deleted_transactions (user_id);
create index if not exists deleted_accounts_user_idx
  on public.deleted_accounts (user_id);

-- ---------- RLS ----------
alter table public.accounts     enable row level security;
alter table public.transactions enable row level security;
alter table public.debts        enable row level security;
alter table public.categories   enable row level security;

-- accounts
drop policy if exists "accounts_select_own" on public.accounts;
drop policy if exists "accounts_insert_own" on public.accounts;
drop policy if exists "accounts_update_own" on public.accounts;
drop policy if exists "accounts_delete_own" on public.accounts;
create policy "accounts_select_own" on public.accounts for select using (auth.uid() = user_id);
create policy "accounts_insert_own" on public.accounts for insert with check (auth.uid() = user_id);
create policy "accounts_update_own" on public.accounts for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "accounts_delete_own" on public.accounts for delete using (auth.uid() = user_id);

-- transactions
drop policy if exists "transactions_select_own" on public.transactions;
drop policy if exists "transactions_insert_own" on public.transactions;
drop policy if exists "transactions_update_own" on public.transactions;
drop policy if exists "transactions_delete_own" on public.transactions;
create policy "transactions_select_own" on public.transactions for select using (auth.uid() = user_id);
create policy "transactions_insert_own" on public.transactions for insert with check (auth.uid() = user_id);
create policy "transactions_update_own" on public.transactions for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "transactions_delete_own" on public.transactions for delete using (auth.uid() = user_id);

-- debts
drop policy if exists "debts_select_own" on public.debts;
drop policy if exists "debts_insert_own" on public.debts;
drop policy if exists "debts_update_own" on public.debts;
drop policy if exists "debts_delete_own" on public.debts;
create policy "debts_select_own" on public.debts for select using (auth.uid() = user_id);
create policy "debts_insert_own" on public.debts for insert with check (auth.uid() = user_id);
create policy "debts_update_own" on public.debts for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "debts_delete_own" on public.debts for delete using (auth.uid() = user_id);

-- categories: default bisa dibaca semua user, custom hanya milik pemiliknya
drop policy if exists "categories_select" on public.categories;
drop policy if exists "categories_insert_own" on public.categories;
drop policy if exists "categories_update_own" on public.categories;
drop policy if exists "categories_delete_own" on public.categories;
create policy "categories_select" on public.categories for select
  using (is_default = true or auth.uid() = user_id);
create policy "categories_insert_own" on public.categories for insert
  with check (auth.uid() = user_id and is_default = false);
create policy "categories_update_own" on public.categories for update
  using (auth.uid() = user_id) with check (auth.uid() = user_id and is_default = false);
create policy "categories_delete_own" on public.categories for delete
  using (auth.uid() = user_id and is_default = false);

-- deleted_transactions: hanya pemiliknya
alter table public.deleted_transactions enable row level security;
drop policy if exists "deleted_transactions_select_own" on public.deleted_transactions;
drop policy if exists "deleted_transactions_insert_own" on public.deleted_transactions;
drop policy if exists "deleted_transactions_update_own" on public.deleted_transactions;
drop policy if exists "deleted_transactions_delete_own" on public.deleted_transactions;
create policy "deleted_transactions_select_own" on public.deleted_transactions
  for select using (auth.uid() = user_id);
create policy "deleted_transactions_insert_own" on public.deleted_transactions
  for insert with check (auth.uid() = user_id);
create policy "deleted_transactions_update_own" on public.deleted_transactions
  for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "deleted_transactions_delete_own" on public.deleted_transactions
  for delete using (auth.uid() = user_id);

-- deleted_accounts: hanya pemiliknya
alter table public.deleted_accounts enable row level security;
drop policy if exists "deleted_accounts_select_own" on public.deleted_accounts;
drop policy if exists "deleted_accounts_insert_own" on public.deleted_accounts;
drop policy if exists "deleted_accounts_update_own" on public.deleted_accounts;
drop policy if exists "deleted_accounts_delete_own" on public.deleted_accounts;
create policy "deleted_accounts_select_own" on public.deleted_accounts
  for select using (auth.uid() = user_id);
create policy "deleted_accounts_insert_own" on public.deleted_accounts
  for insert with check (auth.uid() = user_id);
create policy "deleted_accounts_update_own" on public.deleted_accounts
  for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "deleted_accounts_delete_own" on public.deleted_accounts
  for delete using (auth.uid() = user_id);

-- ---------- FUNGSI AGREGASI SALDO (dipakai dashboard / laporan) ----------

-- Saldo berjalan satu dompet = saldo awal + income - expense - transfer keluar + transfer masuk.
-- Hanya transaksi yang SUDAH TERJADI (is_completed = true) yang memengaruhi saldo.
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

-- Saldo gabungan dompet induk = saldo induk + semua branch-nya (rekursif).
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

-- ---------- Kategori default (ditarik perangkat baru) ----------
insert into public.categories (id, user_id, name, type, emoji, color, is_default) values
  ('00000000-0000-4000-8000-000000000001', null, 'Makanan', 'expense', '🍜', '#F59E0B', true),
  ('00000000-0000-4000-8000-000000000002', null, 'Transportasi', 'expense', '🚌', '#3B82F6', true),
  ('00000000-0000-4000-8000-000000000003', null, 'Belanja', 'expense', '🛍', '#EC4899', true),
  ('00000000-0000-4000-8000-000000000004', null, 'Tagihan', 'expense', '🧾', '#8B5CF6', true),
  ('00000000-0000-4000-8000-000000000005', null, 'Hiburan', 'expense', '🎮', '#06B6D4', true),
  ('00000000-0000-4000-8000-000000000006', null, 'Kesehatan', 'expense', '💊', '#EF4444', true),
  ('00000000-0000-4000-8000-000000000007', null, 'Pendidikan', 'expense', '📚', '#10B981', true),
  ('00000000-0000-4000-8000-000000000008', null, 'Lainnya', 'expense', '📦', '#6B7280', true),
  ('00000000-0000-4000-8000-000000000009', null, 'Gaji', 'income', '💰', '#10B981', true),
  ('00000000-0000-4000-8000-00000000000a', null, 'Bonus', 'income', '🎁', '#F59E0B', true),
  ('00000000-0000-4000-8000-00000000000b', null, 'Jualan', 'income', '🏪', '#3B82F6', true),
  ('00000000-0000-4000-8000-00000000000c', null, 'Investasi', 'income', '📈', '#8B5CF6', true),
  ('00000000-0000-4000-8000-00000000000d', null, 'Lainnya', 'income', '💵', '#6B7280', true)
on conflict (id) do nothing;
