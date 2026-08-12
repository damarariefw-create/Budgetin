package com.example.budgetin.data.repository

import android.util.Log
import com.example.budgetin.data.local.AccountDao
import com.example.budgetin.data.local.CategoryDao
import com.example.budgetin.data.local.DebtDao
import com.example.budgetin.data.local.TransactionDao
import com.example.budgetin.data.model.SyncState
import com.example.budgetin.data.remote.RemoteAccount
import com.example.budgetin.data.remote.RemoteCategory
import com.example.budgetin.data.remote.RemoteDebt
import com.example.budgetin.data.remote.RemoteTransaction
import com.example.budgetin.data.remote.parseRemoteCategories
import com.example.budgetin.data.remote.parseRemoteAccounts
import com.example.budgetin.data.remote.parseRemoteDebts
import com.example.budgetin.data.remote.parseRemoteTransactions
import com.example.budgetin.data.remote.toRemoteJson
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pusat sinkronisasi offline-first ke Supabase.
 * - push: transaksi/akun/hutang/kategori custom yang berstatus PENDING dikirim lalu ditandai SYNCED.
 * - pull: kategori dari server disimpan lokal (UUID server dipakai lokal).
 *
 * Semua operasi best-effort: gagal = tetap PENDING dan dicoba lagi di lain waktu.
 */

/**
 * Kriteria multi-filter untuk [SyncRepository.fetchTransactions].
 * Semua field opsional; hanya kriteria terisi yang diterapkan di query Supabase.
 */
data class TransactionRemoteFilter(
    /** Rentang tanggal "yyyy-MM-dd" (inklusif). */
    val dateFrom: String? = null,
    val dateTo: String? = null,
    /** Filter dompet spesifik (main maupun branch) memakai kolom account_id. */
    val accountId: String? = null,
    /** Tipe transaksi: income / expense / transfer. */
    val type: String? = null,
    /** UUID kategori (sub-kategori). */
    val categoryId: String? = null,
    /** Rentang nominal (inklusif). */
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    /** Status transaksi: true = Sudah Terjadi, false = Belum Terjadi. */
    val isCompleted: Boolean? = null,
)

