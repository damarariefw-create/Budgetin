package com.example.budgetin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = EmeraldDark,
    onSecondary = Color.White,
    background = BackgroundLight,
    onBackground = Color(0xFF181D1A),
    surface = SurfaceLight,
    onSurface = Color(0xFF181D1A),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    error = ExpenseRed,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF5EDBAA),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF005239),
    onPrimaryContainer = Color(0xFFA7F5D2),
    secondary = Color(0xFF66DBAA),
    onSecondary = Color(0xFF003824),
    background = BackgroundDark,
    onBackground = Color(0xFFE2E9E5),
    surface = SurfaceDark,
    onSurface = Color(0xFFE2E9E5),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    error = Color(0xFFFF8A80),
    onError = Color(0xFF690005),
)

@Composable
fun BudgetinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
