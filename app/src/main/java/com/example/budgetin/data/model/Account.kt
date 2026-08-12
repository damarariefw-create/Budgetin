package com.example.budgetin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Jenis akun/dompet. Nilai name dipakai saat sinkron ke kolom `account_type`.
 */
enum class AccountType(val label: String) {
    CASH("Tunai"),
    BANK("Bank"),
    EWALLET("E-Wallet"),
    DEBT("Hutang"),
}

/**
 * Akun/dompet tempat uang disimpan.
 * - [balance] = saldo awal (opening). Saldo berjalan dihitung dari transaksi.
 * - [includeInTotal] = false bila saldo dompet tidak ikut saldo utama (mis. dompet khusus).
 * - [parentId] = null berarti Dompet Utama (Main) yang berdiri sendiri;
 *                berisi UUID dompet induk bila akun ini adalah Branch/Ranting.
 * - offline-first: akun baru berstatus PENDING sampai berhasil dikirim.
 */
@Entity(tableName = "accounts")
data class Account(
    val name: String,
    val type: AccountType = AccountType.CASH,
    val balance: Double = 0.0,
    val icon: String = "💳",
    val color: Long = 0xFF10B981,
    val includeInTotal: Boolean = true,
    val isArchived: Boolean = false,
    /** UUID dompet induk (null = Dompet Utama/Main). */
    val parentId: String? = null,
    /** ID user pemilik akun (isolasi data antar-user di perangkat yang sama). */
    val userId: String = "",
    val syncState: SyncState = SyncState.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
) {
    /** true bila dompet ini adalah Branch/Ranting dari dompet lain. */
    val isBranch: Boolean get() = parentId != null

    /** true bila dompet ini Dompet Utama (Main) yang berdiri sendiri. */
    val isMain: Boolean get() = parentId == null

    companion object {
        /** Akun bawaan yang dibuat otomatis saat pertama kali memakai aplikasi. */
        val DEFAULT = Account(
            name = "Dompet",
            type = AccountType.CASH,
            icon = "👛",
            color = 0xFF00A86B,
        )
    }
}
