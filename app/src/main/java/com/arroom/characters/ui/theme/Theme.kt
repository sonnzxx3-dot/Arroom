package com.arroom.characters.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Тёмная «стеклянная» палитра: поверх видео с камеры любой светлый UI слепит,
// поэтому весь интерфейс — полупрозрачное тёмное стекло с одним акцентом.
val AccentViolet = Color(0xFF8B6CFF)
val AccentCyan = Color(0xFF3DDCFF)
val Glass = Color(0xCC12121A)
val GlassLight = Color(0x33FFFFFF)

private val Scheme = darkColorScheme(
    primary = AccentViolet,
    onPrimary = Color.White,
    secondary = AccentCyan,
    background = Color.Transparent,
    surface = Glass,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB9B9C6)
)

@Composable
fun ARRoomTheme(content: @Composable () -> Unit) {
    isSystemInDarkTheme() // AR-интерфейс всегда тёмный
    MaterialTheme(colorScheme = Scheme, content = content)
}
