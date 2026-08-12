package com.example.budgetin.data.local

import androidx.room.TypeConverter
import com.example.budgetin.data.model.AccountType
import com.example.budgetin.data.model.DebtType
import com.example.budgetin.data.model.RecurringPeriod
import com.example.budgetin.data.model.SyncState
import com.example.budgetin.data.model.TransactionStatus
import com.example.budgetin.data.model.TransactionType

/** Converter agar enum dapat disimpan Room sebagai String. */
class Converters {
    @TypeConverter
    fun fromType(type: TransactionType): String = type.name

    @TypeConverter
    fun toType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromStatus(status: TransactionStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): TransactionStatus = TransactionStatus.valueOf(value)

    @TypeConverter
    fun fromSyncState(state: SyncState): String = state.name

    @TypeConverter
    fun toSyncState(value: String): SyncState = SyncState.valueOf(value)

    @TypeConverter
    fun fromRecurringPeriod(period: RecurringPeriod?): String? = period?.name

    @TypeConverter
    fun toRecurringPeriod(value: String?): RecurringPeriod? =
        value?.let { RecurringPeriod.valueOf(it) }

    @TypeConverter
    fun fromAccountType(type: AccountType): String = type.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    @TypeConverter
    fun fromDebtType(type: DebtType): String = type.name

    @TypeConverter
    fun toDebtType(value: String): DebtType = DebtType.valueOf(value)
}
