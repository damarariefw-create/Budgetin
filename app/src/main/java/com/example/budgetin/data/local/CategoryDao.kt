package com.example.budgetin.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.budgetin.data.model.Category
import com.example.budgetin.data.model.SyncState
import com.example.budgetin.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<Category>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: Category): Long

    /** Kategori yang dipakai di form transaksi: default/shared + milik user. */
    @Query("SELECT * FROM categories WHERE type = :type AND (userId = '' OR userId = :userId) ORDER BY isDefault DESC, name ASC")
    fun observeByType(userId: String, type: TransactionType): Flow<List<Category>>

    /** Semua kategori default + milik user (untuk filter riwayat). */
    @Query("SELECT * FROM categories WHERE userId = '' OR userId = :userId ORDER BY isDefault DESC, name ASC")
    fun observeAll(userId: String): Flow<List<Category>>

    /** Hanya kategori custom milik user (untuk layar kelola kategori). */
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY type ASC, name ASC")
    fun observeCustom(userId: String): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAll(): List<Category>

    @Query("SELECT COUNT(*) FROM categories WHERE userId = :userId")
    suspend fun count(userId: String): Int

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): Category?

    @Query("SELECT * FROM categories WHERE name = :name AND type = :type AND (userId = '' OR userId = :userId) LIMIT 1")
    suspend fun findByName(userId: String, name: String, type: TransactionType): Category?

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Kategori custom yang belum terkirim ke Supabase. */
    @Query("SELECT * FROM categories WHERE userId = :userId AND syncState IN ('PENDING', 'FAILED')")
    suspend fun pending(userId: String): List<Category>

    @Query("UPDATE categories SET syncState = :state WHERE id IN (:ids)")
    suspend fun updateSyncState(ids: List<String>, state: SyncState)

    /** Ganti primary key kategori (dipakai saat menyamakan id lokal dengan id server). */
    @Query("UPDATE categories SET id = :newId WHERE id = :oldId")
    suspend fun renameId(oldId: String, newId: String)
}