@Singleton
class SyncRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val debtDao: DebtDao,
    private val categoryDao: CategoryDao,
) {

    /** Ambil kategori default (is_default = true) dari Supabase. */
    suspend fun fetchDefaultCategories(): List<RemoteCategory> {
        val result = supabase.postgrest.from("categories").select {
            filter { eq("is_default", true) }
        }
        return parseRemoteCategories(result.data)
    }

    /** Ambil kategori custom milik user yang sedang login. */
    suspend fun fetchCustomCategories(): List<RemoteCategory> {
        val user = supabase.auth.currentUserOrNull() ?: return emptyList()
        val result = supabase.postgrest.from("categories").select {
            filter { eq("user_id", user.id) }
            filter { eq("is_default", false) }
        }
        return parseRemoteCategories(result.data)
    }

    /** Ambil semua hutang-piutang milik user yang sedang login. */
    suspend fun fetchRemoteDebts(): List<RemoteDebt> {
        val user = supabase.auth.currentUserOrNull() ?: return emptyList()
        val result = supabase.postgrest.from("debts").select {
            filter { eq("user_id", user.id) }
        }
        return parseRemoteDebts(result.data)
    }

    /**
     * Query multi-filter transaksi ke Supabase dengan pagination.
     * Semua kriteria opsional; hanya yang terisi yang dipakai. Dipakai untuk
     * Riwayat (filter tanggal/dompet/kategori/nominal/status) & laporan statistik.
     */
    suspend fun fetchTransactions(
        filter: TransactionRemoteFilter = TransactionRemoteFilter(),
        page: Int = 0,
        pageSize: Int = 50,
    ): List<RemoteTransaction> {
        val user = supabase.auth.currentUserOrNull() ?: return emptyList()
        val result = supabase.postgrest.from("transactions").select {
            filter {
                eq("user_id", user.id)
                filter.dateFrom?.let { gte("transaction_date", it) }
                filter.dateTo?.let { lte("transaction_date", it) }
                filter.type?.let { eq("type", it) }
                filter.categoryId?.let { eq("category_id", it) }
                filter.minAmount?.let { gte("amount", it) }
                filter.maxAmount?.let { lte("amount", it) }
                filter.isCompleted?.let { eq("is_completed", it) }
                filter.accountId?.let { id ->
                    or { eq("account_id", id); eq("transfer_to_account_id", id) }
                }
            }
            order("transaction_date", Order.DESCENDING)
            range((page * pageSize).toLong(), ((page + 1) * pageSize - 1).toLong())
        }
        return parseRemoteTransactions(result.data)
    }

    /**
     * Ambil semua dompet milik user (termasuk branch). Dipakai untuk
     * menyesuaikan data lokal dari perangkat lain / multi-perangkat.
     */
    suspend fun fetchRemoteAccounts(): List<RemoteAccount> {
        val user = supabase.auth.currentUserOrNull() ?: return emptyList()
        val result = supabase.postgrest.from("accounts").select {
            filter { eq("user_id", user.id) }
        }
        return parseRemoteAccounts(result.data)
    }

    /**
     * Ambil SEMUA transaksi milik user dari Supabase (tanpa filter), dengan
     * pagination lengkap. Dipakai saat login untuk menyamakan data lokal
     * dengan perangkat lain (multi-perangkat).
     *
     * Nama kategori (denormalisasi) ikut diisi dari tabel kategori lokal agar
     * tampilan transaksi hasil pull tidak menjadi kosong/"Lainnya".
     */
    suspend fun fetchAllRemoteTransactions(): List<RemoteTransaction> {
        val user = supabase.auth.currentUserOrNull() ?: return emptyList()
        val all = mutableListOf<RemoteTransaction>()
        var page = 0
        val pageSize = 1000
        while (true) {
            val result = supabase.postgrest.from("transactions").select {
                filter { eq("user_id", user.id) }
                order("transaction_date", Order.DESCENDING)
                range((page * pageSize).toLong(), ((page + 1) * pageSize - 1).toLong())
            }
            val batch = parseRemoteTransactions(result.data)
            all += batch
            if (batch.size < pageSize) break
            page++
        }
        val categories = categoryDao.getAll().associateBy { it.id }
        return all.map { t ->
            if (t.categoryId != null && categories[t.categoryId] != null) {
                t.copy(categoryName = categories[t.categoryId]!!.name)
            } else {
                t
            }
        }
    }

    /** Kirim semua data lokal yang belum sinkron ke server. */
    suspend fun syncAll() {
        val user = supabase.auth.currentUserOrNull() ?: return
        val userId = user.id
        // Push data baru/ubah dulu, baru hapus (agar akun/transaksi yang dibuat
        // offline lalu dihapus offline tidak tertinggal sebagai baris server).
        syncAccounts(userId)
        syncDeletedAccounts(userId)
        syncTransactions(userId)
        syncDeletedTransactions(userId)
        syncDebts(userId)
        syncCategories(userId)
    }

    /**
     * Kirim penghapusan transaksi ke server: tandai baris server dengan
     * status='deleted' (soft-delete memakai kolom yang sudah ada, tanpa tabel
     * atau migration baru). Baris sengaja TIDAK dihapus permanen dari server
     * agar perangkat lain dengan akun yang sama bisa melihat penandanya saat
     * pull dan ikut menghapus transaksi ini.
     *
     * Bila baris belum pernah ada di server (dibuat lalu dihapus offline),
     * update hanya berdampak 0 baris — aman, karena perangkat lain tak pernah
     * memiliki transaksi tsb. Gagal/offline = tombstone tetap tersimpan.
     */
    private suspend fun syncDeletedTransactions(userId: String) {
        runCatchingBlock("transactions_soft_delete") {
            val tombstoneIds = transactionDao.allTombstoneIds()
            if (tombstoneIds.isEmpty()) return@runCatchingBlock
            for (id in tombstoneIds) {
                supabase.postgrest.from("transactions").update(
                    buildJsonObject { put("status", "deleted") }
                ) {
                    filter {
                        eq("id", id)
                        eq("user_id", userId)
                    }
                }
            }
            transactionDao.deleteTombstones(tombstoneIds)
        }
    }

    /**
     * Kirim penghapusan dompet/rekening ke server: tandai baris server dengan
     * sync_state='deleted' (soft-delete memakai kolom yang sudah ada, tanpa
     * tabel atau migration baru). Baris sengaja TIDAK dihapus permanen dari
     * server agar perangkat lain bisa melihat penandanya saat pull. Sama
     * idempoten seperti [syncDeletedTransactions]: gagal/offline = tombstone
     * tetap tersimpan dan dicoba lagi nanti.
     */
    private suspend fun syncDeletedAccounts(userId: String) {
        runCatchingBlock("accounts_soft_delete") {
            val tombstoneIds = accountDao.allTombstoneIds()
            if (tombstoneIds.isEmpty()) return@runCatchingBlock
            for (id in tombstoneIds) {
                supabase.postgrest.from("accounts").update(
                    buildJsonObject { put("sync_state", "deleted") }
                ) {
                    filter {
                        eq("id", id)
                        eq("user_id", userId)
                    }
                }
            }
            accountDao.deleteTombstones(tombstoneIds)
        }
    }

    /**
     * Lengkapi nama kategori transaksi yang kosong (hasil pull saat kategori
     * belum selesai disinkronkan). Idempoten; nama diambil dari tabel kategori
     * lokal yang sudah selaras dengan server.
     */
    suspend fun backfillTransactionCategories() {
        try {
            val user = supabase.auth.currentUserOrNull() ?: return
            val categories = categoryDao.getAll().associateBy { it.id }
            for (t in transactionDao.getAll(user.id)) {
                val name = t.categoryId?.let { categories[it]?.name }
                if (!name.isNullOrEmpty() && t.category != name) {
                    transactionDao.updateCategoryName(t.id, name)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("SyncRepository", "Backfill nama kategori gagal", e)
        }
    }

    private suspend fun syncCategories(userId: String) {
        runCatchingBlock("categories") {
            val pending = categoryDao.pending(userId)
            if (pending.isNotEmpty()) {
                supabase.postgrest.from("categories")
                    .upsert(pending.map { it.toRemoteJson(userId) }) { onConflict = "id" }
                categoryDao.updateSyncState(pending.map { it.id }, SyncState.SYNCED)
            }
        }
    }

    private suspend fun syncAccounts(userId: String) {
        runCatchingBlock("accounts") {
            val pending = accountDao.pending(userId)
            if (pending.isNotEmpty()) {
                supabase.postgrest.from("accounts")
                    .upsert(pending.map { it.toRemoteJson(userId) }) { onConflict = "id" }
                accountDao.updateSyncState(pending.map { it.id }, SyncState.SYNCED)
            }
        }
    }

    private suspend fun syncTransactions(userId: String) {
        runCatchingBlock("transactions") {
            val pending = transactionDao.pending(userId)
            if (pending.isNotEmpty()) {
                supabase.postgrest.from("transactions")
                    .upsert(pending.map { it.toRemoteJson(userId) }) { onConflict = "id" }
                transactionDao.updateSyncState(pending.map { it.id }, SyncState.SYNCED)
            }
        }
    }

    private suspend fun syncDebts(userId: String) {
        runCatchingBlock("debts") {
            val pending = debtDao.pending(userId)
            if (pending.isNotEmpty()) {
                supabase.postgrest.from("debts")
                    .upsert(pending.map { it.toRemoteJson(userId) }) { onConflict = "id" }
                debtDao.updateSyncState(pending.map { it.id }, SyncState.SYNCED)
            }
        }
    }

    private inline fun runCatchingBlock(table: String, block: () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Offline/gagal: biarkan PENDING, coba lagi nanti.
            Log.e("SyncRepository", "Sinkron tabel '$table' gagal", e)
        }
    }
}
