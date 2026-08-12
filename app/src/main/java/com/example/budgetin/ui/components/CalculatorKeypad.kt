package com.example.budgetin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgetin.ui.theme.amountFontSize
import com.example.budgetin.util.Calculator

/**
 * Keypad kalkulator untuk input nominal transaksi.
 * - [value] ekspresi mentah (contoh: "50000+15000").
 * - [onValueChange] menerima ekspresi baru setelah setiap penekanan tombol.
 *
 * Tombol: 0-9, `000`, + - * /, `=` (evaluasi), C (clear), ⌫ (delete).
 */
@Composable
fun CalculatorKeypad(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KeypadRow(listOf(
            KeySpec("C", KeyStyle.FUNCTION) { onValueChange(Calculator.pressClear()) },
            KeySpec("⌫", KeyStyle.FUNCTION) { onValueChange(Calculator.pressDelete(value)) },
            KeySpec("000", KeyStyle.ACCENT) { onValueChange(Calculator.pressThousand(value)) },
            KeySpec("÷", KeyStyle.OPERATOR) { onValueChange(Calculator.pressOperator(value, '/')) },
        ))

        KeypadRow(listOf(
            KeySpec("7") { onValueChange(Calculator.pressDigit(value, '7')) },
            KeySpec("8") { onValueChange(Calculator.pressDigit(value, '8')) },
            KeySpec("9") { onValueChange(Calculator.pressDigit(value, '9')) },
            KeySpec("×", KeyStyle.OPERATOR) { onValueChange(Calculator.pressOperator(value, '*')) },
        ))

        KeypadRow(listOf(
            KeySpec("4") { onValueChange(Calculator.pressDigit(value, '4')) },
            KeySpec("5") { onValueChange(Calculator.pressDigit(value, '5')) },
            KeySpec("6") { onValueChange(Calculator.pressDigit(value, '6')) },
            KeySpec("−", KeyStyle.OPERATOR) { onValueChange(Calculator.pressOperator(value, '-')) },
        ))

        KeypadRow(listOf(
            KeySpec("1") { onValueChange(Calculator.pressDigit(value, '1')) },
            KeySpec("2") { onValueChange(Calculator.pressDigit(value, '2')) },
            KeySpec("3") { onValueChange(Calculator.pressDigit(value, '3')) },
            KeySpec("+", KeyStyle.OPERATOR) { onValueChange(Calculator.pressOperator(value, '+')) },
        ))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeypadKey(
                spec = KeySpec("0") { onValueChange(Calculator.pressDigit(value, '0')) },
                modifier = Modifier.weight(1f),
            )
            KeypadKey(
                spec = KeySpec("=", KeyStyle.EQUALS) { onValueChange(Calculator.pressEquals(value)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private enum class KeyStyle {
    NUMBER,
    FUNCTION,
    OPERATOR,
    ACCENT,
    EQUALS,
}

private data class KeySpec(
    val label: String,
    val style: KeyStyle = KeyStyle.NUMBER,
    val onClick: () -> Unit,
)

@Composable
private fun KeypadRow(keys: List<KeySpec>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { key ->
            KeypadKey(spec = key, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun KeypadKey(spec: KeySpec, modifier: Modifier = Modifier) {
    val background: Color
    val content: Color
    when (spec.style) {
        KeyStyle.FUNCTION -> {
            background = MaterialTheme.colorScheme.surfaceVariant
            content = MaterialTheme.colorScheme.onSurfaceVariant
        }
        KeyStyle.OPERATOR -> {
            background = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            content = MaterialTheme.colorScheme.primary
        }
        KeyStyle.ACCENT -> {
            background = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            content = MaterialTheme.colorScheme.primary
        }
        KeyStyle.EQUALS -> {
            background = MaterialTheme.colorScheme.primary
            content = MaterialTheme.colorScheme.onPrimary
        }
        else -> {
            background = MaterialTheme.colorScheme.surfaceVariant
            content = MaterialTheme.colorScheme.onSurface
        }
    }

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = spec.onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = spec.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (spec.style == KeyStyle.NUMBER) FontWeight.SemiBold else FontWeight.Bold,
            color = content,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** Penampil nominal besar + keypad kalkulator (pengganti AmountField). */
@Composable
fun CalculatorAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val displayText = "Rp ${Calculator.formatExpression(value).ifBlank { "0" }}"
        Text(
            text = displayText,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = amountFontSize(displayText, MaterialTheme.typography.headlineMedium.fontSize)
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        CalculatorKeypad(value = value, onValueChange = onValueChange)
    }
}
