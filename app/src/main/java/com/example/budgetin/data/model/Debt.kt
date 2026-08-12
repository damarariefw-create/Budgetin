package com.example.budgetin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Arah hutang-piutang.
 * - OWE  = saya berhutang ke orang lain (uang keluar nanti).
 * - OWED = saya menghutangi orang lain / piutang (uang masuk nanti).
 */
enum class DebtType(val label: String) {
    OWE("Hutang"),
    OWED("Piutang"),
}

/**
 * Catatan hutang-piutang. Offline-first: baru berstatus PENDING sampai
 * berhasil dikirim ke Supabase (sinkron penuh mulai Fase 5).
 *
 * Dua dompet terkait:
 * - [accountId]      = dompet saat mencatat. Untuk Piutang: uang dipinjamkan DARI dompet ini;
 *                      untuk Hutang: uang pinjaman MASUK KE dompet ini.
 * - [settledAccountId] = dompet pelunasan. Untuk Piutang: pelunasan MASUK KE dompet ini;
 *                      untuk Hutang: pelunasan DIAMBIL DARI dompet ini.
 */
@Entity(tableName = "debts")
data class Debt(
    val counterpartName: String,
    val type: DebtType,
    val amount: Double,
    val note: String = "",
    val dueDate: Long? = null,
    val isSettled: Boolean = false,
    /** ID dompet/akun saat mencatat (dompet asal/terima uang). */
    val accountId: String? = null,
    /** ID dompet/akun saat dilunasi (dompet terima/pembayar). */
    val settledAccountId: String? = null,
    /** ID user pemilik catatan (isolasi data antar-user). */
    val userId: String = "",
    val syncState: SyncState = SyncState.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
)
