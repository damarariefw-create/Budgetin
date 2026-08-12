package com.example.budgetin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Penanda (tombstone) penghapusan transaksi untuk sinkronisasi offline-first.
 *
 * Saat transaksi dihapus secara lokal, baris ini disimpan agar:
 * - penghapusan ikut dikirim ke Supabase (hapus permanen di server), dan
 * - transaksi yang sama TIDAK ditarik (pull) kembali dari server saat
 *   login/restart (yang membuat saldo dompet kembali seperti sebelum dihapus).
 *
 * Baris dihapus setelah penghapusan berhasil dikirim ke server.
 */
@Entity(tableName = "deleted_transactions")
data class TransactionTombstone(
    @PrimaryKey
    val id: String,
    val deletedAt: Long = System.currentTimeMillis(),
)
