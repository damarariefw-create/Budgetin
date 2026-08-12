package com.example.budgetin.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.budgetin.data.model.Debt
import com.example.budgetin.data.model.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    @Insert
    suspend fun insert(debt: Debt): Long

    @Delete
    suspend fun delete(debt: Debt)

    @Query("SELECT * FROM debts WHERE userId = :userId ORDER BY isSettled ASC, createdAt DESC")
    fun observeAll(userId: String): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE userId = :userId AND isSettled = 0 ORDER BY dueDate ASC, createdAt DESC")
    fun observeActive(userId: String): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAll(userId: String): List<Debt>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getById(id: String): Debt?

    @Query("UPDATE debts SET isSettled = :settled, syncState = 'PENDING', updatedAt = :updatedAt WHERE id = :id")
    suspend fun setSettled(id: String, settled: Boolean, updatedAt: Long)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM debts WHERE userId = :userId AND syncState IN ('PENDING', 'FAILED')")
    suspend fun pending(userId: String): List<Debt>

    @Query("UPDATE debts SET syncState = :state WHERE id IN (:ids)")
    suspend fun updateSyncState(ids: List<String>, state: SyncState)

    /** Aduk data lama (userId kosong) ke user yang baru login, sekali pakai. */
    @Query("UPDATE debts SET userId = :userId WHERE userId = ''")
    suspend fun adoptOrphans(userId: String)
}
