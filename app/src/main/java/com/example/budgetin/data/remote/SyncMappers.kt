package com.example.budgetin.data.remote

import com.example.budgetin.data.model.Account
import com.example.budgetin.data.model.AccountType
import com.example.budgetin.data.model.Category
import com.example.budgetin.data.model.Debt
import com.example.budgetin.data.model.DebtType
import com.example.budgetin.data.model.Transaction
import com.example.budgetin.data.model.TransactionType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ---------------------------------------------------------------------------
// Helper konversi nilai lokal <-> kolom Supabase
// ---------------------------------------------------------------------------

internal fun Long.toIsoDate(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(this))

internal fun Long.toIsoTime(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(this))

/** ARGB Long -> "#RRGGBB" (format kolom color di Supabase). */
internal fun Long.colorToHex(): String = String.format("#%06X", this and 0xFFFFFF)

/** "#RRGGBB" -> ARGB Long. */
internal fun hexToColor(hex: String): Long =
    hex.removePrefix("#").toLongOrNull(16)?.or(0xFF000000.toLong()) ?: 0xFF6B7280

// ---------------------------------------------------------------------------
// Payload insert (JsonObject punya serializer bawaan, tanpa plugin @Serializable)
// ---------------------------------------------------------------------------

internal fun Account.toRemoteJson(userId: String) = buildJsonObject {
    put("id", id)
    put("user_id", userId)
    put("name", name)
    put("type", type.name.lowercase())
    put("balance", balance)
    put("icon", icon)
    put("color", color.colorToHex())
    put("count_in_total", includeInTotal)
    put("is_archived", isArchived)
    if (parentId != null) put("parent_id", parentId) else put("parent_id", JsonNull)
    put("created_at", createdAt.toIsoTime())
    put("updated_at", updatedAt.toIsoTime())
}

internal fun Transaction.toRemoteJson(userId: String) = buildJsonObject {
    put("id", id)
    put("user_id", userId)
    put("type", type.name.lowercase())
    put("amount", amount)
    put("admin_fee", adminFee)
    if (categoryId != null) put("category_id", categoryId) else put("category_id", JsonNull)
    put("account_id", accountId)
    if (transferToAccountId != null) {
        put("transfer_to_account_id", transferToAccountId)
    } else {
        put("transfer_to_account_id", JsonNull)
    }
    put("note", note)
    put("status", status.name.lowercase())
    put("is_completed", isCompleted)
    put("is_recurring", isRecurring)
    put("transaction_date", timestamp.toIsoDate())
    put("sync_state", "synced")
    put("created_at", createdAt.toIsoTime())
    put("updated_at", updatedAt.toIsoTime())
}

internal fun Debt.toRemoteJson(userId: String) = buildJsonObject {
    put("id", id)
    put("user_id", userId)
    put("counterpart_name", counterpartName)
    put("type", type.name.lowercase())
    put("amount", amount)
    put("note", note)
    if (dueDate != null) put("due_date", dueDate.toIsoDate()) else put("due_date", JsonNull)
    put("is_settled", isSettled)
    if (accountId != null) put("account_id", accountId) else put("account_id", JsonNull)
    if (settledAccountId != null) {
        put("settled_account_id", settledAccountId)
    } else {
        put("settled_account_id", JsonNull)
    }
    put("sync_state", "synced")
    put("created_at", createdAt.toIsoTime())
    put("updated_at", updatedAt.toIsoTime())
}

/** Payload kategori custom (milik user, bukan default). */
internal fun Category.toRemoteJson(userId: String) = buildJsonObject {
    put("id", id)
    put("user_id", userId)
    put("name", name)
    put("type", type.name.lowercase())
    put("emoji", emoji)
    put("color", color.colorToHex())
    put("is_default", false)
}

// ---------------------------------------------------------------------------
// Hasil SELECT (parsing manual karena tanpa plugin @Serializable)
// ---------------------------------------------------------------------------

data class RemoteCategory(
    val id: String,
    val name: String,
    val type: TransactionType,
    val emoji: String,
    val color: Long,
    val isDefault: Boolean,
)

