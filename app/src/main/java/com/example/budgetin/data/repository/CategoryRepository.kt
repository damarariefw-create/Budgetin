package com.example.budgetin.data.repository

import com.example.budgetin.data.CurrentUser
import com.example.budgetin.data.local.CategoryDao
import com.example.budgetin.data.local.TransactionDao
import com.example.budgetin.data.model.Category
import com.example.budgetin.data.model.SyncState
import com.example.budgetin.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val dao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val syncRepository: SyncRepository,
    private val currentUser: CurrentUser,
) {

    /** Kategori untuk form transaksi: default/shared + custom milik user. */
    fun observeByType(type: TransactionType): Flow<List<Category>> =
        currentUser.idFlow.flatMapLatest { dao.observeByType(it, type) }

    /** Semua kategori (default + custom) untuk filter riwayat. */
    fun observeAll(): Flow<List<Category>> =
        currentUser.idFlow.flatMapLatest { dao.observeAll(it) }

    /** Hanya kategori custom milik user (layar kelola kategori). */
    fun observeCustom(): Flow<List<Category>> =
        currentUser.idFlow.flatMapLatest { dao.observeCustom(it) }

    /** Isi kategori default lokal bila belum ada (bisa offline). */
    suspend fun ensureSeeded() {
        if (dao.getAll().any { it.isDefault }) return
        dao.upsertAll(Category.EXPENSE_CATEGORIES + Category.INCOME_CATEGORIES)
    }

    /**
     * Sinkronkan kategori default dengan server: gunakan UUID server sebagai id lokal
     * (agar referensi transaksi cocok saat push). Hanya menyentuh kategori default.
     */
    suspend fun reconcileFromRemote() {
        val remote = runCatching { syncRepository.fetchDefaultCategories() }.getOrNull() ?: return
        if (remote.isEmpty()) return
        val localDefaults = dao.getAll().filter { it.userId.isEmpty() }
        for (r in remote) {
            val local = localDefaults.firstOrNull { it.name == r.name && it.type == r.type }
            when {
                local == null -> dao.upsert(
                    Category(
                        name = r.name,
                        emoji = r.emoji,
                        color = r.color,
                        type = r.type,
                        isDefault = true,
                        syncState = SyncState.SYNCED,
                        id = r.id,
                    )
                )

                local.id != r.id -> {
                    transactionDao.replaceCategoryId(local.id, r.id)
                    dao.renameId(local.id, r.id)
                }
            }
        }
    }

    /** Tarik kategori custom milik user dari server (untuk perangkat baru). */
    suspend fun reconcileCustomFromRemote() {
        val userId = currentUser.id()
        if (userId.isEmpty()) return
        val remote = runCatching { syncRepository.fetchCustomCategories() }.getOrNull() ?: return
        if (remote.isEmpty()) return
        val localIds = dao.getAll().map { it.id }.toSet()
        for (r in remote) {
            if (r.id in localIds) continue
            dao.upsert(
                Category(
                    name = r.name,
                    emoji = r.emoji,
                    color = r.color,
                    type = r.type,
                    isDefault = false,
                    userId = userId,
                    syncState = SyncState.SYNCED,
                    id = r.id,
                )
            )
        }
    }

    /** Buat kategori custom baru (user dipakai dari sesi aktif). */
    suspend fun add(category: Category): Long =
        dao.upsert(
            category.copy(
                userId = currentUser.id(),
                isDefault = false,
                syncState = SyncState.PENDING,
            )
        )

    /** Perbarui kategori custom milik user. */
    suspend fun update(category: Category): Long =
        dao.upsert(
            category.copy(
                userId = currentUser.id(),
                isDefault = false,
                syncState = SyncState.PENDING,
            )
        )

    suspend fun delete(category: Category) = dao.deleteById(category.id)

    suspend fun getById(id: String): Category? = dao.getById(id)

    /** Cek nama sudah dipakai (default atau custom user) untuk nama+tipe yang sama. */
    suspend fun exists(name: String, type: TransactionType, excludeId: String?): Boolean {
        val found = dao.findByName(currentUser.id(), name, type) ?: return false
        return found.id != excludeId
    }
}
