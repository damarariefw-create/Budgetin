package com.example.budgetin.data.repository

import com.example.budgetin.data.CurrentUser
import com.example.budgetin.data.local.CategoryTotal
import com.example.budgetin.data.local.TransactionDao
import com.example.budgetin.data.model.SyncState
import com.example.budgetin.data.model.Transaction
import com.example.budgetin.data.model.TransactionTombstone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

/** Satu-satunya sumber data transaksi untuk UI; dibungkus Hilt agar mudah di-test. */
@Singleton
class TransactionRepository @Inject constructor(
    private val dao: TransactionDao,
    private val currentUser: CurrentUser,
    private val syncRepository: SyncRepository,
) {

    fun observeAll(): Flow<List<Transaction>> =
        currentUser.idFlow.flatMapLatest { dao.observeAll(it) }

    fun observeByAccount(accountId: String): Flow<List<Transaction>> =
        currentUser.idFlow.flatMapLatest { dao.observeByAccount(it, accountId) }

    fun observeRecent(limit: Int): Flow<List<Transaction>> =
        currentUser.idFlow.flatMapLatest { dao.observeRecent(it, limit) }

    fun observeSince(start: Long): Flow<List<Transaction>> =
        currentUser.idFlow.flatMapLatest { dao.observeSince(it, start) }

    fun observeTotalIncome(): Flow<Double> =
        currentUser.idFlow.flatMapLatest { dao.observeTotalIncome(it) }

    fun observeTotalExpense(): Flow<Double> =
        currentUser.idFlow.flatMapLatest { dao.observeTotalExpense(it) }

    fun observeIncomeBetween(start: Long, end: Long): Flow<Double> =
        currentUser.idFlow.flatMapLatest { dao.observeIncomeBetween(it, start, end) }

    fun observeExpenseBetween(start: Long, end: Long): Flow<Double> =
        currentUser.idFlow.flatMapLatest { dao.observeExpenseBetween(it, start, end) }

    fun observeExpenseByCategory(start: Long, end: Long): Flow<List<CategoryTotal>> =
        currentUser.idFlow.flatMapLatest { dao.observeExpenseByCategory(it, start, end) }

    suspend fun add(transaction: Transaction): Long =
        dao.insert(transaction.copy(userId = currentUser.id()))

    suspend fun update(transaction: Transaction) =
        dao.update(transaction.copy(userId = currentUser.id()))

    suspend fun getById(id: String): Transaction? = dao.getById(id)

    /**
     * Hapus transaksi + catat tombstone agar penghapusan ikut dikirim ke server
     * dan transaksi tidak ditarik kembali saat sinkron berikutnya (yang membuat
     * saldo dompet kembali seperti sebelum transaksi dihapus).
     */
    suspend fun delete(transaction: Transaction) {
        dao.insertTombstone(TransactionTombstone(id = transaction.id))
        dao.delete(transaction)
    }

    /** Aduk transaksi lama (sebelum pemisahan per-user) ke user yang baru login. */
    suspend fun adoptOrphans() {
        val userId = currentUser.id()
        if (userId.isNotEmpty()) dao.adoptOrphans(userId)
    }

    /**
     * Tarik transaksi dari server ke lokal (untuk perangkat baru / multi-perangkat).
     * 1) Transaksi yang dihapus di perangkat lain ikut dihapus di sini sesuai
     *    tombstone server, supaya tidak "hidup lagi" (dan mengembalikan saldo).
     * 2) Hanya menyentuh data yang belum ada secara lokal, atau yang lebih lama
     *    dari server; data lokal yang masih PENDING (belum terkirim) TIDAK ditimpa.
     */
    suspend fun reconcileFromRemote() {
        val userId = currentUser.id()
        if (userId.isEmpty()) return
        // 1) Tarik semua transaksi (termasuk penanda hapus status='deleted').
        val remote = runCatching { syncRepository.fetchAllRemoteTransactions() }.getOrNull() ?: return
        if (remote.isEmpty()) return
        // 2) Hapus transaksi yang ditandai status='deleted' di server (dihapus di
        //    perangkat lain). Tombstone lokal dipasang agar tidak ditarik kembali
        //    bila penanda belum terlihat (race), lalu ikut terkirim saat push.
        val existingTombstones = dao.allTombstoneIds().toSet()
        remote.filter { it.isDeletedMarker }.forEach { r ->
            if (r.id !in existingTombstones && dao.getById(r.id) != null) {
                dao.insertTombstone(TransactionTombstone(id = r.id))
                dao.deleteById(r.id)
            }
        }
        // 3) Tarik/update transaksi lain; lewati penanda hapus & tombstone lokal.
        val tombstones = dao.allTombstoneIds().toSet()
        for (r in remote) {
            if (r.isDeletedMarker || r.id in tombstones) continue
            val local = dao.getById(r.id)
            when {
                local == null -> dao.insert(
                    Transaction(
                        type = r.type,
                        amount = r.amount,
                        adminFee = r.adminFee,
                        category = r.categoryName,
                        categoryId = r.categoryId,
                        accountId = r.accountId,
                        transferToAccountId = r.transferToAccountId,
                        note = r.note,
                        isCompleted = r.isCompleted,
                        timestamp = r.transactionDate,
                        userId = userId,
                        syncState = SyncState.SYNCED,
                        createdAt = r.createdAt,
                        updatedAt = r.updatedAt,
                        id = r.id,
                    )
                )

                local.syncState == SyncState.SYNCED && r.updatedAt > local.updatedAt -> dao.update(
                    local.copy(
                        type = r.type,
                        amount = r.amount,
                        adminFee = r.adminFee,
                        category = r.categoryName,
                        categoryId = r.categoryId,
                        accountId = r.accountId,
                        transferToAccountId = r.transferToAccountId,
                        note = r.note,
                        isCompleted = r.isCompleted,
                        timestamp = r.transactionDate,
                        createdAt = r.createdAt,
                        updatedAt = r.updatedAt,
                    )
                )
            }
        }
    }
}