internal fun parseRemoteCategories(json: String): List<RemoteCategory> {
    val root = Json.parseToJsonElement(json)
    return root.jsonArray.mapNotNull { element ->
        val obj = element.jsonObject
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val typeRaw = obj["type"]?.jsonPrimitive?.contentOrNull ?: "expense"
        RemoteCategory(
            id = id,
            name = name,
            type = if (typeRaw == "income") TransactionType.INCOME else TransactionType.EXPENSE,
            emoji = obj["emoji"]?.jsonPrimitive?.contentOrNull ?: "📦",
            color = hexToColor(obj["color"]?.jsonPrimitive?.contentOrNull ?: "#6B7280"),
            isDefault = obj["is_default"]?.jsonPrimitive?.contentOrNull != "false",
        )
    }
}

// ---------------------------------------------------------------------------
// Debts (hutang-piutang) hasil SELECT
// ---------------------------------------------------------------------------

data class RemoteDebt(
    val id: String,
    val counterpartName: String,
    val type: DebtType,
    val amount: Double,
    val note: String,
    val dueDate: Long?,
    val isSettled: Boolean,
    val accountId: String?,
    val settledAccountId: String?,
    val updatedAt: Long = 0L,
)

/** "yyyy-MM-dd" (UTC) -> epoch millis (UTC tengah malam). */
internal fun isoDateToMillis(date: String): Long =
    SimpleDateFormat("yyyy-MM-dd", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .parse(date)?.time ?: 0L

internal fun parseRemoteDebts(json: String): List<RemoteDebt> {
    val root = Json.parseToJsonElement(json)
    return root.jsonArray.mapNotNull { element ->
        val obj = element.jsonObject
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val name = obj["counterpart_name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val typeRaw = obj["type"]?.jsonPrimitive?.contentOrNull ?: "owe"
        val amount = obj["amount"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: return@mapNotNull null
        RemoteDebt(
            id = id,
            counterpartName = name,
            type = if (typeRaw == "owed") DebtType.OWED else DebtType.OWE,
            amount = amount,
            note = obj["note"]?.jsonPrimitive?.contentOrNull ?: "",
            dueDate = obj["due_date"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "null" }
                ?.let { isoDateToMillis(it) },
            isSettled = obj["is_settled"]?.jsonPrimitive?.contentOrNull == "true",
            accountId = obj["account_id"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "null" },
            settledAccountId = obj["settled_account_id"]?.jsonPrimitive?.contentOrNull
                ?.takeIf { it != "null" },
            updatedAt = obj["updated_at"]?.jsonPrimitive?.contentOrNull?.let { isoTimeToMillis(it) }
                ?: 0L,
        )
    }
}

// ---------------------------------------------------------------------------
// Accounts (dompet) hasil SELECT
// ---------------------------------------------------------------------------

data class RemoteAccount(
    val id: String,
    val name: String,
    val type: AccountType,
    val balance: Double,
    val icon: String,
    val color: Long,
    val countInTotal: Boolean,
    val isArchived: Boolean,
    val parentId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    /** sync_state server; "deleted" = dompet dihapus di perangkat lain. */
    val syncState: String = "synced",
) {
    /** true bila baris ini penanda hapus (soft-delete) dari perangkat lain. */
    val isDeletedMarker: Boolean get() = syncState == "deleted"
}

/** "2026-08-11T12:34:56.789Z" atau "...+00:00" (UTC) -> epoch millis.
 *  Supabase kadang mengirim offset "+00:00"; fraksi milidetik bisa 0-9 digit. */
internal fun isoTimeToMillis(time: String): Long {
    val cleaned = time.trim().replace(Regex("\\.\\d+"), "")
    try {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US)
        return fmt.parse(cleaned)?.time ?: System.currentTimeMillis()
    } catch (_: Exception) {
        try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            return fmt.parse(cleaned)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            return System.currentTimeMillis()
        }
    }
}

internal fun parseRemoteAccounts(json: String): List<RemoteAccount> {
    val root = Json.parseToJsonElement(json)
    return root.jsonArray.mapNotNull { element ->
        val obj = element.jsonObject
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val typeRaw = obj["type"]?.jsonPrimitive?.contentOrNull ?: "cash"
        val balance = obj["balance"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0
        RemoteAccount(
            id = id,
            name = name,
            type = when (typeRaw) {
                "bank" -> AccountType.BANK
                "ewallet" -> AccountType.EWALLET
                "debt" -> AccountType.DEBT
                else -> AccountType.CASH
            },
            balance = balance,
            icon = obj["icon"]?.jsonPrimitive?.contentOrNull ?: "💳",
            color = hexToColor(obj["color"]?.jsonPrimitive?.contentOrNull ?: "#10B981"),
            countInTotal = obj["count_in_total"]?.jsonPrimitive?.contentOrNull != "false",
            isArchived = obj["is_archived"]?.jsonPrimitive?.contentOrNull == "true",
            parentId = obj["parent_id"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "null" },
            createdAt = obj["created_at"]?.jsonPrimitive?.contentOrNull?.let { isoTimeToMillis(it) }
                ?: System.currentTimeMillis(),
            updatedAt = obj["updated_at"]?.jsonPrimitive?.contentOrNull?.let { isoTimeToMillis(it) }
                ?: System.currentTimeMillis(),
            syncState = obj["sync_state"]?.jsonPrimitive?.contentOrNull ?: "synced",
        )
    }
}

// ---------------------------------------------------------------------------
// Transactions hasil SELECT (dipakai pull / laporan)
// ---------------------------------------------------------------------------

data class RemoteTransaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val adminFee: Double,
    val categoryId: String?,
    /** Nama kategori (denormalisasi) yang diisi dari tabel kategori lokal saat pull. */
    val categoryName: String = "",
    val accountId: String,
    val transferToAccountId: String?,
    val note: String,
    val isCompleted: Boolean,
    val transactionDate: Long,
    val createdAt: Long,
    val updatedAt: Long,
    /** status server; "deleted" = transaksi dihapus di perangkat lain. */
    val status: String = "confirmed",
) {
    /** true bila baris ini penanda hapus (soft-delete) dari perangkat lain. */
    val isDeletedMarker: Boolean get() = status == "deleted"
}

internal fun parseRemoteTransactions(json: String): List<RemoteTransaction> {
    val root = Json.parseToJsonElement(json)
    return root.jsonArray.mapNotNull { element ->
        val obj = element.jsonObject
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val typeRaw = obj["type"]?.jsonPrimitive?.contentOrNull ?: "expense"
        val amount = obj["amount"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: return@mapNotNull null
        val accountId = obj["account_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        RemoteTransaction(
            id = id,
            type = when (typeRaw) {
                "income" -> TransactionType.INCOME
                "transfer" -> TransactionType.TRANSFER
                else -> TransactionType.EXPENSE
            },
            amount = amount,
            adminFee = obj["admin_fee"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0,
            categoryId = obj["category_id"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "null" },
            accountId = accountId,
            transferToAccountId = obj["transfer_to_account_id"]?.jsonPrimitive?.contentOrNull
                ?.takeIf { it != "null" },
            note = obj["note"]?.jsonPrimitive?.contentOrNull ?: "",
            isCompleted = obj["is_completed"]?.jsonPrimitive?.contentOrNull != "false",
            transactionDate = obj["transaction_date"]?.jsonPrimitive?.contentOrNull
                ?.takeIf { it != "null" }?.let { isoDateToMillis(it) } ?: 0L,
            createdAt = obj["created_at"]?.jsonPrimitive?.contentOrNull?.let { isoTimeToMillis(it) }
                ?: System.currentTimeMillis(),
            updatedAt = obj["updated_at"]?.jsonPrimitive?.contentOrNull?.let { isoTimeToMillis(it) }
                ?: System.currentTimeMillis(),
            status = obj["status"]?.jsonPrimitive?.contentOrNull ?: "confirmed",
        )
    }
}
