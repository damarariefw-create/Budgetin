package com.example.budgetin.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.Shape
import com.example.budgetin.ui.screens.statistics.MonthlyTotal
import com.example.budgetin.ui.theme.ExpenseRed
import com.example.budgetin.ui.theme.IncomeGreen
import java.util.Locale

/**
 * Grafik batang 6 bulan (pemasukan vs pengeluaran) menggunakan Vico 2.0.0-alpha.22.
 *
 * API divalidasi langsung dari source artifact Vico versi tersebut:
 * - `rememberCartesianChart(layer, startAxis = ..., bottomAxis = ...)`
 * - `ColumnCartesianLayer.ColumnProvider.series(rememberLineComponent(...), ...)`
 * - `modelProducer.runTransaction { columnSeries(y) }` (satu panggilan = satu seri)
 */
@Composable
fun MonthlyBarChart(
    data: List<MonthlyTotal>,
    modifier: Modifier = Modifier,
) {
    val incomeSeries = remember(data) { data.map { it.income.toFloat() } }
    val expenseSeries = remember(data) { data.map { it.expense.toFloat() } }
    val labels = remember(data) { data.map { it.label } }

    // Catatan: Defaults.COLUMN_OUTSIDE_SPACING di Vico alpha.22 = 32dp (sangat lebar).
    // Tanpa override spacing, 6 grup kolom akan melebihi lebar plot dan kolom
    // terakhir (satu-satunya yang berisi data) jatuh di luar layar.
    val columnLayer = rememberColumnCartesianLayer(
        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
            rememberLineComponent(
                color = IncomeGreen,
                thickness = 12.dp,
                shape = Shape.rounded(25),
            ),
            rememberLineComponent(
                color = ExpenseRed,
                thickness = 12.dp,
                shape = Shape.rounded(25),
            ),
        ),
        spacing = 8.dp,
        innerSpacing = 6.dp,
    )

    val chart = rememberCartesianChart(
        columnLayer,
        startAxis = rememberStartAxis(
            valueFormatter = CartesianValueFormatter { value, _, _ -> compactAxisLabel(value) },
        ),
        bottomAxis = rememberBottomAxis(
            valueFormatter = CartesianValueFormatter { value, _, _ ->
                val index = value.toInt()
                if (index in labels.indices) labels[index] else ""
            },
        ),
    )

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(incomeSeries, expenseSeries) {
        modelProducer.runTransaction {
            columnSeries {
                series(incomeSeries)
                series(expenseSeries)
            }
        }
    }

    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        modifier = modifier,
    )
}

/** Label sumbu Y ringkas: "5 rb", "1,2 jt". */
private fun compactAxisLabel(value: Float): String {
    val v = value.toDouble()
    val single = { n: Double ->
        if (n % 1.0 == 0.0) n.toLong().toString()
        else String.format(Locale.US, "%.1f", n)
    }
    return when {
        v >= 1_000_000 -> "${single(v / 1_000_000)} jt"
        v >= 1_000 -> "${single(v / 1_000)} rb"
        else -> v.toLong().toString()
    }
}
