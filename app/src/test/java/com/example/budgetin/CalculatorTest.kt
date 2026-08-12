package com.example.budgetin

import com.example.budgetin.util.Calculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pengujian mesin kalkulator nominal:
 * - contoh skenario: 50 lalu 000 lalu + lalu 15 lalu 000 lalu = -> 65.000
 * - operasi dasar + - * /, prioritas, tombol C & ⌫, ekspresi tidak valid.
 */
class CalculatorTest {

    @Test
    fun thousandShortcutScenario_fromTask() {
        // 50 -> 000 -> + -> 15 -> 000 -> =
        var expr = Calculator.pressDigit("", '5')
        expr = Calculator.pressDigit(expr, '0')
        expr = Calculator.pressThousand(expr)                       // "50000"
        expr = Calculator.pressOperator(expr, '+')                  // "50000+"
        expr = Calculator.pressDigit(expr, '1')
        expr = Calculator.pressDigit(expr, '5')
        expr = Calculator.pressThousand(expr)                       // "50000+15000"
        expr = Calculator.pressEquals(expr)                         // "65000"
        assertEquals("65000", expr)
        assertEquals(65_000.0, Calculator.evaluate(expr) ?: 0.0, 0.001)
    }

    @Test
    fun addition() {
        assertEquals(7_000.0, Calculator.evaluate("5000+2000") ?: 0.0, 0.001)
    }

    @Test
    fun subtraction() {
        assertEquals(-3_000.0, Calculator.evaluate("5000-8000") ?: 0.0, 0.001)
    }

    @Test
    fun multiplication() {
        assertEquals(120_000.0, Calculator.evaluate("12*10000") ?: 0.0, 0.001)
    }

    @Test
    fun divisionRoundsToInteger() {
        // 10000 / 3 = 3333.33... -> dibulatkan 3333
        assertEquals(3_333.0, Calculator.evaluate("10000/3") ?: 0.0, 0.001)
    }

    @Test
    fun operatorPrecedence() {
        // 2 + 3 * 4 = 14 (bukan 20)
        assertEquals(14.0, Calculator.evaluate("2+3*4") ?: 0.0, 0.001)
    }

    @Test
    fun replaceOperatorInsteadOfChaining() {
        var expr = Calculator.pressOperator("500", '+')
        expr = Calculator.pressOperator(expr, '-')
        assertEquals("500-", expr)
    }

    @Test
    fun deleteRemovesLastChar() {
        assertEquals("5000", Calculator.pressDelete("50000"))
    }

    @Test
    fun clearResets() {
        assertEquals("", Calculator.pressClear())
    }

    @Test
    fun pressEqualsChainsComputation() {
        // 10 + 5 = -> "15"; lalu * 2 = -> "30"
        var expr = Calculator.pressEquals("10+5")
        assertEquals("15", expr)
        expr = Calculator.pressOperator(expr, '*')
        expr = Calculator.pressDigit(expr, '2')
        expr = Calculator.pressEquals(expr)
        assertEquals("30", expr)
    }

    @Test
    fun divisionByZeroInvalid() {
        assertNull(Calculator.evaluate("10/0"))
    }

    @Test
    fun emptyExpressionInvalid() {
        assertNull(Calculator.evaluate(""))
    }

    @Test
    fun trailingOperatorInvalid() {
        assertNull(Calculator.evaluate("5000+"))
    }

    @Test
    fun formatExpressionShowsThousandSeparators() {
        assertEquals("50.000 + 15.000", Calculator.formatExpression("50000+15000"))
        assertEquals("1.000.000", Calculator.formatExpression("1000000"))
    }

    @Test
    fun operatorAtStartIgnored() {
        var expr = Calculator.pressOperator("", '+')
        assertEquals("", expr)
    }
}
