package com.example.budgetin.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.budgetin.data.model.Account
import com.example.budgetin.data.model.AccountTombstone
import com.example.budgetin.data.model.Category
import com.example.budgetin.data.model.Debt
import com.example.budgetin.data.model.Transaction
import com.example.budgetin.data.model.TransactionTombstone

@Database(
    entities = [
        Transaction::class,
        Account::class,
        Category::class,
        Debt::class,
        TransactionTombstone::class,
        AccountTombstone::class,
    ],
    version = 12,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun debtDao(): DebtDao
}
