package com.example.budgetin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Penanda (tombstone) penghapusan dompet/rekening untuk sinkronisasi offline-first.
 *
 * Saat dompet dihapus secara lokal, baris ini disimpan agar:
 * - penghapusan ikut dikirim ke Supabase (hapus permanen di server), dan
 * - dompet yang sama TIDAK ditarik (pull) kembali dari server saat
 *   login/restart (yang membuat dompet yang dihapus "hidup lagi").
 *
 * Baris dihapus setelah penghapusan berhasil dikirim ke server.
 */
@Entity(tableName = "deleted_accounts")
data class AccountTombstone(
    @PrimaryKey
    val id: String,
    val deletedAt: Long = System.currentTimeMillis(),
)
