package com.arroom.characters.ui.theme

import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

// Обратная совместимость: старые файлы ссылаются на эти имена напрямую.
// Новый код берёт значения из Tokens.
val AccentViolet = Tokens.Violet
val AccentCyan = Tokens.Cyan
val Glass = Tokens.Glass
val GlassLight = Tokens.GlassStroke

/** Доступно ли анимировать. Скилл: уважать системный prefers-reduced-motion. */
val LocalReducedMotion = staticCompositionLocalOf { false }

private val Scheme = darkColorScheme(
    primary = Tokens.Violet,
    onPrimary = Tokens.TextPrimary,
    secondary = Tokens.Cyan,
    background = Color.Transparent,
    surface = Tokens.Surface,
    onSurface = Tokens.TextPrimary,
    onSurfaceVariant = Tokens.TextSecondary,
    error = Tokens.Recording,
    outline = Tokens.GlassStroke
)

@Composable
fun ARRoomTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // ANIMATOR_DURATION_SCALE = 0 означает «анимации выключены» в спец-возможностях
    val reducedMotion = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE
        ) == 0f
    }.getOrDefault(false)

    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        MaterialTheme(
            colorScheme = Scheme,
            typography = ARRoomTypography,
            content = content
        )
    }
}
