package com.example.budgetin.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.budgetin.ui.screens.statistics.MonthlyTotal
import com.example.budgetin.ui.theme.ExpenseRed
import com.example.budgetin.ui.theme.IncomeGreen
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineSpec
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.shader.color
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.common.shape.Shape
import com.patrykandpatrick.vico.core.common.shader.DynamicShader
import java.util.Locale

/**
 * Grafik garis tren pemasukan vs pengeluaran menggunakan Vico 2.0.0-alpha.22.
 * - `rememberLineCartesianLayer(lines = listOf(rememberLineSpec(...), ...))`
 * - `lineSeries { series(y) }` (satu panggilan = satu seri)
 */
@Composable
fun MonthlyLineChart(
    data: List<MonthlyTotal>,
    modifier: Modifier = Modifier,
) {
    val incomeSeries = remember(data) { data.map { it.income.toFloat() } }
    val expenseSeries = remember(data) { data.map { it.expense.toFloat() } }
    val labels = remember(data) { data.map { it.label } }

    val lineLayer = rememberLineCartesianLayer(
        lines = listOf(
            rememberLineSpec(
                shader = DynamicShader.color(IncomeGreen),
                thickness = 3.dp,
                point = rememberShapeComponent(shape = Shape.Pill, color = IncomeGreen),
                pointSize = 5.dp,
            ),
            rememberLineSpec(
                shader = DynamicShader.color(ExpenseRed),
                thickness = 3.dp,
                point = rememberShapeComponent(shape = Shape.Pill, color = ExpenseRed),
                pointSize = 5.dp,
            ),
        ),
    )

    val chart = rememberCartesianChart(
        lineLayer,
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
            lineSeries {
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
