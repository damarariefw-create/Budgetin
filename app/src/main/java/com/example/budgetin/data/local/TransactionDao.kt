package com.example.budgetin.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.budgetin.data.model.SyncState
import com.example.budgetin.data.model.Transaction
import com.example.budgetin.data.model.TransactionTombstone
import kotlinx.coroutines.flow.Flow

/** Hasil agregasi pengeluaran per kategori (query GROUP BY). */
data class CategoryTotal(
    val category: String,
    val total: Double,
)

/** Pergerakan saldo satu akun (keluar/masuk) untuk menghitung saldo berjalan. */
data class AccountMovement(
    val accountId: String,
    val movement: Double,
)

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Insert
    suspend fun insertAll(transactions: List<Transaction>)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): Transaction?

    /** ID semua transaksi yang memakai [accountId] (sumber atau tujuan transfer). */
    @Query("SELECT id FROM transactions WHERE accountId = :accountId OR transferToAccountId = :accountId")
    suspend fun idsByAccount(accountId: String): List<String>

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    // ---------------------------------------------------------------------
    // Tombstone penghapusan: agar hapus transaksi ikut tersinkron ke server
    // dan tidak ditarik kembali saat pull (multi-perangkat / restart).
    // ---------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTombstone(tombstone: TransactionTombstone)

    /**
     * ID semua transaksi yang dihapus lokal (jumlahnya kecil & transien,
     * langsung dihapus setelah push sukses). Dipakai untuk push ke server
     * dan untuk memeriksa saat pull agar transaksi tidak "hidup lagi".
     */
    @Query("SELECT id FROM deleted_transactions")
    suspend fun allTombstoneIds(): List<String>

    @Query("DELETE FROM deleted_transactions WHERE id IN (:ids)")
    suspend fun deleteTombstones(ids: List<String>)

    @Query("SELECT * FROM transactions WHERE userId = :userId")
    suspend fun getAll(userId: String): List<Transaction>

    @Query("UPDATE transactions SET category = :name WHERE id = :id")
    suspend fun updateCategoryName(id: String, name: String)

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun observeAll(userId: String): Flow<List<Transaction>>

    /** Transaksi yang memakai [accountId] (sebagai sumber atau tujuan transfer). */
    @Query(
        "SELECT * FROM transactions WHERE userId = :userId AND (accountId = :accountId " +
            "OR transferToAccountId = :accountId) ORDER BY timestamp DESC"
    )
    fun observeByAccount(userId: String, accountId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(userId: String, limit: Int): Flow<List<Transaction>>

    /** Transaksi mulai dari [start] (naik) untuk dikelompokkan per bulan di lapisan Kotlin. */
    @Query("SELECT * FROM transactions WHERE userId = :userId AND timestamp >= :start ORDER BY timestamp ASC")
    fun observeSince(userId: String, start: Long): Flow<List<Transaction>>

    @Query("SELECT COALESCE(SUM(amount - adminFee), 0) FROM transactions WHERE userId = :userId AND type = 'INCOME'")
    fun observeTotalIncome(userId: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount + adminFee), 0) FROM transactions WHERE userId = :userId AND type = 'EXPENSE'")
    fun observeTotalExpense(userId: String): Flow<Double>

    @Query(
        "SELECT COALESCE(SUM(amount - adminFee), 0) FROM transactions " +
            "WHERE userId = :userId AND type = 'INCOME' AND timestamp BETWEEN :start AND :end"
    )
    fun observeIncomeBetween(userId: String, start: Long, end: Long): Flow<Double>

    @Query(
        "SELECT COALESCE(SUM(amount + adminFee), 0) FROM transactions " +
            "WHERE userId = :userId AND type = 'EXPENSE' AND timestamp BETWEEN :start AND :end"
    )
    fun observeExpenseBetween(userId: String, start: Long, end: Long): Flow<Double>

    /** Pengeluaran per kategori pada rentang tanggal (untuk statistik/dashboard). */
    @Query(
        "SELECT category, SUM(amount + adminFee) AS total FROM transactions " +
            "WHERE userId = :userId AND type = 'EXPENSE' AND timestamp BETWEEN :start AND :end " +
            "GROUP BY category ORDER BY total DESC"
    )
    fun observeExpenseByCategory(userId: String, start: Long, end: Long): Flow<List<CategoryTotal>>

    /** Uang yang keluar dari tiap akun (income masuk, expense/transfer keluar). */
    @Query(
        "SELECT accountId, COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount - adminFee " +
            "WHEN type = 'EXPENSE' THEN -(amount + adminFee) " +
            "WHEN type = 'TRANSFER' THEN -(amount + adminFee) END), 0) AS movement " +
            "FROM transactions WHERE userId = :userId AND isCompleted = 1 GROUP BY accountId"
    )
    fun observeOutflowByAccount(userId: String): Flow<List<AccountMovement>>

    /** Uang masuk ke akun tujuan dari transfer. */
    @Query(
        "SELECT transferToAccountId AS accountId, COALESCE(SUM(amount), 0) AS movement " +
            "FROM transactions WHERE userId = :userId AND type = 'TRANSFER' AND isCompleted = 1 " +
            "AND transferToAccountId IS NOT NULL GROUP BY transferToAccountId"
    )
    fun observeInflowByAccount(userId: String): Flow<List<AccountMovement>>

    /** Transaksi yang belum terkirim ke Supabase (milik user pemilik). */
    @Query("SELECT * FROM transactions WHERE userId = :userId AND syncState IN ('PENDING', 'FAILED')")
    suspend fun pending(userId: String): List<Transaction>

    @Query("UPDATE transactions SET syncState = :state WHERE id IN (:ids)")
    suspend fun updateSyncState(ids: List<String>, state: SyncState)

    @Query("UPDATE transactions SET categoryId = :newId WHERE categoryId = :oldId")
    suspend fun replaceCategoryId(oldId: String, newId: String)

    /** Aduk data lama (userId kosong) ke user yang baru login, sekali pakai. */
    @Query("UPDATE transactions SET userId = :userId WHERE userId = ''")
    suspend fun adoptOrphans(userId: String)
}
