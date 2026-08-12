package com.example.budgetin.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetin.data.local.CategoryTotal
import com.example.budgetin.data.model.Transaction
import com.example.budgetin.data.model.TransactionType
import com.example.budgetin.data.repository.TransactionRepository
import com.example.budgetin.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

/** Total pemasukan & pengeluaran satu bulan untuk grafik. */
data class MonthlyTotal(
    val year: Int,
    val month: Int,
    val income: Double,
    val expense: Double,
) {
    val label: String
        get() = DateUtil.monthName(year, month).take(3)
}

/** Jenis grafik tren: batang atau garis. */
enum class ChartType { BAR, LINE }

/** Rentang waktu grafik tren. */
enum class StatsRange(val label: String, val months: Int?) {
    THREE("3 bulan", 3),
    SIX("6 bulan", 6),
    TWELVE("12 bulan", 12),
    ALL("Semua", null),
}

/** Statistik: tren per rentang waktu + rincian pengeluaran kategori bulan ini. */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    repository: TransactionRepository,
) : ViewModel() {

    private val now = Calendar.getInstance()
    private val monthStart = DateUtil.startOfMonth(now).timeInMillis
    private val monthEnd = DateUtil.endOfMonth(now).timeInMillis

    private val _range = MutableStateFlow(StatsRange.SIX)
    val range: StateFlow<StatsRange> = _range.asStateFlow()

    private val _chartType = MutableStateFlow(ChartType.BAR)
    val chartType: StateFlow<ChartType> = _chartType.asStateFlow()

    /** Tren per bulan sesuai [range]. */
    val monthlyTotals: StateFlow<List<MonthlyTotal>> =
        _range
            .flatMapLatest { selected ->
                repository.observeSince(0L)
                    .map { transactions ->
                        val from = if (selected.months == null) {
                            // "Semua": mulai dari bulan transaksi tertua.
                            transactions.minOfOrNull { it.timestamp } ?: System.currentTimeMillis()
                        } else {
                            (Calendar.getInstance().clone() as Calendar)
                                .apply { add(Calendar.MONTH, -(selected.months - 1)) }
                                .let { DateUtil.startOfMonth(it).timeInMillis }
                        }
                        groupByMonth(transactions, from)
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val expenseByCategory: StateFlow<List<CategoryTotal>> =
        repository.observeExpenseByCategory(monthStart, monthEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthLabel: String = DateUtil.monthLabel(now)

    fun setRange(value: StatsRange) { _range.value = value }
    fun setChartType(value: ChartType) { _chartType.value = value }

    private fun groupByMonth(transactions: List<Transaction>, from: Long): List<MonthlyTotal> {
        // Bulan target: dari `from` sampai bulan berjalan.
        val months = mutableListOf<MonthlyTotal>()
        var cursor = Calendar.getInstance().apply { timeInMillis = from }
        val end = Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis() }
        while (!cursor.after(end)) {
            months.add(
                MonthlyTotal(
                    year = cursor.get(Calendar.YEAR),
                    month = cursor.get(Calendar.MONTH),
                    income = 0.0,
                    expense = 0.0,
                )
            )
            cursor.add(Calendar.MONTH, 1)
        }

        // Indeks bulan untuk pencarian O(1). Transaksi di luar rentang diabaikan.
        val indexByKey = months
            .mapIndexed { index, total -> (total.year to total.month) to index }
            .toMap()
        for (t in transactions) {
            val c = Calendar.getInstance().apply { timeInMillis = t.timestamp }
            val idx = indexByKey[c.get(Calendar.YEAR) to c.get(Calendar.MONTH)] ?: continue
            val cur = months[idx]
            months[idx] = when (t.type) {
                TransactionType.INCOME -> cur.copy(income = cur.income + t.amount - t.adminFee)
                TransactionType.EXPENSE -> cur.copy(expense = cur.expense + t.amount + t.adminFee)
                TransactionType.TRANSFER -> cur
            }
        }
        return months
    }
}
