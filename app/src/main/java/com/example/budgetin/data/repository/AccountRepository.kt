package com.example.budgetin.data.repository

import com.example.budgetin.data.CurrentUser
import com.example.budgetin.data.local.AccountDao
import com.example.budgetin.data.local.TransactionDao
import com.example.budgetin.data.model.Account
import com.example.budgetin.data.model.AccountTombstone
import com.example.budgetin.data.model.SyncState
import com.example.budgetin.data.model.TransactionTombstone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val dao: AccountDao,
    private val transactionDao: TransactionDao,
    private val currentUser: CurrentUser,
    private val syncRepository: SyncRepository,
) {

    fun observeAll(): Flow<List<Account>> =
        currentUser.idFlow.flatMapLatest { dao.observeAll(it) }

    fun observeAllIncludingArchived(): Flow<List<Account>> =
        currentUser.idFlow.flatMapLatest { dao.observeAllIncludingArchived(it) }

    /** Dompet Utama (Main) aktif: parentId null. */
    fun observeMains(): Flow<List<Account>> =
        currentUser.idFlow.flatMapLatest { dao.observeMains(it) }

    /** Branch/Ranting aktif milik dompet induk [parentId]. */
    fun observeChildren(parentId: String): Flow<List<Account>> =
        currentUser.idFlow.flatMapLatest { dao.observeChildren(it, parentId) }

    /**
     * Saldo berjalan tiap akun = saldo awal + pergerakan semua transaksi
     * (income masuk, expense/transfer keluar, transfer masuk ke tujuan).
     *
     * Untuk Dompet Utama (Main), saldo yang ditampilkan = gabungan dirinya
     * sendiri + seluruh branch-nya (saldo individu branch tetap tampil sendiri).
     * Otomatis re-emit saat akun atau transaksi berubah (invalidasi Room).
     */
    fun observeBalances(): Flow<List<Pair<Account, Double>>> =
        observeBalancesFor(observeAll())

    fun observeBalancesIncludingArchived(): Flow<List<Pair<Account, Double>>> =
        observeBalancesFor(observeAllIncludingArchived())

    /** Saldo hanya untuk Dompet Utama (Main) — dipakai total saldo & carousel.
     *  Dihitung dari SEMUA akun (main + branch) lalu difilter ke main saja,
     *  agar saldo main sudah termasuk gabungan seluruh branch-nya. */
    fun observeMainBalances(): Flow<List<Pair<Account, Double>>> =
        observeBalancesFor(observeAll()).map { pairs ->
            pairs.filter { it.first.parentId == null }
        }

    /**
     * Saldo milik sendiri tiap akun (tanpa agregasi branch).
     * Dipakai detail dompet: nominal dompet itu sendiri + nominal tiap branch.
     */
    fun observeOwnBalances(): Flow<List<Pair<Account, Double>>> =
        observeOwnBalancesFor(observeAll())

    private fun observeOwnBalancesFor(accountsFlow: Flow<List<Account>>): Flow<List<Pair<Account, Double>>> =
        currentUser.idFlow.flatMapLatest { userId ->
            combine(
                accountsFlow,
                transactionDao.observeOutflowByAccount(userId),
                transactionDao.observeInflowByAccount(userId),
            ) { accounts, outflows, inflows ->
                val out = outflows.associate { it.accountId to it.movement }
                val inc = inflows.associate { it.accountId to it.movement }
                accounts.map { account ->
                    account to (account.balance + (inc[account.id] ?: 0.0) + (out[account.id] ?: 0.0))
                }
            }
        }

    private fun observeBalancesFor(accountsFlow: Flow<List<Account>>): Flow<List<Pair<Account, Double>>> =
        observeOwnBalancesFor(accountsFlow).map { pairs ->
            val own = pairs.associate { it.first.id to it.second }
            val childrenByParent = pairs
                .filter { it.first.parentId != null && !it.first.isArchived }
                .groupBy { it.first.parentId!! }
            pairs.map { (account, ownBalance) ->
                val displayed = if (account.parentId == null) {
                    ownBalance + (childrenByParent[account.id]?.sumOf { own[it.first.id]!! } ?: 0.0)
                } else {
                    ownBalance
                }
                account to displayed
            }
        }

    /** Buat akun default hanya untuk user yang benar-benar baru (belum pernah
     *  menghapus dompet), agar dompet yang sengaja dihapus tidak "hidup lagi". */
    suspend fun ensureDefault() {
        val userId = currentUser.id()
        if (userId.isNotEmpty() && !hasDeletedAccounts && dao.count(userId) == 0) {
            dao.upsert(Account.DEFAULT.copy(userId = userId))
        }
    }

    /** Aduk akun lama (sebelum pemisahan per-user) ke user yang baru login. */
    suspend fun adoptOrphans() {
        val userId = currentUser.id()
        if (userId.isNotEmpty()) dao.adoptOrphans(userId)
    }

    suspend fun add(account: Account): Long =
        dao.upsert(account.copy(userId = currentUser.id()))

    suspend fun update(account: Account): Long =
        dao.upsert(account.copy(userId = currentUser.id()))

    suspend fun archive(id: String) = dao.setArchived(id, true)

    suspend fun restore(id: String) = dao.setArchived(id, false)

    /**
     * User pernah menghapus dompet (lokal atau dari perangkat lain). Dipakai
     * agar [ensureDefault] TIDAK membuat dompet "Dompet" baru lagi setelah
     * user sengaja menghapus semua dompetnya (di perangkat mana pun).
     */
    private var hasDeletedAccounts = false

    suspend fun getById(id: String): Account? = dao.getById(id)

    /**
     * Hapus akun beserta seluruh transaksinya; bila Dompet Utama, seluruh
     * branch-nya ikut terhapus. Setiap penghapusan dicatat tombstone (akun &
     * transaksi) agar ikut terkirim ke server dan perangkat lain dengan akun
     * yang sama ikut menghapus datanya.
     */
    suspend fun delete(id: String) {
        val account = dao.getById(id) ?: return
        // Kumpulkan akun yang ikut terhapus: dompet induk beserta seluruh cabangnya.
        val idsToDelete = if (account.parentId == null) {
            listOf(id) + dao.childIds(id)
        } else {
            listOf(id)
        }
        idsToDelete.forEach { deleteAccountAndDependents(it) }
    }

    /**
     * Hapus satu akun + seluruh transaksinya secara lokal dan catat tombstone
     * (akun & transaksi) agar penghapusan ikut dikirim ke server (tombstone
     * transaksi membuat riwayat yang memakai dompet ini ikut terhapus di
     * perangkat lain juga).
     */
    private suspend fun deleteAccountAndDependents(id: String) {
        hasDeletedAccounts = true
        dao.insertTombstone(AccountTombstone(id))
        val transactionIds = transactionDao.idsByAccount(id)
        transactionIds.forEach { transactionDao.insertTombstone(TransactionTombstone(it)) }
        if (transactionIds.isNotEmpty()) {
            transactionDao.deleteByIds(transactionIds)
        }
        dao.deleteById(id)
    }

    /**
     * Tarik dompet dari server ke lokal (untuk perangkat baru / multi-perangkat).
     * 1) Dompet yang dihapus di perangkat lain ikut dihapus di sini (beserta
     *    transaksinya) sesuai tombstone server, supaya tidak "hidup lagi".
     * 2) Hanya menyentuh data yang belum ada secara lokal, atau yang lebih lama
     *    dari server; data lokal yang masih PENDING (belum terkirim) TIDAK ditimpa.
     */
    suspend fun reconcileFromRemote() {
        val userId = currentUser.id()
        if (userId.isEmpty()) return
        // 1) Tarik semua dompet (termasuk penanda hapus sync_state='deleted').
        val remote = runCatching { syncRepository.fetchRemoteAccounts() }.getOrNull() ?: return
        if (remote.isEmpty()) return
        // 2) Hapus dompet yang ditandai sync_state='deleted' di server (dihapus
        //    di perangkat lain), beserta transaksi yang mereferensikannya.
        remote.filter { it.isDeletedMarker }.forEach { r ->
            if (dao.getById(r.id) == null && transactionDao.idsByAccount(r.id).isEmpty()) return@forEach
            deleteAccountAndDependents(r.id)
        }
        // 3) Tarik dompet lain; lewati penanda hapus & tombstone lokal.
        val tombstones = dao.allTombstoneIds().toSet()
        for (r in remote) {
            if (r.isDeletedMarker || r.id in tombstones) continue
            val local = dao.getById(r.id)
            when {
                local == null -> dao.upsert(
                    Account(
                        name = r.name,
                        type = r.type,
                        balance = r.balance,
                        icon = r.icon,
                        color = r.color,
                        includeInTotal = r.countInTotal,
                        isArchived = r.isArchived,
                        parentId = r.parentId,
                        userId = userId,
                        syncState = SyncState.SYNCED,
                        createdAt = r.createdAt,
                        updatedAt = r.updatedAt,
                        id = r.id,
                    )
                )

                local.syncState == SyncState.SYNCED && r.updatedAt > local.updatedAt -> dao.upsert(
                    local.copy(
                        name = r.name,
                        type = r.type,
                        balance = r.balance,
                        icon = r.icon,
                        color = r.color,
                        includeInTotal = r.countInTotal,
                        isArchived = r.isArchived,
                        parentId = r.parentId,
                        createdAt = r.createdAt,
                        updatedAt = r.updatedAt,
                    )
                )
            }
        }
    }
}
