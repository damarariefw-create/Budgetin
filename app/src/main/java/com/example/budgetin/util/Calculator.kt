package com.example.budgetin.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Mesin kalkulator untuk input nominal transaksi (tanpa dependensi Android,
 * agar mudah diuji). Memegang ekspresi mentah (mis. "50000+15000") dan
 * mendukung digit, tombol cepat `000`, operator + - * /, =, C (hapus semua),
 * dan ⌫ (hapus satu karakter).
 *
 * Nilai akhir selalu bilangan bulat (format Rupiah tanpa desimal).
 */
object Calculator {

    const val MAX_LENGTH = 18

    private val operators = setOf('+', '-', '*', '/')

    /** Apakah [c] adalah operator aritmatika. */
    fun isOperator(c: Char): Boolean = c in operators

    /** Tambah satu digit angka ke ekspresi. */
    fun pressDigit(expr: String, digit: Char): String {
        if (!digit.isDigit() || expr.length >= MAX_LENGTH) return expr
        return expr + digit
    }

    /** Tombol cepat `000`: tambahkan tiga nol (memudahkan input ribuan). */
    fun pressThousand(expr: String): String {
        if (expr.length + 3 > MAX_LENGTH) return expr
        return expr + "000"
    }

    /** Tekan operator; bila karakter terakhir operator, ganti operator tsb. */
    fun pressOperator(expr: String, op: Char): String {
        if (op !in operators) return expr
        if (expr.isEmpty()) return expr
        val last = expr.last()
        return if (isOperator(last)) expr.dropLast(1) + op else expr + op
    }

    /** Hapus satu karakter terakhir. */
    fun pressDelete(expr: String): String =
        if (expr.isEmpty()) expr else expr.dropLast(1)

    /** Bersihkan seluruh ekspresi. */
    fun pressClear(): String = ""

    /** Tekan `=`: evaluasi lalu ganti ekspresi dengan hasil (atau biarkan bila tidak valid). */
    fun pressEquals(expr: String): String {
        val result = evaluate(expr) ?: return expr
        val rounded = BigDecimal(result).setScale(0, RoundingMode.HALF_UP).toLong()
        return rounded.toString()
    }

    /** Evaluasi ekspresi aritmatika dasar; null bila ekspresi tidak valid / bagi nol. */
    fun evaluate(expr: String): Double? {
        val tokens = tokenize(expr) ?: return null
        if (tokens.isEmpty()) return null

        val values = mutableListOf<BigDecimal>()
        val ops = mutableListOf<Char>()
        val precedence = mapOf('+' to 1, '-' to 1, '*' to 2, '/' to 2)

        values.add(tokens[0].number ?: return null)
        var index = 1
        while (index < tokens.size) {
            val op = tokens[index].operator ?: return null
            val num = tokens[index + 1].number ?: return null
            while (ops.isNotEmpty() && precedence[ops.last()]!! >= precedence[op]!!) {
                if (!applyTop(values, ops)) return null
            }
            ops.add(op)
            values.add(num)
            index += 2
        }
        while (ops.isNotEmpty()) {
            if (!applyTop(values, ops)) return null
        }
        val result = values.last().setScale(0, RoundingMode.HALF_UP)
        return result.toDouble()
    }

    /** Satu token ekspresi: angka atau operator. */
    private data class Token(val number: BigDecimal? = null, val operator: Char? = null)

    /** Parsing ekspresi -> daftar token bergantian [angka, operator, angka, ...]. */
    private fun tokenize(expr: String): List<Token>? {
        if (expr.isBlank()) return null
        val pattern = Regex("(\\d+)|([+\\-*/])")
        val tokens = mutableListOf<String>()
        var pos = 0
        for (match in pattern.findAll(expr)) {
            if (match.range.first != pos) return null // karakter tak dikenal
            tokens.add(match.value)
            pos = match.range.last + 1
        }
        if (pos != expr.length) return null
        if (tokens.isEmpty() || tokens.size % 2 == 0) return null

        val result = mutableListOf<Token>()
        tokens.forEachIndexed { i, token ->
            when {
                i % 2 == 0 -> {
                    val num = token.toLongOrNull() ?: return null
                    result.add(Token(number = BigDecimal.valueOf(num)))
                }
                else -> {
                    if (token.length != 1 || token[0] !in operators) return null
                    result.add(Token(operator = token[0]))
                }
            }
        }
        return result
    }

    private fun applyTop(values: MutableList<BigDecimal>, ops: MutableList<Char>): Boolean {
        if (values.size < 2 || ops.isEmpty()) return false
        val right = values.removeAt(values.lastIndex)
        val left = values.removeAt(values.lastIndex)
        val op = ops.removeAt(ops.lastIndex)
        val result = when (op) {
            '+' -> left.add(right)
            '-' -> left.subtract(right)
            '*' -> left.multiply(right)
            '/' -> if (right.signum() == 0) return false else left.divide(right, 10, RoundingMode.HALF_UP)
            else -> return false
        }
        values.add(result)
        return true
    }

    /** Format ekspresi untuk tampilan: angka dipisah ribuan, operator diberi spasi. */
    fun formatExpression(expr: String): String {
        if (expr.isBlank()) return ""
        val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
        val sb = StringBuilder()
        val current = StringBuilder()
        for (c in expr) {
            if (isOperator(c)) {
                sb.append(formatNumberToken(current.toString(), formatter))
                sb.append(' ').append(c).append(' ')
                current.setLength(0)
            } else {
                current.append(c)
            }
        }
        sb.append(formatNumberToken(current.toString(), formatter))
        return sb.toString()
    }

    private fun formatNumberToken(token: String, formatter: NumberFormat): String {
        if (token.isEmpty()) return ""
        return formatter.format(token.toLongOrNull() ?: 0L)
    }
}
