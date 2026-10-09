package com.dpa.sportpro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = DarkBackground,
    primaryContainer = CardBackground,
    onPrimaryContainer = TextWhite,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkBackground,
    onSurface = TextWhite,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextMuted,
    error = ErrorRed
)

@Composable
fun SportProAppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
