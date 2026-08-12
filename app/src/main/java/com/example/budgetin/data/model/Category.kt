package com.example.budgetin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Kategori transaksi dengan emoji dan warna (disimpan sebagai ARGB Long agar
 * data layer tetap murni tanpa dependensi Compose).
 *
 * Sekaligus entity Room; kategori default dari Supabase memakai UUID aslinya,
 * sehingga id lokal sama dengan id server.
 */
@Entity(tableName = "categories")
data class Category(
    val name: String,
    val emoji: String,
    val color: Long,
    val type: TransactionType = TransactionType.EXPENSE,
    /** true = bawaan/shared dari Supabase; false = kategori custom milik user. */
    val isDefault: Boolean = true,
    /** ID user pemilik (kosong untuk kategori default/shared). */
    val userId: String = "",
    val syncState: SyncState = SyncState.PENDING,
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
) {
    companion object {
        val EXPENSE_CATEGORIES = listOf(
            Category("Makanan", "🍜", 0xFFF59E0B, TransactionType.EXPENSE, syncState = SyncState.SYNCED),
            Category("Transportasi", "🚌", 0xFF3B82F6, TransactionType.EXPENSE, syncState = SyncState.SYNCED),
            Category("Belanja", "🛍", 0xFFEC4899, TransactionType.EXPENSE, syncState = SyncState.SYNCED),
            Category("Tagihan", "🧾", 0xFF8B5CF6, TransactionType.EXPENSE, syncState = SyncState.SYNCED),
            Category("Hiburan", "🎮", 0xFF06B6D4, TransactionType.EXPENSE, syncState = SyncState.SYNCED),
            Category("Kesehatan", "💊", 0xFFEF4444, TransactionType.EXPENSE, syncState = SyncState.SYNCED),
            Category("Pendidikan", "📚", 0xFF10B981, TransactionType.EXPENSE, syncState = SyncState.SYNCED),
            Category("Lainnya", "📦", 0xFF6B7280, TransactionType.EXPENSE, syncState = SyncState.SYNCED),
        )

        val INCOME_CATEGORIES = listOf(
            Category("Gaji", "💰", 0xFF10B981, TransactionType.INCOME, syncState = SyncState.SYNCED),
            Category("Bonus", "🎁", 0xFFF59E0B, TransactionType.INCOME, syncState = SyncState.SYNCED),
            Category("Jualan", "🏪", 0xFF3B82F6, TransactionType.INCOME, syncState = SyncState.SYNCED),
            Category("Investasi", "📈", 0xFF8B5CF6, TransactionType.INCOME, syncState = SyncState.SYNCED),
            Category("Lainnya", "💵", 0xFF6B7280, TransactionType.INCOME, syncState = SyncState.SYNCED),
        )

        /** Kategori default bila nama tidak ditemukan (mis. data lama). */
        val FALLBACK = Category("Lainnya", "📦", 0xFF6B7280, syncState = SyncState.SYNCED)

        fun forType(type: TransactionType): List<Category> = when (type) {
            TransactionType.EXPENSE -> EXPENSE_CATEGORIES
            TransactionType.INCOME -> INCOME_CATEGORIES
            TransactionType.TRANSFER -> emptyList()
        }

        fun find(name: String, type: TransactionType): Category =
            forType(type).firstOrNull { it.name == name } ?: FALLBACK
    }
}
