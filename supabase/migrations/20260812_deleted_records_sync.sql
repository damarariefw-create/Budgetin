-- ============================================================
-- Budgetin - Migration: Sinkronisasi Penghapusan (multi-perangkat)
-- Jalankan di Supabase SQL Editor untuk project yang sudah berjalan.
-- Semua statement idempotent (aman dijalankan berulang).
--
-- CATATAN: Migration ini sekarang OPSIONAL / tidak wajib.
-- Aplikasi terbaru memakai soft-delete pada kolom yang sudah ada
-- (transactions.status = 'deleted' dan accounts.sync_state = 'deleted'),
-- jadi penghapusan tersinkron TANPA tabel tambahan. Tabel di bawah
-- dipertahankan hanya untuk kompatibilitas/arsip.
-- ============================================================
-- Tabel tombstone ini mencatat ID data yang dihapus di satu perangkat
-- agar perangkat lain dengan akun yang sama ikut menghapus datanya
-- (penghapusan menyebar / eventual consistency). Baris bersifat permanen
-- dan sangat kecil; hanya dipakai untuk sinkronisasi hapus.

-- 1) Tombstone transaksi yang dihapus (riwayat).
create table if not exists public.deleted_transactions (
  id          uuid primary key,
  user_id     uuid not null references auth.users (id) on delete cascade,
  deleted_at  timestamptz not null default now()
);

-- 2) Tombstone dompet/rekening yang dihapus.
create table if not exists public.deleted_accounts (
  id          uuid primary key,
  user_id     uuid not null references auth.users (id) on delete cascade,
  deleted_at  timestamptz not null default now()
);

-- 3) Indeks performa filter per user.
create index if not exists deleted_transactions_user_idx
  on public.deleted_transactions (user_id);
create index if not exists deleted_accounts_user_idx
  on public.deleted_accounts (user_id);

-- 4) RLS: hanya pemiliknya yang bisa membaca / mencatat / memperbarui / menghapus.
--    Kebijakan UPDATE ikut dibuat agar upsert ON CONFLICT DO UPDATE (bila dipakai)
--    tidak tertolak oleh RLS.
alter table public.deleted_transactions enable row level security;
alter table public.deleted_accounts enable row level security;

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
