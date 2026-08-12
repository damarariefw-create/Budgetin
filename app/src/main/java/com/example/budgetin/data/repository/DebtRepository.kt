package com.example.budgetin.data.repository

import com.example.budgetin.data.CurrentUser
import com.example.budgetin.data.local.DebtDao
import com.example.budgetin.data.model.Debt
import com.example.budgetin.data.model.DebtType
import com.example.budgetin.data.model.SyncState
import com.example.budgetin.data.model.Transaction
import com.example.budgetin.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebtRepository @Inject constructor(
    private val dao: DebtDao,
    private val syncRepository: SyncRepository,
    private val transactionRepository: TransactionRepository,
    private val currentUser: CurrentUser,
) {

    fun observeAll(): Flow<List<Debt>> =
        currentUser.idFlow.flatMapLatest { dao.observeAll(it) }

    fun observeActive(): Flow<List<Debt>> =
        currentUser.idFlow.flatMapLatest { dao.observeActive(it) }

    /**
     * Catat hutang/piutang baru. Bila dompet pinjaman dipilih, uang langsung
     * berpindah: Piutang = dana diambil (pengeluaran), Hutang = uang masuk (pemasukan).
     */
    suspend fun add(debt: Debt): Long {
        val saved = dao.insert(debt.copy(userId = currentUser.id()))
        createLoanTransaction(debt)
        return saved
    }

    private suspend fun createLoanTransaction(debt: Debt) {
        val loanAccountId = debt.accountId ?: return
        val isOwed = debt.type == DebtType.OWED
        transactionRepository.add(
            Transaction(
                type = if (isOwed) TransactionType.EXPENSE else TransactionType.INCOME,
                amount = debt.amount,
                category = if (isOwed) "Piutang" else "Hutang",
                accountId = loanAccountId,
                note = if (isOwed) "Pinjaman ke ${debt.counterpartName}".trim()
                else "Pinjaman dari ${debt.counterpartName}".trim(),
                timestamp = debt.createdAt,
                syncState = SyncState.PENDING,
            )
        )
    }

    suspend fun delete(debt: Debt) = dao.delete(debt)

    suspend fun deleteById(id: String) = dao.deleteById(id)

    /**
     * Tandai lunas. Bila ada dompet pelunasan (settledAccountId, fallback accountId),
     * uangnya dipindahkan:
     * - OWE  (hutang)  -> transaksi EXPENSE dari dompet tsb (uang keluar).
     * - OWED (piutang) -> transaksi INCOME ke dompet tsb (uang masuk).
     */
    suspend fun settle(debt: Debt) {
        val settling = !debt.isSettled
        val settleAccount = debt.settledAccountId ?: debt.accountId
        if (settling && settleAccount != null) {
            val type = if (debt.type == DebtType.OWED) TransactionType.INCOME else TransactionType.EXPENSE
            val category = if (debt.type == DebtType.OWED) "Piutang" else "Hutang"
            transactionRepository.add(
                Transaction(
                    type = type,
                    amount = debt.amount,
                    category = category,
                    accountId = settleAccount,
                    note = "Pelunasan ${debt.type.label.lowercase()} ${debt.counterpartName}".trim(),
                    syncState = SyncState.PENDING,
                )
            )
        }
        dao.setSettled(debt.id, settling, System.currentTimeMillis())
        syncRepository.syncAll()
    }

    suspend fun setSettled(id: String, settled: Boolean) =
        dao.setSettled(id, settled, System.currentTimeMillis())

    /**
     * Tarik hutang-piutang dari server ke lokal (untuk perangkat baru /
     * multi-perangkat). Data milik user yang sedang login saja; dilewati saat offline.
     * Data lokal yang masih PENDING tidak ditimpa; sisanya diperbarui bila
     * versi server lebih baru.
     */
    suspend fun reconcileFromRemote() {
        val userId = currentUser.id()
        if (userId.isEmpty()) return
        val remote = runCatching { syncRepository.fetchRemoteDebts() }.getOrNull() ?: return
        if (remote.isEmpty()) return
        for (r in remote) {
            val local = dao.getById(r.id)
            when {
                local == null -> dao.insert(
                    Debt(
                        counterpartName = r.counterpartName,
                        type = r.type,
                        amount = r.amount,
                        note = r.note,
                        dueDate = r.dueDate,
                        isSettled = r.isSettled,
                        accountId = r.accountId,
                        settledAccountId = r.settledAccountId,
                        userId = userId,
                        syncState = SyncState.SYNCED,
                        updatedAt = r.updatedAt,
                        id = r.id,
                    )
                )

                local.syncState == SyncState.SYNCED && r.updatedAt > local.updatedAt -> dao.insert(
                    local.copy(
                        counterpartName = r.counterpartName,
                        type = r.type,
                        amount = r.amount,
                        note = r.note,
                        dueDate = r.dueDate,
                        isSettled = r.isSettled,
                        accountId = r.accountId,
                        settledAccountId = r.settledAccountId,
                        syncState = SyncState.SYNCED,
                        updatedAt = r.updatedAt,
                    )
                )
            }
        }
    }

    /** Aduk catatan lama (sebelum pemisahan per-user) ke user yang baru login. */
    suspend fun adoptOrphans() {
        val userId = currentUser.id()
        if (userId.isNotEmpty()) dao.adoptOrphans(userId)
    }
}
