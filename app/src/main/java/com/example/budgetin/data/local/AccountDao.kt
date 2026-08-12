package com.example.budgetin.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.budgetin.data.model.Account
import com.example.budgetin.data.model.AccountTombstone
import com.example.budgetin.data.model.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: Account): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(accounts: List<Account>)

    @Query("SELECT * FROM accounts WHERE userId = :userId AND isArchived = 0 ORDER BY createdAt ASC")
    fun observeAll(userId: String): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY createdAt ASC")
    fun observeAllIncludingArchived(userId: String): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun getAll(userId: String): List<Account>

    /** Dompet Utama (Main) aktif: parentId kosong, tidak diarsipkan. */
    @Query("SELECT * FROM accounts WHERE userId = :userId AND parentId IS NULL AND isArchived = 0 ORDER BY createdAt ASC")
    fun observeMains(userId: String): Flow<List<Account>>

    /** Branch/Ranting aktif milik [parentId] (dompet induk). */
    @Query("SELECT * FROM accounts WHERE userId = :userId AND parentId = :parentId AND isArchived = 0 ORDER BY createdAt ASC")
    fun observeChildren(userId: String, parentId: String): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: String): Account?

    /** ID semua branch milik dompet induk [parentId]. */
    @Query("SELECT id FROM accounts WHERE parentId = :parentId")
    suspend fun childIds(parentId: String): List<String>

    @Query("SELECT COUNT(*) FROM accounts WHERE userId = :userId")
    suspend fun count(userId: String): Int

    @Query("UPDATE accounts SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: String)

    // ---------------------------------------------------------------------
    // Tombstone penghapusan: agar hapus dompet ikut tersinkron ke server
    // dan tidak ditarik kembali saat pull (multi-perangkat / restart).
    // ---------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTombstone(tombstone: AccountTombstone)

    /**
     * ID semua dompet yang dihapus lokal (jumlahnya kecil & transien, langsung
     * dihapus setelah push sukses). Dipakai untuk push ke server dan untuk
     * memeriksa saat pull agar dompet tidak "hidup lagi".
     */
    @Query("SELECT id FROM deleted_accounts")
    suspend fun allTombstoneIds(): List<String>

    @Query("DELETE FROM deleted_accounts WHERE id IN (:ids)")
    suspend fun deleteTombstones(ids: List<String>)

    /** Akun yang belum terkirim ke Supabase (milik user pemilik). */
    @Query("SELECT * FROM accounts WHERE userId = :userId AND syncState IN ('PENDING', 'FAILED')")
    suspend fun pending(userId: String): List<Account>

    @Query("UPDATE accounts SET syncState = :state WHERE id IN (:ids)")
    suspend fun updateSyncState(ids: List<String>, state: SyncState)

    /** Aduk data lama (userId kosong) ke user yang baru login, sekali pakai. */
    @Query("UPDATE accounts SET userId = :userId WHERE userId = ''")
    suspend fun adoptOrphans(userId: String)
}
