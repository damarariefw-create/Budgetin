package com.example.budgetin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Jenis transaksi: pemasukan, pengeluaran, atau transfer antar akun.
 */
enum class TransactionType(val label: String) {
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran"),
    TRANSFER("Transfer"),
}

/**
 * Status transaksi. OUTSTANDING = transaksi belum lunas (mis. cicilan).
 */
enum class TransactionStatus(val label: String) {
    CONFIRMED("Terkonfirmasi"),
    OUTSTANDING("Belum Lunas"),
}

/**
 * Status sinkronisasi ke Supabase (offline-first: lokal dulu, kirim belakangan).
 */
enum class SyncState {
    PENDING,
    SYNCED,
    FAILED,
}

/**
 * Periode pengulangan transaksi otomatis (untuk pemasukan rutin).
 */
enum class RecurringPeriod(val label: String) {
    DAILY("Harian"),
    WEEKLY("Mingguan"),
    MONTHLY("Bulanan"),
    YEARLY("Tahunan"),
}

/**
 * Model transaksi sekaligus entity Room.
 * - id UUID lokal yang juga dipakai di Supabase (memudahkan sinkronisasi).
 * - offline-first: setiap transaksi baru berawal PENDING, lalu dikirim ke server.
 * - [isCompleted] = true berarti "Sudah Terjadi" (memengaruhi saldo),
 *                  false berarti "Belum Terjadi" (catatan, belum masuk saldo).
 */
@Entity(tableName = "transactions")
data class Transaction(
    val type: TransactionType,
    val amount: Double,
    val adminFee: Double = 0.0,
    /** Nama kategori (denormalisasi untuk tampilan cepat). */
    val category: String = "",
    /** UUID kategori (referensi tabel categories). */
    val categoryId: String? = null,
    /** UUID akun asal. */
    val accountId: String = "",
    /** UUID akun tujuan (khusus tipe TRANSFER). */
    val transferToAccountId: String? = null,
    val note: String = "",
    val status: TransactionStatus = TransactionStatus.CONFIRMED,
    /** true = Sudah Terjadi (dihitung ke saldo), false = Belum Terjadi. */
    val isCompleted: Boolean = true,
    val isRecurring: Boolean = false,
    val recurringPeriod: RecurringPeriod? = null,
    val timestamp: Long = System.currentTimeMillis(),
    /** ID user pemilik transaksi (isolasi data antar-user). */
    val userId: String = "",
    val syncState: SyncState = SyncState.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
)
