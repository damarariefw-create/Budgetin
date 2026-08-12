package com.example.budgetin.ui.screens.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.budgetin.data.model.Category
import com.example.budgetin.data.model.TransactionType
import com.example.budgetin.ui.components.BudgetinCard
import com.example.budgetin.ui.components.ColorDot
import com.example.budgetin.ui.components.EmptyState
import com.example.budgetin.ui.components.MonthlyBarChart
import com.example.budgetin.ui.components.MonthlyLineChart
import com.example.budgetin.ui.components.SectionTitle
import com.example.budgetin.ui.theme.ExpenseRed
import com.example.budgetin.ui.theme.IncomeGreen
import com.example.budgetin.ui.theme.ResponsiveContainer
import com.example.budgetin.util.Money

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val monthlyTotals by viewModel.monthlyTotals.collectAsStateWithLifecycle()
    val expenseByCategory by viewModel.expenseByCategory.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()
    val chartType by viewModel.chartType.collectAsStateWithLifecycle()

    ResponsiveContainer {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(
                    text = "Statistik 📊",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = viewModel.monthLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            BudgetinCard {
                SectionTitle(
                    title = "Pemasukan vs Pengeluaran",
                    subtitle = "Tren ${range.label.lowercase()}",
                )
                Spacer(Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatsRange.entries.forEach { option ->
                        val selected = option == range
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { viewModel.setRange(option) }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    ChartTypeToggle(current = chartType, onChange = viewModel::setChartType)
                }

                Spacer(Modifier.height(12.dp))
                if (monthlyTotals.all { it.income == 0.0 && it.expense == 0.0 }) {
                    EmptyState(
                        emoji = "📈",
                        title = "Belum ada data",
                        message = "Data ${range.label.lowercase()} akan muncul di sini.",
                    )
                } else {
                    if (chartType == ChartType.LINE) {
                        MonthlyLineChart(data = monthlyTotals)
                    } else {
                        MonthlyBarChart(data = monthlyTotals)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ChartLegendItem(color = IncomeGreen, label = "Pemasukan")
                        ChartLegendItem(color = ExpenseRed, label = "Pengeluaran")
                    }
                }
            }
        }

        item {
            BudgetinCard {
                SectionTitle(title = "Rincian Bulan Ini", subtitle = viewModel.monthLabel)
                Spacer(Modifier.height(16.dp))
                if (expenseByCategory.isEmpty()) {
                    EmptyState(
                        emoji = "🎉",
                        title = "Tidak ada pengeluaran",
                        message = "Rincian pengeluaran per kategori akan muncul di sini.",
                    )
                } else {
                    val maxTotal = expenseByCategory.maxOf { it.total }
                    expenseByCategory.forEach { categoryTotal ->
                        CategoryDetailRow(
                            name = categoryTotal.category,
                            amount = categoryTotal.total,
                            percent = (categoryTotal.total / maxTotal).toFloat(),
                        )
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }
        }
        }
    }
}

/** Toggle jenis grafik: batang atau garis. */
@Composable
private fun ChartTypeToggle(
    current: ChartType,
    onChange: (ChartType) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ChartType.entries.forEach { option ->
            val selected = option == current
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { onChange(option) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = if (option == ChartType.BAR) "▮▮ Batang" else "📉 Garis",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ChartLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ColorDot(color)
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CategoryDetailRow(name: String, amount: Double, percent: Float) {
    val color = Color(Category.find(name, TransactionType.EXPENSE).color)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = Category.find(name, TransactionType.EXPENSE).emoji, fontSize = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = Money.format(amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { percent.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}
